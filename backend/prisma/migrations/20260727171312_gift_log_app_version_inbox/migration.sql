-- CreateEnum
CREATE TYPE "GiftItemKind" AS ENUM ('SPECIES', 'EGG');

-- CreateEnum
CREATE TYPE "InboxMessageType" AS ENUM ('EGG_RECEIVED', 'EGG_HATCHED', 'ADMIN_GIFT');

-- CreateTable
CREATE TABLE "gift_logs" (
    "id" TEXT NOT NULL,
    "recipientId" TEXT NOT NULL,
    "itemKind" "GiftItemKind" NOT NULL,
    "itemId" TEXT NOT NULL,
    "itemName" TEXT NOT NULL,
    "quantity" INTEGER NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "gift_logs_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "app_version_config" (
    "id" INTEGER NOT NULL DEFAULT 1,
    "latestVersionCode" INTEGER NOT NULL DEFAULT 1,
    "minSupportedVersionCode" INTEGER NOT NULL DEFAULT 1,
    "updateUrl" TEXT NOT NULL DEFAULT 'https://play.google.com/store/apps/details?id=com.cozypomo.app',
    "updateTitle" TEXT NOT NULL DEFAULT 'Cần cập nhật phiên bản mới',
    "updateMessage" TEXT NOT NULL DEFAULT 'Phiên bản bạn đang dùng đã quá cũ, vui lòng cập nhật để tiếp tục sử dụng CozyPomo.',

    CONSTRAINT "app_version_config_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "inbox_messages" (
    "id" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "type" "InboxMessageType" NOT NULL,
    "title" TEXT NOT NULL,
    "body" TEXT NOT NULL,
    "payload" JSONB,
    "isRead" BOOLEAN NOT NULL DEFAULT false,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "inbox_messages_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE INDEX "gift_logs_recipientId_idx" ON "gift_logs"("recipientId");

-- CreateIndex
CREATE INDEX "inbox_messages_userId_isRead_idx" ON "inbox_messages"("userId", "isRead");

-- CreateIndex
CREATE INDEX "inbox_messages_userId_createdAt_idx" ON "inbox_messages"("userId", "createdAt");

-- AddForeignKey
ALTER TABLE "gift_logs" ADD CONSTRAINT "gift_logs_recipientId_fkey" FOREIGN KEY ("recipientId") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "inbox_messages" ADD CONSTRAINT "inbox_messages_userId_fkey" FOREIGN KEY ("userId") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;

