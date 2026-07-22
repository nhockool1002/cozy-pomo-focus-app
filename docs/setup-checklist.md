# CozyPomo — Setup Checklist (GitHub / aaPanel / Domain)

Repo: https://github.com/nhockool1002/cozy-pomo-focus-app · Cấu trúc: `app/` (Android) · `backend/` (NestJS) · `docs/` (tài liệu)

> Không dán mật khẩu, token, SSH key riêng tư vào chat với Claude — mọi secret nên nằm trong **GitHub Actions Secrets** hoặc file `.env` trên server (không commit git). Claude không có quyền truy cập server/aaPanel/DNS của bạn nên các mục dưới đây bạn cần tự làm, trừ các mục đã đánh dấu là Claude thực hiện được qua `git`/`gh`/Docker cục bộ.

## Quy ước release (bắt buộc để 2 workflow tự chọn đúng job)

Repo là monorepo nên **tên tag khi tạo GitHub Release quyết định workflow nào chạy**:
- Tag bắt đầu `app-v` (VD `app-v0.1.0`) → build AAB + APK ký sẵn, đính kèm vào release
- Tag bắt đầu `backend-v` (VD `backend-v0.1.0`) → build Docker image, push ghcr.io, SSH deploy lên aaPanel

Cả 2 workflow đều có nút **Run workflow** (workflow_dispatch) để test thử mà không cần tạo release thật.

## 1. GitHub

- [x] Repo đã tồn tại: `nhockool1002/cozy-pomo-focus-app` (đang **Public**)
- [x] Claude: `git init`, cấu trúc `app/` `backend/` `docs/`, commit & push nhánh `main`
- [x] Claude: scaffold NestJS backend thật (auth email/password, Prisma, Docker) — đã build + chạy thử thành công với Postgres thật
- [x] Claude: scaffold Android app thật (Compose, Hilt, Retrofit, Navigation) — xem trạng thái build ở cuối file
- [x] Claude: viết `.github/workflows/android-release.yml` và `.github/workflows/backend-deploy.yml`
- [ ] Cân nhắc chuyển repo sang **Private** — vì sau này chứa workflow trỏ tới server thật (Settings → General → Danger Zone → Change visibility)
- [ ] Settings → Actions → General → đảm bảo **Allow all actions** đang bật
- [ ] (Khuyến nghị) Tạo **Environment** tên `production` (Settings → Environments), có thể bật "required reviewers" để deploy phải duyệt tay
- [ ] Thêm **Actions Secrets** (Settings → Secrets and variables → Actions):

  **Cho `android-release.yml`:**
  - `ANDROID_KEYSTORE_BASE64` — nội dung file `.jks` release, encode base64 (`base64 -i keystore.jks | pbcopy`)
  - `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`

  **Cho `backend-deploy.yml`:**
  - `DEPLOY_HOST` — IP hoặc domain server
  - `DEPLOY_USER` — user SSH dùng để deploy
  - `DEPLOY_SSH_KEY` — private key riêng cho deploy (không phải key cá nhân)
  - `DEPLOY_PATH` — thư mục trên server chứa `docker-compose.prod.yml` + `.env`, VD `/www/wwwroot/cozypomo-backend`

  Không cần tạo tài khoản registry riêng — `ghcr.io` dùng chung `GITHUB_TOKEN` mặc định của Actions.

## 2. aaPanel (trên VPS)

- [ ] Cài aaPanel lên server nếu chưa có (https://www.aapanel.com)
- [ ] Đăng nhập aaPanel → App Store → cài **Docker** (kèm Docker Compose)
- [ ] Tạo cặp khoá SSH riêng cho deploy — chạy trên máy bạn: `ssh-keygen -t ed25519 -C "deploy-cozypomo"`; thêm public key vào `~/.ssh/authorized_keys` trên server; private key dán vào GitHub Secret `DEPLOY_SSH_KEY`
- [ ] Tạo thư mục đúng bằng giá trị secret `DEPLOY_PATH`, VD `/www/wwwroot/cozypomo-backend/`
- [ ] Copy 2 file từ `backend/` trong repo lên đúng thư mục đó: `docker-compose.prod.yml` và `.env` (tạo `.env` từ `backend/.env.example`, điền giá trị thật — **không copy `.env.example` nguyên văn**)
- [ ] Điền `.env` production: `DATABASE_URL` (trỏ vào service `db` cùng compose, VD `postgresql://cozypomo:<DB_PASSWORD>@db:5432/cozypomo?schema=public`), `JWT_SECRET`, `JWT_REFRESH_SECRET`, `DB_PASSWORD`
- [ ] Kiểm tra firewall: 80/443 mở (thường tự mở khi tạo site), 3000/5432 **không** mở ra ngoài (`docker-compose.prod.yml` đã bind `127.0.0.1`)
- [ ] Chạy lần đầu thủ công để tạo container + áp migration: `docker compose -f docker-compose.prod.yml up -d` (các lần sau để GitHub Actions tự `pull && up -d` khi có release `backend-v*`)
- [ ] Sau khi có site (mục 3) → aaPanel → site đó → **Reverse Proxy** → trỏ về `127.0.0.1:3000`

## 3. Domain

- [ ] Chọn subdomain cho API, VD `api.cozypomo.app` (hoặc domain bạn đang sở hữu) — build Android release hiện đang trỏ cứng `BuildConfig.API_BASE_URL` tới `https://api.cozypomo.app/api/v1/`, đổi trong `app/app/build.gradle.kts` nếu dùng domain khác
- [ ] Ở nhà cung cấp domain (Cloudflare/Namecheap/…): thêm bản ghi **A** trỏ subdomain → IP server
- [ ] aaPanel → Website → Add site → nhập domain đó
- [ ] Bật **SSL miễn phí** (Let's Encrypt) ngay trong aaPanel — 1 click, tự gia hạn
- [ ] Test: `curl https://api.cozypomo.app/health` phải trả `{"status":"ok",...}` (endpoint này chạy thật, không cần JWT)
- [ ] Test tài liệu API: mở `https://api.cozypomo.app/docs` (Swagger UI, tự sinh từ code)

## Trạng thái hiện tại

| Mục | Trạng thái |
|---|---|
| Repo GitHub + cấu trúc `app/`/`backend/`/`docs/` | ✅ Đã tạo và push |
| NestJS backend (`backend/`) | ✅ Scaffold xong — auth register/login/refresh/me + `/health` đã test thật với Postgres thật (local + trong Docker container thật), build Docker image thành công |
| Android app (`app/`) | ✅ Scaffold xong — Compose + Hilt + Navigation + Retrofit, 4 tab (Home/Forest/Shop/Stats) placeholder, theme đúng Brand Guide. Icon launcher là placeholder hình học, **cần thay bằng art thật trước khi phát hành** |
| `.github/workflows/android-release.yml` | ✅ Đã viết, kích hoạt bởi tag `app-v*` |
| `.github/workflows/backend-deploy.yml` | ✅ Đã viết, kích hoạt bởi tag `backend-v*` |
| Server/aaPanel/domain | ⬜ Bạn tự chuẩn bị theo checklist trên — Claude không có quyền truy cập |
| Secrets trong GitHub | ⬜ Bạn tự thêm theo danh sách mục 1 |
