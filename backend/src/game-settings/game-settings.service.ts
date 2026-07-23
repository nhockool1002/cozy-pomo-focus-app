import { Injectable } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

const SINGLETON_ID = 1;

/** Cấu hình kinh tế toàn cục (VD Xu Lá/phút tập trung) — 1 dòng duy nhất, admin chỉnh qua AdminJS. */
@Injectable()
export class GameSettingsService {
  constructor(private readonly prisma: PrismaService) {}

  async get() {
    const existing = await this.prisma.gameSettings.findUnique({ where: { id: SINGLETON_ID } });
    if (existing) return existing;
    // Chưa seed thì tự tạo với giá trị mặc định trong schema, tránh app crash vì thiếu cấu hình.
    return this.prisma.gameSettings.create({ data: { id: SINGLETON_ID } });
  }
}
