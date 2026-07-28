import { ApiProperty } from '@nestjs/swagger';
import { IsOptional, IsString } from 'class-validator';

export class UseItemDto {
  @ApiProperty({
    required: false,
    description: 'Bắt buộc khi dùng vật phẩm bổ trợ HATCH_MINUTES — id trứng đang ấp để cộng thêm phút.',
  })
  @IsOptional()
  @IsString()
  ownedEggId?: string;
}
