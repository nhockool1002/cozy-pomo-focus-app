-- AlterTable
ALTER TABLE "sessions" ADD COLUMN     "coinsEarned" INTEGER;

-- AlterTable
ALTER TABLE "shop_items" ADD COLUMN     "description" TEXT,
ADD COLUMN     "eggTypeId" TEXT;

-- AddForeignKey
ALTER TABLE "shop_items" ADD CONSTRAINT "shop_items_eggTypeId_fkey" FOREIGN KEY ("eggTypeId") REFERENCES "egg_types"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "sessions" ADD CONSTRAINT "sessions_resultSpeciesId_fkey" FOREIGN KEY ("resultSpeciesId") REFERENCES "species"("id") ON DELETE SET NULL ON UPDATE CASCADE;
