import React, { useEffect, useMemo, useState } from 'react';
import { ApiClient } from 'adminjs';
import type { ActionProps, RecordJSON } from 'adminjs';
import { CARD_FX_CSS, RARITY_BADGE } from './species-art.js';
import { renderEggArt } from './egg-art.js';

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

const tabStyle = (active: boolean): React.CSSProperties => ({
  fontSize: 13.5,
  fontWeight: 700,
  padding: '10px 18px',
  borderRadius: '12px 12px 0 0',
  border: `1px solid ${BRAND.border}`,
  borderBottom: active ? `2px solid ${BRAND.surface}` : `1px solid ${BRAND.border}`,
  background: active ? BRAND.surface : 'transparent',
  color: active ? BRAND.primaryInk : BRAND.inkSoft,
  cursor: 'pointer',
  marginBottom: -1,
  whiteSpace: 'nowrap',
});

type WeightCardProps = { record: RecordJSON; colorHex: string; eggName: string };

/** 1 dòng Trọng số cấp bậc — tách riêng để chỉ thẻ đang gõ re-render (giống `DropCard` ở
 * `EggDropEntryList.tsx`, dù mỗi tab ở đây chỉ có vài thẻ nên không thật sự cần thiết, vẫn giữ
 * cùng khuôn mẫu cho nhất quán 2 màn hình chị em). */
const WeightCard: React.FC<WeightCardProps> = ({ record, colorHex, eggName }) => {
  const initialWeight = String(record.params.weight ?? 1);
  const [weight, setWeight] = useState(initialWeight);
  // Xem comment ở `DropCard` (EggDropEntryList.tsx) — so với giá trị đã LƯU GẦN NHẤT, không phải
  // `record.params.weight` gốc đứng yên suốt vòng đời component.
  const lastSaved = React.useRef(initialWeight);
  const [saved, setSaved] = useState(true);
  const rarity = String(record.params.rarity);
  const badge = RARITY_BADGE[rarity];
  const icon = renderEggArt({ colorHex, name: eggName });

  const commit = () => {
    if (lastSaved.current === weight) return;
    setSaved(false);
    api
      .recordAction({ resourceId: 'EggRarityWeight', recordId: record.id, actionName: 'edit', data: { weight } } as any)
      .then(() => {
        lastSaved.current = weight;
      })
      .finally(() => setSaved(true));
  };

  return (
    <div
      style={{
        background: BRAND.surface,
        border: `1px solid ${BRAND.border}`,
        borderRadius: 14,
        padding: '18px 10px 14px',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 8,
        position: 'relative',
      }}
    >
      {badge ? (
        <span className="sp-badge" style={{ background: badge.bg, color: badge.fg }}>
          {rarity}
        </span>
      ) : null}
      <div className="sp-icon-wrap" style={{ width: 56, height: 56 }} dangerouslySetInnerHTML={{ __html: icon }} />
      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
        <span style={{ fontSize: 11, color: BRAND.inkSoft }}>Trọng số</span>
        <input
          type="number"
          min={0}
          value={weight}
          onChange={(e) => setWeight(e.target.value)}
          onBlur={commit}
          onKeyDown={(e) => {
            if (e.key === 'Enter') commit();
          }}
          style={{ width: 56, padding: '4px 6px', borderRadius: 8, border: `1px solid ${BRAND.border}`, fontSize: 12.5, textAlign: 'center' }}
        />
        {!saved ? <span style={{ fontSize: 10, color: BRAND.inkSoft }}>...</span> : null}
      </div>
    </div>
  );
};

/** "Trọng Số Cấp Bậc Trứng" (T-134, Dev1002 yêu cầu) — thay bảng liệt kê mặc định bằng lưới thẻ
 * (icon trứng theo màu + badge cấp bậc) chia TAB theo Loại trứng, sửa trọng số trực tiếp trên thẻ
 * — không cần vào trang chi tiết/Sửa riêng nữa. Chỉ 23 dòng tổng nên không cần lọc/tìm kiếm thêm.
 */
const EggRarityWeightList: React.FC<ActionProps> = () => {
  const [records, setRecords] = useState<RecordJSON[]>([]);
  const [eggTypeRecords, setEggTypeRecords] = useState<RecordJSON[]>([]);
  const [loading, setLoading] = useState(true);
  const [eggTypeId, setEggTypeId] = useState('');

  useEffect(() => {
    setLoading(true);
    Promise.all([
      api.resourceAction({ resourceId: 'EggRarityWeight', actionName: 'list', params: { perPage: 200 } } as any),
      api.resourceAction({ resourceId: 'EggType', actionName: 'list', params: { perPage: 250, sortBy: 'name', direction: 'asc' } } as any),
    ])
      .then(([weightRes, eggRes]) => {
        setRecords(((weightRes.data as any).records ?? []) as RecordJSON[]);
        const eggTypes = ((eggRes.data as any).records ?? []) as RecordJSON[];
        setEggTypeRecords(eggTypes);
        if (eggTypes.length > 0) setEggTypeId(eggTypes[0].id);
      })
      .finally(() => setLoading(false));
  }, []);

  const currentEggType = useMemo(() => eggTypeRecords.find((e) => e.id === eggTypeId), [eggTypeRecords, eggTypeId]);

  // @adminjs/prisma phơi FK qua path TÊN QUAN HỆ ("eggType"), không phải tên cột ("eggTypeId").
  const filtered = useMemo(() => records.filter((r) => String(r.params.eggType) === eggTypeId), [records, eggTypeId]);

  return (
    <div style={{ background: BRAND.bg, padding: '24px', fontFamily: 'inherit' }}>
      <style>{CARD_FX_CSS}</style>

      <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>
        {eggTypeRecords.map((et) => (
          <span key={et.id} style={tabStyle(eggTypeId === et.id)} onClick={() => setEggTypeId(et.id)}>
            {String(et.params.name)}
          </span>
        ))}
      </div>
      <div style={{ background: BRAND.surface, border: `1px solid ${BRAND.border}`, borderRadius: '0 10px 10px 10px', padding: 20 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12, marginBottom: 16 }}>
          <h1 style={{ fontSize: 20, fontWeight: 700, color: BRAND.ink, margin: 0 }}>
            Trọng số cấp bậc — {currentEggType ? String(currentEggType.params.name) : ''}
          </h1>
          <a
            href="/admin/resources/EggRarityWeight/actions/new"
            style={{ fontSize: 13, fontWeight: 700, padding: '9px 16px', borderRadius: 999, background: BRAND.primary, color: BRAND.primaryInk, textDecoration: 'none' }}
          >
            + Thêm cấp bậc
          </a>
        </div>

        {loading ? (
          <p style={{ color: BRAND.inkSoft }}>Đang tải...</p>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(130px, 1fr))', gap: 12 }}>
            {filtered.map((record) => (
              <WeightCard key={record.id} record={record} colorHex={currentEggType ? String(currentEggType.params.colorHex) : '#A8D08D'} eggName={currentEggType ? String(currentEggType.params.name) : ''} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default EggRarityWeightList;
