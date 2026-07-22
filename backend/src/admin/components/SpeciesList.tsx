import React, { useEffect, useMemo, useState } from 'react';
import { ApiClient } from 'adminjs';
import type { ActionProps, RecordJSON } from 'adminjs';
import SpeciesThumbnail from './SpeciesThumbnail.js';
import { RARITY_COLORS } from './species-art.js';

const api = new ApiClient();

const BRAND = {
  bg: '#F9F6F0',
  surface: '#FFFFFF',
  ink: '#6D594E',
  inkSoft: '#95816F',
  primary: '#A8D08D',
  primaryInk: '#3F5C2E',
  border: 'rgba(109,89,78,0.16)',
};

const CATEGORIES = [
  { value: '', label: 'Tất cả' },
  { value: 'FOREST', label: 'Thú rừng' },
  { value: 'SEA', label: 'Sinh vật biển' },
  { value: 'PLANT', label: 'Thực vật' },
  { value: 'MYTHIC', label: 'Thần thú' },
];
const RARITIES = ['', 'B', 'A', 'S', 'SS', 'SSR'];

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

const SpeciesList: React.FC<ActionProps> = ({ resource }) => {
  const [records, setRecords] = useState<RecordJSON[]>([]);
  const [loading, setLoading] = useState(true);
  const [category, setCategory] = useState('');
  const [rarity, setRarity] = useState('');
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
      if (category && r.params.category !== category) return false;
      if (rarity && r.params.rarity !== rarity) return false;
      if (search && !String(r.params.name).toLowerCase().includes(search.toLowerCase())) return false;
      return true;
    });
  }, [records, category, rarity, search]);

  return (
    <div style={{ background: BRAND.bg, padding: '24px', fontFamily: 'inherit' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12, marginBottom: 16 }}>
        <h1 style={{ fontSize: 22, fontWeight: 700, color: BRAND.ink, margin: 0 }}>
          Species <span style={{ color: BRAND.inkSoft, fontWeight: 400 }}>({filtered.length}/{records.length})</span>
        </h1>
        <a
          href={`/admin/resources/${resource.id}/actions/new`}
          style={{
            fontSize: 13, fontWeight: 700, padding: '9px 16px', borderRadius: 999,
            background: BRAND.primary, color: BRAND.primaryInk, textDecoration: 'none',
          }}
        >
          + Tạo loài mới
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

      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 8 }}>
        {CATEGORIES.map((c) => (
          <span key={c.value} style={pillStyle(category === c.value)} onClick={() => setCategory(c.value)}>
            {c.label}
          </span>
        ))}
      </div>
      <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginBottom: 20 }}>
        {RARITIES.map((r) => (
          <span key={r || 'all'} style={pillStyle(rarity === r)} onClick={() => setRarity(r)}>
            {r || 'Mọi cấp bậc'}
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
          {filtered.map((record) => (
            <a
              key={record.id}
              href={`/admin/resources/${resource.id}/records/${record.id}/show`}
              style={{
                background: BRAND.surface,
                border: `1px solid ${BRAND.border}`,
                borderRadius: 14,
                padding: '14px 10px',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: 8,
                textDecoration: 'none',
                color: 'inherit',
              }}
            >
              <SpeciesThumbnail
                category={String(record.params.category)}
                archetype={String(record.params.archetype)}
                paletteIdx={Number(record.params.paletteIdx)}
                name={String(record.params.name)}
                rarity={String(record.params.rarity)}
                size={72}
              />
              <span style={{ fontSize: 12.5, fontWeight: 600, color: BRAND.ink, textAlign: 'center', lineHeight: 1.3 }}>
                {record.params.name}
              </span>
              <span
                style={{
                  fontSize: 10,
                  fontWeight: 700,
                  padding: '2px 8px',
                  borderRadius: 999,
                  background: (RARITY_COLORS[String(record.params.rarity)] ?? '#B7A896') + '33',
                  color: BRAND.ink,
                }}
              >
                {record.params.rarity}
              </span>
            </a>
          ))}
        </div>
      )}
    </div>
  );
};

export default SpeciesList;
