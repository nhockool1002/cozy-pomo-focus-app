import { ApiProperty } from '@nestjs/swagger';
import { CurrencyType, Rarity } from '@prisma/client';
import { IsEnum, IsInt, IsOptional, IsString, Min } from 'class-validator';

export class GrantCurrencyDto {
  @ApiProperty({ enum: CurrencyType })
  @IsEnum(CurrencyType)
  currency!: CurrencyType;

  @ApiProperty({ minimum: 1, default: 1000 })
  @IsInt()
  @Min(1)
  amount!: number;
}

export class GrantEggDto {
  @ApiProperty({ required: false, description: 'Mặc định "Trứng Bí Ẩn" nếu không truyền' })
  @IsOptional()
  @IsString()
  eggTypeName?: string;
}

export class GrantSpeciesDto {
  @ApiProperty({ enum: Rarity })
  @IsEnum(Rarity)
  rarity!: Rarity;
}
