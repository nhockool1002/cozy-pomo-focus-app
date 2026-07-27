import { Module } from '@nestjs/common';
import { ShopController } from './shop.controller';
import { ShopService } from './shop.service';
import { CurrencyModule } from '../currency/currency.module';
import { OwnedEggsModule } from '../owned-eggs/owned-eggs.module';
import { InboxModule } from '../inbox/inbox.module';

@Module({
  imports: [CurrencyModule, OwnedEggsModule, InboxModule],
  controllers: [ShopController],
  providers: [ShopService],
  exports: [ShopService],
})
export class ShopModule {}
