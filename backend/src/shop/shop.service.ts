import { BadRequestException, ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { BoostType, CurrencyType, LedgerReason, Prisma, PrismaClient, ShopCategory } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';
import { CurrencyService } from '../currency/currency.service';
import { OwnedEggsService } from '../owned-eggs/owned-eggs.service';
import { InboxService } from '../inbox/inbox.service';

type Tx = Prisma.TransactionClient | PrismaClient;

@Injectable()
export class ShopService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly currencyService: CurrencyService,
    private readonly ownedEggsService: OwnedEggsService,
    private readonly inboxService: InboxService,
  ) {}

  findAll(category?: ShopCategory) {
    return this.prisma.shopItem.findMany({
      where: { isActive: true, category },
      include: { eggType: true },
      orderBy: { priceCoin: 'asc' },
    });
  }

  getInventory(userId: string) {
    return this.prisma.inventoryItem.findMany({
      where: { userId },
      include: { shopItem: true },
      orderBy: { acquiredAt: 'desc' },
    });
  }

  /**
   * Mua vật phẩm. Trứng (category EGG): trả bằng Xu Lá HOẶC Giờ tích luỹ (giá lấy từ
   * EggType.priceCoin/priceHours, admin cấu hình riêng từng loại) — mỗi lần mua tạo 1
   * `OwnedEgg` mới (ấp riêng, không cộng dồn số lượng). Bình/nhạc: như cũ, trả Xu Lá,
   * chỉ mua được 1 lần.
   */
  async purchase(userId: string, shopItemId: string, clientEventId?: string, payWith?: CurrencyType) {
    const shopItem = await this.prisma.shopItem.findUnique({
      where: { id: shopItemId },
      include: { eggType: true },
    });
    if (!shopItem || !shopItem.isActive) {
      throw new NotFoundException('Không tìm thấy vật phẩm này');
    }
    if (!shopItem.purchasable) {
      // T-116 — trứng Truyền thuyết: hiện trong Cửa hàng để biết sự tồn tại nhưng chỉ Admin phát
      // qua AdminJS (tạo thẳng `OwnedEgg`), chặn cả phía server phòng client bỏ qua UI disable.
      throw new ForbiddenException('Vật phẩm này không bán — chỉ nhận được qua phần thưởng từ Admin');
    }

    if (shopItem.category === ShopCategory.EGG) {
      if (!shopItem.eggType) {
        throw new NotFoundException('Vật phẩm trứng này chưa gắn với loại trứng nào');
      }
      const currency = payWith ?? CurrencyType.COIN;
      const price = currency === CurrencyType.FOCUS_MINUTE ? shopItem.eggType.priceHours : shopItem.eggType.priceCoin;

      return this.prisma.$transaction(async (tx) => {
        await this.currencyService.spend(userId, price, LedgerReason.PURCHASE, {
          currency,
          refShopItemId: shopItemId,
          clientEventId,
          tx,
        });
        const ownedEgg = await this.ownedEggsService.create(userId, shopItem.eggType!.id, tx);
        await this.inboxService.create(
          userId,
          'EGG_RECEIVED',
          'Bạn vừa nhận được trứng mới!',
          `Bạn đã mua thành công ${shopItem.eggType!.name} — bắt đầu ấp ngay trong phiên tập trung tiếp theo.`,
          { eggTypeId: shopItem.eggType!.id, ownedEggId: ownedEgg.id },
          tx,
        );
        return ownedEgg;
      });
    }

    if (shopItem.category === ShopCategory.BOOST) {
      // Vật phẩm bổ trợ — mua nhiều lần được, cộng dồn quantity (khác JAR_SKIN/MUSIC chỉ mua 1 lần).
      return this.prisma.$transaction(async (tx) => {
        await this.currencyService.spend(userId, shopItem.priceCoin, LedgerReason.PURCHASE, {
          currency: CurrencyType.COIN,
          refShopItemId: shopItemId,
          clientEventId,
          tx,
        });
        return this.grantItem(userId, shopItemId, 1, tx);
      });
    }

    const existing = await this.prisma.inventoryItem.findUnique({
      where: { userId_shopItemId: { userId, shopItemId } },
    });
    if (existing) {
      throw new ForbiddenException('Bạn đã sở hữu vật phẩm này rồi');
    }

    return this.prisma.$transaction(async (tx) => {
      await this.currencyService.spend(userId, shopItem.priceCoin, LedgerReason.PURCHASE, {
        currency: CurrencyType.COIN,
        refShopItemId: shopItemId,
        clientEventId,
        tx,
      });
      return tx.inventoryItem.create({ data: { userId, shopItemId, quantity: 1 } });
    });
  }

  /**
   * Cấp thẳng 1 vật phẩm ShopItem vào kho đồ user (cộng dồn quantity nếu đã có) — dùng chung bởi
   * purchase() (mua BOOST), GiftPage handler (Phát quà kind SHOP_ITEM) và StreaksService (quà
   * streak kind SHOP_ITEM). Không kiểm tra category/purchasable — nơi gọi tự chịu trách nhiệm.
   */
  async grantItem(userId: string, shopItemId: string, quantity: number, tx: Tx) {
    const existing = await tx.inventoryItem.findUnique({
      where: { userId_shopItemId: { userId, shopItemId } },
    });
    if (existing) {
      return tx.inventoryItem.update({ where: { id: existing.id }, data: { quantity: { increment: quantity } } });
    }
    return tx.inventoryItem.create({ data: { userId, shopItemId, quantity } });
  }

  /**
   * Dùng 1 vật phẩm bổ trợ (BOOST) trong kho đồ — trừ 1 quantity (xoá dòng nếu về 0), rồi áp
   * dụng hiệu ứng: FOCUS_MINUTES cộng thẳng Giờ tích luỹ, HATCH_MINUTES cộng phút ấp cho
   * `ownedEggId` người chơi chọn (bắt buộc, tái dùng OwnedEggsService.incubate — tự roll loài +
   * tạo thư EGG_HATCHED nếu đủ ngưỡng, y hệt ấp trứng qua 1 phiên tập trung thật).
   */
  async useItem(userId: string, inventoryItemId: string, ownedEggId?: string) {
    const item = await this.prisma.inventoryItem.findUnique({
      where: { id: inventoryItemId },
      include: { shopItem: true },
    });
    if (!item || item.userId !== userId) {
      throw new NotFoundException('Không tìm thấy vật phẩm trong kho đồ của bạn');
    }
    if (item.shopItem.category !== ShopCategory.BOOST || item.quantity < 1) {
      throw new ForbiddenException('Vật phẩm này không dùng theo cách này');
    }
    if (item.shopItem.boostType === BoostType.HATCH_MINUTES && !ownedEggId) {
      throw new BadRequestException('Chọn 1 trứng đang ấp để dùng vật phẩm này');
    }
    if (item.shopItem.boostType === BoostType.HATCH_MINUTES) {
      // Xác nhận trứng thuộc user này và đang ấp trước khi vào transaction — báo lỗi rõ ràng.
      await this.ownedEggsService.getIncubatingOrThrow(userId, ownedEggId!);
    }

    const boostAmount = item.shopItem.boostAmount ?? 0;

    return this.prisma.$transaction(async (tx) => {
      const remaining = item.quantity - 1;
      if (remaining <= 0) {
        await tx.inventoryItem.delete({ where: { id: item.id } });
      } else {
        await tx.inventoryItem.update({ where: { id: item.id }, data: { quantity: remaining } });
      }

      if (item.shopItem.boostType === BoostType.FOCUS_MINUTES) {
        await this.currencyService.earn(userId, boostAmount, LedgerReason.ITEM_USE, {
          currency: CurrencyType.FOCUS_MINUTE,
          refShopItemId: item.shopItemId,
          tx,
        });
        return { kind: 'FOCUS_MINUTES' as const, amount: boostAmount };
      }

      const result = await this.ownedEggsService.incubate(ownedEggId!, boostAmount, tx);
      return {
        kind: 'HATCH_MINUTES' as const,
        amount: boostAmount,
        ownedEgg: result.ownedEgg,
        resultSpecies: result.resultSpecies,
        hatched: result.hatched,
      };
    });
  }

  /** Bật/tắt trang bị — tự tắt các item cùng danh mục khi bật cái mới (chỉ 1 bình/1 nhạc đang dùng). */
  async toggleEquip(userId: string, inventoryItemId: string) {
    const item = await this.prisma.inventoryItem.findUnique({
      where: { id: inventoryItemId },
      include: { shopItem: true },
    });
    if (!item || item.userId !== userId) {
      throw new NotFoundException('Không tìm thấy vật phẩm trong kho đồ của bạn');
    }

    const nextEquipped = !item.equipped;
    if (nextEquipped) {
      await this.prisma.inventoryItem.updateMany({
        where: { userId, shopItem: { category: item.shopItem.category }, id: { not: item.id } },
        data: { equipped: false },
      });
    }
    return this.prisma.inventoryItem.update({
      where: { id: inventoryItemId },
      data: { equipped: nextEquipped },
    });
  }
}
