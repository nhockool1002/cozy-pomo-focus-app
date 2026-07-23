# CozyPomo — Plan & Trạng thái dự án

> Tài liệu này theo dõi tiến độ toàn dự án ở mức task cụ thể. Cập nhật thủ công mỗi khi có thay đổi lớn — không tự động sinh. Tham chiếu: [`docs/technical-spec.md`](docs/technical-spec.md) (kiến trúc/Screen List/Function List), [`docs/setup-checklist.md`](docs/setup-checklist.md) (checklist hạ tầng), [`docs/deploy_backend.md`](docs/deploy_backend.md) (hướng dẫn deploy).
>
> Tác giả sản phẩm: Dev1002. Cập nhật lần cuối: 2026-07-23.

## Tổng quan tình trạng

| Mảng | Trạng thái | Ghi chú |
|---|---|---|
| Backend API (NestJS) | ✅ Hoàn chỉnh, đã test thật | Toàn bộ endpoint theo Function List đã có |
| Trang quản trị AdminJS | ✅ Hoàn chỉnh, đã Việt hoá + brand hoá | Species/EggType có UI card riêng, có trang liệt kê API |
| Dữ liệu seed | ✅ 175 loài + 4 loại trứng + 10 tài khoản tester | `backend/prisma/seed.ts` |
| CI/CD workflows | ✅ Đã viết, ⬜ chưa test chạy thật trên server | Cần Secrets + xử lý ghcr visibility, xem `docs/deploy_backend.md` |
| Branding (icon/favicon/logo) | ✅ Hoàn chỉnh | `docs/branding/` |
| Android app | 🟥 Chỉ mới scaffold điều hướng | Toàn bộ 13 màn hình trong Screen List còn là placeholder |
| Test suite (backend + Android) | ⬜ Chưa có | 0 file test ở cả 2 phía |
| Hạ tầng thật (server/domain/SSL) | ⬜ Việc của bạn, Claude không có quyền truy cập | Theo `docs/deploy_backend.md` |
| Có thay đổi chưa commit | 🟡 Có | Xem mục "Việc cần dọn ngay" bên dưới |

---

## 🟡 Việc cần dọn ngay (ưu tiên cao nhất, làm trước mọi task khác)

### T-000. Commit các thay đổi phiên làm việc gần nhất
**Mô tả:** Các file sau đang ở trạng thái uncommitted trong working tree — cần review rồi commit trước khi tiếp tục, tránh mất việc hoặc lẫn lộn với các thay đổi tiếp theo:
- Sửa: `app/app/src/main/res/drawable/ic_launcher_background.xml`, `ic_launcher_foreground.xml` (icon app thật thay placeholder)
- Sửa: `backend/src/admin/admin.module.ts`, `main.ts`, `admin/components/SpeciesList.tsx`, `SpeciesShow.tsx`, `SpeciesThumbnail.tsx`, `species-art.ts` (hiệu ứng vầng hào quang theo wireframe, brand hoá, Việt hoá)
- Mới: `backend/src/admin/admin-i18n.ts`, `components/ApiExplorer.tsx`, `components/EggTypeList.tsx`, `components/egg-art.ts`, `backend/public/` (favicon+logo), `docs/branding/`
- Sửa: `docs/setup-checklist.md` (bổ sung mục ghcr.io package visibility)
**Việc cần làm:** Review diff (`git status`, `git diff`), xác nhận không dính secret, rồi commit theo từng nhóm logic hoặc gộp 1 commit tuỳ bạn.

---

## ✅ Đã hoàn thành

### Backend (`backend/`)
- **T-001 — Scaffold NestJS + Prisma + Docker.** Cấu trúc module chuẩn Nest, kết nối Postgres qua Prisma, `docker-compose.yml` cho dev local.
- **T-002 — Auth module.** Đăng ký/đăng nhập email+mật khẩu, JWT access + refresh token, endpoint `/auth/register` `/auth/login` `/auth/refresh` `/auth/me`.
- **T-003 — Species module.** `GET /species`, `GET /species/:id`, lọc theo category/rarity — phục vụ `CollectionRepository.getCollection`/`getSpeciesDetail` phía Android.
- **T-004 — Eggs module.** `GET /egg-types`, `GET /egg-types/:id`, `GET /egg-types/:id/odds` (tỉ lệ nở) — phục vụ `EggRepository`.
- **T-005 — Sessions module.** `POST /sessions` (bắt đầu phiên), `PATCH /sessions/:id/complete`, `PATCH /sessions/:id/give-up`, `GET /sessions` — phục vụ `TimerRepository`, có random rarity-roll theo `rarity_weights`/`egg_drop_table`.
- **T-006 — Currency module.** `GET /currency/balance`, `GET /currency/ledger` — sổ giao dịch Xu Lá, cộng/trừ nội bộ khi hoàn thành phiên hoặc mua hàng.
- **T-007 — Collection module.** `GET /collection`, `GET /collection/progress`, `PATCH /collection/:speciesId/favorite`.
- **T-008 — Shop module.** `GET /shop-items`, `POST /shop-items/:id/purchase`, `GET /inventory`, `PATCH /inventory/:id/equip`.
- **T-009 — Stats module.** `GET /stats/daily`, `/stats/range`, `/stats/summary` — phục vụ màn Thống kê (S-06).
- **T-010 — Settings module.** `GET`/`PATCH /settings` — thời gian focus/break mặc định, Strict Mode, sound theme.
- **T-011 — Sync module.** `POST /sync/batch` — nhận outbox từ app khi mạng chập chờn/offline→online.
- **T-012 — Health check.** `GET /health`, không cần JWT, không bị `setGlobalPrefix` chặn — dùng để healthcheck từ ngoài.
- **T-013 — Swagger/OpenAPI.** Tự sinh tại `/docs` (UI) và `/docs-json` (raw spec) từ decorator `@ApiTags`/`@ApiBearerAuth` trong code — luôn khớp thực tế.
- **T-014 — Seed data.** `backend/prisma/seed.ts`: 175 loài (50 Thú rừng/50 Sinh vật biển/50 Thực vật/25 Thần thú SSR), 4 loại trứng + bảng tỉ lệ rơi, vật phẩm cửa hàng, 10 tài khoản tester có lịch sử phiên/Xu Lá/bộ sưu tập giả lập.
- **T-015 — Dockerfile production.** Multi-stage build, entrypoint tự chạy `prisma migrate deploy` trước khi start server — không cần thao tác tay khi deploy.

### Trang quản trị AdminJS (`backend/src/admin/`)
- **T-016 — Species List/Show tuỳ biến.** Lưới thẻ loài giống wireframe: vầng hào quang theo cấp bậc (B/A/S/SS/SSR), hiệu ứng bay bổng/xoay/chớp sáng cho SSR ("rực lửa" — quầng sáng mix-blend screen, vòng xoay 2 chiều, tia lửa đa màu), badge cấp bậc, bộ lọc theo danh mục/cấp bậc, tìm kiếm theo tên. File chính: `species-art.ts` (bộ sinh SVG + CSS hiệu ứng dùng chung), `SpeciesList.tsx`, `SpeciesShow.tsx`, `SpeciesThumbnail.tsx`.
- **T-017 — EggType List tuỳ biến.** Lưới thẻ trứng cùng ngôn ngữ hình ảnh với Species: icon trứng sinh theo `colorHex` riêng từng loại, vầng hào quang theo mốc giá (Thường/Hiếm/Huyền thoại). File: `egg-art.ts`, `EggTypeList.tsx`.
- **T-018 — Việt hoá toàn bộ giao diện AdminJS.** `admin-i18n.ts` — dịch nhãn điều hướng, nút bấm, thông báo lỗi, nhãn thuộc tính + giá trị enum cho toàn bộ 11 resource (kể cả bảng mặc định như Session/LedgerEntry), trang đăng nhập, dashboard chào mừng.
- **T-019 — Trang "API cho App".** `ApiExplorer.tsx` — custom page trong sidebar, đọc `/docs-json` để liệt kê toàn bộ endpoint (method, path, tag, có cần JWT hay không), có filter theo nhóm tính năng + tìm kiếm, nút mở Swagger UI để thử request thật.
- **T-020 — Branding trang quản trị.** Favicon + logo thật (bình ấp trứng phát sáng, đúng bảng màu chính thức) thay vì mặc định AdminJS — wiring qua `useStaticAssets` trong `main.ts` + `branding.favicon`/`branding.logo` trong `admin.module.ts`.

### Branding (`docs/branding/`)
- **T-021 — Icon mark + logo ngang.** `icon-mark.svg` (bình ấp trứng, đã kiểm tra nằm gọn safe-zone 66dp adaptive icon), `logo-horizontal.svg` (icon + wordmark "CozyPomo", font Arial Rounded MT Bold). Export sẵn: favicon multi-size, apple-touch-icon, icon 512/1024 (Play Store hi-res, opaque).
- **T-022 — Android adaptive icon thật.** Thay `ic_launcher_background.xml`/`ic_launcher_foreground.xml` placeholder bằng path data thật port từ icon mark.

### Hạ tầng / DevOps
- **T-023 — Repo GitHub + cấu trúc monorepo.** `app/` `backend/` `docs/`, đã push nhánh `main`.
- **T-024 — `.github/workflows/android-release.yml`.** Kích hoạt bởi tag `app-v*`, build AAB+APK ký sẵn.
- **T-025 — `.github/workflows/backend-deploy.yml`.** Kích hoạt bởi tag `backend-v*` hoặc chạy tay, build+push Docker image lên ghcr.io, SSH deploy lên aaPanel.
- **T-026 — Tài liệu vận hành.** `docs/setup-checklist.md`, `docs/deploy_backend.md` (hướng dẫn chi tiết từng bước deploy backend, bao gồm cả gotcha ghcr.io package visibility).

### Android scaffold (`app/`)
- **T-027 — Khung điều hướng.** Compose + Hilt + Navigation Compose, 4 tab Bottom Nav (Trang chủ/Khu rừng/Cửa hàng/Thống kê), theme đúng Brand Guide (`ui/theme/`).
- **T-028 — Tầng network.** Retrofit `ApiService`, `AuthInterceptor` tự gắn JWT, `TokenProvider`, DI qua Hilt (`data/network/di/NetworkModule.kt`).

---

## ⬜ Chưa làm

### Nhóm A — Android app: xây từng màn hình thật (việc lớn nhất còn lại)

Hiện tại 4 file màn hình (`HomeScreen.kt`, `ForestScreen.kt`, `ShopScreen.kt`, `StatsScreen.kt`) mỗi file chỉ 12 dòng, gọi chung 1 `PlaceholderScreen` hiển thị tiêu đề + mô tả — chưa có UI, chưa có ViewModel, chưa có data layer nào phía app (ngoài tầng network gọi API thô). Cần xây theo đúng Screen List trong `docs/technical-spec.md` mục 2, và các hàm Repository ở mục 3.

- **T-029 — Room DB local-first.** Tạo schema Room phản chiếu dữ liệu cần cache offline (session đang chạy, inventory trứng, collection đã mở khoá, settings) — nền tảng bắt buộc trước khi làm bất kỳ ViewModel nào, vì kiến trúc đề ra là local-first + đồng bộ nền qua WorkManager (xem `SyncRepository` mục 3.8 và bộ nhớ dự án `cozypomo-db-recommendation`).
- **T-030 — TimerRepository + Foreground Service (S-01).** Đếm ngược chính xác kể cả khi khoá màn hình (dùng `SystemClock.elapsedRealtime()`, không dùng `Handler.postDelayed` đơn thuần vì trôi giờ), các hàm `startSession`/`giveUpSession`/`completeSession`/`observeActiveSession`. Đây là lõi của toàn bộ app — nên làm đầu tiên trong nhóm A.
- **T-031 — Màn Trang chủ/Timer (S-01) UI thật.** Bình ấp trên giá gỗ, đồng hồ đếm ngược lớn, slider 10–120 phút, nút Bắt đầu → đổi thành nút đỏ "Bỏ cuộc" khi đang chạy, nút "+" mở popup chọn trứng.
- **T-032 — Popup chọn Trứng/Hạt giống (S-02).** Danh sách trứng đã sở hữu (từ `EggRepository.getOwnedEggs`), trạng thái khoá nếu chưa mua.
- **T-033 — Dialog xác nhận Bỏ cuộc.** Cảnh báo mất tiến trình (Strict Mode), 2 nút Huỷ/Đồng ý.
- **T-034 — Hoạt ảnh nở trứng (S-01a).** Animation nở khi timer về 0, reveal loài mới (gọi `TimerRepository.completeSession` → `EggRepository.rollHatchResult`), hiện số Xu Lá nhận được, điều hướng sang "Xem khu rừng" hoặc quay lại S-01.
- **T-035 — Màn Khu rừng/Bộ sưu tập (S-04) UI thật.** Tabs lọc Tất cả/Thú rừng/Sinh vật biển/Thực vật, lưới thẻ loài + badge cấp bậc (gọi `CollectionRepository.getCollection`), thẻ chưa mở khoá hiện bóng đen + dấu "?".
- **T-036 — Chi tiết loài/Lore (S-03).** Popup khi chạm thẻ đã mở khoá — ảnh loài, tên, cấp bậc, lore text, nút yêu thích (`toggleFavorite`).
- **T-037 — Màn Cửa hàng (S-05) UI thật.** Tabs danh mục (Trứng mới/Bình thuỷ tinh/Nhạc nền), danh sách item với trạng thái nút theo số dư Xu Lá (`ShopRepository.getShopItems`), dialog xác nhận mua trước khi `purchaseItem`.
- **T-038 — Màn Thống kê (S-06) UI thật.** Biểu đồ ngày/tuần/tháng (`StatsRepository.getDailyStats`/`getRangeStats`), streak, tổng phiên thành công/thất bại.
- **T-039 — Màn Cài đặt (S-07) + Sao lưu & Đồng bộ (S-07a).** Cấu hình thời gian mặc định, Strict Mode, âm thanh (`SettingsRepository`); kết nối Google Drive, xem thời điểm sao lưu gần nhất, nút "Sao lưu ngay"/"Khôi phục" (`SyncRepository`).
- **T-040 — Splash + Onboarding (S-00, S-00b).** Làm nóng Room DB/DataStore lúc khởi động; 2-3 slide giới thiệu cơ chế ấp trứng + xin quyền thông báo (chỉ hiện lần đầu).
- **T-041 — Màn Đăng nhập/Đăng ký.** Chưa có trong Screen List gốc nhưng bắt buộc phải có trước khi gọi được API cần JWT — form gọi `/auth/register` `/auth/login`, lưu token qua `TokenProvider` đã có sẵn ở tầng network.
- **T-042 — SoundManager/NotificationManager.** Nhạc nền lofi/mưa qua Media3, tiếng "ting" hoàn thành qua SoundPool, thông báo khi màn hình khoá lúc hết giờ (mục 3.9 technical-spec).
- **T-043 — WorkManager đồng bộ offline→online.** Hàng đợi outbox cho phiên/giao dịch gửi thất bại lúc mất mạng, gọi `POST /sync/batch` khi có mạng lại.

### Nhóm B — Backend polish (nhỏ, không chặn deploy)

- **T-044 — Thêm `@ApiOperation({ summary })` cho từng endpoint.** Hiện Swagger/trang "API cho App" đang hiện tên hàm (`AuthController_login`) thay vì mô tả tiếng Việt dễ hiểu — cải thiện trải nghiệm người tích hợp app.
- **T-045 — Test suite backend.** Hiện 0 file `.spec.ts`. Ít nhất nên có unit test cho: rarity-roll (`egg_drop_table`/`rarity_weights`), currency ledger (không cho phép số dư âm), auth guard.

### Nhóm C — Test suite Android

- **T-046 — Test tầng logic.** Theo tech-spec đề xuất JUnit5 + Turbine (test Flow) cho Repository, đặc biệt `TimerRepository` (đếm giờ dễ lệch) và rarity-roll phía app nếu có logic client-side.
- **T-047 — Compose UI Test + Robolectric.** Cho các màn hình sau khi build xong ở Nhóm A.

### Nhóm D — Hạ tầng thật (việc của bạn, Claude không có quyền truy cập)

Chi tiết từng bước đã có ở [`docs/deploy_backend.md`](docs/deploy_backend.md) và [`docs/setup-checklist.md`](docs/setup-checklist.md), tóm tắt:

- **T-048 — GitHub Secrets cho backend deploy.** `DEPLOY_HOST`/`DEPLOY_USER`/`DEPLOY_SSH_KEY`/`DEPLOY_PATH`.
- **T-049 — Xử lý ghcr.io package visibility.** Đổi Public hoặc `docker login` trên server — nếu bỏ qua, `docker compose pull` sẽ lỗi 401.
- **T-050 — Cài aaPanel + Docker trên VPS thật.**
- **T-051 — Tạo `.env` production trên server** với secret thật (JWT, admin, DB password) — không copy `.env.example` nguyên văn.
- **T-052 — Domain + SSL cho API.** Trỏ A record, tạo site aaPanel, bật Let's Encrypt, Reverse Proxy vào `127.0.0.1:3000`.
- **T-053 — GitHub Secrets cho Android release.** `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` — cần có keystore `.jks` release trước (tự tạo bằng `keytool`, giữ bí mật, mất là không update được app cũ trên Play Store).
- **T-054 — Test trigger cả 2 workflow thật** trên server/tài khoản Play Console thật.

### Nhóm E — Chuẩn bị phát hành Play Store

- **T-055 — Feature graphic + ảnh chụp màn hình thật.** Cần Android app chạy được (sau Nhóm A) mới chụp được.
- **T-056 — Chính sách bảo mật (Privacy Policy).** Bắt buộc với Play Console, đặc biệt vì app có lưu dữ liệu người dùng + đồng bộ Google Drive.
- **T-057 — Data safety form trên Play Console.** Khai báo rõ việc lưu tiến trình qua Google Drive, loại dữ liệu thu thập.
- **T-058 — Content rating.** Chọn mức phù hợp (dự kiến mọi lứa tuổi).

---

## Đề xuất thứ tự ưu tiên tiếp theo

1. **T-000** — commit dọn dẹp (vài phút, tránh mất việc)
2. **T-029 → T-030 → T-031** — Room DB + TimerRepository + màn Trang chủ thật (lõi sản phẩm, mọi màn khác phụ thuộc vào đây)
3. **T-041** — màn Đăng nhập (chặn mọi API cần JWT)
4. Các màn còn lại theo thứ tự Screen List (T-032 → T-039)
5. Song song lúc rảnh: **T-044** (Swagger summary), **T-048–T-052** (hạ tầng thật, bạn tự làm theo `docs/deploy_backend.md`)
6. Test suite (Nhóm B/C) và chuẩn bị Play Store (Nhóm E) làm cuối, khi app đã chạy được end-to-end
