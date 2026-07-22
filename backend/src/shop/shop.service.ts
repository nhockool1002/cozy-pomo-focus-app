import { ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { LedgerReason, ShopCategory } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';
import { CurrencyService } from '../currency/currency.service';

@Injectable()
export class ShopService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly currencyService: CurrencyService,
  ) {}

  findAll(category?: ShopCategory) {
    return this.prisma.shopItem.findMany({
      where: { isActive: true, category },
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

  /** Mua vật phẩm — trứng thì cộng dồn số lượng, bình/nhạc chỉ mua được 1 lần. */
  async purchase(userId: string, shopItemId: string, clientEventId?: string) {
    const shopItem = await this.prisma.shopItem.findUnique({ where: { id: shopItemId } });
    if (!shopItem || !shopItem.isActive) {
      throw new NotFoundException('Không tìm thấy vật phẩm này');
    }

    const stackable = shopItem.category === ShopCategory.EGG;
    if (!stackable) {
      const existing = await this.prisma.inventoryItem.findUnique({
        where: { userId_shopItemId: { userId, shopItemId } },
      });
      if (existing) {
        throw new ForbiddenException('Bạn đã sở hữu vật phẩm này rồi');
      }
    }

    return this.prisma.$transaction(async (tx) => {
      await this.currencyService.spend(userId, shopItem.priceCoin, LedgerReason.PURCHASE, {
        refShopItemId: shopItemId,
        clientEventId,
        tx,
      });

      if (stackable) {
        return tx.inventoryItem.upsert({
          where: { userId_shopItemId: { userId, shopItemId } },
          create: { userId, shopItemId, quantity: 1 },
          update: { quantity: { increment: 1 } },
        });
      }
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
