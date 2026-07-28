import { Module } from '@nestjs/common';
import { CollectionModule } from '../collection/collection.module';
import { CurrencyModule } from '../currency/currency.module';
import { InboxModule } from '../inbox/inbox.module';
import { OwnedEggsModule } from '../owned-eggs/owned-eggs.module';
import { ShopModule } from '../shop/shop.module';
import { StreaksService } from './streaks.service';

@Module({
  imports: [CollectionModule, OwnedEggsModule, ShopModule, CurrencyModule, InboxModule],
  providers: [StreaksService],
  exports: [StreaksService],
})
export class StreaksModule {}
