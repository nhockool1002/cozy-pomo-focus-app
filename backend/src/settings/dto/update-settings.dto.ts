import { ApiProperty } from '@nestjs/swagger';
import { IsBoolean, IsInt, IsOptional, IsString, Max, Min } from 'class-validator';

export class UpdateSettingsDto {
  @ApiProperty({ required: false, minimum: 10, maximum: 120 })
  @IsOptional()
  @IsInt()
  @Min(10)
  @Max(120)
  focusMinutes?: number;

  @ApiProperty({ required: false, minimum: 1, maximum: 60 })
  @IsOptional()
  @IsInt()
  @Min(1)
  @Max(60)
  breakMinutes?: number;

  @ApiProperty({ required: false })
  @IsOptional()
  @IsBoolean()
  strictModeEnabled?: boolean;

  @ApiProperty({ required: false })
  @IsOptional()
  @IsString()
  soundTheme?: string;
}
