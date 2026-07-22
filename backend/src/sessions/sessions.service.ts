import { ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { LedgerReason, SessionStatus } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';
import { EggsService } from '../eggs/eggs.service';
import { CurrencyService } from '../currency/currency.service';
import { CollectionService } from '../collection/collection.service';
import { CreateSessionDto } from './dto/create-session.dto';

@Injectable()
export class SessionsService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly eggsService: EggsService,
    private readonly currencyService: CurrencyService,
    private readonly collectionService: CollectionService,
  ) {}

  async create(userId: string, dto: CreateSessionDto) {
    if (dto.clientEventId) {
      const existing = await this.prisma.session.findUnique({
        where: { clientEventId: dto.clientEventId },
      });
      if (existing) return existing;
    }

    const eggType = await this.prisma.eggType.findUnique({ where: { id: dto.eggTypeId } });
    if (!eggType) {
      throw new NotFoundException('Không tìm thấy loại trứng này');
    }

    return this.prisma.session.create({
      data: {
        userId,
        eggTypeId: dto.eggTypeId,
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
      include: { eggType: true, resultSpecies: true },
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

  /** Hoàn thành phiên: roll loài theo trứng, cộng Xu Lá, cập nhật khu rừng — idempotent theo clientEventId. */
  async complete(userId: string, sessionId: string, clientEventId?: string) {
    const session = await this.getOwnedRunningSession(userId, sessionId);

    if (session.status === SessionStatus.COMPLETED) {
      // Đã xử lý trước đó (VD app gửi lại do mất mạng) — trả kết quả cũ, không cộng thêm Xu.
      const resultSpecies = session.resultSpeciesId
        ? await this.prisma.species.findUnique({ where: { id: session.resultSpeciesId } })
        : null;
      return { session, resultSpecies, coinsEarned: session.coinsEarned ?? 0 };
    }
    if (session.status !== SessionStatus.RUNNING) {
      throw new ForbiddenException('Phiên này đã kết thúc trước đó (bỏ cuộc)');
    }

    const resultSpecies = await this.eggsService.rollSpecies(session.eggTypeId);
    const coinsEarned = session.plannedMin;

    const updated = await this.prisma.$transaction(async (tx) => {
      const updatedSession = await tx.session.update({
        where: { id: sessionId },
        data: {
          status: SessionStatus.COMPLETED,
          endedAt: new Date(),
          resultSpeciesId: resultSpecies.id,
          coinsEarned,
        },
      });
      await this.currencyService.earn(userId, coinsEarned, LedgerReason.SESSION_REWARD, {
        refSessionId: sessionId,
        clientEventId: clientEventId ? `${clientEventId}:earn` : undefined,
        tx,
      });
      await this.collectionService.recordHatch(userId, resultSpecies.id, tx);
      return updatedSession;
    });

    return { session: updated, resultSpecies, coinsEarned };
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
