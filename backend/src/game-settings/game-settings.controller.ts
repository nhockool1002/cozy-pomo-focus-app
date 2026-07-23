import { Controller, Get } from '@nestjs/common';
import { ApiTags } from '@nestjs/swagger';
import { GameSettingsService } from './game-settings.service';

/** Công khai (không cần JWT) — app cần đọc tỉ lệ quy đổi để hiển thị trước khi đăng nhập cũng được. */
@ApiTags('game-settings')
@Controller('game-settings')
export class GameSettingsController {
  constructor(private readonly gameSettingsService: GameSettingsService) {}

  @Get()
  get() {
    return this.gameSettingsService.get();
  }
}
