-- AlterTable
ALTER TABLE "shop_items" ADD COLUMN     "purchasable" BOOLEAN NOT NULL DEFAULT true;

-- DropTable
DROP TABLE "rarity_weights";

-- CreateTable
CREATE TABLE "egg_rarity_weights" (
    "eggTypeId" TEXT NOT NULL,
    "rarity" "Rarity" NOT NULL,
    "weight" INTEGER NOT NULL,

    CONSTRAINT "egg_rarity_weights_pkey" PRIMARY KEY ("eggTypeId","rarity")
);

-- AddForeignKey
ALTER TABLE "egg_rarity_weights" ADD CONSTRAINT "egg_rarity_weights_eggTypeId_fkey" FOREIGN KEY ("eggTypeId") REFERENCES "egg_types"("id") ON DELETE CASCADE ON UPDATE CASCADE;

