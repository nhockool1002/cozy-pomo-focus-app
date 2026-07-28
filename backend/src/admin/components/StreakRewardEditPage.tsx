import React, { useEffect, useMemo, useState } from 'react';
import { ApiClient } from 'adminjs';
import type { ActionProps, RecordJSON } from 'adminjs';
import SpeciesThumbnail from './SpeciesThumbnail.js';
import { CARD_FX_CSS, RARITY_BADGE } from './species-art.js';
import { renderEggArt, renderEggAura, eggTierForPrice, EGG_TIER_BADGE } from './egg-art.js';

const api = new ApiClient();

const BRAND = {
  bg: '#F9F6F0',
  surface: '#FFFFFF',
  ink: '#6D594E',
  inkSoft: '#95816F',
  primary: '#A8D08D',
  primaryInk: '#3F5C2E',
  accent: '#F4D160',
  warnBg: '#FBE0D7',
  warnInk: '#B23F22',
  border: 'rgba(109,89,78,0.16)',
};

type Tab = 'FOREST' | 'SEA' | 'PLANT' | 'MYTHIC' | 'EGG' | 'BOOST';
const TABS: Array<{ value: Tab; label: string }> = [
  { value: 'FOREST', label: 'Thú rừng' },
  { value: 'SEA', label: 'Sinh vật biển' },
  { value: 'PLANT', label: 'Thực vật' },
  { value: 'MYTHIC', label: 'Thần thú' },
  { value: 'EGG', label: 'Trứng' },
  { value: 'BOOST', label: 'Vật phẩm hỗ trợ' },
];

type RewardKind = 'SPECIES' | 'EGG_TYPE' | 'SHOP_ITEM' | 'COIN';
type SelectedReward = { kind: RewardKind; id?: string; name: string; quantity: number };

type SubmitResult = { ok: boolean; error?: string };

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

function parseExistingRewards(raw: unknown): SelectedReward[] {
  if (!raw) return [];
  const value = typeof raw === 'string' ? JSON.parse(raw || '[]') : raw;
  if (!Array.isArray(value)) return [];
  return value.map((r: any) => ({ kind: r.kind, id: r.id, name: r.name, quantity: r.quantity }));
}

/** Cấu hình quà streak cho 1 ngày (record = StreakRewardDay, id = số ngày 1-7) — clone bố cục
 * GiftPage.tsx (chọn vật phẩm theo tab) nhưng scope theo 1 record, không có phần "người nhận",
 * thêm ô "Tặng kèm Xu Lá" (kind COIN không có id thật). */
const StreakRewardEditPage: React.FC<ActionProps> = ({ record }) => {
  const day = record ? Number(record.params.day ?? record.id) : 0;

  const [speciesRecords, setSpeciesRecords] = useState<RecordJSON[]>([]);
  const [eggRecords, setEggRecords] = useState<RecordJSON[]>([]);
  const [shopItemRecords, setShopItemRecords] = useState<RecordJSON[]>([]);
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState<Tab>('FOREST');
  const [search, setSearch] = useState('');
  const [selected, setSelected] = useState<Record<string, SelectedReward>>({});
  const [coinAmount, setCoinAmount] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<SubmitResult | null>(null);

  useEffect(() => {
    Promise.all([
      api.resourceAction({ resourceId: 'Species', actionName: 'list', params: { perPage: 250, sortBy: 'name', direction: 'asc' } } as any),
      api.resourceAction({ resourceId: 'EggType', actionName: 'list', params: { perPage: 250, sortBy: 'name', direction: 'asc' } } as any),
      api.resourceAction({ resourceId: 'ShopItem', actionName: 'list', params: { perPage: 250, sortBy: 'name', direction: 'asc' } } as any),
    ])
      .then(([speciesRes, eggRes, shopItemRes]) => {
        setSpeciesRecords((speciesRes.data as any).records ?? []);
        setEggRecords((eggRes.data as any).records ?? []);
        setShopItemRecords((shopItemRes.data as any).records ?? []);
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (!record) return;
    const existing = parseExistingRewards(record.params.rewards);
    const next: Record<string, SelectedReward> = {};
    let coin = 0;
    existing.forEach((r) => {
      if (r.kind === 'COIN') {
        coin += r.quantity;
      } else {
        next[`${r.kind}:${r.id}`] = r;
      }
    });
    setSelected(next);
    setCoinAmount(coin > 0 ? String(coin) : '');
  }, [record]);

  const filteredSpecies = useMemo(() => {
    if (tab === 'EGG' || tab === 'BOOST') return [];
    return speciesRecords.filter((r) => {
      if (r.params.category !== tab) return false;
      if (search && !String(r.params.name).toLowerCase().includes(search.toLowerCase())) return false;
      return true;
    });
  }, [speciesRecords, tab, search]);

  const filteredEggs = useMemo(() => {
    if (tab !== 'EGG') return [];
    return eggRecords.filter((r) => !search || String(r.params.name).toLowerCase().includes(search.toLowerCase()));
  }, [eggRecords, tab, search]);

  const filteredBoostItems = useMemo(() => {
    if (tab !== 'BOOST') return [];
    return shopItemRecords.filter((r) => {
      if (r.params.category !== 'BOOST') return false;
      if (search && !String(r.params.name).toLowerCase().includes(search.toLowerCase())) return false;
      return true;
    });
  }, [shopItemRecords, tab, search]);

  const toggleSelect = (kind: RewardKind, id: string, name: string) => {
    setResult(null);
    setSelected((prev) => {
      const key = `${kind}:${id}`;
      if (prev[key]) {
        const next = { ...prev };
        delete next[key];
        return next;
      }
      return { ...prev, [key]: { kind, id, name, quantity: 1 } };
    });
  };

  const setQuantity = (key: string, quantity: number) => {
    setSelected((prev) => ({ ...prev, [key]: { ...prev[key], quantity: Math.max(1, Math.min(99, quantity)) } }));
  };

  const removeSelected = (key: string) => {
    setSelected((prev) => {
      const next = { ...prev };
      delete next[key];
      return next;
    });
  };

  const selectedList = Object.entries(selected);
  const coinQuantity = Number(coinAmount) || 0;

  const handleSubmit = async () => {
    setSubmitting(true);
    setResult(null);
    try {
      const rewards: SelectedReward[] = selectedList.map(([, v]) => v);
      if (coinQuantity > 0) {
        rewards.push({ kind: 'COIN', name: 'Xu Lá', quantity: coinQuantity });
      }
      const res = await api.recordAction({
        resourceId: 'StreakRewardDay',
        recordId: String(record?.id ?? day),
        actionName: 'edit',
        data: { rewards: JSON.stringify(rewards) },
      } as any);
      const data = res.data as any;
      setResult({ ok: data?.ok !== false, error: data?.error });
    } catch (err: any) {
      setResult({ ok: false, error: err?.message ?? 'Có lỗi xảy ra, thử lại sau' });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={{ background: BRAND.bg, padding: '24px', fontFamily: 'inherit' }}>
      <style>{CARD_FX_CSS}</style>
      <h1 style={{ fontSize: 22, fontWeight: 700, color: BRAND.ink, margin: '0 0 6px' }}>Quà streak — Ngày {day}</h1>
      <p style={{ fontSize: 13, color: BRAND.inkSoft, margin: '0 0 20px', maxWidth: 640 }}>
        Chọn 1 hoặc nhiều vật phẩm (và/hoặc Xu Lá) làm quà cho người chơi đạt streak {day} ngày liên
        tiếp. Streak chỉ phát thưởng 1 lần cho tới hết ngày 7 — từ ngày 8 trở đi không phát thêm cho
        tới khi streak đứt và chạy lại từ ngày 1.
      </p>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 340px', gap: 20, alignItems: 'start' }}>
        <div>
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 12 }}>
            {TABS.map((t) => (
              <span key={t.value} style={pillStyle(tab === t.value)} onClick={() => setTab(t.value)}>
                {t.label}
              </span>
            ))}
          </div>
          <input
            type="text"
            placeholder="Tìm theo tên..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            style={{
              width: '100%', maxWidth: 320, padding: '9px 12px', marginBottom: 16,
              borderRadius: 10, border: `1px solid ${BRAND.border}`, fontSize: 13,
            }}
          />

          {loading ? (
            <p style={{ color: BRAND.inkSoft }}>Đang tải...</p>
          ) : (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(140px, 1fr))', gap: 10 }}>
              {tab !== 'EGG' && tab !== 'BOOST' && filteredSpecies.map((r) => {
                const id = String(r.id);
                const name = String(r.params.name);
                const rarity = String(r.params.rarity);
                const key = `SPECIES:${id}`;
                const isSelected = Boolean(selected[key]);
                const badge = RARITY_BADGE[rarity];
                return (
                  <div
                    key={key}
                    onClick={() => toggleSelect('SPECIES', id, name)}
                    style={{
                      background: BRAND.surface,
                      border: `2px solid ${isSelected ? BRAND.primary : BRAND.border}`,
                      borderRadius: 14,
                      padding: '14px 8px 10px',
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'center',
                      gap: 6,
                      cursor: 'pointer',
                      position: 'relative',
                    }}
                  >
                    {badge ? (
                      <span className="sp-badge" style={{ background: badge.bg, color: badge.fg }}>{rarity}</span>
                    ) : null}
                    <SpeciesThumbnail
                      category={String(r.params.category)}
                      archetype={String(r.params.archetype)}
                      paletteIdx={Number(r.params.paletteIdx)}
                      name={name}
                      rarity={rarity}
                      size={64}
                    />
                    <span style={{ fontSize: 12, fontWeight: 600, color: BRAND.ink, textAlign: 'center', lineHeight: 1.3 }}>{name}</span>
                  </div>
                );
              })}
              {tab === 'EGG' && filteredEggs.map((r) => {
                const id = String(r.id);
                const name = String(r.params.name);
                const colorHex = String(r.params.colorHex);
                const priceCoin = Number(r.params.priceCoin) || 0;
                const eggTier = eggTierForPrice(priceCoin);
                const badge = EGG_TIER_BADGE[eggTier];
                const key = `EGG_TYPE:${id}`;
                const isSelected = Boolean(selected[key]);
                const icon = renderEggArt({ colorHex, name });
                const aura = renderEggAura(colorHex, priceCoin);
                return (
                  <div
                    key={key}
                    onClick={() => toggleSelect('EGG_TYPE', id, name)}
                    style={{
                      background: BRAND.surface,
                      border: `2px solid ${isSelected ? BRAND.primary : BRAND.border}`,
                      borderRadius: 14,
                      padding: '14px 8px 10px',
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'center',
                      gap: 6,
                      cursor: 'pointer',
                      position: 'relative',
                    }}
                  >
                    <span className="sp-badge" style={{ background: badge.bg, color: badge.fg }}>{name}</span>
                    <div className="sp-icon-wrap" style={{ width: 64, height: 64 }} dangerouslySetInnerHTML={{ __html: aura + icon }} />
                    <span style={{ fontSize: 11, fontWeight: 700, color: BRAND.inkSoft }}>{priceCoin} Xu Lá</span>
                  </div>
                );
              })}
              {tab === 'BOOST' && filteredBoostItems.map((r) => {
                const id = String(r.id);
                const name = String(r.params.name);
                const purchasable = Boolean(r.params.purchasable);
                const priceCoin = Number(r.params.priceCoin) || 0;
                const key = `SHOP_ITEM:${id}`;
                const isSelected = Boolean(selected[key]);
                return (
                  <div
                    key={key}
                    onClick={() => toggleSelect('SHOP_ITEM', id, name)}
                    style={{
                      background: BRAND.surface,
                      border: `2px solid ${isSelected ? BRAND.primary : BRAND.border}`,
                      borderRadius: 14,
                      padding: '14px 8px',
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'center',
                      justifyContent: 'center',
                      gap: 6,
                      cursor: 'pointer',
                      minHeight: 96,
                      textAlign: 'center',
                    }}
                  >
                    <span style={{ fontSize: 12.5, fontWeight: 700, color: BRAND.ink, lineHeight: 1.3 }}>{name}</span>
                    <span style={{ fontSize: 11, fontWeight: 600, color: BRAND.inkSoft }}>
                      {purchasable ? `${priceCoin} Xu Lá` : 'Không bán — chỉ sự kiện'}
                    </span>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        <div style={{ background: BRAND.surface, border: `1px solid ${BRAND.border}`, borderRadius: 14, padding: 16, position: 'sticky', top: 16 }}>
          <h3 style={{ fontSize: 14, fontWeight: 700, color: BRAND.ink, margin: '0 0 10px' }}>Tặng kèm Xu Lá</h3>
          <input
            type="number"
            min={0}
            placeholder="0"
            value={coinAmount}
            onChange={(e) => setCoinAmount(e.target.value)}
            style={{ width: '100%', padding: '8px 10px', borderRadius: 10, border: `1px solid ${BRAND.border}`, fontSize: 13, marginBottom: 16 }}
          />

          <h3 style={{ fontSize: 14, fontWeight: 700, color: BRAND.ink, margin: '0 0 10px' }}>
            Vật phẩm đã chọn ({selectedList.length})
          </h3>
          {selectedList.length === 0 ? (
            <p style={{ fontSize: 12.5, color: BRAND.inkSoft, margin: '0 0 14px' }}>Chưa chọn vật phẩm nào.</p>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6, marginBottom: 14, maxHeight: 220, overflowY: 'auto' }}>
              {selectedList.map(([key, item]) => (
                <div key={key} style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 12.5 }}>
                  <span style={{ flex: 1, color: BRAND.ink, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{item.name}</span>
                  <input
                    type="number"
                    min={1}
                    max={99}
                    value={item.quantity}
                    onChange={(e) => setQuantity(key, Number(e.target.value) || 1)}
                    style={{ width: 44, padding: '3px 4px', borderRadius: 6, border: `1px solid ${BRAND.border}`, fontSize: 12 }}
                  />
                  <span onClick={() => removeSelected(key)} style={{ cursor: 'pointer', color: BRAND.warnInk, fontWeight: 700 }}>×</span>
                </div>
              ))}
            </div>
          )}

          <button
            onClick={handleSubmit}
            disabled={submitting}
            style={{
              width: '100%', padding: '10px 16px', borderRadius: 999, border: 'none',
              background: BRAND.primary,
              color: BRAND.primaryInk,
              fontWeight: 700, fontSize: 13.5, cursor: submitting ? 'not-allowed' : 'pointer',
            }}
          >
            {submitting ? 'Đang lưu...' : 'Lưu quà streak'}
          </button>

          {result ? (
            <div
              style={{
                marginTop: 14, padding: 10, borderRadius: 10, fontSize: 12,
                background: result.ok ? BRAND.primary : BRAND.warnBg,
                color: result.ok ? BRAND.primaryInk : BRAND.warnInk,
              }}
            >
              {result.ok ? 'Đã lưu quà streak cho ngày này.' : (result.error || 'Có lỗi xảy ra')}
            </div>
          ) : null}
        </div>
      </div>
    </div>
  );
};

export default StreakRewardEditPage;
