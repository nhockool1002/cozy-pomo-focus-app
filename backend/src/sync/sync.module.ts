import { Module } from '@nestjs/common';
import { SyncController } from './sync.controller';
import { SyncService } from './sync.service';
import { SessionsModule } from '../sessions/sessions.module';
import { ShopModule } from '../shop/shop.module';

@Module({
  imports: [SessionsModule, ShopModule],
  controllers: [SyncController],
  providers: [SyncService],
})
export class SyncModule {}
