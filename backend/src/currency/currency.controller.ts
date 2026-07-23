import { Controller, Get, Query, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { CurrencyType } from '@prisma/client';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CurrentUser } from '../auth/decorators/current-user.decorator';
import { CurrencyService } from './currency.service';

@ApiTags('currency')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('currency')
export class CurrencyController {
  constructor(private readonly currencyService: CurrencyService) {}

  /** `balance` = Xu Lá (giữ tên cũ cho tương thích ngược), `focusMinutes` = Giờ tích luỹ (phút). */
  @Get('balance')
  async getBalance(@CurrentUser() user: { userId: string }) {
    const { coin, focusMinutes } = await this.currencyService.getBalances(user.userId);
    return { balance: coin, focusMinutes };
  }

  @Get('ledger')
  getLedger(
    @CurrentUser() user: { userId: string },
    @Query('from') from?: string,
    @Query('to') to?: string,
    @Query('currency') currency?: CurrencyType,
  ) {
    return this.currencyService.getLedger(
      user.userId,
      from ? new Date(from) : undefined,
      to ? new Date(to) : undefined,
      currency,
    );
  }
}
