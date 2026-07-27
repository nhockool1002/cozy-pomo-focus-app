/**
 * Reseed AN TOÀN cho production (2026-07-27, theo yêu cầu Dev1002): khác hẳn `seed.ts` (xoá sạch
 * TOÀN BỘ dữ liệu), script này CHỈ:
 *   1. Cập nhật giá 4 loại trứng thường (Rừng/Biển/Hoa/Bí Ẩn) — UPDATE tại chỗ, không xoá/tạo lại
 *      EggType (giữ nguyên ID, không phá vỡ FK của bất kỳ OwnedEgg/Session nào đang tham chiếu).
 *   2. Xoá + tạo lại NGẪU NHIÊN 10 tài khoản tester (tester01..10@cozypomo.dev) — lịch sử
 *      phiên/Xu Lá/bộ sưu tập cũ của riêng 10 tài khoản này bị thay hoàn toàn bằng dữ liệu demo mới.
 *
 * KHÔNG đụng tới: Species, 3 trứng Truyền Thuyết, ShopItem, EggDropEntry, EggRarityWeight,
 * GameSettings, và quan trọng nhất — KHÔNG đụng tới bất kỳ user nào khác ngoài 10 tester trên
 * (id/email/password/lịch sử/item của mọi user khác, kể cả tài khoản thật nhininh410@gmail.com,
 * giữ nguyên 100% vì không nằm trong danh sách email bị xoá).
 */
import {
  AuthProvider,
  CurrencyType,
  LedgerReason,
  OwnedEggStatus,
  PrismaClient,
  Rarity,
  SessionStatus,
  ShopCategory,
} from '@prisma/client';
import * as bcrypt from 'bcryptjs';

const prisma = new PrismaClient();

// ---------- PRNG — y hệt seed.ts, để dữ liệu demo sinh ra cùng phong cách/độ đa dạng ----------
function hashStr(s: string): number {
  let h = 1779033703;
  for (let i = 0; i < s.length; i++) {
    h = Math.imul(h ^ s.charCodeAt(i), 3432918353);
    h = (h << 13) | (h >>> 19);
  }
  return h >>> 0;
}
function mulberry32(a: number) {
  return function () {
    a |= 0;
    a = (a + 0x6d2b79f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}
function rngFor(seed: string) {
  return mulberry32(hashStr(seed));
}

const TESTER_EMAILS = Array.from({ length: 10 }, (_, i) => `tester${String(i + 1).padStart(2, '0')}@cozypomo.dev`);

async function main() {
  console.log('Cập nhật giá 4 loại trứng thường (Rừng/Biển/Hoa 300 Xu, Bí Ẩn 450 Xu)...');
  const COIN_PER_HOUR_MINUTE = 10;
  const priceUpdates: Array<[name: string, priceCoin: number]> = [
    ['Trứng Rừng', 300],
    ['Trứng Biển', 300],
    ['Trứng Hoa', 300],
    ['Trứng Bí Ẩn', 450],
  ];
  for (const [name, priceCoin] of priceUpdates) {
    const result = await prisma.eggType.updateMany({
      where: { name },
      data: { priceCoin, priceHours: priceCoin / COIN_PER_HOUR_MINUTE },
    });
    console.log(`  ${result.count > 0 ? '✓' : '⚠ KHÔNG tìm thấy'} ${name} → ${priceCoin} Xu (${result.count} dòng)`);
  }
  // Cập nhật luôn ShopItem tương ứng (danh mục EGG) — đây là giá thật sự hiện ở Cửa hàng, khác
  // bảng EggType chỉ là nguồn tham chiếu (xem seed.ts — ShopItem.priceCoin copy từ EggType lúc tạo,
  // không tự đồng bộ lại khi EggType đổi giá sau này).
  for (const [name, priceCoin] of priceUpdates) {
    await prisma.shopItem.updateMany({ where: { name }, data: { priceCoin } });
  }

  console.log('Nạp lại danh sách loài/trứng/vật phẩm HIỆN CÓ (không xoá/tạo lại)...');
  const [forestSpecies, seaSpecies, plantSpecies, mythicSpecies, eggForest, eggSea, eggPlant, eggMystery, jarSkins, musicItems] =
    await Promise.all([
      prisma.species.findMany({ where: { category: 'FOREST' } }),
      prisma.species.findMany({ where: { category: 'SEA' } }),
      prisma.species.findMany({ where: { category: 'PLANT' } }),
      prisma.species.findMany({ where: { category: 'MYTHIC' } }),
      prisma.eggType.findFirstOrThrow({ where: { name: 'Trứng Rừng' } }),
      prisma.eggType.findFirstOrThrow({ where: { name: 'Trứng Biển' } }),
      prisma.eggType.findFirstOrThrow({ where: { name: 'Trứng Hoa' } }),
      prisma.eggType.findFirstOrThrow({ where: { name: 'Trứng Bí Ẩn' } }),
      prisma.shopItem.findMany({ where: { category: ShopCategory.JAR_SKIN } }),
      prisma.shopItem.findMany({ where: { category: ShopCategory.MUSIC } }),
    ]);
  const allSpecies = [...forestSpecies, ...seaSpecies, ...plantSpecies, ...mythicSpecies];
  const eggPools = [
    { egg: eggForest, species: forestSpecies },
    { egg: eggSea, species: seaSpecies },
    { egg: eggPlant, species: plantSpecies },
    { egg: eggMystery, species: allSpecies },
  ];
  const rarityWeightMap: Record<Rarity, number> = { B: 450, A: 300, S: 150, SS: 95, SSR: 5 };
  function rollFromPool(species: typeof allSpecies, rnd: () => number) {
    const total = species.reduce((sum, s) => sum + rarityWeightMap[s.rarity], 0);
    let roll = rnd() * total;
    for (const s of species) {
      roll -= rarityWeightMap[s.rarity];
      if (roll <= 0) return s;
    }
    return species[species.length - 1];
  }

  console.log(`Xoá 10 tài khoản tester cũ (${TESTER_EMAILS[0]}..${TESTER_EMAILS[9]}) — KHÔNG đụng user nào khác...`);
  const deleted = await prisma.user.deleteMany({ where: { email: { in: TESTER_EMAILS } } });
  console.log(`  Đã xoá ${deleted.count} tester cũ (cascade xoá theo sessions/owned_eggs/collection/ledger/inventory riêng của họ).`);

  const PASSWORD = 'Tester123!';
  const passwordHash = await bcrypt.hash(PASSWORD, 10);
  const DURATIONS = [10, 15, 20, 25, 25, 30, 45, 60];

  async function seedDemoDataForUser(user: { id: string; email: string }, sessionCount: number) {
    const rnd = rngFor(user.email);
    let balance = 0;
    let cursor = new Date();
    const activeEggId: Record<string, string | null> = {};
    for (const pool of eggPools) activeEggId[pool.egg.id] = null;

    for (let j = 0; j < sessionCount; j++) {
      cursor = new Date(cursor.getTime() - (8 + rnd() * 22) * 3600 * 1000);
      const poolIdx = rnd() < 0.15 ? 3 : Math.floor(rnd() * 3);
      const pool = eggPools[poolIdx];
      const plannedMin = DURATIONS[Math.floor(rnd() * DURATIONS.length)];
      const isCompleted = rnd() < 0.85;

      if (isCompleted) {
        const incubationRatio = Math.round((0.4 + rnd() * 0.6) * 100) / 100;
        const minutesIncubated = Math.round(plannedMin * incubationRatio);
        const remainingMin = plannedMin - minutesIncubated;
        const rewardCurrency = rnd() < 0.5 ? CurrencyType.COIN : CurrencyType.FOCUS_MINUTE;
        const coinsEarned = rewardCurrency === CurrencyType.COIN ? Math.round(remainingMin * 10) : 0;
        const minutesAccumulated = rewardCurrency === CurrencyType.FOCUS_MINUTE ? remainingMin : 0;

        let ownedEggId = activeEggId[pool.egg.id];
        if (!ownedEggId) {
          const newEgg = await prisma.ownedEgg.create({
            data: { userId: user.id, eggTypeId: pool.egg.id, acquiredAt: cursor },
          });
          ownedEggId = newEgg.id;
          activeEggId[pool.egg.id] = ownedEggId;
        }

        const eggRow = await prisma.ownedEgg.findUniqueOrThrow({ where: { id: ownedEggId } });
        const newIncubated = eggRow.incubatedMin + minutesIncubated;
        let resultSpecies: (typeof allSpecies)[number] | null = null;
        if (newIncubated >= pool.egg.hatchDurationMin) {
          resultSpecies = rollFromPool(pool.species, rnd);
          await prisma.ownedEgg.update({
            where: { id: ownedEggId },
            data: {
              incubatedMin: pool.egg.hatchDurationMin,
              status: OwnedEggStatus.HATCHED,
              hatchedAt: cursor,
              resultSpeciesId: resultSpecies.id,
            },
          });
          activeEggId[pool.egg.id] = null;
        } else {
          await prisma.ownedEgg.update({ where: { id: ownedEggId }, data: { incubatedMin: newIncubated } });
        }

        const session = await prisma.session.create({
          data: {
            userId: user.id,
            ownedEggId,
            incubationRatio,
            rewardCurrency,
            plannedMin,
            strictMode: true,
            status: SessionStatus.COMPLETED,
            startedAt: cursor,
            endedAt: new Date(cursor.getTime() + plannedMin * 60 * 1000),
            coinsEarned,
            minutesAccumulated,
            minutesIncubated,
          },
        });
        if (rewardCurrency === CurrencyType.COIN) {
          balance += coinsEarned;
          await prisma.ledgerEntry.create({
            data: {
              userId: user.id,
              amount: coinsEarned,
              currency: CurrencyType.COIN,
              reason: LedgerReason.SESSION_REWARD,
              refSessionId: session.id,
              createdAt: cursor,
            },
          });
        } else if (minutesAccumulated > 0) {
          await prisma.ledgerEntry.create({
            data: {
              userId: user.id,
              amount: minutesAccumulated,
              currency: CurrencyType.FOCUS_MINUTE,
              reason: LedgerReason.SESSION_REWARD,
              refSessionId: session.id,
              createdAt: cursor,
            },
          });
        }
        if (resultSpecies) {
          await prisma.collectionEntry.upsert({
            where: { userId_speciesId: { userId: user.id, speciesId: resultSpecies.id } },
            create: {
              userId: user.id,
              speciesId: resultSpecies.id,
              hatchCount: 1,
              ownedCount: 1,
              firstHatchedAt: cursor,
              lastHatchedAt: cursor,
            },
            update: { hatchCount: { increment: 1 }, ownedCount: { increment: 1 }, lastHatchedAt: cursor },
          });
        }
      } else {
        await prisma.session.create({
          data: {
            userId: user.id,
            ownedEggId: activeEggId[pool.egg.id],
            plannedMin,
            strictMode: true,
            status: SessionStatus.GIVEN_UP,
            startedAt: cursor,
            endedAt: new Date(cursor.getTime() + Math.floor(plannedMin * rnd()) * 60 * 1000),
          },
        });
      }
    }

    for (const item of [...jarSkins, ...musicItems]) {
      if (balance >= item.priceCoin && rnd() < 0.6) {
        balance -= item.priceCoin;
        await prisma.ledgerEntry.create({
          data: {
            userId: user.id,
            amount: -item.priceCoin,
            currency: CurrencyType.COIN,
            reason: LedgerReason.PURCHASE,
            refShopItemId: item.id,
          },
        });
        await prisma.inventoryItem.create({ data: { userId: user.id, shopItemId: item.id, quantity: 1 } });
      }
    }
    const ownedJars = await prisma.inventoryItem.findMany({ where: { userId: user.id, shopItem: { category: ShopCategory.JAR_SKIN } } });
    if (ownedJars.length > 0) {
      await prisma.inventoryItem.update({ where: { id: ownedJars[0].id }, data: { equipped: true } });
    }

    const bonusPools: { species: typeof allSpecies; count: number }[] = [
      { species: forestSpecies, count: 3 },
      { species: seaSpecies, count: 2 },
      { species: plantSpecies, count: 3 },
      { species: mythicSpecies, count: 2 },
    ];
    for (const { species, count } of bonusPools) {
      const bonusSpecies = [...species].sort(() => rnd() - 0.5).slice(0, count);
      for (const s of bonusSpecies) {
        const c = 1 + Math.floor(rnd() * 3);
        await prisma.collectionEntry.upsert({
          where: { userId_speciesId: { userId: user.id, speciesId: s.id } },
          create: { userId: user.id, speciesId: s.id, hatchCount: c, ownedCount: c, isFavorite: rnd() > 0.7 },
          update: {},
        });
      }
    }

    console.log(`  ✓ ${user.email} — ${sessionCount} phiên, số dư còn lại ${balance} Xu Lá`);
  }

  console.log('Tạo lại 10 tài khoản tester với lịch sử phiên/Xu Lá/bộ sưu tập MỚI (ngẫu nhiên)...');
  for (let i = 1; i <= 10; i++) {
    const email = TESTER_EMAILS[i - 1];
    const rnd = rngFor(email);
    const user = await prisma.user.create({
      data: {
        email,
        passwordHash,
        authProvider: AuthProvider.LOCAL,
        displayName: `Tester ${String(i).padStart(2, '0')}`,
        settings: { create: { focusMinutes: 25, breakMinutes: 5, strictModeEnabled: rnd() > 0.3 } },
      },
    });
    const sessionCount = 12 + i * 2;
    await seedDemoDataForUser(user, sessionCount);
  }

  console.log('Xong — chỉ 10 tester bị thay đổi, mọi user khác (kể cả tài khoản thật) giữ nguyên 100%.');
}

main()
  .catch((e) => {
    console.error(e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
