import React, { useEffect, useMemo, useState } from 'react';
import { ApiClient } from 'adminjs';
import type { ActionProps, RecordJSON } from 'adminjs';
import { CARD_FX_CSS } from './species-art.js';
import { renderEggArt, renderEggAura, eggTierForPrice, EGG_TIER_LABEL, EGG_TIER_BADGE, EggTier } from './egg-art.js';

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

const TIERS: Array<{ value: '' | EggTier; label: string }> = [
  { value: '', label: 'Tất cả' },
  { value: 'common', label: 'Thường' },
  { value: 'rare', label: 'Hiếm' },
  { value: 'legendary', label: 'Huyền thoại' },
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

const EggTypeList: React.FC<ActionProps> = ({ resource }) => {
  const [records, setRecords] = useState<RecordJSON[]>([]);
  const [loading, setLoading] = useState(true);
  const [tier, setTier] = useState<'' | EggTier>('');
  const [search, setSearch] = useState('');

  useEffect(() => {
    setLoading(true);
    api
      .resourceAction({
        resourceId: resource.id,
        actionName: 'list',
        params: { perPage: 250, sortBy: 'name', direction: 'asc' },
      } as any)
      .then((res) => setRecords((res.data as any).records ?? []))
      .finally(() => setLoading(false));
  }, [resource.id]);

  const filtered = useMemo(() => {
    return records.filter((r) => {
      const priceCoin = Number(r.params.priceCoin) || 0;
      if (tier && eggTierForPrice(priceCoin) !== tier) return false;
      if (search && !String(r.params.name).toLowerCase().includes(search.toLowerCase())) return false;
      return true;
    });
  }, [records, tier, search]);

  return (
    <div style={{ background: BRAND.bg, padding: '24px', fontFamily: 'inherit' }}>
      <style>{CARD_FX_CSS}</style>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12, marginBottom: 16 }}>
        <h1 style={{ fontSize: 22, fontWeight: 700, color: BRAND.ink, margin: 0 }}>
          Danh sách trứng <span style={{ color: BRAND.inkSoft, fontWeight: 400 }}>({filtered.length}/{records.length})</span>
        </h1>
        <a
          href={`/admin/resources/${resource.id}/actions/new`}
          style={{
            fontSize: 13, fontWeight: 700, padding: '9px 16px', borderRadius: 999,
            background: BRAND.primary, color: BRAND.primaryInk, textDecoration: 'none',
          }}
        >
          + Tạo loại trứng mới
        </a>
      </div>

      <input
        type="text"
        placeholder="Tìm theo tên..."
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        style={{
          width: '100%', maxWidth: 320, padding: '9px 12px', marginBottom: 12,
          borderRadius: 10, border: `1px solid ${BRAND.border}`, fontSize: 13,
        }}
      />

      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 20 }}>
        {TIERS.map((t) => (
          <span key={t.value || 'all'} style={pillStyle(tier === t.value)} onClick={() => setTier(t.value)}>
            {t.label}
          </span>
        ))}
      </div>

      {loading ? (
        <p style={{ color: BRAND.inkSoft }}>Đang tải...</p>
      ) : (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(150px, 1fr))',
            gap: 12,
          }}
        >
          {filtered.map((record) => {
            const name = String(record.params.name);
            const colorHex = String(record.params.colorHex);
            const priceCoin = Number(record.params.priceCoin) || 0;
            const cardTier = eggTierForPrice(priceCoin);
            const badge = EGG_TIER_BADGE[cardTier];
            const isLegendary = cardTier === 'legendary';
            const icon = renderEggArt({ colorHex, name });
            const aura = renderEggAura(colorHex, priceCoin);
            return (
              <a
                key={record.id}
                href={`/admin/resources/${resource.id}/records/${record.id}/show`}
                style={{
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
                  boxShadow: isLegendary ? '0 0 0 1px #F4D160' : undefined,
                }}
              >
                <span className="sp-badge" style={{ background: badge.bg, color: badge.fg }}>
                  {EGG_TIER_LABEL[cardTier]}
                </span>
                <div
                  className="sp-icon-wrap"
                  style={{ width: 72, height: 72 }}
                  // eslint-disable-next-line react/no-danger
                  dangerouslySetInnerHTML={{ __html: aura + icon }}
                />
                <span style={{ fontSize: 12.5, fontWeight: 600, color: BRAND.ink, textAlign: 'center', lineHeight: 1.3 }}>
                  {name}
                </span>
                <span style={{ fontSize: 11, fontWeight: 700, color: BRAND.accentInk }}>{priceCoin} Xu Lá</span>
              </a>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default EggTypeList;
