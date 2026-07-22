import { Body, Controller, Get, Param, Patch, Post, Query, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { SessionStatus } from '@prisma/client';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CurrentUser } from '../auth/decorators/current-user.decorator';
import { SessionsService } from './sessions.service';
import { CreateSessionDto } from './dto/create-session.dto';
import { CompleteSessionDto } from './dto/complete-session.dto';

@ApiTags('sessions')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('sessions')
export class SessionsController {
  constructor(private readonly sessionsService: SessionsService) {}

  @Post()
  create(@CurrentUser() user: { userId: string }, @Body() dto: CreateSessionDto) {
    return this.sessionsService.create(user.userId, dto);
  }

  @Get()
  findAll(
    @CurrentUser() user: { userId: string },
    @Query('from') from?: string,
    @Query('to') to?: string,
    @Query('status') status?: SessionStatus,
  ) {
    return this.sessionsService.findAll(
      user.userId,
      from ? new Date(from) : undefined,
      to ? new Date(to) : undefined,
      status,
    );
  }

  @Patch(':id/complete')
  complete(
    @CurrentUser() user: { userId: string },
    @Param('id') id: string,
    @Body() dto: CompleteSessionDto,
  ) {
    return this.sessionsService.complete(user.userId, id, dto.clientEventId);
  }

  @Patch(':id/give-up')
  giveUp(@CurrentUser() user: { userId: string }, @Param('id') id: string) {
    return this.sessionsService.giveUp(user.userId, id);
  }
}
