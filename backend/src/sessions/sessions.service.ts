import { ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { CurrencyType, LedgerReason, SessionStatus } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';
import { CurrencyService } from '../currency/currency.service';
import { GameSettingsService } from '../game-settings/game-settings.service';
import { OwnedEggsService } from '../owned-eggs/owned-eggs.service';
import { CreateSessionDto } from './dto/create-session.dto';

@Injectable()
export class SessionsService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly currencyService: CurrencyService,
    private readonly gameSettingsService: GameSettingsService,
    private readonly ownedEggsService: OwnedEggsService,
  ) {}

  async create(userId: string, dto: CreateSessionDto) {
    if (dto.clientEventId) {
      const existing = await this.prisma.session.findUnique({
        where: { clientEventId: dto.clientEventId },
      });
      if (existing) return existing;
    }

    let incubationRatio: number | null = null;
    if (dto.ownedEggId) {
      // Chỉ cần xác nhận trứng thuộc về user này và đang ấp — ném lỗi rõ ràng nếu không.
      await this.ownedEggsService.getIncubatingOrThrow(userId, dto.ownedEggId);
      incubationRatio = dto.incubationRatio ?? 1;
    }

    return this.prisma.session.create({
      data: {
        userId,
        ownedEggId: dto.ownedEggId,
        incubationRatio,
        plannedMin: dto.plannedMin,
        strictMode: dto.strictMode,
        clientEventId: dto.clientEventId,
      },
    });
  }

  findAll(userId: string, from?: Date, to?: Date, status?: SessionStatus) {
    return this.prisma.session.findMany({
      where: {
        userId,
        status,
        startedAt: from || to ? { gte: from, lte: to } : undefined,
      },
      include: { ownedEgg: { include: { eggType: true } } },
      orderBy: { startedAt: 'desc' },
    });
  }

  private async getOwnedRunningSession(userId: string, sessionId: string) {
    const session = await this.prisma.session.findUnique({ where: { id: sessionId } });
    if (!session || session.userId !== userId) {
      throw new NotFoundException('Không tìm thấy phiên này');
    }
    return session;
  }

  /**
   * Hoàn thành phiên — cộng Xu Lá theo GameSettings.coinsPerFocusMinute, chia phút còn lại
   * giữa Giờ tích luỹ và trứng đang ấp (nếu có) theo `incubationRatio`. Idempotent theo
   * clientEventId: gọi lại không cộng trùng.
   */
  async complete(userId: string, sessionId: string, clientEventId?: string) {
    const session = await this.getOwnedRunningSession(userId, sessionId);

    if (session.status === SessionStatus.COMPLETED) {
      // Đã xử lý trước đó (VD app gửi lại do mất mạng) — trả kết quả cũ, không cộng thêm.
      const ownedEgg = session.ownedEggId
        ? await this.prisma.ownedEgg.findUnique({
            where: { id: session.ownedEggId },
            include: { eggType: true, resultSpecies: true },
          })
        : null;
      return {
        session,
        coinsEarned: session.coinsEarned ?? 0,
        minutesAccumulated: session.minutesAccumulated ?? 0,
        ownedEgg,
        resultSpecies: null,
        hatched: false,
      };
    }
    if (session.status !== SessionStatus.RUNNING) {
      throw new ForbiddenException('Phiên này đã kết thúc trước đó (bỏ cuộc)');
    }

    const gameSettings = await this.gameSettingsService.get();
    const coinsEarned = Math.round(session.plannedMin * gameSettings.coinsPerFocusMinute);

    const ratio = session.ownedEggId ? (session.incubationRatio ?? 1) : 0;
    const minutesIncubated = session.ownedEggId ? Math.round(session.plannedMin * ratio) : 0;
    const minutesAccumulated = session.plannedMin - minutesIncubated;

    const result = await this.prisma.$transaction(async (tx) => {
      const updatedSession = await tx.session.update({
        where: { id: sessionId },
        data: {
          status: SessionStatus.COMPLETED,
          endedAt: new Date(),
          coinsEarned,
          minutesAccumulated,
          minutesIncubated,
        },
      });

      await this.currencyService.earn(userId, coinsEarned, LedgerReason.SESSION_REWARD, {
        currency: CurrencyType.COIN,
        refSessionId: sessionId,
        clientEventId: clientEventId ? `${clientEventId}:coin` : undefined,
        tx,
      });
      await this.currencyService.earn(userId, minutesAccumulated, LedgerReason.SESSION_REWARD, {
        currency: CurrencyType.FOCUS_MINUTE,
        refSessionId: sessionId,
        clientEventId: clientEventId ? `${clientEventId}:focus` : undefined,
        tx,
      });

      let ownedEgg = null;
      let resultSpecies = null;
      let hatched = false;
      if (session.ownedEggId && minutesIncubated > 0) {
        const incubateResult = await this.ownedEggsService.incubate(session.ownedEggId, minutesIncubated, tx);
        ownedEgg = incubateResult.ownedEgg;
        resultSpecies = incubateResult.resultSpecies;
        hatched = incubateResult.hatched;
      }

      return { session: updatedSession, ownedEgg, resultSpecies, hatched };
    });

    return { ...result, coinsEarned, minutesAccumulated };
  }

  async giveUp(userId: string, sessionId: string) {
    const session = await this.getOwnedRunningSession(userId, sessionId);
    if (session.status !== SessionStatus.RUNNING) {
      return session; // idempotent: đã kết thúc rồi thì trả nguyên trạng
    }
    return this.prisma.session.update({
      where: { id: sessionId },
      data: { status: SessionStatus.GIVEN_UP, endedAt: new Date() },
    });
  }
}
