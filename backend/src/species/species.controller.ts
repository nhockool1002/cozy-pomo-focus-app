import { Controller, Get, NotFoundException, Param, Query } from '@nestjs/common';
import { ApiTags } from '@nestjs/swagger';
import { SpeciesService } from './species.service';
import { SpeciesCategory, Rarity } from '@prisma/client';

@ApiTags('species')
@Controller('species')
export class SpeciesController {
  constructor(private readonly speciesService: SpeciesService) {}

  @Get()
  findAll(
    @Query('category') category?: SpeciesCategory,
    @Query('rarity') rarity?: Rarity,
  ) {
    return this.speciesService.findAll({ category, rarity });
  }

  @Get(':id')
  async findOne(@Param('id') id: string) {
    const species = await this.speciesService.findOne(id);
    if (!species) {
      throw new NotFoundException('Không tìm thấy loài này');
    }
    return species;
  }
}
