import React from 'react';
import type { ActionProps } from 'adminjs';
import { renderSpeciesArt, RARITY_COLORS } from './species-art.js';

const BRAND = {
  bg: '#F9F6F0',
  surface: '#FFFFFF',
  ink: '#6D594E',
  inkSoft: '#95816F',
  primary: '#A8D08D',
  primaryInk: '#3F5C2E',
  warn: '#E76F51',
  border: 'rgba(109,89,78,0.16)',
};

const CATEGORY_LABEL: Record<string, string> = {
  FOREST: 'Thú rừng',
  SEA: 'Sinh vật biển',
  PLANT: 'Thực vật',
  MYTHIC: 'Thần thú',
};

const Row: React.FC<{ label: string; children: React.ReactNode }> = ({ label, children }) => (
  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px 0', borderTop: `1px solid ${BRAND.border}` }}>
    <span style={{ fontSize: 12.5, color: BRAND.inkSoft }}>{label}</span>
    <span style={{ fontSize: 13.5, fontWeight: 600, color: BRAND.ink }}>{children}</span>
  </div>
);

const SpeciesShow: React.FC<ActionProps> = ({ record, resource }) => {
  if (!record) return null;
  const { name, category, archetype, paletteIdx, rarity, lore, isActive } = record.params as Record<string, any>;
  const svg = renderSpeciesArt({ category, archetype, paletteIdx: Number(paletteIdx), name });
  const ringColor = RARITY_COLORS[rarity] ?? '#B7A896';

  return (
    <div style={{ background: BRAND.bg, padding: 24, maxWidth: 480 }}>
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 12, marginBottom: 20 }}>
        <div
          style={{
            width: 160, height: 160, borderRadius: '50%', background: BRAND.surface,
            border: `4px solid ${ringColor}`, display: 'flex', alignItems: 'center', justifyContent: 'center',
            boxShadow: '0 4px 16px rgba(109,89,78,0.12)',
          }}
          // eslint-disable-next-line react/no-danger
          dangerouslySetInnerHTML={{ __html: svg }}
        />
        <h1 style={{ fontSize: 22, fontWeight: 700, color: BRAND.ink, margin: 0, textAlign: 'center' }}>{name}</h1>
        <span
          style={{
            fontSize: 12, fontWeight: 700, padding: '4px 12px', borderRadius: 999,
            background: ringColor + '33', color: BRAND.ink,
          }}
        >
          Cấp {rarity} · {CATEGORY_LABEL[category] ?? category}
        </span>
      </div>

      <div style={{ background: BRAND.surface, border: `1px solid ${BRAND.border}`, borderRadius: 14, padding: '4px 16px' }}>
        <Row label="Archetype">{archetype}</Row>
        <Row label="Palette Idx">{paletteIdx}</Row>
        <Row label="Đang hoạt động">{isActive ? 'Có' : 'Không'}</Row>
        {lore ? <Row label="Lore">{lore}</Row> : null}
      </div>

      <div style={{ display: 'flex', gap: 10, marginTop: 20 }}>
        <a
          href={`/admin/resources/${resource.id}/records/${record.id}/edit`}
          style={{ flex: 1, textAlign: 'center', fontSize: 13, fontWeight: 700, padding: '10px 0', borderRadius: 999, background: BRAND.primary, color: BRAND.primaryInk, textDecoration: 'none' }}
        >
          Sửa
        </a>
        <a
          href={`/admin/resources/${resource.id}/records/${record.id}/delete`}
          style={{ flex: 1, textAlign: 'center', fontSize: 13, fontWeight: 700, padding: '10px 0', borderRadius: 999, background: 'transparent', color: BRAND.warn, border: `1px solid ${BRAND.warn}`, textDecoration: 'none' }}
        >
          Xoá
        </a>
        <a
          href={`/admin/resources/${resource.id}`}
          style={{ flex: 1, textAlign: 'center', fontSize: 13, fontWeight: 700, padding: '10px 0', borderRadius: 999, background: 'transparent', color: BRAND.inkSoft, border: `1px solid ${BRAND.border}`, textDecoration: 'none' }}
        >
          ← Danh sách
        </a>
      </div>
    </div>
  );
};

export default SpeciesShow;
