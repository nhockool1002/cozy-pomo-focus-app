import { DynamicModule, Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import * as path from 'path';
import { PrismaModule } from '../prisma/prisma.module';
import { PrismaService } from '../prisma/prisma.service';
import { ADMIN_VI_TRANSLATIONS } from './admin-i18n';

// Bảng màu CozyPomo chính thức — xem docs/technical-spec.md / Brand Field Guide.
const BRAND_COLORS = {
  bg: '#F9F6F0',
  container: '#FFFFFF',
  sidebar: '#FFFFFF',
  ink: '#3E332A',
  inkSoft: '#6D594E',
  primary: '#4F7A3D',
  primaryLight: '#DCEACB',
  accent: '#F4D160',
  warn: '#E76F51',
  border: 'rgba(109,89,78,0.16)',
};

const COMPONENTS_DIR = path.join(process.cwd(), 'src/admin/components');

// adminjs / @adminjs/nestjs / @adminjs/prisma chỉ xuất bản ESM — project này build CommonJS,
// nên phải nạp bằng dynamic import() thay vì import tĩnh (Nest tự await Promise trong `imports`).
async function buildAdminJsModule(): Promise<DynamicModule> {
  const [{ AdminModule: AdminJSNestModule }, { default: AdminJS, ComponentLoader }, { Database, Resource, getModelByName }] =
    await Promise.all([
      import('@adminjs/nestjs'),
      import('adminjs'),
      import('@adminjs/prisma'),
    ]);

  AdminJS.registerAdapter({ Database, Resource });

  const componentLoader = new ComponentLoader();
  const SpeciesList = componentLoader.add('SpeciesList', path.join(COMPONENTS_DIR, 'SpeciesList'));
  const SpeciesShow = componentLoader.add('SpeciesShow', path.join(COMPONENTS_DIR, 'SpeciesShow'));
  const EggTypeList = componentLoader.add('EggTypeList', path.join(COMPONENTS_DIR, 'EggTypeList'));
  const ApiExplorer = componentLoader.add('ApiExplorer', path.join(COMPONENTS_DIR, 'ApiExplorer'));

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
        componentLoader,
        locale: {
          language: 'vi',
          availableLanguages: ['vi'],
          translations: { vi: ADMIN_VI_TRANSLATIONS },
        },
        branding: {
          companyName: 'CozyPomo Admin',
          softwareBrothers: false,
          logo: '/branding/logo.png',
          favicon: '/branding/favicon.png',
          theme: {
            colors: {
              primary100: BRAND_COLORS.primary,
              primary80: '#6E9857',
              primary60: '#8DB675',
              primary40: '#B6D2A0',
              primary20: BRAND_COLORS.primaryLight,
              accent: BRAND_COLORS.accent,
              text: BRAND_COLORS.ink,
              grey100: BRAND_COLORS.ink,
              grey80: BRAND_COLORS.inkSoft,
              grey60: '#95816F',
              grey40: '#C7B8A8',
              grey20: '#F1EADA',
              bg: BRAND_COLORS.bg,
              container: BRAND_COLORS.container,
              sidebar: BRAND_COLORS.sidebar,
              border: BRAND_COLORS.border,
              separator: BRAND_COLORS.border,
              highlight: '#F1EADA',
              errorDark: '#B23F22',
              error: BRAND_COLORS.warn,
              errorLight: '#FBE0D7',
              successDark: '#3F5C2E',
              success: '#7FAE64',
              successLight: BRAND_COLORS.primaryLight,
              warningDark: '#8A6A10',
              warning: BRAND_COLORS.accent,
              warningLight: '#FBF0CE',
              love: BRAND_COLORS.warn,
            },
          },
        },
        resources: [
          {
            resource: { model: getModelByName('Species'), client: prisma },
            options: {
              navigation: { name: 'Nội dung game' },
              actions: {
                list: { component: SpeciesList },
                show: { component: SpeciesShow },
              },
            },
          },
          {
            resource: { model: getModelByName('EggType'), client: prisma },
            options: {
              navigation: { name: 'Nội dung game' },
              actions: {
                list: { component: EggTypeList },
              },
            },
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
        pages: {
          'api-explorer': {
            component: ApiExplorer,
          },
        },
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
