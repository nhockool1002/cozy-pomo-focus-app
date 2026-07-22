import { BadRequestException, Controller, Get, Query, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CurrentUser } from '../auth/decorators/current-user.decorator';
import { StatsService } from './stats.service';

@ApiTags('stats')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('stats')
export class StatsController {
  constructor(private readonly statsService: StatsService) {}

  @Get('daily')
  getDaily(@CurrentUser() user: { userId: string }, @Query('date') date?: string) {
    return this.statsService.getDaily(user.userId, date ? new Date(date) : new Date());
  }

  @Get('range')
  getRange(
    @CurrentUser() user: { userId: string },
    @Query('start') start?: string,
    @Query('end') end?: string,
  ) {
    if (!start || !end) {
      throw new BadRequestException('Cần truyền cả start và end (YYYY-MM-DD)');
    }
    return this.statsService.getRange(user.userId, new Date(start), new Date(end));
  }

  @Get('summary')
  async getSummary(@CurrentUser() user: { userId: string }) {
    const [streak, totalFocusMinutes] = await Promise.all([
      this.statsService.getStreak(user.userId),
      this.statsService.getTotalFocusMinutes(user.userId),
    ]);
    return { streak, totalFocusMinutes };
  }
}
