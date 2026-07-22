import { ApiProperty } from '@nestjs/swagger';
import { IsOptional, IsString } from 'class-validator';

export class CompleteSessionDto {
  @ApiProperty({ required: false })
  @IsOptional()
  @IsString()
  clientEventId?: string;
}
