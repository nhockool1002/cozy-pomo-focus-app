-- CreateEnum
CREATE TYPE "AuthProvider" AS ENUM ('LOCAL', 'GOOGLE');

-- CreateEnum
CREATE TYPE "SpeciesCategory" AS ENUM ('FOREST', 'SEA', 'PLANT', 'MYTHIC');

-- CreateEnum
CREATE TYPE "Rarity" AS ENUM ('B', 'A', 'S', 'SS', 'SSR');

-- CreateEnum
CREATE TYPE "ShopCategory" AS ENUM ('EGG', 'JAR_SKIN', 'MUSIC');

-- CreateEnum
CREATE TYPE "SessionStatus" AS ENUM ('RUNNING', 'COMPLETED', 'GIVEN_UP');

-- CreateEnum
CREATE TYPE "LedgerReason" AS ENUM ('SESSION_REWARD', 'PURCHASE', 'REFUND', 'ADMIN_ADJUST');

-- CreateTable
CREATE TABLE "users" (
    "id" TEXT NOT NULL,
    "email" TEXT NOT NULL,
    "passwordHash" TEXT,
    "authProvider" "AuthProvider" NOT NULL DEFAULT 'LOCAL',
    "displayName" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "users_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "user_settings" (
    "userId" TEXT NOT NULL,
    "focusMinutes" INTEGER NOT NULL DEFAULT 25,
    "breakMinutes" INTEGER NOT NULL DEFAULT 5,
    "strictModeEnabled" BOOLEAN NOT NULL DEFAULT true,
    "soundTheme" TEXT NOT NULL DEFAULT 'default',

    CONSTRAINT "user_settings_pkey" PRIMARY KEY ("userId")
);

-- CreateTable
CREATE TABLE "species" (
    "id" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "category" "SpeciesCategory" NOT NULL,
    "archetype" TEXT NOT NULL,
    "paletteIdx" INTEGER NOT NULL,
    "rarity" "Rarity" NOT NULL,
    "lore" TEXT,
    "isActive" BOOLEAN NOT NULL DEFAULT true,

    CONSTRAINT "species_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "egg_types" (
    "id" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "colorHex" TEXT NOT NULL,
    "priceCoin" INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT "egg_types_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "egg_drop_table" (
    "id" TEXT NOT NULL,
    "eggTypeId" TEXT NOT NULL,
    "speciesId" TEXT NOT NULL,
    "weight" INTEGER NOT NULL DEFAULT 1,

    CONSTRAINT "egg_drop_table_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "rarity_weights" (
    "rarity" "Rarity" NOT NULL,
    "weight" INTEGER NOT NULL,

    CONSTRAINT "rarity_weights_pkey" PRIMARY KEY ("rarity")
);

-- CreateTable
CREATE TABLE "shop_items" (
    "id" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "category" "ShopCategory" NOT NULL,
    "priceCoin" INTEGER NOT NULL,
    "isActive" BOOLEAN NOT NULL DEFAULT true,

    CONSTRAINT "shop_items_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "user_inventory" (
    "id" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "shopItemId" TEXT NOT NULL,
    "acquiredAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "equipped" BOOLEAN NOT NULL DEFAULT false,

    CONSTRAINT "user_inventory_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "sessions" (
    "id" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "eggTypeId" TEXT NOT NULL,
    "plannedMin" INTEGER NOT NULL,
    "strictMode" BOOLEAN NOT NULL DEFAULT true,
    "status" "SessionStatus" NOT NULL DEFAULT 'RUNNING',
    "startedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "endedAt" TIMESTAMP(3),
    "resultSpeciesId" TEXT,
    "clientEventId" TEXT,

    CONSTRAINT "sessions_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "currency_ledger" (
    "id" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "amount" INTEGER NOT NULL,
    "reason" "LedgerReason" NOT NULL,
    "refSessionId" TEXT,
    "refShopItemId" TEXT,
    "clientEventId" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "currency_ledger_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "user_collection" (
    "id" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "speciesId" TEXT NOT NULL,
    "hatchCount" INTEGER NOT NULL DEFAULT 0,
    "isFavorite" BOOLEAN NOT NULL DEFAULT false,
    "firstHatchedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "lastHatchedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "user_collection_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "users_email_key" ON "users"("email");

-- CreateIndex
CREATE UNIQUE INDEX "egg_drop_table_eggTypeId_speciesId_key" ON "egg_drop_table"("eggTypeId", "speciesId");

-- CreateIndex
CREATE UNIQUE INDEX "user_inventory_userId_shopItemId_key" ON "user_inventory"("userId", "shopItemId");

-- CreateIndex
CREATE UNIQUE INDEX "sessions_clientEventId_key" ON "sessions"("clientEventId");

-- CreateIndex
CREATE INDEX "sessions_userId_startedAt_idx" ON "sessions"("userId", "startedAt");

-- CreateIndex
CREATE UNIQUE INDEX "currency_ledger_clientEventId_key" ON "currency_ledger"("clientEventId");

-- CreateIndex
CREATE INDEX "currency_ledger_userId_createdAt_idx" ON "currency_ledger"("userId", "createdAt");

-- CreateIndex
CREATE UNIQUE INDEX "user_collection_userId_speciesId_key" ON "user_collection"("userId", "speciesId");

-- AddForeignKey
ALTER TABLE "user_settings" ADD CONSTRAINT "user_settings_userId_fkey" FOREIGN KEY ("userId") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "egg_drop_table" ADD CONSTRAINT "egg_drop_table_eggTypeId_fkey" FOREIGN KEY ("eggTypeId") REFERENCES "egg_types"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "egg_drop_table" ADD CONSTRAINT "egg_drop_table_speciesId_fkey" FOREIGN KEY ("speciesId") REFERENCES "species"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "user_inventory" ADD CONSTRAINT "user_inventory_userId_fkey" FOREIGN KEY ("userId") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "user_inventory" ADD CONSTRAINT "user_inventory_shopItemId_fkey" FOREIGN KEY ("shopItemId") REFERENCES "shop_items"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "sessions" ADD CONSTRAINT "sessions_userId_fkey" FOREIGN KEY ("userId") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "sessions" ADD CONSTRAINT "sessions_eggTypeId_fkey" FOREIGN KEY ("eggTypeId") REFERENCES "egg_types"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "currency_ledger" ADD CONSTRAINT "currency_ledger_userId_fkey" FOREIGN KEY ("userId") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "user_collection" ADD CONSTRAINT "user_collection_userId_fkey" FOREIGN KEY ("userId") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "user_collection" ADD CONSTRAINT "user_collection_speciesId_fkey" FOREIGN KEY ("speciesId") REFERENCES "species"("id") ON DELETE CASCADE ON UPDATE CASCADE;
