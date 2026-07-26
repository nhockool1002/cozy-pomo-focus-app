import { ApiProperty } from '@nestjs/swagger';
import { IsOptional, IsString } from 'class-validator';

export class BuyListingDto {
  @ApiProperty({ required: false, description: 'Dùng để chống trùng khi app đồng bộ lại lúc mất mạng' })
  @IsOptional()
  @IsString()
  clientEventId?: string;
}
