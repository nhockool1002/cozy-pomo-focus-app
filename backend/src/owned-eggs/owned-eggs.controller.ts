import { Controller, Get, Query, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { OwnedEggStatus } from '@prisma/client';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CurrentUser } from '../auth/decorators/current-user.decorator';
import { OwnedEggsService } from './owned-eggs.service';

@ApiTags('owned-eggs')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('owned-eggs')
export class OwnedEggsController {
  constructor(private readonly ownedEggsService: OwnedEggsService) {}

  /** Danh sách trứng người dùng sở hữu (đang ấp + đã nở) — dùng cho popup chọn trứng khi tập trung. */
  @Get()
  findMine(@CurrentUser() user: { userId: string }, @Query('status') status?: OwnedEggStatus) {
    return this.ownedEggsService.findMine(user.userId, status);
  }
}
