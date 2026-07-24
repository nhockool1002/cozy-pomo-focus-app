# CozyPomo — Technical Spec

Tác giả: Dev1002 · Nền tảng: Android (native) · Cập nhật: 2026-07-24

Tài liệu này gồm 4 phần: Tech Stack Android, Screen List, Function List, và Backend (NodeJS + Docker Compose + aaPanel).

**Kiến trúc đã chốt (v2 — thay v1 local-only):** app vẫn phải đếm giờ chính xác 100% offline (Foreground Service, không đổi), nhưng kết quả phiên/Xu Lá/bộ sưu tập giờ đồng bộ lên **backend NodeJS tự triển khai** để có trang quản trị (admin) và dữ liệu tài khoản không còn phụ thuộc Google Drive. Room vẫn là cache/ghi-trước offline; backend là nguồn sự thật (source of truth) khi có mạng. Vì vậy Function List ở mục 3 vừa là hợp đồng nội bộ Kotlin, vừa map trực tiếp sang REST endpoint thật ở mục 4.

---

## 1. Tech Stack đề xuất (Android native, hiện đại nhất 2025+)

App 100% Android native — không cần Flutter/KMP/React Native vì yêu cầu cốt lõi (đếm ngược chính xác khi khoá màn hình, tối ưu pin, Foreground Service) cần quyền truy cập hệ thống sâu mà framework cross-platform chỉ mô phỏng lại, không cần thiết khi chỉ nhắm 1 nền tảng.

| Hạng mục | Lựa chọn | Vì sao |
|---|---|---|
| Ngôn ngữ | **Kotlin** (100%) | Chuẩn chính thức của Android, coroutines native |
| UI Toolkit | **Jetpack Compose** + Material 3 | Khai báo UI hiện đại, animation mượt cho hiệu ứng nở trứng/hào quang, thay thế hoàn toàn XML View |
| Font chữ | **Baloo 2** (variable, trục `wght` 400-800, subset `vietnamese` xác nhận qua Google Fonts METADATA.pb) | Bo tròn, "mập mạp", hợp không khí game hơn Nunito (dùng ban đầu ở T-071) |
| Kiến trúc | **MVVM + Clean Architecture** (presentation / domain / data), Unidirectional Data Flow | Tách timer logic khỏi UI, dễ test, dễ mở rộng thêm màn hình |
| Async / State | **Kotlin Coroutines + Flow / StateFlow** | Đếm ngược, lắng nghe số dư Xu Lá, tiến trình phiên theo thời gian thực |
| Dependency Injection | **Hilt** | DI chuẩn Google, tích hợp sẵn với ViewModel/WorkManager/Service |
| Local DB | **Room (SQLite)** + KSP | Khớp với đề xuất DB local-first đã lưu trong bộ nhớ dự án |
| Key-value/Settings | **Jetpack DataStore** (Preferences) | Thay `SharedPreferences`, an toàn kiểu dữ liệu, hỗ trợ Flow |
| Nền/Đồng bộ | **WorkManager** | Sao lưu định kỳ lên Google Drive `appDataFolder`, lên lịch nhắc nhở |
| Đếm giờ chính xác nền | **Foreground Service** dùng `SystemClock.elapsedRealtime()` (không dùng `Handler.postDelayed` đơn thuần vì có thể trôi giờ) | Bắt buộc để đếm ngược đúng khi màn hình khoá — đúng yêu cầu NFR "hoạt động ngầm" |
| Điều hướng | **Navigation Compose** | Điều hướng khai báo giữa 5 tab (Trang chủ/Khu rừng/Cửa hàng/Kho đồ/Thống kê) + các popup/dialog |
| Animation nở trứng | **Compose animation APIs** (`AnimatedContent`, `Canvas`) + **Lottie for Compose** cho hiệu ứng phức tạp (pháo hoa, hào quang SSR) | Cân bằng giữa hiệu năng và độ chi tiết hoạt ảnh |
| Âm thanh | **Media3 (ExoPlayer)** cho nhạc nền lofi/mưa dài hơi, `SoundPool` cho tiếng "ting" ngắn | Media3 quản lý vòng đời phát nhạc nền tốt hơn `MediaPlayer` cũ |
| Widget màn hình khoá/home | **Glance** (Jetpack Compose for App Widgets) | Nếu sau này muốn thêm widget "phiên đang chạy" ngoài home screen |
| Đăng nhập | Form đăng ký/đăng nhập **email + mật khẩu** thường, gọi `POST /auth/register` và `/auth/login` (mục 4.4) → nhận JWT access+refresh. `TokenAuthenticator` (OkHttp `Authenticator`) tự làm mới access token qua `POST /auth/refresh` khi gặp `401` — không cần đăng nhập lại mỗi 15 phút. **Sign in with Google** (qua Credential Manager) để sau, schema đã chừa sẵn chỗ | Ra mắt nhanh hơn, không phụ thuộc cấu hình Google Cloud Console ngay từ đầu |
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

> **Đã build thật, khác với bản thiết kế ban đầu ở vài điểm** (ghi chú "▶" trong cột Thành phần chính): kinh tế 2 tiền tệ (Xu Lá + Giờ tích luỹ, chọn 1 loại thưởng/phiên, tỉ giá 1:10), trứng ấp dần theo instance sở hữu riêng (không roll ngay khi hoàn thành), bottom nav 5 tab (thêm Kho đồ), và **S-07a (Sao lưu & Đồng bộ Google Drive) đã bị bỏ hẳn khỏi phạm vi** theo yêu cầu Dev1002 — không phải "chưa làm", mà là quyết định không làm.

| ID | Màn hình | Mục đích | Vào từ | Thành phần chính | Điều hướng tới |
|---|---|---|---|---|---|
| S-00 | Splash | Khởi động, làm nóng Room DB/DataStore | Mở app | Logo bình + tên app + số phiên bản + âm thanh chào mừng | S-01 (hoặc S-00b nếu lần đầu) |
| S-00b | Onboarding (lần đầu) | Giới thiệu cơ chế ấp trứng, xin quyền thông báo | Sau Splash (chỉ lần đầu) | 2-3 slide, nút "Bắt đầu" | S-01 |
| S-01 | Trang chủ / Timer | Thiết lập & chạy phiên tập trung | Splash, Bottom nav | Bình ấp (màu theo bình đang trang bị ở S-07b), đồng hồ đếm ngược, slider thời gian, ▶ card "Nhận thưởng bằng" (chọn Xu Lá **hoặc** Giờ tích luỹ + % dành cho ấp trứng nếu có chọn trứng), nút Bắt đầu/Bỏ cuộc | S-02 (chọn trứng), Dialog kết quả phiên (nở/đang ấp/không ấp/bỏ cuộc), Dialog xác nhận bỏ cuộc |
| S-02 | Popup chọn Trứng đang ấp | Chọn 1 trong các trứng ĐANG SỞ HỮU (hoặc không chọn) cho phiên sắp chạy | Nút "+" ở S-01 | Danh sách trứng sở hữu kèm tiến trình `đã ấp/tổng phút`, dòng "Không ấp trứng nào" luôn chọn được | Đóng popup → S-01 |
| — | Dialog xác nhận Bỏ cuộc | Cảnh báo mất tiến trình khi thoát giữa chừng (Strict Mode) | Nút "Bỏ cuộc" ở S-01 | Text cảnh báo, 2 nút Huỷ/Đồng ý | S-01 (huỷ) hoặc S-01 reset (đồng ý) |
| — | Dialog kết quả phiên | Báo kết quả khi hết giờ/bỏ cuộc — 4 nhánh nội dung | Timer về 0 hoặc Bỏ cuộc tại S-01 | ▶ 4 nhánh: **Nở trứng** (reveal loài + hào quang cấp bậc), **Đang ấp** (thanh tiến trình), **Không ấp trứng** (chỉ số tiền nhận), **Bỏ cuộc** (an ủi, trứng không đổi) | Đóng → S-01 |
| S-03 | Chi tiết loài / Lore | Xem thông tin, câu chuyện vui về 1 loài | Chạm thẻ đã mở khoá ở S-04 | Ảnh loài, tên, cấp bậc, lore text, nút yêu thích | Đóng → S-04 |
| S-04 | Khu rừng / Bộ sưu tập | Ngắm toàn bộ loài đã/chưa mở khoá | Bottom nav | Tabs lọc (Tất cả/Rừng/Biển/Thực vật), lưới thẻ loài + badge cấp bậc | S-03, Bottom nav |
| S-05 | Cửa hàng | Mua trứng (Xu Lá **hoặc** Giờ tích luỹ)/bình/nhạc | Bottom nav | Tabs danh mục, danh sách item (hiệu ứng bồng bềnh), trạng thái nút theo số dư | Dialog chọn tiền tệ (chỉ mục Trứng), Bottom nav |
| — | Dialog chọn tiền tệ mua trứng | Chọn trả bằng Xu Lá hay Giờ tích luỹ trước khi mua trứng | Nút "Mua ngay" ở S-05 (mục Trứng) | Giá theo cả 2 loại tiền, tự khoá lựa chọn nếu không đủ số dư | Đóng → S-05 |
| S-06 | Thống kê | Xem tổng thời gian tập trung + streak | Bottom nav | Thẻ streak, 2 ô phiên hoàn thành/bỏ cuộc (7 ngày), biểu đồ cột phút tập trung mỗi ngày | Bottom nav |
| S-07 | Cài đặt | Tài khoản, cấu hình thời gian mặc định/Strict Mode/âm thanh, phiên bản, đăng xuất | Icon ⚙ (dùng chung mọi tab, không riêng S-01 nữa) | Danh sách setting, toggle Strict Mode, chip chủ đề âm thanh, ID tài khoản (chạm để copy) | Bottom nav |
| S-07b | Kho đồ | Chọn bình ấp/nhạc nền đang dùng, xem tiến trình trứng sở hữu | Bottom nav | 3 tab (Bình/Trứng/Âm thanh), lưới thẻ 2 cột, thẻ đang dùng có viền + dấu tick | Bottom nav |
| ~~S-07a~~ | ~~Sao lưu & Đồng bộ~~ | **Đã bỏ khỏi phạm vi** theo yêu cầu Dev1002 | — | — | — |

---

## 3. Function List (tầng Repository/UseCase nội bộ)

Các "hàm API" dưới đây là ranh giới giữa ViewModel và tầng dữ liệu. Mỗi Repository giờ có 2 nguồn: **Room** (đọc/ghi tức thời, offline) và **backend NodeJS** (đồng bộ nền qua WorkManager). Ký hiệu kiểu Kotlin, `suspend`/`Flow` theo đúng thực tế bất đồng bộ. Endpoint REST tương ứng xem mục 4.4.

### 3.1 TimerRepository (FR01, FR03) — phiên Pomodoro

> **Đã đổi so với thiết kế ban đầu (economy v2/v3, T-068/T-077):** không còn "1 phiên = 1 lần roll ngay khi xong". Trứng ấp DẦN theo từng `OwnedEgg` sở hữu riêng (mua ở Cửa hàng), và phần phút không dành ấp trứng chỉ đổi ra **1 trong 2** loại tiền do người dùng chọn trước khi bắt đầu (không còn cộng cả 2 như bản đầu economy v2).

| Hàm | Tham số | Trả về | Mô tả |
|---|---|---|---|
| `startSession` | `durationMin: Int, ownedEggId: String?, incubationRatio: Float?, rewardCurrency: "COIN"\|"FOCUS_MINUTE", strictMode: Boolean` | `SessionId` | Tạo phiên mới, khởi động Foreground Service đếm giờ. `ownedEggId=null` = không ấp trứng nào |
| `giveUpSession` | `sessionId: SessionId` | `Unit` | Đánh dấu phiên thất bại — trứng đang ấp **không đổi**/không mất tiến trình, chỉ không nhận thưởng phiên này |
| `completeSession` | `sessionId: SessionId` | `SessionCompletionResult` (`Hatched` \| `Incubating` \| `NoEgg`, sealed) | Gọi khi đếm về 0 — chia phút giữa ấp trứng/quy đổi tiền theo `incubationRatio`, trả đúng 1 trong 3 kết quả |
| `observeActiveSession` | — | `Flow<SessionState>` | Thời gian còn lại, trạng thái, để UI vẽ đồng hồ |
| `getSessionHistory` | `range: DateRange` | `List<SessionRecord>` | Phục vụ màn Thống kê |

### 3.2 EggRepository (FR02) — trứng & tỉ lệ nở
| Hàm | Tham số | Trả về | Mô tả |
|---|---|---|---|
| `getEggTypes` | — | `List<EggType>` | Danh sách loại trứng (Rừng/Biển/Hoa/Bí Ẩn), mỗi loại có `priceCoin`/`priceHours` riêng (tỉ giá 1:10) |
| `getOwnedEggs` | `status: "INCUBATING"\|"HATCHED"?` | `Flow<List<OwnedEgg>>` | Mỗi lần mua trứng là 1 `OwnedEgg` riêng (không cộng dồn số lượng như 1 `ShopItem` thường), tự ấp dần qua nhiều phiên tới khi đạt `hatchDurationMin` thì tự roll loài |
| `getEggStage` | `progress: Float` | `EggStage` (`NORMAL`/`CRACKING`/`ABOUT_TO_HATCH`/`HATCHED`) | Map % `incubatedMin/hatchDurationMin` → giai đoạn hiển thị trong `JarMark` |

### 3.3 CollectionRepository (FR05)
| Hàm | Tham số | Trả về | Mô tả |
|---|---|---|---|
| `getCollection` | `filter: CollectionFilter` | `Flow<List<CollectionEntry>>` | Dữ liệu cho lưới Khu rừng, có badge cấp bậc |
| `getSpeciesDetail` | `speciesId: String` | `SpeciesDetail` | Cho popup Lore |
| `toggleFavorite` | `speciesId: String` | `Unit` | Đánh dấu yêu thích |
| `getCollectionProgress` | — | `Flow<Progress>` | VD: "42/175 đã mở khoá" |

### 3.4 CurrencyRepository (FR06) — 2 loại tiền tệ: Xu Lá + Giờ tích luỹ

> **Đổi so với thiết kế ban đầu:** không phải 1 loại tiền (Xu Lá) mà là 2 — `COIN` (Xu Lá, tiêu ở Cửa hàng) và `FOCUS_MINUTE` (Giờ tích luỹ, đơn vị phút, cũng tiêu mua trứng được). Tỉ giá cố định 1 phút Giờ tích luỹ = 10 Xu Lá (`GameSettings.coinsPerFocusMinute`). Mỗi phiên chỉ nhận **1 trong 2**, người chơi tự chọn trước khi bắt đầu.

| Hàm | Tham số | Trả về | Mô tả |
|---|---|---|---|
| `getBalance` | — | `Flow<{coin: Int, focusMinutes: Int}>` | 2 số dư riêng (SUM từ `LedgerEntry` phân loại theo `currency`) |
| `getLedger` | `range: DateRange` | `List<LedgerEntry>` | Lịch sử giao dịch cả 2 loại tiền, phục vụ đối soát |

### 3.5 ShopRepository (FR06)
| Hàm | Tham số | Trả về | Mô tả |
|---|---|---|---|
| `getShopItems` | `category: "EGG"\|"JAR_SKIN"\|"MUSIC"` | `Flow<List<ShopItem>>` | Danh sách item theo tab (Trứng mới/Bình/Nhạc) |
| `purchaseItem` | `itemId: String, payWith: "COIN"\|"FOCUS_MINUTE"?` | `Result<Unit>` | Mục Trứng: chọn trả bằng Xu Lá hay Giờ tích luỹ (giá riêng mỗi loại), tạo `OwnedEgg` mới. Bình/Nhạc: luôn trả Xu Lá, chỉ mua được 1 lần/món |
| `getInventory` | — | `Flow<List<InventoryItem>>` | Bình/Nhạc đã sở hữu (Trứng không nằm trong Inventory — xem `EggRepository.getOwnedEggs`) |
| `equipItem` | `inventoryItemId: String` | `Unit` | Đổi bình/nhạc đang dùng — 1-chọn-1 theo danh mục (bật món này tự tắt món khác cùng loại), xem màn Kho đồ (S-07b) |

### 3.6 StatsRepository (FR07)
| Hàm | Tham số | Trả về | Mô tả |
|---|---|---|---|
| `getDailyStats` | `date: LocalDate` | `DailyStat` | Tổng phút tập trung trong ngày |
| `getRangeStats` | `start: LocalDate, end: LocalDate` | `List<DailyStat>` | Dữ liệu vẽ biểu đồ tuần/tháng |
| `getStreak` | — | `Int` | Số ngày liên tiếp có phiên thành công |
| `getTotalFocusMinutes` | — | `Long` | Tổng thời gian tập trung all-time |

### 3.7 SettingsRepository — ✅ đã build (T-039/T-095)
| Hàm | Tham số | Trả về | Mô tả |
|---|---|---|---|
| `observeSettings` | — | `Flow<UserSettings>` | Toàn bộ cấu hình người dùng (`GET /settings`, lazy-create nếu thiếu) |
| `updateFocusDuration` | `minutes: Int` | `Unit` | Mặc định 25, khoảng 10–120 — Slider chỉ `PATCH /settings` khi thả tay |
| `updateBreakDuration` | `minutes: Int` | `Unit` | Mặc định 5, khoảng 1–60 |
| `setStrictMode` | `enabled: Boolean` | `Unit` | Bật/tắt cảnh báo thoát app |
| `setSoundTheme` | `themeId: "default"\|"rain"\|"forest"\|"lofi"` | `Unit` | Chỉ **lưu lựa chọn** — chưa thật sự phát âm thanh, đó là việc của SoundManager (mục 3.9, T-042 chưa làm) |

### 3.8 SyncRepository — ⛔ đã bỏ khỏi phạm vi (không phải "chưa làm")

Dev1002 quyết định không làm Sao lưu & Đồng bộ Google Drive (S-07a) — mục này giữ lại trong tài liệu chỉ để tham khảo thiết kế ban đầu, **không nằm trong backlog nữa**. Nếu cần lại, phải bàn phạm vi từ đầu (bao gồm cả OAuth Google Drive scope, quyền truy cập `appDataFolder`).

### 3.9 SoundManager / NotificationManager (service nội bộ, không phải Repository thuần) — ⬜ chưa làm (T-042)

Màn Cài đặt (S-07) đã có UI chọn `soundTheme` (mục 3.7), nhưng chưa có gì trong danh sách dưới đây được cài đặt thật — chọn xong chỉ lưu giá trị, không có âm thanh/thông báo nào thật sự chạy.

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

### 4.4 Endpoint REST tương ứng Function List (mục 3) — ✅ đã build & test thật

Base path `/api/v1`, auth bằng header `Authorization: Bearer <JWT>` (trừ `/auth/*`, `GET /species`, `GET /egg-types`, `GET /shop-items`, `GET /health`).

| Nhóm | Endpoint thật (đã curl-test với 10 tài khoản tester) |
|---|---|
| Auth | `POST /auth/register`, `POST /auth/login`, `POST /auth/refresh`, `GET /auth/me`. *(Sau này thêm `POST /auth/google`)* |
| Species | `GET /species?category=&rarity=`, `GET /species/:id` |
| Eggs | `GET /egg-types`, `GET /egg-types/:id`, `GET /egg-types/:id/odds` (tỉ lệ % theo cấp bậc — minh bạch cho người chơi) |
| Sessions | `POST /sessions` (nhận `ownedEggId?`, `incubationRatio?`, `rewardCurrency`), `PATCH /sessions/:id/complete` (chia phút giữa ấp trứng (gọi `OwnedEggsService.incubate`, tự roll loài theo `egg_drop_table`×`rarity_weights` khi đủ `hatchDurationMin`) và quy đổi **1 trong 2** loại tiền theo `rewardCurrency`, **idempotent** theo `clientEventId`), `PATCH /sessions/:id/give-up` (không đổi tiến trình trứng), `GET /sessions?from=&to=&status=` |
| Eggs sở hữu | `GET /owned-eggs?status=INCUBATING\|HATCHED` — mỗi lần mua trứng là 1 dòng riêng, ấp dần qua nhiều phiên |
| Collection | `GET /collection?category=&rarity=`, `GET /collection/progress`, `PATCH /collection/:speciesId/favorite` |
| Currency | `GET /currency/balance` (trả `{coin, focusMinutes}`), `GET /currency/ledger?from=&to=` |
| Game Settings | `GET /game-settings` (`coinsPerFocusMinute` — tỉ giá quy đổi, hiện `10`) |
| Shop | `GET /shop-items?category=EGG\|JAR_SKIN\|MUSIC`, `POST /shop-items/:id/purchase` (mục Trứng nhận thêm `payWith: COIN\|FOCUS_MINUTE`, tạo `OwnedEgg` mới mỗi lần mua — không cộng dồn `quantity`; Bình/Nhạc chỉ mua 1 lần → 403 nếu mua lại), `GET /inventory` (chỉ Bình/Nhạc), `PATCH /inventory/:id/equip` (1-chọn-1 theo danh mục) |
| Debug/cheat (chỉ tester) | `POST /debug/grant-currency\|grant-egg\|grant-species` — chặn server-side theo email khớp `tester\d{2}@cozypomo.dev`, dùng cho bubble cheat nội bộ (xem menu "Kho đồ cheat" ở Cài đặt) |
| Stats | `GET /stats/daily?date=`, `GET /stats/range?start=&end=`, `GET /stats/summary` (streak + tổng phút) — tính trực tiếp từ `sessions`, chưa cần bảng rollup riêng ở quy mô hiện tại |
| Settings | `GET /settings`, `PATCH /settings` |
| Sync (outbox từ app) | `POST /sync/batch` — mảng event `{clientEventId, type, payload}`, xử lý tuần tự, mỗi event có `status: 'ok'\|'error'` riêng, idempotent theo `clientEventId` |
| Admin | UI đầy đủ tại `/admin` (AdminJS), không cần tự viết endpoint riêng |

**Chưa build:** không còn — toàn bộ Function List ở mục 3 đã có endpoint thật tương ứng.

### 4.5 Repo layout (monorepo)

```
cozy-pomo-focus-app/
├── app/        # Android Studio project (Kotlin, Jetpack Compose)
├── backend/    # NestJS + Prisma + docker-compose.yml
└── docs/       # tài liệu này + setup-checklist.md
```

### 4.6 Checklist chuẩn bị

Checklist đầy đủ (GitHub / aaPanel / domain, có thể tick theo tiến độ) đã tách ra [`docs/setup-checklist.md`](./setup-checklist.md).

### 4.7 Trang quản trị (AdminJS)

`/admin`, đăng nhập bằng `ADMIN_EMAIL`/`ADMIN_PASSWORD` trong `.env` (chưa gắn với bảng `users` — admin là tài khoản riêng, không phải người chơi). Nhóm **Nội dung game** (Species/EggType/EggDropEntry/RarityWeight/ShopItem) cho CRUD đầy đủ để chỉnh tỉ lệ rơi, giá, thêm loài mới không cần deploy lại app. Nhóm **Người dùng** (User/UserSettings/Session/LedgerEntry/CollectionEntry/InventoryItem) chỉ xem — sửa dữ liệu người dùng phải đi qua API để không phá vỡ tính toàn vẹn ledger/collection. Cảnh báo còn tồn: session store admin đang dùng MemoryStore (Express mặc định) — đủ cho demo/1 process, cần đổi sang `connect-pg-simple` hoặc Redis trước khi chạy nhiều instance ở production.

### 4.8 Seed dữ liệu demo

`backend/prisma/seed.ts` (`npm run prisma:seed`) nạp toàn bộ 175 loài (khớp chính xác Creature Atlas), 4 loại trứng + bảng tỉ lệ, 10 vật phẩm cửa hàng, và **10 tài khoản tester** (`tester01`…`tester10@cozypomo.dev`, mật khẩu `Tester123!`) với 14–32 phiên/tài khoản, lịch sử Xu Lá và bộ sưu tập thật — dùng để demo UI hoặc test app Android mà không cần thao tác tay. Script xoá sạch dữ liệu trước khi nạp lại nên **chỉ chạy trên DB dev/demo**.

---

**Liên quan:**
- [`docs/wireframes/use-case-flow.html`](wireframes/use-case-flow.html) — sơ đồ luồng màn hình, đã cập nhật khớp bản build thật 2026-07-24 (5 tab, kinh tế 2 tiền tệ, Kho đồ, không còn Sao lưu Drive).
- Các link artifact dưới đây là **bản thiết kế gốc trước khi build** (2026-07-22) — giữ lại làm tham khảo lịch sử, không còn khớp 100% với app thật, ưu tiên theo `use-case-flow.html` ở trên khi có mâu thuẫn:
  - Wireframe 3 màn hình chính (dùng ảnh loài thật): https://claude.ai/code/artifact/a69b43e2-3331-4573-8df5-3da7e297f196
  - 175 loài + hệ rarity B/A/S/SS/SSR: https://claude.ai/code/artifact/9603829b-817f-4f3e-8b7b-16d6cb128647
  - Brand Field Guide (màu chính thức, kiểu chữ, giọng nói thương hiệu): https://claude.ai/code/artifact/8956dbcb-2415-4be9-88a8-c29cbdf84c44
- Schema DB chi tiết hơn: bộ nhớ dự án `cozypomo-db-recommendation`
