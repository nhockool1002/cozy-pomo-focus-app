-- CreateEnum
CREATE TYPE "SessionLabel" AS ENUM ('STUDY', 'WORK', 'READING', 'CREATIVE', 'OTHER');

-- AlterTable
ALTER TABLE "sessions" ADD COLUMN     "label" "SessionLabel";
