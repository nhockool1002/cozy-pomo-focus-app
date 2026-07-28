import React, { useEffect, useMemo, useState } from 'react';
import { ApiClient } from 'adminjs';
import type { ActionProps, RecordJSON } from 'adminjs';
import SpeciesThumbnail from './SpeciesThumbnail.js';
import { CARD_FX_CSS, RARITY_BADGE } from './species-art.js';
import { renderEggArt, renderEggAura, eggTierForPrice, EGG_TIER_BADGE, EggTier } from './egg-art.js';
import { renderBoostArt, renderBoostAura, boostTierFor, BOOST_TIER_BADGE, BOOST_TIER_LABEL, renderJarArt, renderMusicArt } from './item-art.js';

const api = new ApiClient();

const BRAND = {
  bg: '#F9F6F0',
  surface: '#FFFFFF',
  ink: '#6D594E',
  inkSoft: '#95816F',
  primary: '#A8D08D',
  primaryInk: '#3F5C2E',
  accentInk: '#8A6A10',
  border: 'rgba(109,89,78,0.16)',
};

type Tab = 'SPECIES' | 'EGG_TYPE' | 'SHOP_ITEM';
const TABS: Tab[] = ['SPECIES', 'EGG_TYPE', 'SHOP_ITEM'];
const TAB_RESOURCE: Record<Tab, string> = { SPECIES: 'Species', EGG_TYPE: 'EggType', SHOP_ITEM: 'ShopItem' };
const TAB_LABEL: Record<Tab, string> = { SPECIES: 'Loài', EGG_TYPE: 'Loại Trứng', SHOP_ITEM: 'Vật phẩm hỗ trợ' };

function defaultTabFor(resourceId: string | undefined): Tab {
  if (resourceId === 'EggType') return 'EGG_TYPE';
  if (resourceId === 'ShopItem') return 'SHOP_ITEM';
  return 'SPECIES';
}

const SPECIES_CATEGORIES = [
  { value: '', label: 'Tất cả' },
  { value: 'FOREST', label: 'Thú rừng' },
  { value: 'SEA', label: 'Sinh vật biển' },
  { value: 'PLANT', label: 'Thực vật' },
  { value: 'MYTHIC', label: 'Thần thú' },
];
const SPECIES_RARITIES = ['', 'B', 'A', 'S', 'SS', 'SSR'];

const EGG_TIERS: Array<{ value: '' | EggTier; label: string }> = [
  { value: '', label: 'Tất cả' },
  { value: 'common', label: 'Thường' },
  { value: 'rare', label: 'Hiếm' },
  { value: 'legendary', label: 'Huyền thoại' },
];

type ShopCategory = '' | 'EGG' | 'JAR_SKIN' | 'MUSIC' | 'BOOST';
const SHOP_CATEGORIES: Array<{ value: ShopCategory; label: string }> = [
  { value: '', label: 'Tất cả' },
  { value: 'EGG', label: 'Trứng' },
  { value: 'JAR_SKIN', label: 'Vỏ bình' },
  { value: 'MUSIC', label: 'Nhạc nền' },
  { value: 'BOOST', label: 'Vật phẩm hỗ trợ' },
];

const pillStyle = (active: boolean): React.CSSProperties => ({
  fontSize: 12.5,
  fontWeight: 600,
  padding: '7px 14px',
  borderRadius: 999,
  border: `1px solid ${active ? 'transparent' : BRAND.border}`,
  background: active ? BRAND.primary : BRAND.surface,
  color: active ? BRAND.primaryInk : BRAND.inkSoft,
  cursor: 'pointer',
  whiteSpace: 'nowrap',
});

const tabStyle = (active: boolean): React.CSSProperties => ({
  fontSize: 14.5,
  fontWeight: 700,
  padding: '11px 24px',
  borderRadius: '12px 12px 0 0',
  border: `1px solid ${BRAND.border}`,
  borderBottom: active ? `2px solid ${BRAND.surface}` : `1px solid ${BRAND.border}`,
  background: active ? BRAND.surface : 'transparent',
  color: active ? BRAND.primaryInk : BRAND.inkSoft,
  cursor: 'pointer',
  marginBottom: -1,
});

const cardLinkStyle = (extra?: React.CSSProperties): React.CSSProperties => ({
  background: BRAND.surface,
  border: `1px solid ${BRAND.border}`,
  borderRadius: 14,
  padding: '18px 10px 14px',
  display: 'flex',
  flexDirection: 'column',
  alignItems: 'center',
  gap: 8,
  textDecoration: 'none',
  color: 'inherit',
  position: 'relative',
  ...extra,
});

/** Trang gộp "Quản Lý Item" (T-132, Dev1002 yêu cầu) — 1 link sidebar DUY NHẤT (đăng ký làm `page`
 * riêng, không phải resource nào — xem `pages['item-management']` ở `admin.module.ts`) thay cho 3
 * mục sidebar cũ (Loài/Loại Trứng/Vật phẩm hỗ trợ), chia 3 tab trong CÙNG 1 màn hình. Component này
 * ĐỒNG THỜI cũng được đăng ký làm `list` action của cả 3 resource Species/EggType/ShopItem (dù đã
 * ẩn khỏi sidebar qua `navigation: false`) — cần thiết để khi bấm 1 thẻ mở Sửa trong Drawer
 * (`edit.showInDrawer: true`), route `/admin/resources/<id>/records/:id/edit` vẽ lại ĐÚNG màn hình
 * này (đúng tab tương ứng, nhờ `defaultTabFor`) làm nền phía sau Drawer thay vì màn trống. Vì vậy
 * prop `resource` là OPTIONAL: có khi vào qua `page` (không có), có khi vào qua route resource nói
 * trên (có) — cả 2 trường hợp đều tự chọn đúng tab mặc định, chuyển tab trong lúc xem không điều
 * hướng trang (chỉ đổi state), giữ nguyên bộ lọc riêng của từng tab khi qua lại.
 */
const ItemManagementList: React.FC<Partial<ActionProps>> = ({ resource }) => {
  const [tab, setTab] = useState<Tab>(defaultTabFor(resource?.id));
  const [loading, setLoading] = useState(true);
  const [speciesRecords, setSpeciesRecords] = useState<RecordJSON[]>([]);
  const [eggTypeRecords, setEggTypeRecords] = useState<RecordJSON[]>([]);
  const [shopItemRecords, setShopItemRecords] = useState<RecordJSON[]>([]);

  const [speciesCategory, setSpeciesCategory] = useState('');
  const [speciesRarity, setSpeciesRarity] = useState('');
  const [speciesSearch, setSpeciesSearch] = useState('');

  const [eggTier, setEggTier] = useState<'' | EggTier>('');
  const [eggSearch, setEggSearch] = useState('');

  const [shopCategory, setShopCategory] = useState<ShopCategory>('');
  const [shopSearch, setShopSearch] = useState('');

  useEffect(() => {
    setLoading(true);
    Promise.all([
      api.resourceAction({ resourceId: 'Species', actionName: 'list', params: { perPage: 250, sortBy: 'name', direction: 'asc' } } as any),
      api.resourceAction({ resourceId: 'EggType', actionName: 'list', params: { perPage: 250, sortBy: 'name', direction: 'asc' } } as any),
      api.resourceAction({ resourceId: 'ShopItem', actionName: 'list', params: { perPage: 250, sortBy: 'name', direction: 'asc' } } as any),
    ])
      .then(([speciesRes, eggRes, shopRes]) => {
        setSpeciesRecords(((speciesRes.data as any).records ?? []) as RecordJSON[]);
        setEggTypeRecords(((eggRes.data as any).records ?? []) as RecordJSON[]);
        setShopItemRecords(((shopRes.data as any).records ?? []) as RecordJSON[]);
      })
      .finally(() => setLoading(false));
  }, []);

  const eggTypeColorMap = useMemo(() => {
    const map = new Map<string, string>();
    eggTypeRecords.forEach((r) => map.set(r.id, String(r.params.colorHex)));
    return map;
  }, [eggTypeRecords]);

  const filteredSpecies = useMemo(() => {
    return speciesRecords.filter((r) => {
      if (speciesCategory && r.params.category !== speciesCategory) return false;
      if (speciesRarity && r.params.rarity !== speciesRarity) return false;
      if (speciesSearch && !String(r.params.name).toLowerCase().includes(speciesSearch.toLowerCase())) return false;
      return true;
    });
  }, [speciesRecords, speciesCategory, speciesRarity, speciesSearch]);

  const filteredEggTypes = useMemo(() => {
    return eggTypeRecords.filter((r) => {
      const priceCoin = Number(r.params.priceCoin) || 0;
      if (eggTier && eggTierForPrice(priceCoin) !== eggTier) return false;
      if (eggSearch && !String(r.params.name).toLowerCase().includes(eggSearch.toLowerCase())) return false;
      return true;
    });
  }, [eggTypeRecords, eggTier, eggSearch]);

  const filteredShopItems = useMemo(() => {
    return shopItemRecords.filter((r) => {
      if (shopCategory && r.params.category !== shopCategory) return false;
      if (shopSearch && !String(r.params.name).toLowerCase().includes(shopSearch.toLowerCase())) return false;
      return true;
    });
  }, [shopItemRecords, shopCategory, shopSearch]);

  const newHref = `/admin/resources/${TAB_RESOURCE[tab]}/actions/new`;

  return (
    <div style={{ background: BRAND.bg, padding: '24px', fontFamily: 'inherit' }}>
      <style>{CARD_FX_CSS}</style>

      <div style={{ display: 'flex', gap: 4, marginBottom: 0 }}>
        {TABS.map((t) => (
          <span key={t} style={tabStyle(tab === t)} onClick={() => setTab(t)}>
            {TAB_LABEL[t]}
          </span>
        ))}
      </div>
      <div style={{ background: BRAND.surface, border: `1px solid ${BRAND.border}`, borderRadius: '0 10px 10px 10px', padding: 20 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12, marginBottom: 16 }}>
          <h1 style={{ fontSize: 22, fontWeight: 700, color: BRAND.ink, margin: 0 }}>{TAB_LABEL[tab]}</h1>
          <a href={newHref} style={{ fontSize: 13, fontWeight: 700, padding: '9px 16px', borderRadius: 999, background: BRAND.primary, color: BRAND.primaryInk, textDecoration: 'none' }}>
            + Tạo mới
          </a>
        </div>

        {loading ? (
          <p style={{ color: BRAND.inkSoft }}>Đang tải...</p>
        ) : tab === 'SPECIES' ? (
          <>
            <input
              type="text"
              placeholder="Tìm theo tên..."
              value={speciesSearch}
              onChange={(e) => setSpeciesSearch(e.target.value)}
              style={{ width: '100%', maxWidth: 320, padding: '9px 12px', marginBottom: 12, borderRadius: 10, border: `1px solid ${BRAND.border}`, fontSize: 13 }}
            />
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 8 }}>
              {SPECIES_CATEGORIES.map((c) => (
                <span key={c.value} style={pillStyle(speciesCategory === c.value)} onClick={() => setSpeciesCategory(c.value)}>
                  {c.label}
                </span>
              ))}
            </div>
            <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginBottom: 20 }}>
              {SPECIES_RARITIES.map((r) => (
                <span key={r || 'all'} style={pillStyle(speciesRarity === r)} onClick={() => setSpeciesRarity(r)}>
                  {r || 'Mọi cấp bậc'}
                </span>
              ))}
            </div>
            <div style={{ color: BRAND.inkSoft, fontSize: 12.5, marginBottom: 12 }}>{filteredSpecies.length}/{speciesRecords.length} loài</div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(150px, 1fr))', gap: 12 }}>
              {filteredSpecies.map((record) => {
                const cardRarity = String(record.params.rarity);
                const badge = RARITY_BADGE[cardRarity];
                const isSs = cardRarity === 'SS';
                return (
                  <a
                    key={record.id}
                    href={`/admin/resources/Species/records/${record.id}/edit`}
                    style={cardLinkStyle(isSs ? { boxShadow: '0 0 0 1px rgba(231,111,81,0.5), 0 0 14px 2px rgba(231,111,81,0.28)' } : undefined)}
                  >
                    {badge ? (
                      <span className="sp-badge" style={{ background: badge.bg, color: badge.fg }}>
                        {cardRarity}
                      </span>
                    ) : null}
                    <SpeciesThumbnail
                      category={String(record.params.category)}
                      archetype={String(record.params.archetype)}
                      paletteIdx={Number(record.params.paletteIdx)}
                      name={String(record.params.name)}
                      rarity={cardRarity}
                      size={72}
                    />
                    <span style={{ fontSize: 12.5, fontWeight: 600, color: BRAND.ink, textAlign: 'center', lineHeight: 1.3 }}>{record.params.name}</span>
                  </a>
                );
              })}
            </div>
          </>
        ) : tab === 'EGG_TYPE' ? (
          <>
            <input
              type="text"
              placeholder="Tìm theo tên..."
              value={eggSearch}
              onChange={(e) => setEggSearch(e.target.value)}
              style={{ width: '100%', maxWidth: 320, padding: '9px 12px', marginBottom: 12, borderRadius: 10, border: `1px solid ${BRAND.border}`, fontSize: 13 }}
            />
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 20 }}>
              {EGG_TIERS.map((t) => (
                <span key={t.value || 'all'} style={pillStyle(eggTier === t.value)} onClick={() => setEggTier(t.value)}>
                  {t.label}
                </span>
              ))}
            </div>
            <div style={{ color: BRAND.inkSoft, fontSize: 12.5, marginBottom: 12 }}>{filteredEggTypes.length}/{eggTypeRecords.length} loại trứng</div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(150px, 1fr))', gap: 12 }}>
              {filteredEggTypes.map((record) => {
                const name = String(record.params.name);
                const colorHex = String(record.params.colorHex);
                const priceCoin = Number(record.params.priceCoin) || 0;
                const cardTier = eggTierForPrice(priceCoin);
                const badge = EGG_TIER_BADGE[cardTier];
                const icon = renderEggArt({ colorHex, name });
                const aura = renderEggAura(colorHex, priceCoin, name);
                return (
                  <a key={record.id} href={`/admin/resources/EggType/records/${record.id}/edit`} style={cardLinkStyle(cardTier === 'legendary' ? { boxShadow: '0 0 0 1px #F4D160' } : undefined)}>
                    <span className="sp-badge" style={{ background: badge.bg, color: badge.fg }}>
                      {cardTier === 'legendary' ? 'Huyền thoại' : cardTier === 'rare' ? 'Hiếm' : 'Thường'}
                    </span>
                    <div className="sp-icon-wrap" style={{ width: 72, height: 72 }} dangerouslySetInnerHTML={{ __html: aura + icon }} />
                    <span style={{ fontSize: 12.5, fontWeight: 600, color: BRAND.ink, textAlign: 'center', lineHeight: 1.3 }}>{name}</span>
                    <span style={{ fontSize: 11, fontWeight: 700, color: BRAND.accentInk }}>{priceCoin} Xu Lá</span>
                  </a>
                );
              })}
            </div>
          </>
        ) : (
          <>
            <input
              type="text"
              placeholder="Tìm theo tên..."
              value={shopSearch}
              onChange={(e) => setShopSearch(e.target.value)}
              style={{ width: '100%', maxWidth: 320, padding: '9px 12px', marginBottom: 12, borderRadius: 10, border: `1px solid ${BRAND.border}`, fontSize: 13 }}
            />
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 20 }}>
              {SHOP_CATEGORIES.map((c) => (
                <span key={c.value || 'all'} style={pillStyle(shopCategory === c.value)} onClick={() => setShopCategory(c.value)}>
                  {c.label}
                </span>
              ))}
            </div>
            <div style={{ color: BRAND.inkSoft, fontSize: 12.5, marginBottom: 12 }}>{filteredShopItems.length}/{shopItemRecords.length} vật phẩm</div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(150px, 1fr))', gap: 12 }}>
              {filteredShopItems.map((record) => {
                const name = String(record.params.name);
                const category = String(record.params.category);
                const priceCoin = Number(record.params.priceCoin) || 0;
                const isActive = record.params.isActive === true || record.params.isActive === 'true';
                let icon = '';
                let aura = '';
                let badge: { fg: string; bg: string } | undefined;
                let badgeLabel: string | undefined;
                if (category === 'EGG') {
                  // @adminjs/prisma phơi FK qua path TÊN QUAN HỆ ("eggType"), không phải tên cột
                  // ("eggTypeId") — record.params.eggType giữ thẳng UUID (không phải object lồng).
                  const colorHex = eggTypeColorMap.get(String(record.params.eggType)) ?? '#9CB380';
                  const eggTier = eggTierForPrice(priceCoin);
                  icon = renderEggArt({ colorHex, name });
                  aura = renderEggAura(colorHex, priceCoin, name);
                  badge = EGG_TIER_BADGE[eggTier];
                } else if (category === 'BOOST') {
                  const boostType = record.params.boostType ? String(record.params.boostType) : null;
                  const boostAmount = record.params.boostAmount != null ? Number(record.params.boostAmount) : null;
                  const purchasable = record.params.purchasable === true || record.params.purchasable === 'true';
                  const boostTier = boostTierFor(boostType, boostAmount, purchasable);
                  icon = renderBoostArt({ boostType, tier: boostTier, name });
                  aura = renderBoostAura(boostType, boostTier);
                  badge = BOOST_TIER_BADGE[boostTier];
                  badgeLabel = BOOST_TIER_LABEL[boostTier];
                } else if (category === 'JAR_SKIN') {
                  icon = renderJarArt(name);
                } else {
                  icon = renderMusicArt();
                }
                return (
                  <a key={record.id} href={`/admin/resources/ShopItem/records/${record.id}/edit`} style={cardLinkStyle({ opacity: isActive ? 1 : 0.55 })}>
                    {badge && badgeLabel ? (
                      <span className="sp-badge" style={{ background: badge.bg, color: badge.fg }}>
                        {badgeLabel}
                      </span>
                    ) : null}
                    <div className="sp-icon-wrap" style={{ width: 72, height: 72 }} dangerouslySetInnerHTML={{ __html: aura + icon }} />
                    <span style={{ fontSize: 12.5, fontWeight: 600, color: BRAND.ink, textAlign: 'center', lineHeight: 1.3 }}>{name}</span>
                    <span style={{ fontSize: 11, fontWeight: 700, color: BRAND.accentInk }}>{priceCoin > 0 ? `${priceCoin} Xu Lá` : 'Không bán qua Cửa hàng'}</span>
                  </a>
                );
              })}
            </div>
          </>
        )}
      </div>
    </div>
  );
};

export default ItemManagementList;
