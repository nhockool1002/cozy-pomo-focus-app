import { Module } from '@nestjs/common';
import { OwnedEggsController } from './owned-eggs.controller';
import { OwnedEggsService } from './owned-eggs.service';
import { EggsModule } from '../eggs/eggs.module';
import { CollectionModule } from '../collection/collection.module';

@Module({
  imports: [EggsModule, CollectionModule],
  controllers: [OwnedEggsController],
  providers: [OwnedEggsService],
  exports: [OwnedEggsService],
})
export class OwnedEggsModule {}
