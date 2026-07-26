import { Body, Controller, Get, Param, Patch, Post, Query, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CurrentUser } from '../auth/decorators/current-user.decorator';
import { MarketService } from './market.service';
import { CreateSpeciesListingDto } from './dto/create-species-listing.dto';
import { CreateEggListingDto } from './dto/create-egg-listing.dto';
import { BuyListingDto } from './dto/buy-listing.dto';

@ApiTags('market')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('market')
export class MarketController {
  constructor(private readonly marketService: MarketService) {}

  @Get('listings')
  findAll(@CurrentUser() user: { userId: string }, @Query('mine') mine?: string) {
    return this.marketService.findAll(user.userId, mine === 'true');
  }

  @Post('listings/species')
  createSpeciesListing(@CurrentUser() user: { userId: string }, @Body() dto: CreateSpeciesListingDto) {
    return this.marketService.createSpeciesListing(user.userId, dto);
  }

  @Post('listings/eggs')
  createEggListing(@CurrentUser() user: { userId: string }, @Body() dto: CreateEggListingDto) {
    return this.marketService.createEggListing(user.userId, dto);
  }

  @Patch('listings/:id/cancel')
  cancel(@CurrentUser() user: { userId: string }, @Param('id') id: string) {
    return this.marketService.cancel(user.userId, id);
  }

  @Post('listings/:id/buy')
  buy(@CurrentUser() user: { userId: string }, @Param('id') id: string, @Body() dto: BuyListingDto) {
    return this.marketService.buy(user.userId, id, dto.clientEventId);
  }
}
