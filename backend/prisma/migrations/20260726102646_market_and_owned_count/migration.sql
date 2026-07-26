-- CreateEnum
CREATE TYPE "MarketItemType" AS ENUM ('SPECIES', 'EGG');

-- CreateEnum
CREATE TYPE "MarketListingSellerType" AS ENUM ('USER', 'ADMIN');

-- CreateEnum
CREATE TYPE "MarketListingStatus" AS ENUM ('PENDING_APPROVAL', 'ACTIVE', 'SOLD', 'CANCELLED', 'REJECTED');

-- AlterEnum
-- This migration adds more than one value to an enum.
-- With PostgreSQL versions 11 and earlier, this is not possible
-- in a single migration. This can be worked around by creating
-- multiple migrations, each migration adding only one value to
-- the enum.


ALTER TYPE "LedgerReason" ADD VALUE 'MARKET_BUY';
ALTER TYPE "LedgerReason" ADD VALUE 'MARKET_SELL';

-- AlterTable
ALTER TABLE "currency_ledger" ADD COLUMN     "refMarketListingId" TEXT;

-- AlterTable
ALTER TABLE "game_settings" ADD COLUMN     "marketFeePercent" DOUBLE PRECISION NOT NULL DEFAULT 10,
ADD COLUMN     "maxActiveListingsPerUser" INTEGER NOT NULL DEFAULT 10;

-- AlterTable
ALTER TABLE "user_collection" ADD COLUMN     "ownedCount" INTEGER NOT NULL DEFAULT 0;

-- CreateTable
CREATE TABLE "market_listings" (
    "id" TEXT NOT NULL,
    "itemType" "MarketItemType" NOT NULL,
    "sellerType" "MarketListingSellerType" NOT NULL DEFAULT 'USER',
    "sellerId" TEXT,
    "speciesId" TEXT,
    "ownedEggId" TEXT,
    "priceCoin" INTEGER NOT NULL,
    "status" "MarketListingStatus" NOT NULL DEFAULT 'ACTIVE',
    "buyerId" TEXT,
    "clientEventId" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "soldAt" TIMESTAMP(3),
    "cancelledAt" TIMESTAMP(3),

    CONSTRAINT "market_listings_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "market_listings_clientEventId_key" ON "market_listings"("clientEventId");

-- CreateIndex
CREATE INDEX "market_listings_status_itemType_idx" ON "market_listings"("status", "itemType");

-- CreateIndex
CREATE INDEX "market_listings_sellerId_status_idx" ON "market_listings"("sellerId", "status");

-- AddForeignKey
ALTER TABLE "market_listings" ADD CONSTRAINT "market_listings_sellerId_fkey" FOREIGN KEY ("sellerId") REFERENCES "users"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "market_listings" ADD CONSTRAINT "market_listings_speciesId_fkey" FOREIGN KEY ("speciesId") REFERENCES "species"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "market_listings" ADD CONSTRAINT "market_listings_ownedEggId_fkey" FOREIGN KEY ("ownedEggId") REFERENCES "owned_eggs"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "market_listings" ADD CONSTRAINT "market_listings_buyerId_fkey" FOREIGN KEY ("buyerId") REFERENCES "users"("id") ON DELETE SET NULL ON UPDATE CASCADE;

