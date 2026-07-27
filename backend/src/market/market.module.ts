import { Module } from '@nestjs/common';
import { MarketController } from './market.controller';
import { MarketService } from './market.service';
import { CurrencyModule } from '../currency/currency.module';
import { GameSettingsModule } from '../game-settings/game-settings.module';
import { InboxModule } from '../inbox/inbox.module';

@Module({
  imports: [CurrencyModule, GameSettingsModule, InboxModule],
  controllers: [MarketController],
  providers: [MarketService],
  exports: [MarketService],
})
export class MarketModule {}
