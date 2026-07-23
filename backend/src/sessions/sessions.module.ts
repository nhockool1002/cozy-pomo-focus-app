import { Module } from '@nestjs/common';
import { SessionsController } from './sessions.controller';
import { SessionsService } from './sessions.service';
import { CurrencyModule } from '../currency/currency.module';
import { GameSettingsModule } from '../game-settings/game-settings.module';
import { OwnedEggsModule } from '../owned-eggs/owned-eggs.module';

@Module({
  imports: [CurrencyModule, GameSettingsModule, OwnedEggsModule],
  controllers: [SessionsController],
  providers: [SessionsService],
  exports: [SessionsService],
})
export class SessionsModule {}
