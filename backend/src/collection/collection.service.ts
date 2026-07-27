import { ForbiddenException, Injectable } from '@nestjs/common';
import { Prisma, PrismaClient, Rarity, SpeciesCategory } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';

type Tx = Prisma.TransactionClient | PrismaClient;

@Injectable()
export class CollectionService {
  constructor(private readonly prisma: PrismaService) {}

  findAll(userId: string, filter: { category?: SpeciesCategory; rarity?: Rarity }) {
    return this.prisma.collectionEntry.findMany({
      where: {
        userId,
        species: {
          category: filter.category,
          rarity: filter.rarity,
        },
      },
      include: { species: true },
      orderBy: { lastHatchedAt: 'desc' },
    });
  }

  async getProgress(userId: string) {
    const [unlocked, total] = await Promise.all([
      this.prisma.collectionEntry.count({ where: { userId } }),
      this.prisma.species.count({ where: { isActive: true } }),
    ]);
    return { unlocked, total };
  }

  /** Gọi khi 1 phiên ấp thành công — cộng dồn nếu đã có, tạo mới nếu lần đầu. `ownedCount` tăng
   * cùng `hatchCount` (tự ấp luôn tạo thêm 1 bản đang sở hữu) — khác nhánh mua ở Chợ (T-106) chỉ
   * tăng `ownedCount`, không đụng `hatchCount`. */
  recordHatch(userId: string, speciesId: string, tx?: Tx) {
    const client = tx ?? this.prisma;
    return client.collectionEntry.upsert({
      where: { userId_speciesId: { userId, speciesId } },
      create: { userId, speciesId, hatchCount: 1, ownedCount: 1 },
      update: { hatchCount: { increment: 1 }, ownedCount: { increment: 1 }, lastHatchedAt: new Date() },
    });
  }

  /** T-120 — Admin phát quà (AdminJS "Phát quà"): cộng `quantity` cùng lúc, cùng ngữ nghĩa với
   * `recordHatch` (tăng cả `hatchCount` lẫn `ownedCount` — loài coi như đã "mở khoá" thật sự cho
   * user, không phải kiểu chuyển nhượng tạm như mua ở Chợ chỉ tăng `ownedCount`). */
  grantMany(userId: string, speciesId: string, quantity: number, tx?: Tx) {
    const client = tx ?? this.prisma;
    return client.collectionEntry.upsert({
      where: { userId_speciesId: { userId, speciesId } },
      create: { userId, speciesId, hatchCount: quantity, ownedCount: quantity },
      update: { hatchCount: { increment: quantity }, ownedCount: { increment: quantity }, lastHatchedAt: new Date() },
    });
  }

  async toggleFavorite(userId: string, speciesId: string) {
    const entry = await this.prisma.collectionEntry.findUnique({
      where: { userId_speciesId: { userId, speciesId } },
    });
    if (!entry) {
      throw new ForbiddenException('Bạn chưa mở khoá loài này');
    }
    return this.prisma.collectionEntry.update({
      where: { userId_speciesId: { userId, speciesId } },
      data: { isFavorite: !entry.isFavorite },
    });
  }
}
