-- CreateEnum
CREATE TYPE "CurrencyType" AS ENUM ('COIN', 'FOCUS_MINUTE');

-- CreateEnum
CREATE TYPE "OwnedEggStatus" AS ENUM ('INCUBATING', 'HATCHED');

-- DropForeignKey
ALTER TABLE "sessions" DROP CONSTRAINT "sessions_eggTypeId_fkey";

-- DropForeignKey
ALTER TABLE "sessions" DROP CONSTRAINT "sessions_resultSpeciesId_fkey";

-- DropIndex
DROP INDEX "currency_ledger_userId_createdAt_idx";

-- AlterTable
ALTER TABLE "currency_ledger" ADD COLUMN     "currency" "CurrencyType" NOT NULL DEFAULT 'COIN';

-- AlterTable
ALTER TABLE "egg_types" ADD COLUMN     "hatchDurationMin" INTEGER NOT NULL DEFAULT 60,
ADD COLUMN     "priceHours" INTEGER NOT NULL DEFAULT 0;

-- AlterTable
ALTER TABLE "sessions" DROP COLUMN "eggTypeId",
DROP COLUMN "resultSpeciesId",
ADD COLUMN     "incubationRatio" DOUBLE PRECISION,
ADD COLUMN     "minutesAccumulated" INTEGER,
ADD COLUMN     "minutesIncubated" INTEGER,
ADD COLUMN     "ownedEggId" TEXT;

-- CreateTable
CREATE TABLE "owned_eggs" (
    "id" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "eggTypeId" TEXT NOT NULL,
    "status" "OwnedEggStatus" NOT NULL DEFAULT 'INCUBATING',
    "incubatedMin" INTEGER NOT NULL DEFAULT 0,
    "acquiredAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "hatchedAt" TIMESTAMP(3),
    "resultSpeciesId" TEXT,

    CONSTRAINT "owned_eggs_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "game_settings" (
    "id" INTEGER NOT NULL DEFAULT 1,
    "coinsPerFocusMinute" DOUBLE PRECISION NOT NULL DEFAULT 1,

    CONSTRAINT "game_settings_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE INDEX "owned_eggs_userId_status_idx" ON "owned_eggs"("userId", "status");

-- CreateIndex
CREATE INDEX "currency_ledger_userId_currency_createdAt_idx" ON "currency_ledger"("userId", "currency", "createdAt");

-- AddForeignKey
ALTER TABLE "owned_eggs" ADD CONSTRAINT "owned_eggs_userId_fkey" FOREIGN KEY ("userId") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "owned_eggs" ADD CONSTRAINT "owned_eggs_eggTypeId_fkey" FOREIGN KEY ("eggTypeId") REFERENCES "egg_types"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "owned_eggs" ADD CONSTRAINT "owned_eggs_resultSpeciesId_fkey" FOREIGN KEY ("resultSpeciesId") REFERENCES "species"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "sessions" ADD CONSTRAINT "sessions_ownedEggId_fkey" FOREIGN KEY ("ownedEggId") REFERENCES "owned_eggs"("id") ON DELETE SET NULL ON UPDATE CASCADE;

