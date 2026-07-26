import { ApiProperty } from '@nestjs/swagger';
import { IsInt, IsOptional, IsString, IsUUID, Min } from 'class-validator';

export class CreateEggListingDto {
  @ApiProperty({ description: 'Trứng đang ấp muốn bán — không thuộc phiên tập trung đang RUNNING' })
  @IsUUID()
  ownedEggId!: string;

  @ApiProperty({ minimum: 1 })
  @IsInt()
  @Min(1)
  priceCoin!: number;

  @ApiProperty({ required: false, description: 'Dùng để chống trùng khi app đồng bộ lại lúc mất mạng' })
  @IsOptional()
  @IsString()
  clientEventId?: string;
}
