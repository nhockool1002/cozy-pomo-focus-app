# CozyPomo — Backend

NestJS + Prisma + PostgreSQL. Xem [`../docs/technical-spec.md`](../docs/technical-spec.md) §4 và [`../docs/setup-checklist.md`](../docs/setup-checklist.md).

## Chạy local

```bash
cp .env.example .env        # sửa DATABASE_URL/JWT_SECRET/ADMIN_*... theo máy bạn
docker compose up -d        # api + postgres, api tự chạy migration khi container start
npm run prisma:seed         # (tuỳ chọn) nạp 175 loài + 4 trứng + shop + 10 tài khoản tester
# hoặc chạy ngoài Docker:
npm install
npm run prisma:migrate:dev
npm run start:dev
```

- Swagger UI: `http://localhost:3000/docs`
- Health check: `http://localhost:3000/health`
- **Trang quản trị: `http://localhost:3000/admin`** — đăng nhập bằng `ADMIN_EMAIL`/`ADMIN_PASSWORD` trong `.env`
- Auth: `POST /api/v1/auth/register`, `/login`, `/refresh`, `GET /api/v1/auth/me` (Bearer token)

## Dữ liệu demo (`npm run prisma:seed`)

Xoá sạch và nạp lại: 175 loài (khớp Creature Atlas), 4 loại trứng (Rừng/Biển/Hoa/Bí Ẩn — Bí Ẩn có tỉ lệ nhỏ ra Thần Thú SSR), `rarity_weights`, 10 vật phẩm cửa hàng, và **10 tài khoản tester** `tester01@cozypomo.dev` … `tester10@cozypomo.dev`, mật khẩu chung `Tester123!` — mỗi tài khoản có 14–32 phiên tập trung, lịch sử Xu Lá, bộ sưu tập và kho đồ thật (không phải chuỗi rỗng). Chỉ chạy trên DB dev/demo — script xoá sạch dữ liệu trước khi nạp lại.

**Đã hoàn chỉnh (test end-to-end thật với Postgres + Docker, không phải chỉ compile được):**
- Toàn bộ module API: Auth, Species, Eggs (kèm xem tỉ lệ nở `/egg-types/:id/odds`), Sessions (tạo/hoàn thành có roll loài + cộng Xu + cập nhật bộ sưu tập/hoàn thành idempotent, bỏ cuộc), Collection, Currency, Shop (mua vật phẩm — trứng cộng dồn số lượng, bình/nhạc chỉ mua 1 lần), Stats, Settings, Sync (outbox đồng bộ offline, idempotent theo `clientEventId`)
- **AdminJS** tại `/admin` — theme màu/branding CozyPomo riêng (xem `src/admin/admin.module.ts` → `branding.theme`); quản lý Species/EggType/EggDropEntry/RarityWeight/ShopItem (CRUD đầy đủ), xem User/Session/LedgerEntry/CollectionEntry/InventoryItem (chỉ đọc, sửa phải qua API để giữ đúng nghiệp vụ)
- Resource **Species** có giao diện riêng (`src/admin/components/`): trang List dạng lưới thẻ có ảnh SVG sinh theo archetype/palette (cùng thuật toán Creature Atlas) + lọc theo nhóm/cấp bậc + tìm kiếm; trang Show có ảnh lớn + layout gọn thay vì form mặc định
- Seed script tạo dữ liệu demo phong phú cho 10 tài khoản tester

**Lưu ý deploy:** Dockerfile **cố ý không set** `NODE_ENV=production` — ở chế độ đó AdminJS phục vụ `components.bundle.js` như file tĩnh được ghi bất đồng bộ sau khi server đã nhận request, gây 404 chập chờn ngay sau khi container khởi động (đã tự phát hiện và fix qua test thật, xem comment trong `Dockerfile`).

**Còn TODO:** viết lore cho từng loài (hiện để trống), session store cho AdminJS đang dùng MemoryStore (cảnh báo không hợp production — cân nhắc `connect-pg-simple`/Redis khi lên production thật), test tự động (unit/e2e).
