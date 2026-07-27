import { Injectable } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

const SINGLETON_ID = 1;

/** Cấu hình Force Update (T-121) — 1 dòng duy nhất, admin chỉnh qua AdminJS mỗi lần release app. */
@Injectable()
export class AppVersionService {
  constructor(private readonly prisma: PrismaService) {}

  async get() {
    const existing = await this.prisma.appVersionConfig.findUnique({ where: { id: SINGLETON_ID } });
    if (existing) return existing;
    // Chưa seed thì tự tạo với giá trị mặc định trong schema, tránh app crash vì thiếu cấu hình.
    return this.prisma.appVersionConfig.create({ data: { id: SINGLETON_ID } });
  }
}
