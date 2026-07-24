# --- deps + build ---
FROM node:22-alpine AS builder
WORKDIR /app

COPY package*.json ./
COPY prisma ./prisma
RUN npm ci

COPY . .
RUN npx prisma generate
RUN npm run build

# --- production runtime ---
FROM node:22-alpine AS runner
WORKDIR /app
# Cố ý KHÔNG set NODE_ENV=production: ở chế độ đó AdminJS phục vụ components.bundle.js
# như file tĩnh được ghi sau khi admin.initialize() xong (bất đồng bộ, không được chờ trước
# khi server nhận request) — gây 404 chập chờn ngay sau khi container vừa khởi động. Khi
# NODE_ENV khác 'production', AdminJS dùng route bundle có tự kiểm tra cache, an toàn hơn.

COPY package*.json ./
COPY prisma ./prisma
RUN npm ci --omit=dev && npx prisma generate

COPY --from=builder /app/dist ./dist
# AdminJS bundles các component tuỳ biến (.tsx) bằng webpack riêng của nó lúc khởi động —
# cần có source gốc trong image, không phải bản đã qua tsc.
COPY src/admin/components ./src/admin/components
# Favicon/logo cho AdminJS — xem useStaticAssets trong main.ts.
COPY public ./public
# Chính sách bảo mật cho Play Console — xem useStaticAssets trong main.ts.
COPY legal ./legal

EXPOSE 3000
CMD ["sh", "-c", "npx prisma migrate deploy && node dist/main.js"]
