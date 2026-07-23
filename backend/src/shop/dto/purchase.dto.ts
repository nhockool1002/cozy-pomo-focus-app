import { ApiProperty } from '@nestjs/swagger';
import { CurrencyType } from '@prisma/client';
import { IsEnum, IsOptional, IsString } from 'class-validator';

export class PurchaseDto {
  @ApiProperty({ required: false })
  @IsOptional()
  @IsString()
  clientEventId?: string;

  @ApiProperty({
    required: false,
    enum: CurrencyType,
    default: CurrencyType.COIN,
    description: 'Chỉ áp dụng cho vật phẩm loại EGG — trả bằng Xu Lá hay Giờ tích luỹ. Vật phẩm khác luôn trả bằng Xu Lá.',
  })
  @IsOptional()
  @IsEnum(CurrencyType)
  payWith?: CurrencyType;
}
