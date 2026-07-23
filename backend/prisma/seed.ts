/**
 * Seed dữ liệu demo: 175 loài (khớp Creature Atlas đã thiết kế), 4 loại trứng + bảng tỉ lệ,
 * vật phẩm cửa hàng, và 10 tài khoản tester có lịch sử phiên/Xu Lá/bộ sưu tập thật.
 *
 * Chạy lại sẽ XOÁ SẠCH và tạo lại toàn bộ dữ liệu — chỉ dùng cho môi trường dev/demo,
 * không chạy script này nhắm vào DB đã có người dùng thật.
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
  SpeciesCategory,
} from '@prisma/client';
import * as bcrypt from 'bcryptjs';

const prisma = new PrismaClient();

// ---------- PRNG & rarity hash — y hệt hệ thống trong Creature Atlas artifact ----------
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
function rarityFor(name: string): Rarity {
  const h = hashStr(name) % 100;
  if (h < 45) return Rarity.B;
  if (h < 75) return Rarity.A;
  if (h < 90) return Rarity.S;
  return Rarity.SS;
}

// PALETTE base hex — chỉ để tham chiếu màu loại trứng, client render hình dựa vào paletteIdx.
const PALETTE_HEX = [
  '#E2965F', '#F0D98C', '#9CB380', '#9AC0D9', '#E7A8B0', '#B58BC4', '#7C9A5A',
  '#E8876B', '#D9C29A', '#6FB6A8', '#C9607A', '#7E8FB0', '#E3B04B', '#8FCDB0',
];

type SpeciesSeed = [name: string, archetype: string, paletteIdx: number];

const forestNames: SpeciesSeed[] = [
  ['Cáo Pomodoro', 'fox', 0], ['Cáo Buổi Sớm', 'fox', 7], ['Cáo Áo Len', 'fox', 12], ['Cáo Sương Mù', 'fox', 3], ['Cáo Lá Phong', 'fox', 7],
  ['Thỏ Mộng Mơ', 'rabbit', 4], ['Thỏ Bông Gòn', 'rabbit', 1], ['Thỏ Trà Chiều', 'rabbit', 8], ['Thỏ Cỏ Ba Lá', 'rabbit', 2], ['Thỏ Đêm Trăng', 'rabbit', 11],
  ['Gấu Ngái Ngủ', 'bear', 8], ['Gấu Mật Ong', 'bear', 12], ['Gấu Chăn Ấm', 'bear', 6], ['Gấu Bánh Quy', 'bear', 1], ['Gấu Rừng Thông', 'bear', 9],
  ['Mèo Lười Nắng', 'cat', 12], ['Mèo Đọc Sách', 'cat', 11], ['Mèo Tách Trà', 'cat', 5], ['Mèo Cuộn Len', 'cat', 4], ['Mèo Đêm Sao', 'cat', 3],
  ['Chim Sẻ Rộn Ràng', 'bird', 1], ['Chim Cổ Đỏ', 'bird', 7], ['Chim Vàng Anh', 'bird', 12], ['Chim Ri Đá', 'bird', 8], ['Chim Hoạ Mi Nhỏ', 'bird', 5],
  ['Nhím Ấm Áp', 'hedgehog', 9], ['Nhím Gối Bông', 'hedgehog', 8], ['Nhím Lá Khô', 'hedgehog', 7], ['Nhím Táo Đỏ', 'hedgehog', 10], ['Nhím Đêm Sương', 'hedgehog', 11],
  ['Sóc Tíu Tít', 'squirrel', 0], ['Sóc Hạt Dẻ', 'squirrel', 8], ['Sóc Mùa Thu', 'squirrel', 7], ['Sóc Vân Sam', 'squirrel', 2], ['Sóc Áo Nâu', 'squirrel', 9],
  ['Gấu Mèo Tinh Nghịch', 'raccoon', 11], ['Gấu Mèo Đốm Sao', 'raccoon', 5], ['Gấu Mèo Sương Sớm', 'raccoon', 3], ['Gấu Mèo Lá Vàng', 'raccoon', 12], ['Gấu Mèo Đêm Hè', 'raccoon', 6],
  ['Hươu Thầm Lặng', 'deer', 6], ['Hươu Sương Sớm', 'deer', 3], ['Hươu Cỏ Xanh', 'deer', 2], ['Hươu Chuông Gió', 'deer', 9], ['Hươu Nắng Sớm', 'deer', 12],
  ['Cú Đêm Hiền', 'owl', 11], ['Cú Sách Cổ', 'owl', 6], ['Cú Trăng Non', 'owl', 3], ['Cú Gió Đêm', 'owl', 9], ['Cú Rừng Sâu', 'owl', 2],
];

const seaNames: SpeciesSeed[] = [
  ['Rùa May Mắn', 'turtle', 9], ['Rùa Đá Cuội', 'turtle', 8], ['Rùa Lá Sen', 'turtle', 2], ['Rùa Ngọc Bích', 'turtle', 9], ['Rùa Sóng Vỗ', 'turtle', 3],
  ['Cua Nắng Chiều', 'crab', 7], ['Cua Bọt Biển', 'crab', 3], ['Cua Vỏ Sò', 'crab', 4], ['Cua San Hô', 'crab', 10], ['Cua Đá Ngầm', 'crab', 11],
  ['Ốc Anh Vũ', 'snail', 5], ['Ốc Mộng Mơ', 'snail', 10], ['Ốc Cầu Vồng', 'snail', 13], ['Ốc Xoáy Nước', 'snail', 3], ['Ốc Biếc Ngọc', 'snail', 9],
  ['Cá Vàng Nhỏ', 'fish', 12], ['Cá San Hô', 'fish', 7], ['Cá Vằn Đốm', 'fish', 3], ['Cá Bảy Màu', 'fish', 5], ['Cá Ánh Bạc', 'fish', 11],
  ['Sao Biển Lấp Lánh', 'starfish', 12], ['Sao Biển Cam', 'starfish', 7], ['Sao Biển Đêm', 'starfish', 5], ['Sao Biển Hồng Phấn', 'starfish', 4], ['Sao Biển Sương', 'starfish', 3],
  ['Hải Cẩu Lười', 'seal', 11], ['Hải Cẩu Vịnh Xanh', 'seal', 9], ['Hải Cẩu Băng Giá', 'seal', 3], ['Hải Cẩu Nắng Chiều', 'seal', 7], ['Hải Cẩu Vui Tính', 'seal', 12],
  ['Cá Heo Vui Vẻ', 'dolphin', 3], ['Cá Heo Sóng Nhẹ', 'dolphin', 9], ['Cá Heo Ánh Trăng', 'dolphin', 11], ['Cá Heo Bọt Sóng', 'dolphin', 2], ['Cá Heo Mưa Rào', 'dolphin', 9],
  ['Sứa Đèn Lồng', 'jellyfish', 4], ['Sứa Ánh Trăng', 'jellyfish', 5], ['Sứa Pha Lê', 'jellyfish', 3], ['Sứa Hồng Nhạt', 'jellyfish', 4], ['Sứa Đêm Sâu', 'jellyfish', 11],
  ['Bạch Tuộc Tò Mò', 'octopus', 10], ['Bạch Tuộc Mực Tím', 'octopus', 5], ['Bạch Tuộc Cầu Vồng', 'octopus', 13], ['Bạch Tuộc Đá San Hô', 'octopus', 7], ['Bạch Tuộc Mực Đen', 'octopus', 11],
  ['Cá Ngựa Vằn', 'seahorse', 9], ['Cá Ngựa San Hô', 'seahorse', 7], ['Cá Ngựa Hoàng Hôn', 'seahorse', 10], ['Cá Ngựa Lá Rong', 'seahorse', 2], ['Cá Ngựa Sương Mai', 'seahorse', 9],
];

const plantNames: SpeciesSeed[] = [
  ['Hoa Tulip', 'flowerRound', 4], ['Hoa Cúc Nắng', 'flowerRound', 12], ['Hoa Anh Đào Nhỏ', 'flowerRound', 4], ['Hoa Mẫu Đơn', 'flowerRound', 10], ['Hoa Oải Hương Mini', 'flowerRound', 5],
  ['Hoa Sao Biếc', 'flowerStar', 3], ['Hoa Dạ Yến Sao', 'flowerStar', 5], ['Hoa Cẩm Tú Sao', 'flowerStar', 9], ['Hoa Bìm Bìm Sao', 'flowerStar', 11], ['Hoa Thảo Nguyên Sao', 'flowerStar', 13],
  ['Nấm Chấm Bi', 'mushroom', 7], ['Nấm Rêu Phong', 'mushroom', 2], ['Nấm Đèn Lồng', 'mushroom', 12], ['Nấm San Hô', 'mushroom', 10], ['Nấm Cổ Tích', 'mushroom', 5],
  ['Dương Xỉ Xoăn', 'fern', 2], ['Dương Xỉ Sương Mai', 'fern', 9], ['Dương Xỉ Lá Kim', 'fern', 6], ['Dương Xỉ Rừng Sâu', 'fern', 13], ['Dương Xỉ Ngọc Bích', 'fern', 9],
  ['Sen Đá Hồng', 'succulent', 4], ['Sen Đá Ngọc', 'succulent', 13], ['Sen Đá Mật Ong', 'succulent', 12], ['Sen Đá Tuyết', 'succulent', 3], ['Sen Đá Rêu', 'succulent', 2],
  ['Xương Rồng Tí Hon', 'cactus', 6], ['Xương Rồng Sa Mạc', 'cactus', 8], ['Xương Rồng Hoa Nở', 'cactus', 10], ['Xương Rồng Chấm Sao', 'cactus', 2], ['Xương Rồng Mini Chum', 'cactus', 13],
  ['Bụi Dâu Rừng', 'berry', 10], ['Bụi Việt Quất', 'berry', 11], ['Bụi Phúc Bồn Tử', 'berry', 7], ['Bụi Dâu Tằm', 'berry', 5], ['Bụi Quả Chuông', 'berry', 4],
  ['Trúc Cảnh Nhỏ', 'bamboo', 2], ['Trúc Sương Sớm', 'bamboo', 9], ['Trúc Ngọc Xanh', 'bamboo', 13], ['Trúc Vàng Chiều', 'bamboo', 12], ['Trúc Mini Ban Công', 'bamboo', 6],
  ['Dây Trầu Bà', 'vine', 2], ['Dây Thường Xuân', 'vine', 6], ['Dây Hoa Chuông', 'vine', 5], ['Dây Lan Tim', 'vine', 10], ['Dây Bìm Bìm Leo', 'vine', 3],
  ['Cây Sồi Con', 'tree', 7], ['Cây Phong Nhỏ', 'tree', 10], ['Cây Táo Tí Hon', 'tree', 4], ['Cây Liễu Xanh', 'tree', 2], ['Cây Thông Mini', 'tree', 6],
];

const mythicNames: SpeciesSeed[] = [
  ['Phượng Hoàng Bình Minh', 'phoenix', 12], ['Phượng Hoàng Lửa Ấm', 'phoenix', 7], ['Phượng Hoàng Chiều Tà', 'phoenix', 10], ['Phượng Hoàng Tro Tàn Tái Sinh', 'phoenix', 0], ['Phượng Hoàng Ánh Kim', 'phoenix', 1],
  ['Kỳ Lân Ngọc Bích', 'qilin', 9], ['Kỳ Lân Mây Trắng', 'qilin', 3], ['Kỳ Lân Sao Đêm', 'qilin', 11], ['Kỳ Lân Rừng Cổ', 'qilin', 6], ['Kỳ Lân Ánh Bình Minh', 'qilin', 12],
  ['Long Vân Thanh', 'dragon', 3], ['Hoả Long Nhỏ', 'dragon', 7], ['Băng Long Tuyết', 'dragon', 9], ['Thổ Long Cổ Mộc', 'dragon', 8], ['Kim Long Ánh Nắng', 'dragon', 12],
  ['Cửu Vĩ Hồ Ly', 'ninetail', 0], ['Hồ Ly Sương Trắng', 'ninetail', 3], ['Hồ Ly Lửa Tím', 'ninetail', 5], ['Hồ Ly Trăng Bạc', 'ninetail', 11], ['Hồ Ly Chín Đuôi Vàng', 'ninetail', 12],
  ['Tiên Hạc Ngàn Năm', 'crane', 3], ['Hạc Trắng Vân Du', 'crane', 8], ['Hạc Đỏ Bình Minh', 'crane', 7], ['Hạc Ngọc Sương Mai', 'crane', 9], ['Hạc Thần Gió Nam', 'crane', 11],
];

async function main() {
  console.log('Xoá dữ liệu cũ...');
  await prisma.ledgerEntry.deleteMany();
  await prisma.collectionEntry.deleteMany();
  await prisma.inventoryItem.deleteMany();
  await prisma.session.deleteMany();
  await prisma.ownedEgg.deleteMany();
  await prisma.userSettings.deleteMany();
  await prisma.user.deleteMany();
  await prisma.shopItem.deleteMany();
  await prisma.eggDropEntry.deleteMany();
  await prisma.eggType.deleteMany();
  await prisma.species.deleteMany();
  await prisma.rarityWeight.deleteMany();
  await prisma.gameSettings.deleteMany();

  console.log('Nạp cấu hình kinh tế (game_settings)...');
  await prisma.gameSettings.create({ data: { id: 1, coinsPerFocusMinute: 10 } });

  console.log('Nạp rarity_weights...');
  await prisma.rarityWeight.createMany({
    data: [
      { rarity: Rarity.B, weight: 450 },
      { rarity: Rarity.A, weight: 300 },
      { rarity: Rarity.S, weight: 150 },
      { rarity: Rarity.SS, weight: 95 },
      { rarity: Rarity.SSR, weight: 5 },
    ],
  });

  console.log('Nạp 175 loài...');
  const insertSpecies = async (list: SpeciesSeed[], category: SpeciesCategory, rarity?: Rarity) => {
    const created = [];
    for (const [name, archetype, paletteIdx] of list) {
      const s = await prisma.species.create({
        data: { name, category, archetype, paletteIdx, rarity: rarity ?? rarityFor(name) },
      });
      created.push(s);
    }
    return created;
  };
  const forestSpecies = await insertSpecies(forestNames, SpeciesCategory.FOREST);
  const seaSpecies = await insertSpecies(seaNames, SpeciesCategory.SEA);
  const plantSpecies = await insertSpecies(plantNames, SpeciesCategory.PLANT);
  const mythicSpecies = await insertSpecies(mythicNames, SpeciesCategory.MYTHIC, Rarity.SSR);
  const allSpecies = [...forestSpecies, ...seaSpecies, ...plantSpecies, ...mythicSpecies];

  console.log('Nạp 4 loại trứng + bảng tỉ lệ nở...');
  // hatchDurationMin/priceHours/priceCoin chỉ là giá trị khởi tạo — admin chỉnh trực tiếp qua AdminJS.
  // Tỉ giá quy đổi cố định: 1 phút Giờ tích luỹ = 10 Xu Lá — priceCoin luôn bằng priceHours * 10
  // để 2 cách trả tiền công bằng như nhau (không có cách nào rẻ hơn cách còn lại).
  const COIN_PER_HOUR_MINUTE = 10;
  const eggForest = await prisma.eggType.create({
    data: { name: 'Trứng Rừng', colorHex: PALETTE_HEX[2], priceCoin: 90 * COIN_PER_HOUR_MINUTE, priceHours: 90, hatchDurationMin: 180 },
  });
  const eggSea = await prisma.eggType.create({
    data: { name: 'Trứng Biển', colorHex: PALETTE_HEX[9], priceCoin: 60 * COIN_PER_HOUR_MINUTE, priceHours: 60, hatchDurationMin: 120 },
  });
  const eggPlant = await prisma.eggType.create({
    data: { name: 'Trứng Hoa', colorHex: PALETTE_HEX[4], priceCoin: 30 * COIN_PER_HOUR_MINUTE, priceHours: 30, hatchDurationMin: 60 },
  });
  const eggMystery = await prisma.eggType.create({
    data: { name: 'Trứng Bí Ẩn', colorHex: PALETTE_HEX[12], priceCoin: 150 * COIN_PER_HOUR_MINUTE, priceHours: 150, hatchDurationMin: 300 },
  });

  await prisma.eggDropEntry.createMany({
    data: [
      ...forestSpecies.map((s) => ({ eggTypeId: eggForest.id, speciesId: s.id, weight: 1 })),
      ...seaSpecies.map((s) => ({ eggTypeId: eggSea.id, speciesId: s.id, weight: 1 })),
      ...plantSpecies.map((s) => ({ eggTypeId: eggPlant.id, speciesId: s.id, weight: 1 })),
      // Trứng Bí Ẩn: rơi ra từ TẤT CẢ các loài (kể cả Thần Thú) — đây là trứng "gacha" cao cấp.
      ...allSpecies.map((s) => ({ eggTypeId: eggMystery.id, speciesId: s.id, weight: 1 })),
    ],
  });

  console.log('Nạp vật phẩm cửa hàng...');
  await prisma.shopItem.create({
    data: { name: eggForest.name, description: 'Trứng thường gặp trong khu rừng ấm áp', category: ShopCategory.EGG, priceCoin: eggForest.priceCoin, eggTypeId: eggForest.id },
  });
  await prisma.shopItem.create({
    data: { name: eggSea.name, description: 'Trứng dạt vào từ những vùng biển xa', category: ShopCategory.EGG, priceCoin: eggSea.priceCoin, eggTypeId: eggSea.id },
  });
  await prisma.shopItem.create({
    data: { name: eggPlant.name, description: 'Trứng nở ra từ những khóm hoa rực rỡ', category: ShopCategory.EGG, priceCoin: eggPlant.priceCoin, eggTypeId: eggPlant.id },
  });
  await prisma.shopItem.create({
    data: { name: eggMystery.name, description: 'Có tỉ lệ nhỏ nở ra Thần Thú huyền thoại', category: ShopCategory.EGG, priceCoin: eggMystery.priceCoin, eggTypeId: eggMystery.id },
  });
  await prisma.shopItem.createMany({
    data: [
      { name: 'Bình Gốm Nung Mộc', description: 'Đổi giao diện màn hình chính', category: ShopCategory.JAR_SKIN, priceCoin: 80 },
      { name: 'Bình Thuỷ Tinh Bạc Hà', description: 'Đổi giao diện màn hình chính', category: ShopCategory.JAR_SKIN, priceCoin: 120 },
      { name: 'Bình Pha Lê Tuyết', description: 'Đổi giao diện màn hình chính', category: ShopCategory.JAR_SKIN, priceCoin: 200 },
      { name: 'Lofi Buổi Sáng', description: 'Nhạc nền', category: ShopCategory.MUSIC, priceCoin: 60 },
      { name: 'Tiếng Dế Đêm Hè', description: 'Nhạc nền', category: ShopCategory.MUSIC, priceCoin: 60 },
      { name: 'Tiếng Mưa Rơi Hiên Nhà', description: 'Nhạc nền', category: ShopCategory.MUSIC, priceCoin: 100 },
    ],
  });
  const jarSkins = await prisma.shopItem.findMany({ where: { category: ShopCategory.JAR_SKIN } });
  const musicItems = await prisma.shopItem.findMany({ where: { category: ShopCategory.MUSIC } });

  console.log('Tạo 10 tài khoản tester với lịch sử phiên/Xu Lá/bộ sưu tập...');
  const eggPools: { egg: typeof eggForest; species: typeof forestSpecies }[] = [
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

  const PASSWORD = 'Tester123!';
  const passwordHash = await bcrypt.hash(PASSWORD, 10);
  const DURATIONS = [10, 15, 20, 25, 25, 30, 45, 60];

  for (let i = 1; i <= 10; i++) {
    const email = `tester${String(i).padStart(2, '0')}@cozypomo.dev`;
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

    const sessionCount = 12 + i * 2; // 14..32 phiên/tester — độ "dày" dữ liệu tăng dần theo tester
    let balance = 0; // Xu Lá — dùng để mô phỏng mua sắm bên dưới
    let cursor = new Date(); // đi ngược thời gian từ hiện tại
    // Mỗi loại trứng có 1 quả đang ấp tại 1 thời điểm — ấp đủ `hatchDurationMin` (cộng dồn qua
    // nhiều phiên) thì nở, sau đó phiên tiếp theo chọn loại đó sẽ tạo trứng mới để ấp tiếp.
    const activeEggId: Record<string, string | null> = {};
    for (const pool of eggPools) activeEggId[pool.egg.id] = null;

    for (let j = 0; j < sessionCount; j++) {
      // Giãn cách 8-30 tiếng giữa các phiên để trải dài trên nhiều ngày, giống người dùng thật
      cursor = new Date(cursor.getTime() - (8 + rnd() * 22) * 3600 * 1000);
      const poolIdx = rnd() < 0.15 ? 3 : Math.floor(rnd() * 3); // 15% chọn Trứng Bí Ẩn, còn lại chia đều 3 loại thường
      const pool = eggPools[poolIdx];
      const plannedMin = DURATIONS[Math.floor(rnd() * DURATIONS.length)];
      const isCompleted = rnd() < 0.85;

      if (isCompleted) {
        const incubationRatio = Math.round((0.4 + rnd() * 0.6) * 100) / 100; // 40-100% dành cho ấp trứng
        const minutesIncubated = Math.round(plannedMin * incubationRatio);
        const remainingMin = plannedMin - minutesIncubated;
        // Phần thời gian không dành cho ấp trứng chỉ quy đổi thành 1 loại tiền — mô phỏng người
        // dùng chọn ngẫu nhiên mỗi phiên, giống logic thật ở SessionsService.complete().
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
          activeEggId[pool.egg.id] = null; // lần sau chọn loại này sẽ tạo trứng mới
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
            create: { userId: user.id, speciesId: resultSpecies.id, hatchCount: 1, firstHatchedAt: cursor, lastHatchedAt: cursor },
            update: { hatchCount: { increment: 1 }, lastHatchedAt: cursor },
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

    // Vài lượt mua sắm cho có dữ liệu inventory/ledger đa dạng (chỉ mua khi đủ Xu).
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

    // Đảm bảo mỗi tester có ít nhất vài loài mỗi nhóm (Thú rừng/Sinh vật biển/Thực vật/Thần Thú)
    // trong bộ sưu tập để demo/kiểm thử UI đủ đa dạng — chỉ dựa vào random rollFromPool ở trên
    // (đặc biệt Thần Thú tỉ lệ rơi thật chỉ 0.5%) không đủ để chắc chắn tester có đại diện mỗi nhóm.
    const bonusPools: { species: typeof allSpecies; count: number }[] = [
      { species: forestSpecies, count: 3 },
      { species: seaSpecies, count: 2 },
      { species: plantSpecies, count: 3 },
      { species: mythicSpecies, count: 2 },
    ];
    for (const { species, count } of bonusPools) {
      const bonusSpecies = [...species].sort(() => rnd() - 0.5).slice(0, count);
      for (const s of bonusSpecies) {
        await prisma.collectionEntry.upsert({
          where: { userId_speciesId: { userId: user.id, speciesId: s.id } },
          create: { userId: user.id, speciesId: s.id, hatchCount: 1 + Math.floor(rnd() * 3), isFavorite: rnd() > 0.7 },
          update: {},
        });
      }
    }

    console.log(`  ✓ ${email} — ${sessionCount} phiên, số dư còn lại ${balance} Xu Lá`);
  }

  console.log('Xong.');
}

main()
  .catch((e) => {
    console.error(e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
