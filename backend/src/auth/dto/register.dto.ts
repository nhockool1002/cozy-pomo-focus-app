import { ApiProperty } from '@nestjs/swagger';
import { IsEmail, IsOptional, IsString, MinLength } from 'class-validator';

export class RegisterDto {
  @ApiProperty({ example: 'user@example.com' })
  @IsEmail()
  email!: string;

  @ApiProperty({ example: 'at-least-8-chars', minLength: 8 })
  @IsString()
  @MinLength(8)
  password!: string;

  @ApiProperty({ required: false, example: 'Dev1002' })
  @IsOptional()
  @IsString()
  displayName?: string;
}
