import { BadRequestException, Injectable } from '@nestjs/common';
import { LedgerReason, Prisma, PrismaClient } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';

type Tx = Prisma.TransactionClient | PrismaClient;

@Injectable()
export class CurrencyService {
  constructor(private readonly prisma: PrismaService) {}

  async getBalance(userId: string): Promise<number> {
    const result = await this.prisma.ledgerEntry.aggregate({
      where: { userId },
      _sum: { amount: true },
    });
    return result._sum.amount ?? 0;
  }

  getLedger(userId: string, from?: Date, to?: Date) {
    return this.prisma.ledgerEntry.findMany({
      where: {
        userId,
        createdAt: from || to ? { gte: from, lte: to } : undefined,
      },
      orderBy: { createdAt: 'desc' },
    });
  }

  /** Cộng Xu Lá — dùng tx tuỳ chọn để gộp chung transaction với hành động gây ra khoản thưởng. */
  earn(
    userId: string,
    amount: number,
    reason: LedgerReason,
    opts: { refSessionId?: string; clientEventId?: string; tx?: Tx } = {},
  ) {
    const client = opts.tx ?? this.prisma;
    return client.ledgerEntry.create({
      data: {
        userId,
        amount,
        reason,
        refSessionId: opts.refSessionId,
        clientEventId: opts.clientEventId,
      },
    });
  }

  /** Trừ Xu Lá — báo lỗi nếu không đủ số dư. */
  async spend(
    userId: string,
    amount: number,
    reason: LedgerReason,
    opts: { refShopItemId?: string; clientEventId?: string; tx?: Tx } = {},
  ) {
    const client = opts.tx ?? this.prisma;
    const balance = await this.getBalanceWith(client, userId);
    if (balance < amount) {
      throw new BadRequestException('Không đủ Xu Lá');
    }
    return client.ledgerEntry.create({
      data: {
        userId,
        amount: -amount,
        reason,
        refShopItemId: opts.refShopItemId,
        clientEventId: opts.clientEventId,
      },
    });
  }

  private async getBalanceWith(client: Tx, userId: string): Promise<number> {
    const result = await client.ledgerEntry.aggregate({
      where: { userId },
      _sum: { amount: true },
    });
    return result._sum.amount ?? 0;
  }
}
