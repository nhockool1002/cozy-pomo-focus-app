import { ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { CurrencyType, LedgerReason, ShopCategory } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';
import { CurrencyService } from '../currency/currency.service';
import { OwnedEggsService } from '../owned-eggs/owned-eggs.service';

@Injectable()
export class ShopService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly currencyService: CurrencyService,
    private readonly ownedEggsService: OwnedEggsService,
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
        return this.ownedEggsService.create(userId, shopItem.eggType!.id, tx);
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
