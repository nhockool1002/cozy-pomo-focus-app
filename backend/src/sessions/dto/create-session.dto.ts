import { ApiProperty } from '@nestjs/swagger';
import { IsBoolean, IsInt, IsNumber, IsOptional, IsString, IsUUID, Max, Min } from 'class-validator';

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

  @ApiProperty({ default: true })
  @IsBoolean()
  strictMode: boolean = true;

  @ApiProperty({ required: false, description: 'Dùng để chống trùng khi app đồng bộ lại lúc mất mạng' })
  @IsOptional()
  @IsString()
  clientEventId?: string;
}
