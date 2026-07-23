-- AlterTable
ALTER TABLE "game_settings" ALTER COLUMN "coinsPerFocusMinute" SET DEFAULT 10;

-- AlterTable
ALTER TABLE "sessions" ADD COLUMN     "rewardCurrency" "CurrencyType" NOT NULL DEFAULT 'COIN';

