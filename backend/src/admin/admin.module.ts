import { DynamicModule, Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import * as path from 'path';
import * as fs from 'fs';
import { PrismaModule } from '../prisma/prisma.module';
import { PrismaService } from '../prisma/prisma.service';
import { CollectionModule } from '../collection/collection.module';
import { CollectionService } from '../collection/collection.service';
import { OwnedEggsModule } from '../owned-eggs/owned-eggs.module';
import { OwnedEggsService } from '../owned-eggs/owned-eggs.service';
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
  const GiftPage = componentLoader.add('GiftPage', path.join(COMPONENTS_DIR, 'GiftPage'));

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
    imports: [ConfigModule, PrismaModule, CollectionModule, OwnedEggsModule],
    inject: [ConfigService, PrismaService, CollectionService, OwnedEggsService],
    useFactory: (
      config: ConfigService,
      prisma: PrismaService,
      collectionService: CollectionService,
      ownedEggsService: OwnedEggsService,
    ) => ({
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
            // T-116 — trọng số cấp bậc RIÊNG theo từng loại trứng (thay `RarityWeight` toàn cục
            // cũ) — admin lọc theo `eggTypeId` để sửa từng "đường cong" tỉ lệ 1 loại trứng.
            resource: { model: getModelByName('EggRarityWeight'), client: prisma },
            options: { navigation: { name: 'Nội dung game' } },
          },
          {
            resource: { model: getModelByName('ShopItem'), client: prisma },
            options: { navigation: { name: 'Nội dung game' } },
          },
          {
            resource: { model: getModelByName('GameSettings'), client: prisma },
            options: {
              navigation: { name: 'Nội dung game' },
              actions: {
                // Chỉ 1 dòng cấu hình duy nhất (id=1) — không cho tạo thêm/xoá, chỉ sửa.
                new: { isAccessible: false },
                delete: { isAccessible: false },
                bulkDelete: { isAccessible: false },
              },
            },
          },
          {
            // T-121 — Force Update: singleton giống GameSettings, admin bump
            // `minSupportedVersionCode` mỗi lần release app cần ép cập nhật.
            resource: { model: getModelByName('AppVersionConfig'), client: prisma },
            options: {
              navigation: { name: 'Nội dung game' },
              actions: {
                new: { isAccessible: false },
                delete: { isAccessible: false },
                bulkDelete: { isAccessible: false },
              },
            },
          },
          {
            // Chợ (T-106) — Admin tạo tin đăng sellerType=ADMIN (vật phẩm siêu hiếm, giữ nguyên
            // action `new` để làm việc này) + duyệt tin PENDING_APPROVAL của user (SSR/MYTHIC) qua
            // đúng form edit chuẩn, chỉ cần đổi field `status` sang ACTIVE hoặc REJECTED.
            resource: { model: getModelByName('MarketListing'), client: prisma },
            options: { navigation: { name: 'Chợ' } },
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
          {
            // T-116 — cho phép `new`: đây chính là cách Admin "phát thưởng" trứng Truyền Thuyết
            // (chọn User + EggType, tạo thẳng 1 OwnedEgg INCUBATING) — vẫn khoá edit/delete để
            // không ai chỉnh tay tiến độ ấp/kết quả nở (phải đi qua API để giữ đúng nghiệp vụ).
            resource: { model: getModelByName('OwnedEgg'), client: prisma },
            options: {
              navigation: { name: 'Người dùng' },
              actions: {
                edit: { isAccessible: false },
                delete: { isAccessible: false },
                bulkDelete: { isAccessible: false },
              },
            },
          },
          {
            // "Phát quà" (T-120) — trước đây là 1 Page rời dưới mục "Trang" (tách biệt khỏi các
            // menu khác); Dev1002 phản hồi đây cũng là 1 chức năng chính, không nên bị hạ thấp vị
            // trí. Chuyển thành resource thật `GiftLog` nằm chung nhóm "Người dùng" — action `new`
            // override bằng UI multi-select cũ (GiftPage.tsx, chỉ đổi endpoint gọi), `list`/`show`
            // mặc định giờ hiện được lịch sử phát quà thật (trước đây submit xong không lưu vết gì).
            resource: { model: getModelByName('GiftLog'), client: prisma },
            options: {
              navigation: { name: 'Người dùng' },
              actions: {
                new: {
                  component: GiftPage,
                  handler: async (request: any) => {
                    if (request.method !== 'post') {
                      return { ok: true };
                    }
                    try {
                      const payload = request.payload ?? {};
                      const recipientsText: string = payload.recipientsText ?? '';
                      let grants: Array<{ kind: 'SPECIES' | 'EGG'; id: string; quantity: number }> = [];
                      try {
                        grants = JSON.parse(payload.grants ?? '[]');
                      } catch {
                        return { ok: false, error: 'Dữ liệu vật phẩm không hợp lệ' };
                      }

                      // Gom token người nhận từ textarea + file CSV (nếu có) — 1 cột ID hoặc email,
                      // chấp nhận cả 2 định dạng cùng lúc (dòng nào giống UUID thì tra theo id, còn
                      // lại tra theo email), bỏ qua dòng tiêu đề CSV thường gặp (id/email/userId).
                      const tokens = new Set<string>();
                      recipientsText
                        .split(/[\n,]/)
                        .map((s: string) => s.trim())
                        .filter(Boolean)
                        .forEach((t: string) => tokens.add(t));

                      const csvFile = payload.csvFile;
                      if (csvFile && csvFile.path) {
                        try {
                          const content = fs.readFileSync(csvFile.path, 'utf-8');
                          content
                            .split(/\r?\n/)
                            .map((line: string) => line.split(',')[0]?.trim())
                            .filter((t: string) => t && !['id', 'email', 'userid'].includes(t.toLowerCase()))
                            .forEach((t: string) => tokens.add(t));
                        } catch {
                          // Đọc file lỗi — vẫn tiếp tục với token đã có từ textarea, không chặn cả submit.
                        }
                      }

                      if (tokens.size === 0) {
                        return { ok: false, error: 'Chưa nhập người nhận nào' };
                      }
                      if (grants.length === 0) {
                        return { ok: false, error: 'Chưa chọn vật phẩm nào' };
                      }

                      const tokenList = [...tokens];
                      const uuidRe = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
                      const idTokens = tokenList.filter((t) => uuidRe.test(t));
                      const emailTokens = tokenList.filter((t) => !uuidRe.test(t));

                      const users = await prisma.user.findMany({
                        where: { OR: [{ id: { in: idTokens } }, { email: { in: emailTokens } }] },
                        select: { id: true, email: true },
                      });
                      const foundTokens = new Set<string>();
                      users.forEach((u) => {
                        foundTokens.add(u.id);
                        foundTokens.add(u.email);
                      });
                      const recipientsNotFound = tokenList.filter((t) => !foundTokens.has(t));

                      // Tra tên vật phẩm thật (species/eggType) để lưu snapshot vào GiftLog +
                      // dùng tạo InboxMessage (T-123) — 1 lượt hỏi DB dùng chung cho mọi user/grant.
                      const speciesIds = grants.filter((g) => g.kind === 'SPECIES').map((g) => g.id);
                      const eggTypeIds = grants.filter((g) => g.kind === 'EGG').map((g) => g.id);
                      const [speciesRows, eggTypeRows] = await Promise.all([
                        speciesIds.length ? prisma.species.findMany({ where: { id: { in: speciesIds } } }) : [],
                        eggTypeIds.length ? prisma.eggType.findMany({ where: { id: { in: eggTypeIds } } }) : [],
                      ]);
                      const itemNameOf = (g: { kind: 'SPECIES' | 'EGG'; id: string }) =>
                        (g.kind === 'SPECIES' ? speciesRows : eggTypeRows).find((r) => r.id === g.id)?.name ?? 'Vật phẩm';

                      let grantsApplied = 0;
                      const errors: string[] = [];
                      for (const user of users) {
                        for (const g of grants) {
                          const itemName = itemNameOf(g);
                          try {
                            await prisma.$transaction(async (tx) => {
                              if (g.kind === 'SPECIES') {
                                await collectionService.grantMany(user.id, g.id, g.quantity, tx);
                              } else {
                                for (let i = 0; i < g.quantity; i++) {
                                  await ownedEggsService.create(user.id, g.id, tx);
                                }
                              }
                              await tx.giftLog.create({
                                data: { recipientId: user.id, itemKind: g.kind, itemId: g.id, itemName, quantity: g.quantity },
                              });
                              await tx.inboxMessage.create({
                                data: {
                                  userId: user.id,
                                  type: 'ADMIN_GIFT',
                                  title: 'Bạn nhận được quà từ Admin!',
                                  body: `Admin đã tặng bạn ${g.quantity} ${itemName}.`,
                                  payload: { kind: g.kind, id: g.id, quantity: g.quantity },
                                },
                              });
                            });
                            grantsApplied += 1;
                          } catch (err: any) {
                            errors.push(`${user.email}: ${err.message}`);
                          }
                        }
                      }

                      return {
                        ok: true,
                        recipientsResolved: users.length,
                        recipientsNotFound,
                        grantsApplied,
                        errors,
                      };
                    } catch (err: any) {
                      return { ok: false, error: err?.message ?? 'Có lỗi xảy ra, thử lại sau' };
                    }
                  },
                },
                edit: { isAccessible: false },
                delete: { isAccessible: false },
                bulkDelete: { isAccessible: false },
              },
            },
          },
          {
            // T-123 — Hộp thư: chỉ xem để hỗ trợ debug/support, không sửa/xoá qua admin (đi qua
            // API để giữ đúng `isRead`/nghiệp vụ, giống các bảng dữ liệu user khác).
            resource: { model: getModelByName('InboxMessage'), client: prisma },
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
