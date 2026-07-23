import { BadRequestException, Injectable } from '@nestjs/common';
import { CurrencyType, LedgerReason, Prisma, PrismaClient } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';

type Tx = Prisma.TransactionClient | PrismaClient;

/** Thông báo lỗi thân thiện khi không đủ số dư, theo từng loại tiền tệ. */
function insufficientBalanceMessage(currency: CurrencyType): string {
  return currency === CurrencyType.FOCUS_MINUTE ? 'Không đủ Giờ tích luỹ' : 'Không đủ Xu Lá';
}

@Injectable()
export class CurrencyService {
  constructor(private readonly prisma: PrismaService) {}

  async getBalance(userId: string, currency: CurrencyType = CurrencyType.COIN): Promise<number> {
    return this.getBalanceWith(this.prisma, userId, currency);
  }

  /** Số dư cả 2 loại tiền tệ — dùng cho GET /currency/balance. */
  async getBalances(userId: string): Promise<{ coin: number; focusMinutes: number }> {
    const [coin, focusMinutes] = await Promise.all([
      this.getBalance(userId, CurrencyType.COIN),
      this.getBalance(userId, CurrencyType.FOCUS_MINUTE),
    ]);
    return { coin, focusMinutes };
  }

  getLedger(userId: string, from?: Date, to?: Date, currency?: CurrencyType) {
    return this.prisma.ledgerEntry.findMany({
      where: {
        userId,
        currency,
        createdAt: from || to ? { gte: from, lte: to } : undefined,
      },
      orderBy: { createdAt: 'desc' },
    });
  }

  /** Cộng tiền — dùng tx tuỳ chọn để gộp chung transaction với hành động gây ra khoản thưởng. */
  earn(
    userId: string,
    amount: number,
    reason: LedgerReason,
    opts: { currency?: CurrencyType; refSessionId?: string; clientEventId?: string; tx?: Tx } = {},
  ) {
    if (amount <= 0) return null;
    const client = opts.tx ?? this.prisma;
    return client.ledgerEntry.create({
      data: {
        userId,
        amount,
        currency: opts.currency ?? CurrencyType.COIN,
        reason,
        refSessionId: opts.refSessionId,
        clientEventId: opts.clientEventId,
      },
    });
  }

  /** Trừ tiền — báo lỗi nếu không đủ số dư của đúng loại tiền tệ đang trừ. */
  async spend(
    userId: string,
    amount: number,
    reason: LedgerReason,
    opts: { currency?: CurrencyType; refShopItemId?: string; clientEventId?: string; tx?: Tx } = {},
  ) {
    const currency = opts.currency ?? CurrencyType.COIN;
    const client = opts.tx ?? this.prisma;
    const balance = await this.getBalanceWith(client, userId, currency);
    if (balance < amount) {
      throw new BadRequestException(insufficientBalanceMessage(currency));
    }
    return client.ledgerEntry.create({
      data: {
        userId,
        amount: -amount,
        currency,
        reason,
        refShopItemId: opts.refShopItemId,
        clientEventId: opts.clientEventId,
      },
    });
  }

  private async getBalanceWith(client: Tx, userId: string, currency: CurrencyType): Promise<number> {
    const result = await client.ledgerEntry.aggregate({
      where: { userId, currency },
      _sum: { amount: true },
    });
    return result._sum.amount ?? 0;
  }
}
