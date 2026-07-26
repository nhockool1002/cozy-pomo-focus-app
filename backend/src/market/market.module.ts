import { Module } from '@nestjs/common';
import { MarketController } from './market.controller';
import { MarketService } from './market.service';
import { CurrencyModule } from '../currency/currency.module';
import { GameSettingsModule } from '../game-settings/game-settings.module';

@Module({
  imports: [CurrencyModule, GameSettingsModule],
  controllers: [MarketController],
  providers: [MarketService],
  exports: [MarketService],
})
export class MarketModule {}
