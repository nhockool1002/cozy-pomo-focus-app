import { ApiProperty } from '@nestjs/swagger';
import { IsBoolean, IsInt, IsOptional, IsString, IsUUID, Max, Min } from 'class-validator';

export class CreateSessionDto {
  @ApiProperty()
  @IsUUID()
  eggTypeId!: string;

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
