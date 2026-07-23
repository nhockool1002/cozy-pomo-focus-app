import { Module } from '@nestjs/common';
import { CurrencyModule } from '../currency/currency.module';
import { DebugController } from './debug.controller';
import { DebugService } from './debug.service';

@Module({
  imports: [CurrencyModule],
  controllers: [DebugController],
  providers: [DebugService],
})
export class DebugModule {}
