-- Fix: @adminjs/prisma loại field ra khỏi property list khi field đó `isReadOnly` trong DMMF
-- (luôn đúng với field vừa là @id vừa là khoá ngoại của 1 quan hệ, hoặc field nằm trong @@id ghép)
-- -> BaseRecord.id() không tìm được property nào isId() -> throw "does not have an id property"
-- -> 500 ở list action AdminJS cho UserSettings và EggRarityWeight.
--
-- Tách khoá chính thật (`id`, không liên quan quan hệ) ra khỏi cột vốn đang gánh 2 vai trò
-- (khoá chính + khoá ngoại/khoá ghép). Cột cũ giữ nguyên, chỉ đổi ràng buộc unique để không phá
-- vỡ nghiệp vụ hiện có (SettingsService.get/update dùng `where: { userId }`,
-- EggsService.loadWeightedDrops dùng `findMany({ where: { eggTypeId } })` — cả 2 đều không đụng
-- tới khoá chính nên không cần sửa code, chỉ sửa schema + migration).
--
-- Không dùng DB-level DEFAULT cho cột "id" (Prisma `@default(uuid())` là default phía
-- application/query-engine, không phải DB) — nên backfill tay bằng gen_random_uuid() (có sẵn
-- trong Postgres 13+ core, không cần cài thêm extension) cho các dòng hiện có trước khi bắt
-- buộc NOT NULL.

-- ============ user_settings ============
ALTER TABLE "user_settings" ADD COLUMN "id" TEXT;
UPDATE "user_settings" SET "id" = gen_random_uuid()::text WHERE "id" IS NULL;
ALTER TABLE "user_settings" ALTER COLUMN "id" SET NOT NULL;

ALTER TABLE "user_settings" DROP CONSTRAINT "user_settings_pkey";
ALTER TABLE "user_settings" ADD CONSTRAINT "user_settings_pkey" PRIMARY KEY ("id");

CREATE UNIQUE INDEX "user_settings_userId_key" ON "user_settings"("userId");

-- ============ egg_rarity_weights ============
ALTER TABLE "egg_rarity_weights" ADD COLUMN "id" TEXT;
UPDATE "egg_rarity_weights" SET "id" = gen_random_uuid()::text WHERE "id" IS NULL;
ALTER TABLE "egg_rarity_weights" ALTER COLUMN "id" SET NOT NULL;

ALTER TABLE "egg_rarity_weights" DROP CONSTRAINT "egg_rarity_weights_pkey";
ALTER TABLE "egg_rarity_weights" ADD CONSTRAINT "egg_rarity_weights_pkey" PRIMARY KEY ("id");

CREATE UNIQUE INDEX "egg_rarity_weights_eggTypeId_rarity_key" ON "egg_rarity_weights"("eggTypeId", "rarity");
