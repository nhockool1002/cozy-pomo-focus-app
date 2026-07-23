import { Injectable, Logger } from '@nestjs/common';
import { SessionsService } from '../sessions/sessions.service';
import { ShopService } from '../shop/shop.service';
import { SyncBatchDto, SyncEventDto } from './dto/sync-batch.dto';

interface SyncResult {
  clientEventId: string;
  status: 'ok' | 'error';
  data?: unknown;
  error?: string;
}

@Injectable()
export class SyncService {
  private readonly logger = new Logger(SyncService.name);

  constructor(
    private readonly sessionsService: SessionsService,
    private readonly shopService: ShopService,
  ) {}

  /**
   * Xử lý tuần tự từng event trong hàng đợi offline của app — 1 event lỗi không chặn
   * các event còn lại, client tự quyết định retry theo status trả về.
   */
  async processBatch(userId: string, dto: SyncBatchDto): Promise<SyncResult[]> {
    const results: SyncResult[] = [];
    for (const event of dto.events) {
      try {
        const data = await this.processOne(userId, event);
        results.push({ clientEventId: event.clientEventId, status: 'ok', data });
      } catch (err) {
        this.logger.warn(`Sync event failed: ${event.type} (${event.clientEventId})`, err as Error);
        results.push({
          clientEventId: event.clientEventId,
          status: 'error',
          error: err instanceof Error ? err.message : 'Unknown error',
        });
      }
    }
    return results;
  }

  private processOne(userId: string, event: SyncEventDto) {
    switch (event.type) {
      case 'session_create': {
        const p = event.payload as {
          ownedEggId?: string;
          incubationRatio?: number;
          plannedMin: number;
          strictMode?: boolean;
        };
        return this.sessionsService.create(userId, {
          ownedEggId: p.ownedEggId,
          incubationRatio: p.incubationRatio,
          plannedMin: p.plannedMin,
          strictMode: p.strictMode ?? true,
          clientEventId: event.clientEventId,
        });
      }
      case 'session_complete': {
        const p = event.payload as { sessionId: string };
        return this.sessionsService.complete(userId, p.sessionId, event.clientEventId);
      }
      case 'session_give_up': {
        const p = event.payload as { sessionId: string };
        return this.sessionsService.giveUp(userId, p.sessionId);
      }
      case 'shop_purchase': {
        const p = event.payload as { shopItemId: string };
        return this.shopService.purchase(userId, p.shopItemId, event.clientEventId);
      }
    }
  }
}
