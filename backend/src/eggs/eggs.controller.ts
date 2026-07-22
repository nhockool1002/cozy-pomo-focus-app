import { Controller, Get, NotFoundException, Param } from '@nestjs/common';
import { ApiTags } from '@nestjs/swagger';
import { EggsService } from './eggs.service';

@ApiTags('eggs')
@Controller('egg-types')
export class EggsController {
  constructor(private readonly eggsService: EggsService) {}

  @Get()
  findAll() {
    return this.eggsService.findAll();
  }

  @Get(':id')
  async findOne(@Param('id') id: string) {
    const eggType = await this.eggsService.findOne(id);
    if (!eggType) {
      throw new NotFoundException('Không tìm thấy loại trứng này');
    }
    return eggType;
  }

  @Get(':id/odds')
  getOdds(@Param('id') id: string) {
    return this.eggsService.getOdds(id);
  }
}
