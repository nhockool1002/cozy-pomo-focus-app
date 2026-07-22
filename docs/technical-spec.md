# CozyPomo — Technical Spec

Tác giả: Dev1002 · Nền tảng: Android (native) · Cập nhật: 2026-07-22

Tài liệu này gồm 4 phần: Tech Stack Android, Screen List, Function List, và Backend (NodeJS + Docker Compose + aaPanel).

**Kiến trúc đã chốt (v2 — thay v1 local-only):** app vẫn phải đếm giờ chính xác 100% offline (Foreground Service, không đổi), nhưng kết quả phiên/Xu Lá/bộ sưu tập giờ đồng bộ lên **backend NodeJS tự triển khai** để có trang quản trị (admin) và dữ liệu tài khoản không còn phụ thuộc Google Drive. Room vẫn là cache/ghi-trước offline; backend là nguồn sự thật (source of truth) khi có mạng. Vì vậy Function List ở mục 3 vừa là hợp đồng nội bộ Kotlin, vừa map trực tiếp sang REST endpoint thật ở mục 4.

---

## 1. Tech Stack đề xuất (Android native, hiện đại nhất 2025+)

App 100% Android native — không cần Flutter/KMP/React Native vì yêu cầu cốt lõi (đếm ngược chính xác khi khoá màn hình, tối ưu pin, Foreground Service) cần quyền truy cập hệ thống sâu mà framework cross-platform chỉ mô phỏng lại, không cần thiết khi chỉ nhắm 1 nền tảng.

| Hạng mục | Lựa chọn | Vì sao |
|---|---|---|
| Ngôn ngữ | **Kotlin** (100%) | Chuẩn chính thức của Android, coroutines native |
| UI Toolkit | **Jetpack Compose** + Material 3 | Khai báo UI hiện đại, animation mượt cho hiệu ứng nở trứng/hào quang, thay thế hoàn toàn XML View |
| Kiến trúc | **MVVM + Clean Architecture** (presentation / domain / data), Unidirectional Data Flow | Tách timer logic khỏi UI, dễ test, dễ mở rộng thêm màn hình |
| Async / State | **Kotlin Coroutines + Flow / StateFlow** | Đếm ngược, lắng nghe số dư Xu Lá, tiến trình phiên theo thời gian thực |
| Dependency Injection | **Hilt** | DI chuẩn Google, tích hợp sẵn với ViewModel/WorkManager/Service |
| Local DB | **Room (SQLite)** + KSP | Khớp với đề xuất DB local-first đã lưu trong bộ nhớ dự án |
| Key-value/Settings | **Jetpack DataStore** (Preferences) | Thay `SharedPreferences`, an toàn kiểu dữ liệu, hỗ trợ Flow |
| Nền/Đồng bộ | **WorkManager** | Sao lưu định kỳ lên Google Drive `appDataFolder`, lên lịch nhắc nhở |
| Đếm giờ chính xác nền | **Foreground Service** dùng `SystemClock.elapsedRealtime()` (không dùng `Handler.postDelayed` đơn thuần vì có thể trôi giờ) | Bắt buộc để đếm ngược đúng khi màn hình khoá — đúng yêu cầu NFR "hoạt động ngầm" |
| Điều hướng | **Navigation Compose** | Điều hướng khai báo giữa 4 tab + các popup/dialog |
| Animation nở trứng | **Compose animation APIs** (`AnimatedContent`, `Canvas`) + **Lottie for Compose** cho hiệu ứng phức tạp (pháo hoa, hào quang SSR) | Cân bằng giữa hiệu năng và độ chi tiết hoạt ảnh |
| Âm thanh | **Media3 (ExoPlayer)** cho nhạc nền lofi/mưa dài hơi, `SoundPool` cho tiếng "ting" ngắn | Media3 quản lý vòng đời phát nhạc nền tốt hơn `MediaPlayer` cũ |
| Widget màn hình khoá/home | **Glance** (Jetpack Compose for App Widgets) | Nếu sau này muốn thêm widget "phiên đang chạy" ngoài home screen |
| Đăng nhập | Form đăng ký/đăng nhập **email + mật khẩu** thường, gọi `POST /auth/register` và `/auth/login` (mục 4.4) → nhận JWT. **Sign in with Google** (qua Credential Manager) để sau, schema đã chừa sẵn chỗ | Ra mắt nhanh hơn, không phụ thuộc cấu hình Google Cloud Console ngay từ đầu |
| Gọi API backend | **Retrofit** + **OkHttp** (interceptor gắn JWT, retry/backoff) + **kotlinx.serialization** | Retrofit interface có thể sinh tự động từ OpenAPI spec do NestJS xuất ra (mục 4) |
| Đồng bộ offline→online | **WorkManager** (hàng đợi outbox: phiên/giao dịch chưa gửi được sẽ retry khi có mạng) | Đảm bảo phiên hoàn thành lúc mất mạng vẫn lên được backend sau |
| Build system | **Gradle Kotlin DSL** + **Version Catalog** (`libs.versions.toml`) | Quản lý phiên bản thư viện tập trung, chuẩn hiện tại của Google |
| Static analysis | **detekt** + **ktlint** | Giữ code style nhất quán khi dự án lớn dần |
| Testing | **JUnit5**, **Turbine** (test Flow), **Compose UI Test**, **Robolectric** | Test riêng logic đếm giờ/rarity-roll vốn dễ sai lệch |
| CI/CD | **GitHub Actions** + Gradle build cache, publish qua **Play Console API** | Tự động build/test mỗi PR, tự động release lên Internal Testing track |
| Modularization | Bắt đầu single-module; tách `core-ui` / `core-data` / `feature-*` khi codebase vượt ~15-20 màn hình | Tránh over-engineer từ ngày đầu, nhưng chuẩn bị sẵn ranh giới rõ ràng (đã chia theo Function List bên dưới) |
| compileSdk / minSdk | compileSdk mới nhất hiện hành (Android 15 / API 35), minSdk 26–28 | minSdk 26 mở khoá `JobScheduler`/notification channel ổn định; cân bằng độ phủ thiết bị và API hiện đại |

---

## 2. Screen List

| ID | Màn hình | Mục đích | Vào từ | Thành phần chính | Điều hướng tới |
|---|---|---|---|---|---|
| S-00 | Splash | Khởi động, làm nóng Room DB/DataStore | Mở app | Logo bình + tên app | S-01 (hoặc S-00b nếu lần đầu) |
| S-00b | Onboarding (lần đầu) | Giới thiệu cơ chế ấp trứng, xin quyền thông báo | Sau Splash (chỉ lần đầu) | 2-3 slide, nút "Bắt đầu" | S-01 |
| S-01 | Trang chủ / Timer | Thiết lập & chạy phiên tập trung | Splash, Bottom nav | Bình ấp, đồng hồ đếm ngược, slider thời gian, nút Bắt đầu/Bỏ cuộc | S-02 (chọn trứng), S-01a (khi hết giờ), Dialog xác nhận bỏ cuộc |
| S-02 | Popup chọn Trứng/Hạt giống | Chọn loại trứng cho phiên sắp chạy | Nút "+" ở S-01 | Danh sách trứng đã sở hữu, trạng thái khoá nếu chưa mua | Đóng popup → S-01 |
| — | Dialog xác nhận Bỏ cuộc | Cảnh báo mất tiến trình khi thoát giữa chừng (Strict Mode) | Nút "Bỏ cuộc" ở S-01 | Text cảnh báo, 2 nút Huỷ/Đồng ý | S-01 (huỷ) hoặc S-01 reset (đồng ý) |
| S-01a | Hoạt ảnh nở trứng | Ăn mừng hoàn thành phiên, lộ diện loài mới | Timer về 0 tại S-01 | Animation nở + reveal loài + số Xu Lá nhận được | S-04 ("Xem khu rừng") hoặc quay lại S-01 |
| S-03 | Chi tiết loài / Lore | Xem thông tin, câu chuyện vui về 1 loài | Chạm thẻ đã mở khoá ở S-04 | Ảnh loài, tên, cấp bậc, lore text, nút yêu thích | Đóng → S-04 |
| S-04 | Khu rừng / Bộ sưu tập | Ngắm toàn bộ loài đã/chưa mở khoá | Bottom nav | Tabs lọc (Tất cả/Rừng/Biển/Thực vật), lưới thẻ loài + badge cấp bậc | S-03, Bottom nav |
| S-05 | Cửa hàng | Tiêu Xu Lá mua trứng/bình/nhạc | Bottom nav | Tabs danh mục, danh sách item, trạng thái nút theo số dư | Dialog xác nhận mua, Bottom nav |
| — | Dialog xác nhận mua | Xác nhận trừ Xu Lá trước khi mua | Nút "Mua ngay" ở S-05 | Tên item, giá, số dư sau khi mua | Đóng → S-05 |
| S-06 | Thống kê | Xem tổng thời gian tập trung theo ngày/tuần/tháng | Bottom nav | Biểu đồ, streak, tổng phiên thành công/thất bại | Bottom nav |
| S-07 | Cài đặt | Cấu hình thời gian mặc định, Strict Mode, âm thanh, sao lưu | Icon ⚙ ở S-01 | Danh sách setting, toggle Strict Mode, mục "Sao lưu & Đồng bộ" | S-07a |
| S-07a | Sao lưu & Đồng bộ | Kết nối Google Drive, xem thời điểm sao lưu gần nhất | S-07 | Trạng thái kết nối, nút "Sao lưu ngay"/"Khôi phục" | S-07 |

---

## 3. Function List (tầng Repository/UseCase nội bộ)

Các "hàm API" dưới đây là ranh giới giữa ViewModel và tầng dữ liệu. Mỗi Repository giờ có 2 nguồn: **Room** (đọc/ghi tức thời, offline) và **backend NodeJS** (đồng bộ nền qua WorkManager). Ký hiệu kiểu Kotlin, `suspend`/`Flow` theo đúng thực tế bất đồng bộ. Endpoint REST tương ứng xem mục 4.4.

### 3.1 TimerRepository (FR01, FR03) — phiên Pomodoro
| Hàm | Tham số | Trả về | Mô tả |
|---|---|---|---|
| `startSession` | `durationMin: Int, eggTypeId: String, strictMode: Boolean` | `SessionId` | Tạo phiên mới, khởi động Foreground Service đếm giờ |
| `giveUpSession` | `sessionId: SessionId` | `Unit` | Đánh dấu phiên thất bại, kích hoạt "trứng vỡ" |
| `completeSession` | `sessionId: SessionId` | `HatchResult` | Gọi khi đếm về 0 — chấm dứt phiên, trigger roll trứng |
| `observeActiveSession` | — | `Flow<SessionState>` | Thời gian còn lại, trạng thái, để UI vẽ đồng hồ |
| `getSessionHistory` | `range: DateRange` | `List<SessionRecord>` | Phục vụ màn Thống kê |

### 3.2 EggRepository (FR02) — trứng & tỉ lệ nở
| Hàm | Tham số | Trả về | Mô tả |
|---|---|---|---|
| `getEggTypes` | — | `List<EggType>` | Danh sách loại trứng (Rừng/Biển/Hoa/Bí Ẩn) |
| `getOwnedEggs` | — | `Flow<List<OwnedEgg>>` | Tồn kho trứng đã mua, chưa dùng |
| `rollHatchResult` | `eggTypeId: String` | `Species` | Random có trọng số theo `egg_drop_table` + `rarity_weights` |
| `getEggStage` | `elapsedRatio: Float` | `EggStage` | Map % thời gian đã trôi → 1 trong 4 giai đoạn hiển thị |

### 3.3 CollectionRepository (FR05)
| Hàm | Tham số | Trả về | Mô tả |
|---|---|---|---|
| `getCollection` | `filter: CollectionFilter` | `Flow<List<CollectionEntry>>` | Dữ liệu cho lưới Khu rừng, có badge cấp bậc |
| `getSpeciesDetail` | `speciesId: String` | `SpeciesDetail` | Cho popup Lore |
| `toggleFavorite` | `speciesId: String` | `Unit` | Đánh dấu yêu thích |
| `getCollectionProgress` | — | `Flow<Progress>` | VD: "42/175 đã mở khoá" |

### 3.4 CurrencyRepository (FR06) — Xu Lá
| Hàm | Tham số | Trả về | Mô tả |
|---|---|---|---|
| `getBalance` | — | `Flow<Int>` | Số dư hiện tại (SUM từ ledger) |
| `earn` | `amount: Int, reason: LedgerReason, refSessionId: String?` | `Unit` | Cộng Xu sau phiên thành công |
| `spend` | `amount: Int, reason: LedgerReason, refItemId: String?` | `Result<Unit>` | Trừ Xu khi mua, fail nếu không đủ |
| `getLedger` | `range: DateRange` | `List<LedgerEntry>` | Lịch sử giao dịch, phục vụ đối soát |

### 3.5 ShopRepository (FR06)
| Hàm | Tham số | Trả về | Mô tả |
|---|---|---|---|
| `getShopItems` | `category: ShopCategory` | `Flow<List<ShopItem>>` | Danh sách item theo tab (Trứng/Bình/Nhạc) |
| `purchaseItem` | `itemId: String` | `Result<Unit>` | Gọi `CurrencyRepository.spend` rồi thêm vào inventory |
| `getInventory` | — | `Flow<List<OwnedItem>>` | Item đã sở hữu |
| `equipItem` | `itemId: String, slot: EquipSlot` | `Unit` | Đổi bình/nhạc đang dùng |

### 3.6 StatsRepository (FR07)
| Hàm | Tham số | Trả về | Mô tả |
|---|---|---|---|
| `getDailyStats` | `date: LocalDate` | `DailyStat` | Tổng phút tập trung trong ngày |
| `getRangeStats` | `start: LocalDate, end: LocalDate` | `List<DailyStat>` | Dữ liệu vẽ biểu đồ tuần/tháng |
| `getStreak` | — | `Int` | Số ngày liên tiếp có phiên thành công |
| `getTotalFocusMinutes` | — | `Long` | Tổng thời gian tập trung all-time |

### 3.7 SettingsRepository
| Hàm | Tham số | Trả về | Mô tả |
|---|---|---|---|
| `observeSettings` | — | `Flow<UserSettings>` | Toàn bộ cấu hình người dùng |
| `updateFocusDuration` | `minutes: Int` | `Unit` | Mặc định 25, khoảng 10–120 |
| `updateBreakDuration` | `minutes: Int` | `Unit` | Mặc định 5 |
| `setStrictMode` | `enabled: Boolean` | `Unit` | Bật/tắt cảnh báo thoát app |
| `setSoundTheme` | `themeId: String` | `Unit` | Đổi nhạc nền/âm thanh môi trường |

### 3.8 SyncRepository — sao lưu Google Drive
| Hàm | Tham số | Trả về | Mô tả |
|---|---|---|---|
| `exportBackup` | — | `Result<BackupBlob>` | Gom các bảng user-data thành 1 blob |
| `uploadToDrive` | `blob: BackupBlob` | `Result<Unit>` | Đẩy lên `appDataFolder` |
| `restoreFromDrive` | — | `Result<Unit>` | Kéo blob mới nhất về, nhập lại Room |
| `getLastSyncTime` | — | `Flow<Instant?>` | Hiển thị ở S-07a |

### 3.9 SoundManager / NotificationManager (service nội bộ, không phải Repository thuần)
| Hàm | Tham số | Trả về | Mô tả |
|---|---|---|---|
| `playAmbientTrack` | `trackId: String` | `Unit` | Nhạc nền lofi/mưa qua Media3 |
| `playCompletionChime` | — | `Unit` | Tiếng "ting" khi hết giờ |
| `scheduleSessionEndNotification` | `sessionId: SessionId, triggerAt: Instant` | `Unit` | Thông báo khi màn hình khoá |
| `cancelSessionNotification` | `sessionId: SessionId` | `Unit` | Huỷ khi người dùng bỏ cuộc sớm |

---

## 4. Backend (NodeJS + Docker Compose + aaPanel)

**Trả lời ngắn gọn: có, mô hình này rất chuẩn và phổ biến — hoàn toàn làm được.** Luồng đề xuất: GitHub Actions build image → đẩy lên container registry → SSH vào server chạy `docker compose pull && up -d`. aaPanel không cần tự "hiểu" CI/CD — nó chỉ cần Docker chạy sẵn và một site Nginx trỏ vào container, phần "auto deploy" do GitHub Actions đảm nhiệm.

### 4.1 Stack Backend đề xuất (hiện đại, hợp với NodeJS)

| Hạng mục | Lựa chọn | Vì sao |
|---|---|---|
| Framework | **NestJS** (TypeScript) | Kiến trúc module + DI giống Hilt bên Android → tư duy nhất quán 2 phía, có sẵn Guard/Interceptor cho auth |
| ORM | **Prisma** | Type-safe, migration rõ ràng, sinh client tự động khớp schema đã thiết kế (species, sessions, ledger...) |
| Database | **PostgreSQL 16** (container riêng trong compose) | Đồng nhất giữa local dev và production, không phụ thuộc DB có sẵn của aaPanel |
| Auth | **Email + mật khẩu** (`bcrypt` hash) + tự cấp **JWT** (access + refresh). Cột `auth_provider` (`local` / `google`) và `password_hash` (nullable) trong bảng `users` ngay từ đầu để thêm Google Sign-In sau không cần migrate lại | Ra mắt nhanh, không phụ thuộc Google Cloud Console; vẫn mở đường thêm Google sau này |
| Admin panel | **AdminJS** gắn thẳng vào NestJS/Prisma | Có UI quản trị CRUD (species, shop_items, rarity_weights, users) trong vài giờ setup thay vì tự viết frontend admin |
| API docs | **@nestjs/swagger** (OpenAPI) | Từ OpenAPI spec, sinh Retrofit client Kotlin bằng `openapi-generator` → 2 đầu Android/Backend luôn khớp field |
| Validation | `class-validator` + `class-transformer` | Chuẩn NestJS, chặn payload sai ngay tầng DTO |
| Cache/Queue (tuỳ chọn, thêm sau) | **Redis + BullMQ** | Chỉ cần khi có tác vụ nền (VD: xoay trứng sự kiện giới hạn thời gian) — không bắt buộc cho v1 |
| Container | **Docker** multi-stage build (`node:22-alpine`) | Image nhỏ, build cache nhanh |

### 4.2 docker-compose.yml (phác thảo)

```yaml
services:
  api:
    build: ./backend
    restart: unless-stopped
    env_file: .env
    ports:
      - "127.0.0.1:3000:3000"   # chỉ mở localhost — Nginx của aaPanel proxy vào đây
    depends_on:
      - db
  db:
    image: postgres:16-alpine
    restart: unless-stopped
    environment:
      POSTGRES_DB: cozypomo
      POSTGRES_USER: cozypomo
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - db_data:/var/lib/postgresql/data
    ports:
      - "127.0.0.1:5432:5432"   # chỉ cần nếu muốn truy cập DB từ ngoài để debug
volumes:
  db_data:
```

Chủ đích để `api` và `db` chỉ bind vào `127.0.0.1` — không mở port ra Internet trực tiếp; toàn bộ traffic vào qua site Nginx mà aaPanel tạo (aaPanel tự xin SSL Let's Encrypt miễn phí cho domain).

### 4.3 Luồng auto-deploy (đã triển khai)

Repo là monorepo (app + backend) nên trigger là **tạo GitHub Release với tag đặt tên theo quy ước**, không phải push thường — tránh một commit chạm `backend/` vô tình build/deploy production, và tách biệt lifecycle release của app Android (App Store/Play Store) khỏi backend.

1. Tạo Release trên GitHub với tag `backend-v1.0.0` (VD).
2. `.github/workflows/backend-deploy.yml` khớp điều kiện `startsWith(tag, 'backend-v')` → build Docker image từ `backend/Dockerfile` → đẩy lên **ghcr.io** (`ghcr.io/nhockool1002/cozy-pomo-focus-app/backend`), gắn tag theo tên release và `latest`.
3. Job kế tiếp SSH vào server bằng `DEPLOY_SSH_KEY` → `cd $DEPLOY_PATH && docker compose -f docker-compose.prod.yml pull && up -d`.
4. aaPanel: App Store cài **Docker**, tạo site (domain) trỏ Nginx reverse-proxy về `127.0.0.1:3000`, bật SSL miễn phí. aaPanel dùng để **xem log/trạng thái container và quản lý domain/SSL**, không chứa logic deploy.
5. Tương tự, tag `app-v1.0.0` kích hoạt `.github/workflows/android-release.yml` — build AAB + APK ký sẵn, đính kèm vào chính Release đó (không đụng tới server).

Cả hai workflow có `workflow_dispatch` để chạy thử tay (build không cần ký/không cần deploy thật) — dùng khi setup secrets lần đầu để xác minh workflow chạy được trước khi tạo release thật.

### 4.4 Endpoint REST tương ứng Function List (mục 3)

Base path `/api/v1`, auth bằng header `Authorization: Bearer <JWT>` (trừ `/auth/*`).

| Nhóm | Endpoint mẫu |
|---|---|
| Auth | `POST /auth/register` (email+mật khẩu), `POST /auth/login`, `POST /auth/refresh`. *(Sau này thêm `POST /auth/google` khi làm Sign in with Google)* |
| Sessions | `POST /sessions` (tạo), `PATCH /sessions/:id/complete`, `PATCH /sessions/:id/give-up`, `GET /sessions?from=&to=` |
| Eggs | `GET /egg-types`, `POST /eggs/:eggTypeId/roll` |
| Collection | `GET /collection`, `GET /species/:id`, `PATCH /collection/:speciesId/favorite` |
| Currency | `GET /currency/balance`, `GET /currency/ledger` |
| Shop | `GET /shop-items?category=`, `POST /shop-items/:id/purchase`, `GET /inventory` |
| Stats | `GET /stats/daily?date=`, `GET /stats/range?start=&end=` |
| Settings | `GET /settings`, `PATCH /settings` |
| Sync (outbox từ app) | `POST /sync/batch` — app gửi hàng loạt sự kiện tích lại lúc offline (phiên hoàn thành, giao dịch) theo thứ tự, backend xử lý idempotent theo `clientEventId` để tránh cộng Xu Lá 2 lần nếu gửi trùng |
| Admin | phục vụ qua UI AdminJS (`/admin`), không cần tự viết endpoint riêng |

### 4.5 Repo layout (monorepo)

```
cozy-pomo-focus-app/
├── app/        # Android Studio project (Kotlin, Jetpack Compose)
├── backend/    # NestJS + Prisma + docker-compose.yml
└── docs/       # tài liệu này + setup-checklist.md
```

### 4.6 Checklist chuẩn bị

Checklist đầy đủ (GitHub / aaPanel / domain, có thể tick theo tiến độ) đã tách ra [`docs/setup-checklist.md`](./setup-checklist.md).

---

**Liên quan:**
- Wireframe 3 màn hình chính (dùng ảnh loài thật): https://claude.ai/code/artifact/a69b43e2-3331-4573-8df5-3da7e297f196
- 175 loài + hệ rarity B/A/S/SS/SSR: https://claude.ai/code/artifact/9603829b-817f-4f3e-8b7b-16d6cb128647
- Brand Field Guide (màu chính thức, kiểu chữ, giọng nói thương hiệu): https://claude.ai/code/artifact/8956dbcb-2415-4be9-88a8-c29cbdf84c44
- Schema DB chi tiết hơn: bộ nhớ dự án `cozypomo-db-recommendation`
