# CozyPomo — Setup Checklist (GitHub / aaPanel / Domain)

Repo: https://github.com/nhockool1002/cozy-pomo-focus-app · Cấu trúc: `app/` (Android) · `backend/` (NestJS) · `docs/` (tài liệu)

> Không dán mật khẩu, token, SSH key riêng tư vào chat với Claude — mọi secret nên nằm trong **GitHub Actions Secrets** hoặc file `.env` trên server (không commit git). Claude không có quyền truy cập server/aaPanel/DNS của bạn nên các mục dưới đây bạn cần tự làm, trừ các mục đã đánh dấu là Claude thực hiện được qua `git`/`gh` cục bộ.

## 1. GitHub

- [x] Repo đã tồn tại: `nhockool1002/cozy-pomo-focus-app` (đang **Public**)
- [x] Claude: `git init`, tạo cấu trúc `app/` `backend/` `docs/`, commit & push nhánh `main` ban đầu
- [ ] Cân nhắc chuyển repo sang **Private** — vì sau này chứa workflow trỏ tới server thật (Settings → General → Danger Zone → Change visibility)
- [ ] Settings → Actions → General → đảm bảo **Allow all actions** đang bật
- [ ] (Khuyến nghị) Tạo **Environment** tên `production` (Settings → Environments) để nhóm secrets, có thể bật "required reviewers" để deploy phải duyệt tay
- [ ] Thêm **Actions Secrets** (Settings → Secrets and variables → Actions):
  - `DEPLOY_SSH_KEY` — private key riêng cho deploy (không phải key cá nhân của bạn)
  - `DEPLOY_HOST` — IP hoặc domain server
  - `DEPLOY_USER` — user SSH dùng để deploy
  - `DATABASE_URL`, `JWT_SECRET`, `JWT_REFRESH_SECRET`, `DB_PASSWORD` — nếu muốn workflow tự ghi `.env` lúc deploy (hoặc bỏ qua, để `.env` cố định sẵn trên server — xem mục aaPanel)
- [ ] Không cần tạo thêm tài khoản registry — `ghcr.io` dùng chung `GITHUB_TOKEN`, chỉ cần khai báo `permissions: packages: write` trong workflow
- [ ] Nhờ Claude viết `.github/workflows/deploy-backend.yml` khi bạn đã có server sẵn sàng (build image → push ghcr.io → SSH deploy)

## 2. aaPanel (trên VPS)

- [ ] Cài aaPanel lên server nếu chưa có (https://www.aapanel.com)
- [ ] Đăng nhập aaPanel → App Store → cài **Docker** (kèm Docker Compose)
- [ ] Tạo cặp khoá SSH riêng cho deploy — chạy trên máy bạn: `ssh-keygen -t ed25519 -C "deploy-cozypomo"`; thêm public key vào `~/.ssh/authorized_keys` trên server; private key dán vào GitHub Secret `DEPLOY_SSH_KEY` ở trên
- [ ] Tạo thư mục project trên server, VD `/www/wwwroot/cozypomo-backend/`, nơi đặt `docker-compose.yml` + `.env`
- [ ] Tạo file `.env` production **trực tiếp trên server** (không qua git): `DATABASE_URL`, `JWT_SECRET`, `JWT_REFRESH_SECRET`, `DB_PASSWORD`, tài khoản admin đầu tiên cho AdminJS
- [ ] Kiểm tra firewall: 80/443 mở (thường tự mở khi tạo site), 3000/5432 **không** mở ra ngoài (docker-compose đã bind `127.0.0.1`)
- [ ] Sau khi có site (mục 3) → aaPanel → site đó → **Reverse Proxy** → trỏ về `127.0.0.1:3000`

## 3. Domain

- [ ] Chọn subdomain cho API, VD `api.cozypomo.app` (hoặc domain bạn đang sở hữu)
- [ ] Ở nhà cung cấp domain (Cloudflare/Namecheap/…): thêm bản ghi **A** trỏ subdomain → IP server
- [ ] aaPanel → Website → Add site → nhập domain đó
- [ ] Bật **SSL miễn phí** (Let's Encrypt) ngay trong aaPanel — 1 click, tự gia hạn
- [ ] Sau khi backend có endpoint `/health`, test: `curl https://api.cozypomo.app/health` phải trả `200`

## Trạng thái hiện tại

| Mục | Trạng thái |
|---|---|
| Repo GitHub + cấu trúc `app/`/`backend/`/`docs/` | ✅ Claude đã tạo và push |
| NestJS project trong `backend/` | ⬜ Chưa scaffold — chờ xác nhận |
| Android project trong `app/` | ⬜ Chưa scaffold — chờ xác nhận |
| `.github/workflows/deploy-backend.yml` | ⬜ Chờ bạn có server để test |
| Server/aaPanel/domain | ⬜ Bạn tự chuẩn bị theo checklist trên |
