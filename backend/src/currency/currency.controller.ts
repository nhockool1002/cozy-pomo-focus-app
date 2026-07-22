import { Controller, Get, Query, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CurrentUser } from '../auth/decorators/current-user.decorator';
import { CurrencyService } from './currency.service';

@ApiTags('currency')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('currency')
export class CurrencyController {
  constructor(private readonly currencyService: CurrencyService) {}

  @Get('balance')
  async getBalance(@CurrentUser() user: { userId: string }) {
    return { balance: await this.currencyService.getBalance(user.userId) };
  }

  @Get('ledger')
  getLedger(
    @CurrentUser() user: { userId: string },
    @Query('from') from?: string,
    @Query('to') to?: string,
  ) {
    return this.currencyService.getLedger(
      user.userId,
      from ? new Date(from) : undefined,
      to ? new Date(to) : undefined,
    );
  }
}
