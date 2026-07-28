-- CreateEnum
CREATE TYPE "BoostType" AS ENUM ('FOCUS_MINUTES', 'HATCH_MINUTES');

-- CreateEnum
CREATE TYPE "StreakRewardItemKind" AS ENUM ('SPECIES', 'EGG_TYPE', 'SHOP_ITEM', 'COIN');

-- AlterEnum
ALTER TYPE "GiftItemKind" ADD VALUE 'SHOP_ITEM';

-- AlterEnum
ALTER TYPE "InboxMessageType" ADD VALUE 'STREAK_REWARD';

-- AlterEnum
-- This migration adds more than one value to an enum.
-- With PostgreSQL versions 11 and earlier, this is not possible
-- in a single migration. This can be worked around by creating
-- multiple migrations, each migration adding only one value to
-- the enum.


ALTER TYPE "LedgerReason" ADD VALUE 'ITEM_USE';
ALTER TYPE "LedgerReason" ADD VALUE 'STREAK_REWARD';

-- AlterEnum
ALTER TYPE "ShopCategory" ADD VALUE 'BOOST';

-- AlterTable
ALTER TABLE "shop_items" ADD COLUMN     "boostAmount" INTEGER,
ADD COLUMN     "boostType" "BoostType";

-- CreateTable
CREATE TABLE "streak_reward_days" (
    "day" INTEGER NOT NULL,
    "rewards" JSONB,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "streak_reward_days_pkey" PRIMARY KEY ("day")
);

-- CreateTable
CREATE TABLE "streak_claims" (
    "id" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "claimedOn" TEXT NOT NULL,
    "streakDay" INTEGER NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "streak_claims_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "streak_claims_userId_claimedOn_key" ON "streak_claims"("userId", "claimedOn");

-- AddForeignKey
ALTER TABLE "streak_claims" ADD CONSTRAINT "streak_claims_userId_fkey" FOREIGN KEY ("userId") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;
