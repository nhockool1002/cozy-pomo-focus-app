import { DynamicModule, Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { PrismaModule } from '../prisma/prisma.module';
import { PrismaService } from '../prisma/prisma.service';

// adminjs / @adminjs/nestjs / @adminjs/prisma chỉ xuất bản ESM — project này build CommonJS,
// nên phải nạp bằng dynamic import() thay vì import tĩnh (Nest tự await Promise trong `imports`).
async function buildAdminJsModule(): Promise<DynamicModule> {
  const [{ AdminModule: AdminJSNestModule }, { default: AdminJS }, { Database, Resource, getModelByName }] =
    await Promise.all([
      import('@adminjs/nestjs'),
      import('adminjs'),
      import('@adminjs/prisma'),
    ]);

  AdminJS.registerAdapter({ Database, Resource });

  // Các bảng dữ liệu người dùng chỉ xem, không sửa/xoá qua admin — tránh làm hỏng
  // tính toàn vẹn ledger/collection (sửa trực tiếp nên đi qua API để giữ đúng nghiệp vụ).
  const readOnly = {
    actions: {
      new: { isAccessible: false },
      edit: { isAccessible: false },
      delete: { isAccessible: false },
      bulkDelete: { isAccessible: false },
    },
  };

  return AdminJSNestModule.createAdminAsync({
    imports: [ConfigModule, PrismaModule],
    inject: [ConfigService, PrismaService],
    useFactory: (config: ConfigService, prisma: PrismaService) => ({
      adminJsOptions: {
        rootPath: '/admin',
        branding: { companyName: 'CozyPomo Admin', softwareBrothers: false },
        resources: [
          {
            resource: { model: getModelByName('Species'), client: prisma },
            options: { navigation: { name: 'Nội dung game' } },
          },
          {
            resource: { model: getModelByName('EggType'), client: prisma },
            options: { navigation: { name: 'Nội dung game' } },
          },
          {
            resource: { model: getModelByName('EggDropEntry'), client: prisma },
            options: { navigation: { name: 'Nội dung game' } },
          },
          {
            resource: { model: getModelByName('RarityWeight'), client: prisma },
            options: { navigation: { name: 'Nội dung game' } },
          },
          {
            resource: { model: getModelByName('ShopItem'), client: prisma },
            options: { navigation: { name: 'Nội dung game' } },
          },
          {
            resource: { model: getModelByName('User'), client: prisma },
            options: {
              navigation: { name: 'Người dùng' },
              properties: { passwordHash: { isVisible: false } },
              ...readOnly,
            },
          },
          {
            resource: { model: getModelByName('UserSettings'), client: prisma },
            options: { navigation: { name: 'Người dùng' }, ...readOnly },
          },
          {
            resource: { model: getModelByName('Session'), client: prisma },
            options: { navigation: { name: 'Người dùng' }, ...readOnly },
          },
          {
            resource: { model: getModelByName('LedgerEntry'), client: prisma },
            options: { navigation: { name: 'Người dùng' }, ...readOnly },
          },
          {
            resource: { model: getModelByName('CollectionEntry'), client: prisma },
            options: { navigation: { name: 'Người dùng' }, ...readOnly },
          },
          {
            resource: { model: getModelByName('InventoryItem'), client: prisma },
            options: { navigation: { name: 'Người dùng' }, ...readOnly },
          },
        ],
      },
      auth: {
        authenticate: async (email: string, password: string) => {
          const adminEmail = config.get<string>('ADMIN_EMAIL');
          const adminPassword = config.get<string>('ADMIN_PASSWORD');
          if (adminEmail && adminPassword && email === adminEmail && password === adminPassword) {
            return { email };
          }
          return null;
        },
        cookieName: 'cozypomo-admin',
        cookiePassword: config.get<string>('ADMIN_COOKIE_SECRET') ?? 'change-me-in-env',
      },
      sessionOptions: {
        resave: false,
        saveUninitialized: false,
        secret: config.get<string>('ADMIN_COOKIE_SECRET') ?? 'change-me-in-env',
      },
    }),
  });
}

@Module({
  imports: [buildAdminJsModule()],
})
export class AdminModule {}
