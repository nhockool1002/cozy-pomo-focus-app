import { Injectable } from '@nestjs/common';
import { SessionStatus } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';

function dayKey(d: Date): string {
  return d.toISOString().slice(0, 10);
}

@Injectable()
export class StatsService {
  constructor(private readonly prisma: PrismaService) {}

  /**
   * Không dùng bảng rollup riêng — với quy mô hiện tại (tester/early users) tính trực tiếp
   * từ bảng sessions là đủ nhanh. Cân nhắc thêm stats_daily nếu dữ liệu lớn dần.
   */
  async getRange(userId: string, start: Date, end: Date) {
    const sessions = await this.prisma.session.findMany({
      where: { userId, startedAt: { gte: start, lte: end } },
      select: { startedAt: true, status: true, plannedMin: true },
    });

    const byDay = new Map<string, { totalFocusMinutes: number; completedCount: number; givenUpCount: number }>();
    for (const s of sessions) {
      const key = dayKey(s.startedAt);
      const entry = byDay.get(key) ?? { totalFocusMinutes: 0, completedCount: 0, givenUpCount: 0 };
      if (s.status === SessionStatus.COMPLETED) {
        entry.totalFocusMinutes += s.plannedMin;
        entry.completedCount += 1;
      } else if (s.status === SessionStatus.GIVEN_UP) {
        entry.givenUpCount += 1;
      }
      byDay.set(key, entry);
    }

    return Array.from(byDay.entries())
      .map(([date, v]) => ({ date, ...v }))
      .sort((a, b) => a.date.localeCompare(b.date));
  }

  async getDaily(userId: string, date: Date) {
    const start = new Date(date);
    start.setUTCHours(0, 0, 0, 0);
    const end = new Date(start);
    end.setUTCDate(end.getUTCDate() + 1);

    const range = await this.getRange(userId, start, end);
    return range[0] ?? { date: dayKey(start), totalFocusMinutes: 0, completedCount: 0, givenUpCount: 0 };
  }

  async getStreak(userId: string): Promise<number> {
    const sessions = await this.prisma.session.findMany({
      where: { userId, status: SessionStatus.COMPLETED },
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

  async getTotalFocusMinutes(userId: string): Promise<number> {
    const result = await this.prisma.session.aggregate({
      where: { userId, status: SessionStatus.COMPLETED },
      _sum: { plannedMin: true },
    });
    return result._sum.plannedMin ?? 0;
  }
}
