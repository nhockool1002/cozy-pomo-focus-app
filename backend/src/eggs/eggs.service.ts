import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class EggsService {
  constructor(private readonly prisma: PrismaService) {}

  findAll() {
    return this.prisma.eggType.findMany({ orderBy: { name: 'asc' } });
  }

  findOne(id: string) {
    return this.prisma.eggType.findUnique({ where: { id } });
  }

  private async loadWeightedDrops(eggTypeId: string) {
    const [drops, rarityWeights] = await Promise.all([
      this.prisma.eggDropEntry.findMany({
        where: { eggTypeId },
        include: { species: true },
      }),
      this.prisma.rarityWeight.findMany(),
    ]);
    const rarityWeightMap = new Map(rarityWeights.map((r) => [r.rarity, r.weight]));
    return drops.map((d) => ({
      species: d.species,
      effectiveWeight: d.weight * (rarityWeightMap.get(d.species.rarity) ?? 1),
    }));
  }

  /** Xác suất nở theo từng cấp bậc — hiển thị minh bạch cho người chơi trước khi ấp. */
  async getOdds(eggTypeId: string) {
    const eggType = await this.findOne(eggTypeId);
    if (!eggType) {
      throw new NotFoundException('Không tìm thấy loại trứng này');
    }
    const weighted = await this.loadWeightedDrops(eggTypeId);
    const total = weighted.reduce((sum, w) => sum + w.effectiveWeight, 0);

    const byRarity = new Map<string, number>();
    for (const w of weighted) {
      byRarity.set(w.species.rarity, (byRarity.get(w.species.rarity) ?? 0) + w.effectiveWeight);
    }

    return {
      eggType,
      totalSpecies: weighted.length,
      odds: Array.from(byRarity.entries()).map(([rarity, weight]) => ({
        rarity,
        percent: total > 0 ? Number(((weight / total) * 100).toFixed(3)) : 0,
      })),
    };
  }

  /** Random có trọng số: weight = trọng số riêng trong egg_drop_table × trọng số cấp bậc toàn cục. */
  async rollSpecies(eggTypeId: string) {
    const weighted = await this.loadWeightedDrops(eggTypeId);
    if (weighted.length === 0) {
      throw new NotFoundException('Loại trứng này chưa có loài nào để nở');
    }
    const total = weighted.reduce((sum, w) => sum + w.effectiveWeight, 0);
    let roll = Math.random() * total;
    for (const w of weighted) {
      roll -= w.effectiveWeight;
      if (roll <= 0) {
        return w.species;
      }
    }
    return weighted[weighted.length - 1].species;
  }
}
