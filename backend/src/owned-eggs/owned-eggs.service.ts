import { ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { OwnedEggStatus, Prisma, PrismaClient } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';
import { EggsService } from '../eggs/eggs.service';
import { CollectionService } from '../collection/collection.service';

type Tx = Prisma.TransactionClient | PrismaClient;

/** Kết quả cộng phút ấp cho 1 trứng — resultSpecies chỉ khác null khi vừa đủ để nở trong lần này. */
export interface IncubateResult {
  ownedEgg: Prisma.OwnedEggGetPayload<{ include: { eggType: true; resultSpecies: true } }>;
  resultSpecies: Prisma.SpeciesGetPayload<object> | null;
  hatched: boolean;
}

@Injectable()
export class OwnedEggsService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly eggsService: EggsService,
    private readonly collectionService: CollectionService,
  ) {}

  findMine(userId: string, status?: OwnedEggStatus) {
    return this.prisma.ownedEgg.findMany({
      where: { userId, status },
      include: { eggType: true, resultSpecies: true },
      orderBy: { acquiredAt: 'desc' },
    });
  }

  create(userId: string, eggTypeId: string, tx?: Tx) {
    const client = tx ?? this.prisma;
    return client.ownedEgg.create({ data: { userId, eggTypeId } });
  }

  /** Trứng đang ấp của đúng user này — báo lỗi nếu không tìm thấy hoặc đã nở rồi. */
  async getIncubatingOrThrow(userId: string, ownedEggId: string) {
    const egg = await this.prisma.ownedEgg.findUnique({
      where: { id: ownedEggId },
      include: { eggType: true },
    });
    if (!egg || egg.userId !== userId) {
      throw new NotFoundException('Không tìm thấy trứng này trong bộ sưu tập của bạn');
    }
    if (egg.status !== OwnedEggStatus.INCUBATING) {
      throw new ForbiddenException('Trứng này đã nở rồi, không thể ấp thêm');
    }
    return egg;
  }

  /**
   * Cộng thêm phút ấp cho 1 trứng — nếu đạt đủ `hatchDurationMin` của loại trứng thì roll
   * loài (theo egg_drop_table/rarity_weights như cũ) và ghi vào bộ sưu tập. Phải gọi trong
   * cùng transaction với phần cộng Xu Lá/Giờ tích luỹ của phiên để đảm bảo nhất quán.
   */
  async incubate(ownedEggId: string, minutes: number, tx: Tx): Promise<IncubateResult> {
    const egg = await tx.ownedEgg.findUniqueOrThrow({
      where: { id: ownedEggId },
      include: { eggType: true },
    });

    const newIncubated = Math.min(egg.incubatedMin + minutes, egg.eggType.hatchDurationMin);
    const willHatch = egg.status === OwnedEggStatus.INCUBATING && newIncubated >= egg.eggType.hatchDurationMin;

    if (!willHatch) {
      const updated = await tx.ownedEgg.update({
        where: { id: ownedEggId },
        data: { incubatedMin: newIncubated },
        include: { eggType: true, resultSpecies: true },
      });
      return { ownedEgg: updated, resultSpecies: null, hatched: false };
    }

    const resultSpecies = await this.eggsService.rollSpecies(egg.eggTypeId);
    const updated = await tx.ownedEgg.update({
      where: { id: ownedEggId },
      data: {
        incubatedMin: newIncubated,
        status: OwnedEggStatus.HATCHED,
        hatchedAt: new Date(),
        resultSpeciesId: resultSpecies.id,
      },
      include: { eggType: true, resultSpecies: true },
    });
    await this.collectionService.recordHatch(egg.userId, resultSpecies.id, tx);
    return { ownedEgg: updated, resultSpecies, hatched: true };
  }
}
