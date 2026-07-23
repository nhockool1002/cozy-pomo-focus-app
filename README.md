# CozyPomo — Khu rừng Tập trung

Ứng dụng Pomodoro kết hợp gamification (ấp trứng nở thú/cây) cho Android, phong cách cozy pastel.

- [`app/`](app/) — ứng dụng Android (Kotlin, Jetpack Compose, Hilt, Retrofit) — build: `cd app && ./gradlew :app:assembleDebug`
- [`backend/`](backend/) — API (NestJS, Prisma, PostgreSQL) — dev: `cd backend && docker compose up -d`
- [`docs/`](docs/) — tài liệu kỹ thuật
  - [`technical-spec.md`](docs/technical-spec.md) — Tech Stack, Screen List, Function List, kiến trúc Backend
  - [`setup-checklist.md`](docs/setup-checklist.md) — checklist thiết lập GitHub / aaPanel / Domain
  - [`deploy_backend.md`](docs/deploy_backend.md) — hướng dẫn chi tiết từng bước deploy backend lên aaPanel
  - [`branding/`](docs/branding/) — icon, favicon, logo chính thức
- [`.github/workflows/`](.github/workflows/) — tạo Release với tag `app-v*` để build AAB/APK, tag `backend-v*` để build + deploy backend lên aaPanel
- [`plan.md`](plan.md) — trạng thái dự án, danh sách task đã/chưa làm kèm mô tả chi tiết

Tác giả: Dev1002
