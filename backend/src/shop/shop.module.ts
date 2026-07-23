import { Module } from '@nestjs/common';
import { ShopController } from './shop.controller';
import { ShopService } from './shop.service';
import { CurrencyModule } from '../currency/currency.module';
import { OwnedEggsModule } from '../owned-eggs/owned-eggs.module';

@Module({
  imports: [CurrencyModule, OwnedEggsModule],
  controllers: [ShopController],
  providers: [ShopService],
  exports: [ShopService],
})
export class ShopModule {}
