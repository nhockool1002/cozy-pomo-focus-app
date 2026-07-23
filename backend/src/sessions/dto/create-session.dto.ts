import { ApiProperty } from '@nestjs/swagger';
import { CurrencyType } from '@prisma/client';
import { IsBoolean, IsEnum, IsInt, IsNumber, IsOptional, IsString, IsUUID, Max, Min } from 'class-validator';

export class CreateSessionDto {
  @ApiProperty({
    required: false,
    description: 'Trứng đang sở hữu muốn ấp trong phiên này — không bắt buộc, có thể tập trung mà không ấp trứng nào',
  })
  @IsOptional()
  @IsUUID()
  ownedEggId?: string;

  @ApiProperty({
    required: false,
    minimum: 0,
    maximum: 1,
    default: 1,
    description: 'Tỉ lệ % thời gian dành cho ấp trứng (phần còn lại vào Giờ tích luỹ) — chỉ có ý nghĩa khi có ownedEggId',
  })
  @IsOptional()
  @IsNumber()
  @Min(0)
  @Max(1)
  incubationRatio?: number;

  @ApiProperty({ minimum: 10, maximum: 120 })
  @IsInt()
  @Min(10)
  @Max(120)
  plannedMin!: number;

  @ApiProperty({
    required: false,
    enum: CurrencyType,
    default: CurrencyType.COIN,
    description: 'Phần thời gian không dành cho ấp trứng sẽ quy đổi thành CHỈ 1 loại tiền này — Xu Lá hoặc Giờ tích luỹ, không nhận cả 2.',
  })
  @IsOptional()
  @IsEnum(CurrencyType)
  rewardCurrency?: CurrencyType;

  @ApiProperty({ default: true })
  @IsBoolean()
  strictMode: boolean = true;

  @ApiProperty({ required: false, description: 'Dùng để chống trùng khi app đồng bộ lại lúc mất mạng' })
  @IsOptional()
  @IsString()
  clientEventId?: string;
}
