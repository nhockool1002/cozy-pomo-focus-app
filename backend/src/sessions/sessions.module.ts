import { Module } from '@nestjs/common';
import { SessionsController } from './sessions.controller';
import { SessionsService } from './sessions.service';
import { EggsModule } from '../eggs/eggs.module';
import { CurrencyModule } from '../currency/currency.module';
import { CollectionModule } from '../collection/collection.module';

@Module({
  imports: [EggsModule, CurrencyModule, CollectionModule],
  controllers: [SessionsController],
  providers: [SessionsService],
  exports: [SessionsService],
})
export class SessionsModule {}
