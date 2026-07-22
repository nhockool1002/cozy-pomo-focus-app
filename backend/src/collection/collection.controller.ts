import { Controller, Get, Param, Patch, Query, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { Rarity, SpeciesCategory } from '@prisma/client';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CurrentUser } from '../auth/decorators/current-user.decorator';
import { CollectionService } from './collection.service';

@ApiTags('collection')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('collection')
export class CollectionController {
  constructor(private readonly collectionService: CollectionService) {}

  @Get()
  findAll(
    @CurrentUser() user: { userId: string },
    @Query('category') category?: SpeciesCategory,
    @Query('rarity') rarity?: Rarity,
  ) {
    return this.collectionService.findAll(user.userId, { category, rarity });
  }

  @Get('progress')
  getProgress(@CurrentUser() user: { userId: string }) {
    return this.collectionService.getProgress(user.userId);
  }

  @Patch(':speciesId/favorite')
  toggleFavorite(@CurrentUser() user: { userId: string }, @Param('speciesId') speciesId: string) {
    return this.collectionService.toggleFavorite(user.userId, speciesId);
  }
}
