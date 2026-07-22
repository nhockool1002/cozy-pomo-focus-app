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

  /** Gọi khi 1 phiên ấp thành công — cộng dồn nếu đã có, tạo mới nếu lần đầu. */
  recordHatch(userId: string, speciesId: string, tx?: Tx) {
    const client = tx ?? this.prisma;
    return client.collectionEntry.upsert({
      where: { userId_speciesId: { userId, speciesId } },
      create: { userId, speciesId, hatchCount: 1 },
      update: { hatchCount: { increment: 1 }, lastHatchedAt: new Date() },
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
