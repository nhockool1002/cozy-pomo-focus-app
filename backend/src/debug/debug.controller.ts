import { Body, Controller, Post, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CurrentUser } from '../auth/decorators/current-user.decorator';
import { DebugService } from './debug.service';
import { GrantCurrencyDto, GrantEggDto, GrantSpeciesDto } from './dto/debug.dto';

/** Endpoint cheat CHỈ dành cho tài khoản tester (kiểm tra ở DebugService) — mục đích debug/QA,
 * không phải tính năng sản phẩm thật. */
@ApiTags('debug')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('debug')
export class DebugController {
  constructor(private readonly debugService: DebugService) {}

  @Post('grant-currency')
  grantCurrency(@CurrentUser() user: { userId: string; email: string }, @Body() dto: GrantCurrencyDto) {
    return this.debugService.grantCurrency(user.userId, user.email, dto.currency, dto.amount);
  }

  @Post('grant-egg')
  grantEgg(@CurrentUser() user: { userId: string; email: string }, @Body() dto: GrantEggDto) {
    return this.debugService.grantEgg(user.userId, user.email, dto.eggTypeName);
  }

  @Post('grant-species')
  grantSpecies(@CurrentUser() user: { userId: string; email: string }, @Body() dto: GrantSpeciesDto) {
    return this.debugService.grantSpecies(user.userId, user.email, dto.rarity);
  }
}
