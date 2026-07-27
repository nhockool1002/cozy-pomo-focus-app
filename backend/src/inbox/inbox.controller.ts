import { Controller, Get, Param, Patch, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CurrentUser } from '../auth/decorators/current-user.decorator';
import { InboxService } from './inbox.service';

@ApiTags('inbox')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('inbox')
export class InboxController {
  constructor(private readonly inboxService: InboxService) {}

  @Get()
  list(@CurrentUser() user: { userId: string }) {
    return this.inboxService.list(user.userId);
  }

  @Get('unread-count')
  async unreadCount(@CurrentUser() user: { userId: string }) {
    return { count: await this.inboxService.unreadCount(user.userId) };
  }

  @Patch(':id/read')
  markRead(@CurrentUser() user: { userId: string }, @Param('id') id: string) {
    return this.inboxService.markRead(user.userId, id);
  }

  @Patch('read-all')
  markAllRead(@CurrentUser() user: { userId: string }) {
    return this.inboxService.markAllRead(user.userId);
  }
}
