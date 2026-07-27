import { Controller, Get } from '@nestjs/common';
import { ApiTags } from '@nestjs/swagger';
import { AppVersionService } from './app-version.service';

/**
 * Công khai (không cần JWT) — gọi được ngay ở Splash (T-122) trước khi có token, để quyết định
 * chặn Force Update trước cả khi biết user đã đăng nhập hay chưa.
 */
@ApiTags('app-version')
@Controller('app-version')
export class AppVersionController {
  constructor(private readonly appVersionService: AppVersionService) {}

  @Get()
  get() {
    return this.appVersionService.get();
  }
}
