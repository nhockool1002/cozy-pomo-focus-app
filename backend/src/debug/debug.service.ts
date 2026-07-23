import { ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { CurrencyType, LedgerReason, Rarity } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';
import { CurrencyService } from '../currency/currency.service';

/** Chỉ tài khoản tester seed (`testerNN@cozypomo.dev`) mới dùng được — kiểm tra ở SERVER, không
 * chỉ ẩn/hiện UI phía app, để không ai gọi thẳng API bỏ qua giao diện mà vẫn cheat được. */
const TESTER_EMAIL_PATTERN = /^tester\d{2}@cozypomo\.dev$/;

@Injectable()
export class DebugService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly currencyService: CurrencyService,
  ) {}

  private assertTester(email: string) {
    if (!TESTER_EMAIL_PATTERN.test(email)) {
      throw new ForbiddenException('Chức năng debug chỉ dành cho tài khoản tester');
    }
  }

  async grantCurrency(userId: string, email: string, currency: CurrencyType, amount: number) {
    this.assertTester(email);
    await this.currencyService.earn(userId, amount, LedgerReason.ADMIN_ADJUST, { currency });
    const { coin, focusMinutes } = await this.currencyService.getBalances(userId);
    return { balance: coin, focusMinutes };
  }

  async grantEgg(userId: string, email: string, eggTypeName = 'Trứng Bí Ẩn') {
    this.assertTester(email);
    const eggType = await this.prisma.eggType.findFirst({ where: { name: eggTypeName } });
    if (!eggType) {
      throw new NotFoundException(`Không tìm thấy loại trứng "${eggTypeName}"`);
    }
    const created = await this.prisma.ownedEgg.create({ data: { userId, eggTypeId: eggType.id } });
    return this.prisma.ownedEgg.findUniqueOrThrow({
      where: { id: created.id },
      include: { eggType: true, resultSpecies: true },
    });
  }

  async grantSpecies(userId: string, email: string, rarity: Rarity) {
    this.assertTester(email);
    const candidates = await this.prisma.species.findMany({ where: { rarity, isActive: true } });
    if (candidates.length === 0) {
      throw new NotFoundException('Không có loài nào ở cấp bậc này');
    }
    const species = candidates[Math.floor(Math.random() * candidates.length)];
    return this.prisma.collectionEntry.upsert({
      where: { userId_speciesId: { userId, speciesId: species.id } },
      create: { userId, speciesId: species.id, hatchCount: 1 },
      update: { hatchCount: { increment: 1 }, lastHatchedAt: new Date() },
      include: { species: true },
    });
  }
}
