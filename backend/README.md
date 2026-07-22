# CozyPomo — Backend

NestJS + Prisma + PostgreSQL. Xem [`../docs/technical-spec.md`](../docs/technical-spec.md) §4 và [`../docs/setup-checklist.md`](../docs/setup-checklist.md).

## Chạy local

```bash
cp .env.example .env        # sửa DATABASE_URL/JWT_SECRET... theo máy bạn
docker compose up -d        # api + postgres, api tự chạy migration khi container start
# hoặc chạy ngoài Docker:
npm install
npm run prisma:migrate:dev
npm run start:dev
```

- Swagger UI: `http://localhost:3000/docs`
- Health check: `http://localhost:3000/health`
- Auth: `POST /api/v1/auth/register`, `/login`, `/refresh`, `GET /api/v1/auth/me` (Bearer token)

**Đã có (scaffold, đã test end-to-end với Postgres thật):** schema Prisma đầy đủ (species, egg_types, sessions, currency_ledger, user_collection, shop_items...), module auth email/mật khẩu (bcrypt + JWT access/refresh), `/health`, Dockerfile + docker-compose (dev & prod).
**Chưa có, còn TODO:** AdminJS (trang quản trị), các module sessions/eggs/collection/currency/shop/stats/settings/sync liệt kê ở technical-spec.md §4.4 (hiện chỉ auth + health hoạt động thật).
