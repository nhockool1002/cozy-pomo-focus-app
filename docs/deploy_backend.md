# Deploy Backend lên aaPanel — Hướng dẫn chi tiết từng bước

Tài liệu này gộp toàn bộ quy trình để `backend/` tự động deploy lên VPS chạy aaPanel mỗi khi bạn tạo GitHub Release với tag `backend-v*`. Đọc và làm theo đúng thứ tự — các phần có phụ thuộc lẫn nhau (VD: server phải có key trước khi test SSH).

Tài liệu liên quan:
- [`setup-checklist.md`](setup-checklist.md) — checklist tổng (cả Android lẫn Backend), dùng để tick nhanh
- [`../.github/workflows/backend-deploy.yml`](../.github/workflows/backend-deploy.yml) — workflow thực thi
- [`../backend/docker-compose.prod.yml`](../backend/docker-compose.prod.yml) — cấu hình container trên server
- [`../backend/Dockerfile`](../backend/Dockerfile) — cách image được build

---

## 0. Kiến trúc deploy — hiểu trước khi làm

```
Bạn tạo GitHub Release (tag backend-v*)
        │
        ▼
GitHub Actions (backend-deploy.yml)
  1) build-and-push: build Docker image từ backend/Dockerfile,
     push lên ghcr.io/<user>/cozy-pomo-focus-app/backend:latest
        │
        ▼
  2) deploy: SSH vào server (secrets DEPLOY_HOST/DEPLOY_USER/DEPLOY_SSH_KEY),
     cd vào DEPLOY_PATH, chạy:
       docker compose -f docker-compose.prod.yml pull
       docker compose -f docker-compose.prod.yml up -d
       docker image prune -f
        │
        ▼
Server (aaPanel): container "api" khởi động lại với image mới
  → entrypoint tự chạy `npx prisma migrate deploy` rồi mới `node dist/main.js`
  → Nginx của aaPanel (site đã cấu hình Reverse Proxy) chuyển traffic HTTPS → 127.0.0.1:3012
```

Điểm quan trọng cần nhớ:
- **Migration DB chạy tự động** mỗi lần container khởi động (xem `CMD` trong `Dockerfile`) — bạn không cần SSH vào chạy `prisma migrate deploy` tay, kể cả lần đầu.
- **Container `db` (Postgres) và `api` chỉ bind `127.0.0.1`** — không port nào lộ ra Internet ngoài 80/443 do Nginx aaPanel phục vụ.
- Workflow **không có bước `docker login`** — nghĩa là server phải tự pull được image từ ghcr.io mà không cần đăng nhập, tức package phải ở chế độ **Public** (chi tiết ở bước 1.4).

---

## Phần 1 — Chuẩn bị trên GitHub

### 1.1. Thêm 4 Secrets bắt buộc

Vào repo trên GitHub → **Settings → Secrets and variables → Actions → New repository secret**, thêm lần lượt:

| Tên secret | Giá trị | Lấy ở đâu |
|---|---|---|
| `DEPLOY_HOST` | IP hoặc domain của VPS | Trang quản lý VPS của nhà cung cấp (Vultr/DigitalOcean/…) |
| `DEPLOY_USER` | User SSH dùng để deploy | Tài khoản bạn dùng SSH vào server (VD `root` hoặc user thường có quyền chạy `docker`) |
| `DEPLOY_SSH_KEY` | Private key riêng cho deploy | Tự tạo mới — xem bước 1.2 |
| `DEPLOY_PATH` | Thư mục trên server chứa `docker-compose.prod.yml` + `.env` | Bạn tự đặt, VD `/www/wwwroot/cozypomo-backend` — phải khớp với thư mục thật tạo ở bước 2.3 |

### 1.2. Tạo cặp SSH key riêng cho deploy

**Không dùng lại** key cá nhân bạn đang SSH/git bằng key đó — tạo key mới, chỉ dùng riêng cho việc deploy tự động.

Chạy trên máy của bạn (không phải server, không phải môi trường Claude):

```bash
ssh-keygen -t ed25519 -C "deploy-cozypomo" -f ~/.ssh/deploy_cozypomo -N ""
```

Giải thích flag:
- `-f ~/.ssh/deploy_cozypomo` — đặt tên file riêng, tránh ghi đè key khác
- `-N ""` — để trống passphrase. **Bắt buộc**, vì GitHub Actions chạy tự động, không có ai gõ passphrase khi SSH

Lệnh trên tạo ra 2 file:
- `~/.ssh/deploy_cozypomo` — **private key**, giữ bí mật tuyệt đối, không commit, không dán vào chat/email
- `~/.ssh/deploy_cozypomo.pub` — **public key**, an toàn để chia sẻ, sẽ dán vào server ở bước 2.2

Dán private key vào GitHub Secret `DEPLOY_SSH_KEY`:

```bash
cat ~/.ssh/deploy_cozypomo
```

Copy **toàn bộ** nội dung in ra — kể cả 2 dòng `-----BEGIN OPENSSH PRIVATE KEY-----` và `-----END OPENSSH PRIVATE KEY-----` — dán y nguyên làm giá trị secret `DEPLOY_SSH_KEY`.

> Public key (`deploy_cozypomo.pub`) chưa dùng vội — sẽ dùng ở bước 2.2 khi đã có server.

### 1.3. Kiểm tra Actions đã bật

Settings → Actions → General → mục "Actions permissions" chọn **Allow all actions and reusable workflows**.

### 1.4. ⚠️ ghcr.io package visibility — bước hay bị bỏ sót

Khi Actions build & push image lần đầu bằng `GITHUB_TOKEN` mặc định, package trên `ghcr.io` được tạo ở chế độ **Private**, **kể cả khi repo đang Public**. Vì workflow không có bước `docker login` trên server, nếu package vẫn Private thì bước `docker compose pull` ở server sẽ báo lỗi `401 Unauthorized` / `denied` — cả lần chạy thủ công đầu tiên (bước 2.7) lẫn deploy tự động sau này.

Cách xử lý — làm **sau khi** đã có ít nhất 1 lần build (xem bước 4.1 để trigger build trước), chọn 1 trong 2:

**Cách 1 — đổi package sang Public (khuyến nghị, đơn giản nhất):**
1. Vào `https://github.com/<username>?tab=packages`
2. Bấm vào package `backend`
3. Bên phải → **Package settings**
4. Kéo xuống **Danger Zone** → **Change visibility** → chọn **Public** → xác nhận

**Cách 2 — đăng nhập ghcr.io trên server (nếu muốn giữ image private):**
```bash
docker login ghcr.io -u <username-github> -p <personal-access-token>
```
Token cần tạo riêng tại GitHub → Settings → Developer settings → Personal access tokens → scope tối thiểu `read:packages`. Lệnh này chạy 1 lần trên server, Docker sẽ nhớ đăng nhập cho các lần pull sau.

### 1.5. (Tuỳ chọn) Environment bảo vệ

Nếu muốn deploy phải duyệt tay trước khi chạy: Settings → Environments → New environment → đặt tên `production` → bật **Required reviewers**. Không bắt buộc để deploy hoạt động.

---

## Phần 2 — Chuẩn bị server (aaPanel)

### 2.1. Cài aaPanel (nếu VPS chưa có)

Làm theo hướng dẫn chính thức: https://www.aapanel.com — chạy script cài đặt do aaPanel cung cấp qua SSH, xong sẽ có URL + tài khoản đăng nhập panel.

### 2.2. Cài Docker qua aaPanel

Đăng nhập aaPanel → **App Store** → tìm **Docker** → Install (bản có kèm Docker Compose).

Sau khi cài xong, SSH vào server kiểm tra:
```bash
docker --version
docker compose version
```
Phải ra phiên bản Compose **v2** (lệnh `docker compose`, không phải `docker-compose` gạch nối kiểu cũ — cú pháp workflow đang dùng là `docker compose -f ...`).

### 2.3. Thêm public key vào server

Trên server, xác định đúng user sẽ dùng để deploy (phải khớp secret `DEPLOY_USER`, và user này phải chạy được lệnh `docker` mà không cần `sudo` — tức nằm trong group `docker`, hoặc dùng thẳng `root`).

Từ máy bạn, copy public key đã tạo ở bước 1.2:
```bash
cat ~/.ssh/deploy_cozypomo.pub
```

SSH vào server bằng tài khoản hiện có của bạn, thêm dòng vừa copy vào cuối file:
```bash
echo "<nội dung .pub vừa copy>" >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
```
(nếu deploy bằng `root` thì sửa đúng `/root/.ssh/authorized_keys`)

**Test ngay** từ máy bạn trước khi đi tiếp — nếu bước này sai thì mọi thứ sau vô nghĩa:
```bash
ssh -i ~/.ssh/deploy_cozypomo <DEPLOY_USER>@<DEPLOY_HOST>
```
Vào được server không hỏi mật khẩu → đúng. Nếu bị hỏi password hoặc "Permission denied (publickey)" → kiểm tra lại đường dẫn `authorized_keys`, quyền file (`chmod 700 ~/.ssh`, `chmod 600 ~/.ssh/authorized_keys`), và đúng user.

### 2.4. Tạo thư mục deploy

Thư mục này **phải khớp chính xác** với secret `DEPLOY_PATH` đã đặt ở bước 1.1:
```bash
mkdir -p /www/wwwroot/cozypomo-backend
cd /www/wwwroot/cozypomo-backend
```

### 2.5. Copy `docker-compose.prod.yml` lên server

Từ máy bạn (thư mục repo local), copy nguyên văn file này lên đúng thư mục vừa tạo — dùng `scp`, `rsync`, hoặc copy tay qua trình quản lý file của aaPanel:
```bash
scp backend/docker-compose.prod.yml <DEPLOY_USER>@<DEPLOY_HOST>:/www/wwwroot/cozypomo-backend/
```
Không cần sửa gì trong file này trừ khi bạn đổi tên image (mặc định đang trỏ `ghcr.io/nhockool1002/cozy-pomo-focus-app/backend:latest`).

### 2.6. Tạo và điền `.env` production

Trên server, trong đúng thư mục `DEPLOY_PATH`:
```bash
cd /www/wwwroot/cozypomo-backend
nano .env
```

Điền theo mẫu — **đây là các giá trị thật dùng khi chạy bằng Docker**, không phải giống hệt `.env` lúc dev local:

```bash
PORT=3000
DATABASE_URL="postgresql://cozypomo:<DB_PASSWORD>@db:5432/cozypomo?schema=public"
JWT_SECRET="<chuỗi random dài>"
JWT_ACCESS_EXPIRES_IN="15m"
JWT_REFRESH_SECRET="<chuỗi random dài khác, KHÔNG trùng JWT_SECRET>"
JWT_REFRESH_EXPIRES_IN="30d"

ADMIN_EMAIL="email-thật-của-bạn@..."
ADMIN_PASSWORD="<mật khẩu mạnh>"
ADMIN_COOKIE_SECRET="<chuỗi random dài khác nữa>"

DB_PASSWORD="<mật khẩu Postgres — PHẢI khớp với phần sau dấu : trong DATABASE_URL ở trên>"
```

**Vì sao host DB là `db` chứ không phải `localhost`:** trong `docker-compose.prod.yml`, service Postgres tên `db`, chỉ bind `127.0.0.1:5437` trên **host** (đổi từ 5432 mặc định vì server đã có sẵn service khác chiếm cổng đó), container `api` phải gọi qua mạng nội bộ Docker bằng đúng tên service ở cổng nội bộ **5432** (`db:5432`, không phải `5437` — cổng host chỉ ảnh hưởng việc gọi từ ngoài container), không phải `localhost` (bên trong container `api`, `localhost` là chính nó, không phải container `db`).

**Vì sao `DB_PASSWORD` xuất hiện 2 lần:** Docker Compose tự đọc file `.env` cùng thư mục để thay `${DB_PASSWORD}` vào `POSTGRES_PASSWORD` của service `db` trong `docker-compose.prod.yml`; đồng thời bạn tự gõ password đó vào `DATABASE_URL` để app kết nối đúng. Hai giá trị này bắt buộc phải khớp.

Sinh các chuỗi random (`JWT_SECRET`, `JWT_REFRESH_SECRET`, `ADMIN_COOKIE_SECRET`, `DB_PASSWORD`) — chạy lệnh này riêng cho từng biến, không dùng chung 1 chuỗi:
```bash
openssl rand -base64 48
```

Khoá quyền file vì chứa secret:
```bash
chmod 600 .env
```

### 2.7. Kiểm tra firewall

Trong aaPanel → **Security** (hoặc App Store nếu dùng firewall riêng):
- Mở **80** và **443** (thường tự mở khi tạo site ở Phần 3)
- **Không** mở 3012 và 5437 ra ngoài — không cần, vì `docker-compose.prod.yml` đã tự bind `127.0.0.1`, chỉ Nginx nội bộ gọi được

### 2.8. Chạy lần đầu thủ công

Vẫn trong `DEPLOY_PATH` trên server:
```bash
docker compose -f docker-compose.prod.yml up -d
```
Lệnh này tự pull image (đã xử lý visibility ở bước 1.4), tạo container `db` + `api`, entrypoint tự chạy migration Prisma. Theo dõi log:
```bash
docker compose -f docker-compose.prod.yml logs -f api
```
Thấy dòng kiểu `Nest application successfully started` là container `api` đã lên. `Ctrl+C` để thoát xem log (không tắt container).

Kiểm tra nhanh ngay trên server (chưa cần domain):
```bash
curl http://127.0.0.1:3012/health
```
Phải trả về `{"status":"ok",...}`.

---

## Phần 3 — Domain & SSL (không bắt buộc để test nội bộ, cần nếu muốn app Android gọi qua HTTPS thật)

### 3.1. Trỏ domain

Ở nhà cung cấp domain (Cloudflare/Namecheap/…), thêm bản ghi **A** cho subdomain (VD `api.cozypomo.app`) trỏ về IP của `DEPLOY_HOST`.

### 3.2. Tạo site trong aaPanel

aaPanel → **Website** → **Add site** → nhập đúng domain đó (VD `api.cozypomo.app`) → không cần chọn PHP/database gì thêm vì backend chạy trong Docker riêng.

### 3.3. Bật SSL miễn phí

Vào site vừa tạo → tab **SSL** → chọn **Let's Encrypt** → Apply. aaPanel tự gia hạn định kỳ, không cần làm lại.

### 3.4. Reverse Proxy vào container

Vào site đó → **Reverse Proxy** → **Add reverse proxy**:
- Target URL: `http://127.0.0.1:3012`
- Bật "Send Domain" nếu có tuỳ chọn (giữ header Host đúng)

### 3.5. Đổi `API_BASE_URL` trong app Android (nếu domain khác mặc định)

Nếu bạn không dùng đúng `api.cozypomo.app`, sửa `BuildConfig.API_BASE_URL` trong `app/app/build.gradle.kts` cho khớp domain thật trước khi build release Android.

### 3.6. Test qua domain thật

```bash
curl https://api.cozypomo.app/health
```
Phải trả `{"status":"ok",...}`. Mở thêm 2 URL trên trình duyệt để chắc chắn mọi thứ chạy:
- `https://api.cozypomo.app/docs` — Swagger UI, tự sinh từ code
- `https://api.cozypomo.app/admin` — trang quản trị AdminJS, đăng nhập bằng `ADMIN_EMAIL`/`ADMIN_PASSWORD` đã điền ở bước 2.6

---

## Phần 4 — Kích hoạt & kiểm tra auto-deploy

### 4.1. Test build-only trước (chưa deploy thật)

GitHub repo → tab **Actions** → chọn workflow **Backend Deploy** → **Run workflow** → để `deploy = false` (mặc định) → Run.

Chờ job `build-and-push` chạy xong (xanh) — bước này cũng chính là lần build đầu tiên cần có để xử lý bước 1.4 (ghcr visibility).

### 4.2. Test deploy thật

Chạy lại **Run workflow**, lần này bật `deploy = true`. Job `deploy` sẽ SSH vào server, `pull` + `up -d`. Theo dõi log job trong Actions — nếu bước SSH báo lỗi, xem mục Troubleshooting bên dưới.

### 4.3. Deploy qua Release thật (quy trình dùng lâu dài về sau)

Từ lần này trở đi, mỗi khi muốn deploy bản mới:
1. Merge code vào `main`
2. GitHub → **Releases** → **Draft a new release**
3. Tag đặt đúng định dạng **`backend-v<version>`**, VD `backend-v0.1.0`, `backend-v0.1.1`
4. Publish release → workflow tự chạy build + push + deploy, không cần vào Actions bấm tay

> Tag bắt đầu bằng `app-v` sẽ kích hoạt workflow Android thay vì backend — đừng nhầm tiền tố.

---

## Phần 5 — Xác minh sau mỗi lần deploy

Checklist nhanh sau khi thấy job `deploy` xanh:

- [ ] `curl https://<domain>/health` → `{"status":"ok"}`
- [ ] Trên server: `docker compose -f docker-compose.prod.yml ps` → cả `api` và `db` đều `Up (healthy)`
- [ ] `docker compose -f docker-compose.prod.yml logs api --tail=50` → không có lỗi migration/connect DB
- [ ] Đăng nhập thử `/admin` bằng tài khoản thật
- [ ] Gọi thử 1 API cần JWT (VD `/api/v1/currency/balance`) từ Swagger UI `/docs` (nút "Authorize" dán Bearer token lấy từ `/auth/login`)

---

## Phần 6 — Xử lý sự cố thường gặp

| Triệu chứng | Nguyên nhân khả dĩ | Cách xử lý |
|---|---|---|
| `docker compose pull` báo `401 Unauthorized` / `denied` | Package ghcr.io còn Private | Làm lại bước 1.4 |
| Job `deploy` báo lỗi SSH / timeout | Sai `DEPLOY_HOST`/`DEPLOY_USER`, hoặc public key chưa vào đúng `authorized_keys` | Test lại lệnh `ssh -i ~/.ssh/deploy_cozypomo ...` ở bước 2.3 |
| Container `api` restart liên tục | `DATABASE_URL` sai (thường do quên đổi `localhost` → `db`, hoặc `DB_PASSWORD` không khớp) | Xem lại bước 2.6, `docker compose logs api` |
| Container `api` báo lỗi migration | Đổi schema Prisma nhưng chưa có migration file, hoặc migration bị conflict | Kiểm tra `backend/prisma/migrations` đã commit đủ trước khi build image |
| `curl /health` từ ngoài không vào được nhưng từ server (`127.0.0.1`) thì được | Reverse Proxy chưa đúng, hoặc SSL/site chưa tạo xong | Xem lại Phần 3 |
| Đổi `.env` trên server nhưng app không nhận giá trị mới | Container chưa restart | `docker compose -f docker-compose.prod.yml up -d --force-recreate api` |
| Muốn xem toàn bộ biến môi trường container đang thấy | — | `docker compose -f docker-compose.prod.yml exec api env` |

---

## Phụ lục — Tham chiếu nhanh

- Bí danh image: `ghcr.io/nhockool1002/cozy-pomo-focus-app/backend:latest` (đổi trong `docker-compose.prod.yml` nếu fork repo khác owner)
- Migration DB: tự động, xem `CMD` trong [`backend/Dockerfile`](../backend/Dockerfile)
- Port nội bộ container `api`: `3000` (không đổi, do `PORT` trong `.env`) — bind ra host ở `127.0.0.1:3012`; container `db` nội bộ vẫn `5432`, bind ra host ở `127.0.0.1:5437`
- Seed dữ liệu demo (tuỳ chọn, 175 loài + 10 tài khoản tester): xem `backend/README.md`, chạy `npm run prisma:seed` (cần Node/npm cài trên server hoặc chạy trong container bằng `docker compose exec api npm run prisma:seed`)
