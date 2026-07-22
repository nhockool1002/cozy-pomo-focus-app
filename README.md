# CozyPomo — Khu rừng Tập trung

Ứng dụng Pomodoro kết hợp gamification (ấp trứng nở thú/cây) cho Android, phong cách cozy pastel.

- [`app/`](app/) — ứng dụng Android (Kotlin, Jetpack Compose, Hilt, Retrofit) — build: `cd app && ./gradlew :app:assembleDebug`
- [`backend/`](backend/) — API (NestJS, Prisma, PostgreSQL) — dev: `cd backend && docker compose up -d`
- [`docs/`](docs/) — tài liệu kỹ thuật
  - [`technical-spec.md`](docs/technical-spec.md) — Tech Stack, Screen List, Function List, kiến trúc Backend
  - [`setup-checklist.md`](docs/setup-checklist.md) — checklist thiết lập GitHub / aaPanel / Domain
- [`.github/workflows/`](.github/workflows/) — tạo Release với tag `app-v*` để build AAB/APK, tag `backend-v*` để build + deploy backend lên aaPanel

Tác giả: Dev1002
