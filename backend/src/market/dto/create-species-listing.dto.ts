import { ApiProperty } from '@nestjs/swagger';
import { IsInt, IsOptional, IsString, IsUUID, Min } from 'class-validator';

export class CreateSpeciesListingDto {
  @ApiProperty({ description: 'Loài muốn bán — phải đang sở hữu ít nhất 1 bản (ownedCount ≥ 1)' })
  @IsUUID()
  speciesId!: string;

  @ApiProperty({ minimum: 1 })
  @IsInt()
  @Min(1)
  priceCoin!: number;

  @ApiProperty({ required: false, description: 'Dùng để chống trùng khi app đồng bộ lại lúc mất mạng' })
  @IsOptional()
  @IsString()
  clientEventId?: string;
}
