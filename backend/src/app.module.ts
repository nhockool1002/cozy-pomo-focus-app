import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { ThrottlerGuard, ThrottlerModule } from '@nestjs/throttler';
import { APP_GUARD } from '@nestjs/core';
import { PrismaModule } from './prisma/prisma.module';
import { AuthModule } from './auth/auth.module';
import { HealthController } from './health/health.controller';
import { SpeciesModule } from './species/species.module';
import { EggsModule } from './eggs/eggs.module';
import { SessionsModule } from './sessions/sessions.module';
import { CollectionModule } from './collection/collection.module';
import { CurrencyModule } from './currency/currency.module';
import { ShopModule } from './shop/shop.module';
import { StatsModule } from './stats/stats.module';
import { SettingsModule } from './settings/settings.module';
import { SyncModule } from './sync/sync.module';
import { AdminModule } from './admin/admin.module';

@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true }),
    ThrottlerModule.forRoot([{ ttl: 60000, limit: 100 }]),
    PrismaModule,
    AuthModule,
    SpeciesModule,
    EggsModule,
    SessionsModule,
    CollectionModule,
    CurrencyModule,
    ShopModule,
    StatsModule,
    SettingsModule,
    SyncModule,
    AdminModule,
  ],
  controllers: [HealthController],
  providers: [{ provide: APP_GUARD, useClass: ThrottlerGuard }],
})
export class AppModule {}
