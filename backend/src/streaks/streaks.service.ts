import { Injectable, Logger } from '@nestjs/common';
import { Cron } from '@nestjs/schedule';
import { CurrencyType, LedgerReason, Prisma, PrismaClient, StreakRewardItemKind } from '@prisma/client';
import { CollectionService } from '../collection/collection.service';
import { CurrencyService } from '../currency/currency.service';
import { InboxService } from '../inbox/inbox.service';
import { OwnedEggsService } from '../owned-eggs/owned-eggs.service';
import { PrismaService } from '../prisma/prisma.service';
import { ShopService } from '../shop/shop.service';
import { dayKey } from '../stats/stats.service';

type Tx = Prisma.TransactionClient | PrismaClient;

export interface StreakRewardItem {
  kind: StreakRewardItemKind;
  /** null khi kind = COIN — quantity khi đó là số Xu Lá, không phải số lượng vật phẩm. */
  id?: string;
  name: string;
  quantity: number;
}

export interface StreakRewardResult {
  day: number;
  items: StreakRewardItem[];
}

/**
 * Streak ngày liên tục (1-7) — thưởng chỉ phát 1 lần cho tới hết ngày 7 (Dev1002 chốt
 * 2026-07-28): từ ngày 8 trở đi không phát thêm cho tới khi streak đứt (bỏ lỡ 1 ngày) và chạy
 * lại từ ngày 1. Admin cấu hình quà từng ngày qua AdminJS (StreakRewardDay, xem admin.module.ts).
 */
@Injectable()
export class StreaksService {
  private readonly logger = new Logger(StreaksService.name);

  constructor(
    private readonly collectionService: CollectionService,
    private readonly ownedEggsService: OwnedEggsService,
    private readonly shopService: ShopService,
    private readonly currencyService: CurrencyService,
    private readonly inboxService: InboxService,
    private readonly prisma: PrismaService,
  ) {}

  /**
   * Gọi bên trong transaction hoàn thành phiên (SessionsService.complete) — `tx` phải thấy được
   * phiên COMPLETED vừa cập nhật trong CÙNG transaction để tính streak đúng ngay hôm nay.
   * Idempotent theo `StreakClaim.[userId, claimedOn]`: gọi lại nhiều lần trong cùng 1 ngày (vd
   * nhiều phiên hoàn thành cùng ngày) chỉ tính/phát quà 1 lần.
   */
  async checkAndGrant(userId: string, tx: Tx): Promise<StreakRewardResult | null> {
    const claimedOn = dayKey(new Date());
    const already = await tx.streakClaim.findUnique({ where: { userId_claimedOn: { userId, claimedOn } } });
    if (already) return null;

    const streakCount = await this.computeStreakCount(userId, tx);
    await tx.streakClaim.create({ data: { userId, claimedOn, streakDay: Math.min(streakCount, 7) } });

    if (streakCount < 1 || streakCount > 7) return null;

    const config = await tx.streakRewardDay.findUnique({ where: { day: streakCount } });
    const rewards = ((config?.rewards as unknown as StreakRewardItem[] | null) ?? []).filter((r) => r.quantity > 0);
    if (rewards.length === 0) return null;

    for (const reward of rewards) {
      await this.applyReward(userId, reward, tx);
    }

    await this.inboxService.create(
      userId,
      'STREAK_REWARD',
      `Streak ${streakCount} ngày liên tiếp!`,
      `Bạn đã tập trung ${streakCount} ngày liên tiếp — nhận ngay phần thưởng streak!`,
      { day: streakCount, items: rewards } as unknown as Prisma.InputJsonValue,
      tx,
    );

    return { day: streakCount, items: rewards };
  }

  private async applyReward(userId: string, reward: StreakRewardItem, tx: Tx) {
    switch (reward.kind) {
      case StreakRewardItemKind.SPECIES:
        await this.collectionService.grantMany(userId, reward.id!, reward.quantity, tx);
        break;
      case StreakRewardItemKind.EGG_TYPE:
        for (let i = 0; i < reward.quantity; i++) {
          await this.ownedEggsService.create(userId, reward.id!, tx);
        }
        break;
      case StreakRewardItemKind.SHOP_ITEM:
        await this.shopService.grantItem(userId, reward.id!, reward.quantity, tx);
        break;
      case StreakRewardItemKind.COIN:
        await this.currencyService.earn(userId, reward.quantity, LedgerReason.STREAK_REWARD, {
          currency: CurrencyType.COIN,
          tx,
        });
        break;
    }
  }

  /** Số ngày liên tiếp (tính tới hôm nay) có ít nhất 1 phiên COMPLETED — cùng thuật toán
   * StatsService.getStreak() nhưng query qua `tx` để thấy phiên vừa hoàn thành trong transaction. */
  private async computeStreakCount(userId: string, tx: Tx): Promise<number> {
    const sessions = await tx.session.findMany({
      where: { userId, status: 'COMPLETED' },
      select: { startedAt: true },
    });
    const days = new Set(sessions.map((s) => dayKey(s.startedAt)));

    let streak = 0;
    const cursor = new Date();
    cursor.setUTCHours(0, 0, 0, 0);
    while (days.has(dayKey(cursor))) {
      streak += 1;
      cursor.setUTCDate(cursor.getUTCDate() - 1);
    }
    return streak;
  }

  /**
   * T-128 — nhắc streak sắp mất, chạy 1 lần/ngày lúc 14:00 UTC (~21:00 giờ VN — trễ đủ để hầu hết
   * người dùng đã có cơ hội tập trung trong ngày, còn dư vài giờ trước khi "ngày" theo `dayKey`
   * (UTC) đổi lúc 7:00 sáng giờ VN hôm sau). Chỉ nhắc user có phiên COMPLETED **hôm qua** (UTC)
   * nhưng CHƯA có phiên COMPLETED nào **hôm nay** — nghĩa là streak đang "treo", sẽ mất nếu không
   * tập trung trước khi ngày đổi. Idempotent theo ngày: bỏ qua user đã có `STREAK_REMINDER` tạo
   * hôm nay (phòng job chạy lại/nhiều instance), không dùng `StreakClaim` vì bảng đó chỉ ghi khi
   * ĐANG có phiên hoàn thành, không có dữ liệu cho "chưa làm gì hôm nay".
   */
  @Cron('0 14 * * *')
  async remindAtRiskStreaks(): Promise<void> {
    const now = new Date();
    const todayStart = new Date(`${dayKey(now)}T00:00:00.000Z`);
    const yesterdayStart = new Date(todayStart);
    yesterdayStart.setUTCDate(yesterdayStart.getUTCDate() - 1);
    const tomorrowStart = new Date(todayStart);
    tomorrowStart.setUTCDate(tomorrowStart.getUTCDate() + 1);

    const activeYesterday = await this.prisma.session.findMany({
      where: { status: 'COMPLETED', startedAt: { gte: yesterdayStart, lt: todayStart } },
      distinct: ['userId'],
      select: { userId: true },
    });
    if (activeYesterday.length === 0) return;
    const candidateIds = activeYesterday.map((s) => s.userId);

    const doneToday = await this.prisma.session.findMany({
      where: { status: 'COMPLETED', startedAt: { gte: todayStart, lt: tomorrowStart }, userId: { in: candidateIds } },
      distinct: ['userId'],
      select: { userId: true },
    });
    const doneTodaySet = new Set(doneToday.map((s) => s.userId));
    const atRiskIds = candidateIds.filter((id) => !doneTodaySet.has(id));
    if (atRiskIds.length === 0) return;

    const alreadyReminded = await this.prisma.inboxMessage.findMany({
      where: { type: 'STREAK_REMINDER', userId: { in: atRiskIds }, createdAt: { gte: todayStart, lt: tomorrowStart } },
      select: { userId: true },
    });
    const remindedSet = new Set(alreadyReminded.map((m) => m.userId));

    let sent = 0;
    for (const userId of atRiskIds) {
      if (remindedSet.has(userId)) continue;
      await this.inboxService.create(
        userId,
        'STREAK_REMINDER',
        'Đừng để mất streak!',
        'Bạn đã tập trung liên tục tới hôm qua — hoàn thành 1 phiên hôm nay để giữ streak nhé!',
      );
      sent += 1;
    }
    this.logger.log(`remindAtRiskStreaks: ${atRiskIds.length} at-risk, ${sent} reminder(s) sent`);
  }
}
