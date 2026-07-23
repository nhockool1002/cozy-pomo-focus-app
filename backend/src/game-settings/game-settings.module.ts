import { Module } from '@nestjs/common';
import { GameSettingsController } from './game-settings.controller';
import { GameSettingsService } from './game-settings.service';

@Module({
  controllers: [GameSettingsController],
  providers: [GameSettingsService],
  exports: [GameSettingsService],
})
export class GameSettingsModule {}
