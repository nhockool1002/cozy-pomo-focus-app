import { Body, Controller, Get, Param, Patch, Post, Query, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { ShopCategory } from '@prisma/client';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CurrentUser } from '../auth/decorators/current-user.decorator';
import { ShopService } from './shop.service';
import { PurchaseDto } from './dto/purchase.dto';

@ApiTags('shop')
@Controller()
export class ShopController {
  constructor(private readonly shopService: ShopService) {}

  @Get('shop-items')
  findAll(@Query('category') category?: ShopCategory) {
    return this.shopService.findAll(category);
  }

  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard)
  @Post('shop-items/:id/purchase')
  purchase(
    @CurrentUser() user: { userId: string },
    @Param('id') id: string,
    @Body() dto: PurchaseDto,
  ) {
    return this.shopService.purchase(user.userId, id, dto.clientEventId, dto.payWith);
  }

  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard)
  @Get('inventory')
  getInventory(@CurrentUser() user: { userId: string }) {
    return this.shopService.getInventory(user.userId);
  }

  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard)
  @Patch('inventory/:id/equip')
  toggleEquip(@CurrentUser() user: { userId: string }, @Param('id') id: string) {
    return this.shopService.toggleEquip(user.userId, id);
  }
}
