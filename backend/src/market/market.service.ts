import { ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import {
  CurrencyType,
  LedgerReason,
  MarketItemType,
  MarketListingSellerType,
  MarketListingStatus,
  OwnedEggStatus,
  Rarity,
  SessionStatus,
  SpeciesCategory,
} from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';
import { CurrencyService } from '../currency/currency.service';
import { GameSettingsService } from '../game-settings/game-settings.service';
import { InboxService } from '../inbox/inbox.service';
import { CreateSpeciesListingDto } from './dto/create-species-listing.dto';
import { CreateEggListingDto } from './dto/create-egg-listing.dto';

const LISTING_INCLUDE = {
  species: true,
  ownedEgg: { include: { eggType: true } },
  seller: { select: { id: true, email: true, displayName: true } },
} as const;

/**
 * Chợ (T-106, docs/wireframes/market-feature.html) — user bán bản sao dư loài/thực vật/trứng cho
 * user khác, Admin bán vật phẩm siêu hiếm. Luật chơi theo yêu cầu Dev1002 (2026-07-26):
 * không ép giá sàn/trần, phí giao dịch 10% (GameSettings.marketFeePercent, trừ vào phần người bán
 * nhận — không hoàn ai, coi như currency sink), loài SSR/MYTHIC do USER đăng phải chờ Admin duyệt
 * (PENDING_APPROVAL → ACTIVE/REJECTED), tối đa GameSettings.maxActiveListingsPerUser tin ACTIVE/
 * PENDING_APPROVAL cùng lúc mỗi user.
 */
@Injectable()
export class MarketService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly currencyService: CurrencyService,
    private readonly gameSettingsService: GameSettingsService,
    private readonly inboxService: InboxService,
  ) {}

  /** `mine=true` trả TOÀN BỘ trạng thái (kể cả PENDING_APPROVAL/REJECTED/SOLD/CANCELLED) để user
   * tự theo dõi tin của mình; mặc định (duyệt Chợ) chỉ trả tin đang bán công khai. */
  findAll(userId: string, mine: boolean) {
    return this.prisma.marketListing.findMany({
      where: mine ? { sellerId: userId } : { status: MarketListingStatus.ACTIVE },
      include: LISTING_INCLUDE,
      orderBy: { createdAt: 'desc' },
    });
  }

  private async assertUnderListingCap(userId: string) {
    const gameSettings = await this.gameSettingsService.get();
    const activeCount = await this.prisma.marketListing.count({
      where: {
        sellerId: userId,
        status: { in: [MarketListingStatus.ACTIVE, MarketListingStatus.PENDING_APPROVAL] },
      },
    });
    if (activeCount >= gameSettings.maxActiveListingsPerUser) {
      throw new ForbiddenException(`Bạn chỉ được đăng tối đa ${gameSettings.maxActiveListingsPerUser} tin cùng lúc`);
    }
  }

  async createSpeciesListing(userId: string, dto: CreateSpeciesListingDto) {
    if (dto.clientEventId) {
      const existing = await this.prisma.marketListing.findUnique({ where: { clientEventId: dto.clientEventId } });
      if (existing) return existing;
    }

    await this.assertUnderListingCap(userId);

    const entry = await this.prisma.collectionEntry.findUnique({
      where: { userId_speciesId: { userId, speciesId: dto.speciesId } },
      include: { species: true },
    });
    if (!entry || entry.ownedCount < 1) {
      throw new ForbiddenException('Bạn không sở hữu bản nào của loài này');
    }

    const alreadyListed = await this.prisma.marketListing.count({
      where: {
        sellerId: userId,
        speciesId: dto.speciesId,
        itemType: MarketItemType.SPECIES,
        status: { in: [MarketListingStatus.ACTIVE, MarketListingStatus.PENDING_APPROVAL] },
      },
    });
    if (entry.ownedCount - alreadyListed < 1) {
      throw new ForbiddenException('Bạn đã đăng bán hết số bản đang sở hữu của loài này');
    }

    const needsApproval = entry.species.rarity === Rarity.SSR || entry.species.category === SpeciesCategory.MYTHIC;

    return this.prisma.marketListing.create({
      data: {
        itemType: MarketItemType.SPECIES,
        sellerType: MarketListingSellerType.USER,
        sellerId: userId,
        speciesId: dto.speciesId,
        priceCoin: dto.priceCoin,
        status: needsApproval ? MarketListingStatus.PENDING_APPROVAL : MarketListingStatus.ACTIVE,
        clientEventId: dto.clientEventId,
      },
      include: LISTING_INCLUDE,
    });
  }

  async createEggListing(userId: string, dto: CreateEggListingDto) {
    if (dto.clientEventId) {
      const existing = await this.prisma.marketListing.findUnique({ where: { clientEventId: dto.clientEventId } });
      if (existing) return existing;
    }

    await this.assertUnderListingCap(userId);

    const egg = await this.prisma.ownedEgg.findUnique({ where: { id: dto.ownedEggId } });
    if (!egg || egg.userId !== userId) {
      throw new NotFoundException('Không tìm thấy trứng này trong bộ sưu tập của bạn');
    }
    if (egg.status !== OwnedEggStatus.INCUBATING) {
      throw new ForbiddenException('Chỉ bán được trứng đang ấp');
    }
    const runningSession = await this.prisma.session.findFirst({
      where: { ownedEggId: egg.id, status: SessionStatus.RUNNING },
    });
    if (runningSession) {
      throw new ForbiddenException('Trứng này đang gắn với 1 phiên tập trung đang chạy, không thể đăng bán');
    }
    const existingListing = await this.prisma.marketListing.findFirst({
      where: {
        ownedEggId: egg.id,
        status: { in: [MarketListingStatus.ACTIVE, MarketListingStatus.PENDING_APPROVAL] },
      },
    });
    if (existingListing) {
      throw new ForbiddenException('Trứng này đã có tin đăng bán rồi');
    }

    // Trứng không cần duyệt (chỉ SPECIES rarity SSR/MYTHIC mới cần) — luôn ACTIVE ngay.
    return this.prisma.marketListing.create({
      data: {
        itemType: MarketItemType.EGG,
        sellerType: MarketListingSellerType.USER,
        sellerId: userId,
        ownedEggId: dto.ownedEggId,
        priceCoin: dto.priceCoin,
        status: MarketListingStatus.ACTIVE,
        clientEventId: dto.clientEventId,
      },
      include: LISTING_INCLUDE,
    });
  }

  async cancel(userId: string, listingId: string) {
    const result = await this.prisma.marketListing.updateMany({
      where: {
        id: listingId,
        sellerId: userId,
        status: { in: [MarketListingStatus.ACTIVE, MarketListingStatus.PENDING_APPROVAL] },
      },
      data: { status: MarketListingStatus.CANCELLED, cancelledAt: new Date() },
    });
    if (result.count !== 1) {
      throw new NotFoundException('Không tìm thấy tin đăng đang hoạt động của bạn');
    }
    return this.prisma.marketListing.findUniqueOrThrow({ where: { id: listingId }, include: LISTING_INCLUDE });
  }

  /**
   * Mua 1 tin đăng — idempotent qua chốt trạng thái ACTIVE→SOLD bằng `updateMany` (chặn double-buy
   * race, đúng idiom đã dùng ở `sessions.service.ts`). Phí giao dịch (`marketFeePercent`) trừ vào
   * phần người bán USER nhận được, không hoàn ai — Admin bán thì không có ai nhận tiền nên không
   * áp phí (không có gì để trừ).
   */
  async buy(userId: string, listingId: string, clientEventId?: string) {
    const listing = await this.prisma.marketListing.findUnique({ where: { id: listingId } });
    if (!listing || listing.status !== MarketListingStatus.ACTIVE) {
      throw new ForbiddenException('Tin đăng không còn nữa');
    }
    if (listing.sellerType === MarketListingSellerType.USER && listing.sellerId === userId) {
      throw new ForbiddenException('Không thể tự mua tin đăng của chính mình');
    }

    const gameSettings = await this.gameSettingsService.get();
    const feeRate = gameSettings.marketFeePercent / 100;
    const sellerEarns =
      listing.sellerType === MarketListingSellerType.USER ? Math.round(listing.priceCoin * (1 - feeRate)) : 0;

    // Toàn bộ "chốt tin đăng + chuyển tiền + chuyển quyền sở hữu" phải chung 1 transaction —
    // nếu chỉ updateMany chốt trạng thái riêng rồi mới trừ tiền, lúc spend() báo không đủ Xu Lá
    // (rất hay gặp) sẽ để lại tin đăng kẹt vĩnh viễn ở SOLD dù chưa ai trả tiền (bug thật đã gặp
    // khi tự test luồng này bằng curl — sửa bằng cách gộp updateMany vào cùng $transaction).
    await this.prisma.$transaction(async (tx) => {
      const claimed = await tx.marketListing.updateMany({
        where: { id: listingId, status: MarketListingStatus.ACTIVE },
        data: { status: MarketListingStatus.SOLD, buyerId: userId, soldAt: new Date() },
      });
      if (claimed.count !== 1) {
        throw new ForbiddenException('Tin đăng không còn nữa');
      }

      await this.currencyService.spend(userId, listing.priceCoin, LedgerReason.MARKET_BUY, {
        currency: CurrencyType.COIN,
        refMarketListingId: listing.id,
        clientEventId: clientEventId ? `${clientEventId}:buy` : undefined,
        tx,
      });

      if (listing.sellerType === MarketListingSellerType.USER && listing.sellerId && sellerEarns > 0) {
        await this.currencyService.earn(listing.sellerId, sellerEarns, LedgerReason.MARKET_SELL, {
          currency: CurrencyType.COIN,
          refMarketListingId: listing.id,
          clientEventId: clientEventId ? `${clientEventId}:sell` : undefined,
          tx,
        });
      }

      if (listing.itemType === MarketItemType.SPECIES && listing.speciesId) {
        if (listing.sellerType === MarketListingSellerType.USER && listing.sellerId) {
          await tx.collectionEntry.update({
            where: { userId_speciesId: { userId: listing.sellerId, speciesId: listing.speciesId } },
            data: { ownedCount: { decrement: 1 } },
          });
        }
        await tx.collectionEntry.upsert({
          where: { userId_speciesId: { userId, speciesId: listing.speciesId } },
          create: { userId, speciesId: listing.speciesId, hatchCount: 0, ownedCount: 1 },
          update: { ownedCount: { increment: 1 } },
        });
      } else if (listing.itemType === MarketItemType.EGG && listing.ownedEggId) {
        const ownedEgg = await tx.ownedEgg.update({
          where: { id: listing.ownedEggId },
          data: { userId },
          include: { eggType: true },
        });
        await this.inboxService.create(
          userId,
          'EGG_RECEIVED',
          'Bạn vừa nhận được trứng mới!',
          `Bạn đã mua thành công ${ownedEgg.eggType.name} từ Chợ.`,
          { eggTypeId: ownedEgg.eggTypeId, ownedEggId: ownedEgg.id },
          tx,
        );
      }
    });

    return this.prisma.marketListing.findUniqueOrThrow({ where: { id: listingId }, include: LISTING_INCLUDE });
  }
}
