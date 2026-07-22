import { Injectable } from '@nestjs/common';
import { Rarity, SpeciesCategory } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class SpeciesService {
  constructor(private readonly prisma: PrismaService) {}

  findAll(filter: { category?: SpeciesCategory; rarity?: Rarity }) {
    return this.prisma.species.findMany({
      where: {
        isActive: true,
        category: filter.category,
        rarity: filter.rarity,
      },
      orderBy: [{ category: 'asc' }, { name: 'asc' }],
    });
  }

  findOne(id: string) {
    return this.prisma.species.findUnique({ where: { id } });
  }
}
