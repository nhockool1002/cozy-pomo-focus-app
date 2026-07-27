import { ForbiddenException, Injectable } from '@nestjs/common';
import { InboxMessageType, Prisma, PrismaClient } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';

type Tx = Prisma.TransactionClient | PrismaClient;

/**
 * Hộp thư (T-123) — thông báo trong app khi nhận trứng/trứng nở/admin tặng quà. `create()` nhận
 * `tx?` để gọi được từ trong transaction của service khác (ShopService/MarketService/OwnedEggsService,
 * GiftPage handler ở admin.module.ts) — 1 thư luôn tạo CÙNG lúc với hành động phát sinh ra nó,
 * không tách rời (tránh trường hợp mua trứng thành công nhưng lỡ crash trước khi ghi thư).
 */
@Injectable()
export class InboxService {
  constructor(private readonly prisma: PrismaService) {}

  create(
    userId: string,
    type: InboxMessageType,
    title: string,
    body: string,
    payload?: Prisma.InputJsonValue,
    tx?: Tx,
  ) {
    const client = tx ?? this.prisma;
    return client.inboxMessage.create({ data: { userId, type, title, body, payload } });
  }

  list(userId: string) {
    return this.prisma.inboxMessage.findMany({ where: { userId }, orderBy: { createdAt: 'desc' } });
  }

  unreadCount(userId: string) {
    return this.prisma.inboxMessage.count({ where: { userId, isRead: false } });
  }

  async markRead(userId: string, id: string) {
    const result = await this.prisma.inboxMessage.updateMany({
      where: { id, userId },
      data: { isRead: true },
    });
    if (result.count === 0) {
      throw new ForbiddenException('Không tìm thấy thư này');
    }
    return { ok: true };
  }

  async markAllRead(userId: string) {
    await this.prisma.inboxMessage.updateMany({ where: { userId, isRead: false }, data: { isRead: true } });
    return { ok: true };
  }
}
