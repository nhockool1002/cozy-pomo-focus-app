/**
 * Patch AN TOÀN cho production (2026-07-28, T-125/T-126): nạp nội dung MỚI mà `seed.ts` thường sẽ
 * tạo (nếu chạy từ đầu) nhưng KHÔNG xoá bất kỳ dữ liệu nào đang có — khác hẳn `seed.ts` (xoá sạch
 * toàn bộ) và khác cả `reseed-testers.ts` (vẫn xoá/tạo lại 10 tester). Script này CHỈ:
 *   1. Cập nhật `Species.lore` cho các loài đang `lore = null` (khớp theo tên với SPECIES_LORE) —
 *      UPDATE tại chỗ, không đụng id/rarity/archetype hay bất kỳ cột nào khác.
 *   2. Thêm 6 `ShopItem` category BOOST (Túi Thời Gian x4, Giọt Sương/Ánh Trăng Ấp Trứng) — bỏ qua
 *      item nào đã tồn tại theo tên (idempotent, chạy lại nhiều lần không tạo trùng).
 *   3. Thêm 7 dòng `StreakRewardDay` (ngày 1-7, rewards rỗng — Admin tự điền qua AdminJS) —
 *      `createMany({ skipDuplicates: true })` nên không đè lên cấu hình đã có nếu chạy lại.
 *
 * KHÔNG đụng tới: User, Session, LedgerEntry, CollectionEntry, InventoryItem, OwnedEgg,
 * UserSettings, EggType, EggDropEntry, EggRarityWeight, GameSettings, ShopItem đã có sẵn (EGG/
 * JAR_SKIN/MUSIC) — an toàn tuyệt đối cho MỌI user, kể cả tài khoản thật nhininh410@gmail.com,
 * vì script không có bất kỳ lệnh deleteMany/update nào chạm tới các bảng đó.
 */
import { BoostType, PrismaClient, ShopCategory } from '@prisma/client';
import { SPECIES_LORE } from './species-lore';

const prisma = new PrismaClient();

async function main() {
  console.log('1) Cập nhật lore cho các loài đang null...');
  let loreUpdated = 0;
  for (const [name, lore] of Object.entries(SPECIES_LORE)) {
    const result = await prisma.species.updateMany({ where: { name, lore: null }, data: { lore } });
    loreUpdated += result.count;
  }
  console.log(`   ✓ Đã cập nhật lore cho ${loreUpdated} loài (bỏ qua loài đã có lore hoặc không tìm thấy tên khớp).`);

  console.log('2) Thêm vật phẩm bổ trợ (BOOST) nếu chưa có...');
  const boostItems: Array<{ name: string; description: string; priceCoin: number; purchasable?: boolean; boostType: BoostType; boostAmount: number }> = [
    { name: 'Túi Thời Gian Nhỏ', description: '+10 phút Giờ tích luỹ', priceCoin: 120, boostType: BoostType.FOCUS_MINUTES, boostAmount: 10 },
    { name: 'Túi Thời Gian Vừa', description: '+20 phút Giờ tích luỹ', priceCoin: 240, boostType: BoostType.FOCUS_MINUTES, boostAmount: 20 },
    { name: 'Túi Thời Gian Lớn', description: '+50 phút Giờ tích luỹ', priceCoin: 600, boostType: BoostType.FOCUS_MINUTES, boostAmount: 50 },
    { name: 'Túi Thời Gian Khổng Lồ', description: '+100 phút Giờ tích luỹ', priceCoin: 1200, boostType: BoostType.FOCUS_MINUTES, boostAmount: 100 },
    { name: 'Giọt Sương Ấp Trứng', description: '+60 phút ấp cho 1 trứng đang ấp', priceCoin: 240, boostType: BoostType.HATCH_MINUTES, boostAmount: 60 },
    {
      name: 'Ánh Trăng Ấp Trứng',
      description: '+300 phút ấp cho 1 trứng đang ấp — phần thưởng sự kiện, không bán',
      priceCoin: 0,
      purchasable: false,
      boostType: BoostType.HATCH_MINUTES,
      boostAmount: 300,
    },
  ];
  for (const item of boostItems) {
    const existing = await prisma.shopItem.findFirst({ where: { name: item.name } });
    if (existing) {
      console.log(`   ⏭ ${item.name} đã tồn tại (id ${existing.id}) — bỏ qua.`);
      continue;
    }
    const created = await prisma.shopItem.create({ data: { ...item, category: ShopCategory.BOOST } });
    console.log(`   ✓ Đã tạo ${created.name} (id ${created.id}).`);
  }

  console.log('3) Thêm 7 dòng cấu hình quà streak (ngày 1-7, rỗng) nếu chưa có...');
  const result = await prisma.streakRewardDay.createMany({
    data: Array.from({ length: 7 }, (_, i) => ({ day: i + 1, rewards: [] })),
    skipDuplicates: true,
  });
  console.log(`   ✓ Đã tạo ${result.count} dòng mới (bỏ qua ngày đã có cấu hình).`);

  console.log('Xong — không xoá/đụng tới bất kỳ User/Session/Ledger/Collection/Inventory nào.');
}

main()
  .catch((e) => {
    console.error(e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
