import React, { useEffect, useMemo, useState } from 'react';
import { ApiClient } from 'adminjs';
import type { ActionProps, RecordJSON } from 'adminjs';
import SpeciesThumbnail from './SpeciesThumbnail.js';
import { CARD_FX_CSS, RARITY_BADGE } from './species-art.js';

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

type DropCardProps = {
  record: RecordJSON;
  species: RecordJSON | undefined;
};

/** 1 dòng Tỉ lệ rơi trứng — tách riêng component để chỉ CHÍNH thẻ đang gõ trọng số re-render (có
 * tới 175 thẻ/tab với Trứng Bí Ẩn), không kéo theo re-render toàn bộ lưới mỗi lần gõ phím. */
const DropCard: React.FC<DropCardProps> = ({ record, species }) => {
  const initialWeight = String(record.params.weight ?? 1);
  const [weight, setWeight] = useState(initialWeight);
  // So với giá trị đã LƯU GẦN NHẤT (không phải `record.params.weight` gốc, vốn đứng yên suốt vòng
  // đời component) — nếu không, sửa 1→2 (lưu) rồi gõ lại về đúng 1 sẽ bị coi là "không đổi" so với
  // giá trị gốc và bỏ qua lưu, dù giá trị THẬT trên server lúc đó đang là 2.
  const lastSaved = React.useRef(initialWeight);
  const [saved, setSaved] = useState(true);

  const commit = () => {
    if (lastSaved.current === weight) return;
    setSaved(false);
    api
      .recordAction({ resourceId: 'EggDropEntry', recordId: record.id, actionName: 'edit', data: { weight } } as any)
      .then(() => {
        lastSaved.current = weight;
      })
      .finally(() => setSaved(true));
  };

  if (!species) return null;
  const name = String(species.params.name);
  const rarity = String(species.params.rarity);
  const badge = RARITY_BADGE[rarity];

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
      <SpeciesThumbnail
        category={String(species.params.category)}
        archetype={String(species.params.archetype)}
        paletteIdx={Number(species.params.paletteIdx)}
        name={name}
        rarity={rarity}
        size={64}
      />
      <span style={{ fontSize: 12, fontWeight: 600, color: BRAND.ink, textAlign: 'center', lineHeight: 1.3 }}>{name}</span>
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

/** "Tỉ lệ rơi trứng" (T-134, Dev1002 yêu cầu) — thay bảng liệt kê mặc định bằng lưới thẻ có
 * thumbnail loài + sửa trọng số TRỰC TIẾP trên thẻ (không cần vào trang chi tiết/Sửa riêng nữa).
 * 501 dòng tổng — lọc theo Loại trứng bằng pill (mặc định chọn loại trứng ĐẦU TIÊN thay vì "Tất
 * cả" để tránh vẽ cả 501 thẻ cùng lúc, xem comment `defaultEggTypeId`).
 */
const EggDropEntryList: React.FC<ActionProps> = () => {
  const [records, setRecords] = useState<RecordJSON[]>([]);
  const [speciesRecords, setSpeciesRecords] = useState<RecordJSON[]>([]);
  const [eggTypeRecords, setEggTypeRecords] = useState<RecordJSON[]>([]);
  const [loading, setLoading] = useState(true);
  const [eggTypeId, setEggTypeId] = useState('');
  const [search, setSearch] = useState('');

  useEffect(() => {
    setLoading(true);
    // AdminJS chặn cứng perPage ở 500 (node_modules/adminjs/lib/backend/actions/list/list-action.js
    // — PER_PAGE_LIMIT) trong khi tổng EggDropEntry đã 501 dòng — phải tự gộp thêm trang 2 nếu
    // `meta.total` > số dòng trang 1, không thì mất đúng 1 dòng cuối.
    const fetchAllDropEntries = async (): Promise<RecordJSON[]> => {
      const first = await api.resourceAction({ resourceId: 'EggDropEntry', actionName: 'list', params: { perPage: 500, page: 1 } } as any);
      const firstData = first.data as any;
      let all = (firstData.records ?? []) as RecordJSON[];
      const total = firstData.meta?.total ?? all.length;
      let page = 2;
      while (all.length < total) {
        const next = await api.resourceAction({ resourceId: 'EggDropEntry', actionName: 'list', params: { perPage: 500, page } } as any);
        const nextRecords = ((next.data as any).records ?? []) as RecordJSON[];
        if (nextRecords.length === 0) break;
        all = all.concat(nextRecords);
        page += 1;
      }
      return all;
    };

    Promise.all([
      fetchAllDropEntries(),
      api.resourceAction({ resourceId: 'Species', actionName: 'list', params: { perPage: 250 } } as any),
      api.resourceAction({ resourceId: 'EggType', actionName: 'list', params: { perPage: 250, sortBy: 'name', direction: 'asc' } } as any),
    ])
      .then(([dropRecords, speciesRes, eggRes]) => {
        setRecords(dropRecords);
        setSpeciesRecords(((speciesRes.data as any).records ?? []) as RecordJSON[]);
        const eggTypes = ((eggRes.data as any).records ?? []) as RecordJSON[];
        setEggTypeRecords(eggTypes);
        // Mặc định chọn loại trứng đầu tiên (không phải "Tất cả") — né vẽ hết 501 thẻ cùng lúc.
        if (eggTypes.length > 0) setEggTypeId(eggTypes[0].id);
      })
      .finally(() => setLoading(false));
  }, []);

  const speciesMap = useMemo(() => {
    const map = new Map<string, RecordJSON>();
    speciesRecords.forEach((r) => map.set(r.id, r));
    return map;
  }, [speciesRecords]);

  const filtered = useMemo(() => {
    // @adminjs/prisma phơi FK qua path TÊN QUAN HỆ ("eggType"/"species"), không phải tên cột
    // ("eggTypeId"/"speciesId") — record.params.eggType/species giữ thẳng UUID, không lồng object.
    return records.filter((r) => {
      if (eggTypeId && String(r.params.eggType) !== eggTypeId) return false;
      if (search) {
        const species = speciesMap.get(String(r.params.species));
        if (!species || !String(species.params.name).toLowerCase().includes(search.toLowerCase())) return false;
      }
      return true;
    });
  }, [records, eggTypeId, search, speciesMap]);

  return (
    <div style={{ background: BRAND.bg, padding: '24px', fontFamily: 'inherit' }}>
      <style>{CARD_FX_CSS}</style>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12, marginBottom: 16 }}>
        <h1 style={{ fontSize: 22, fontWeight: 700, color: BRAND.ink, margin: 0 }}>
          Tỉ lệ rơi trứng <span style={{ color: BRAND.inkSoft, fontWeight: 400 }}>({filtered.length}/{records.length})</span>
        </h1>
        <a
          href="/admin/resources/EggDropEntry/actions/new"
          style={{ fontSize: 13, fontWeight: 700, padding: '9px 16px', borderRadius: 999, background: BRAND.primary, color: BRAND.primaryInk, textDecoration: 'none' }}
        >
          + Thêm dòng
        </a>
      </div>

      <input
        type="text"
        placeholder="Tìm theo tên loài..."
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        style={{ width: '100%', maxWidth: 320, padding: '9px 12px', marginBottom: 12, borderRadius: 10, border: `1px solid ${BRAND.border}`, fontSize: 13 }}
      />

      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 20 }}>
        {eggTypeRecords.map((et) => (
          <span key={et.id} style={pillStyle(eggTypeId === et.id)} onClick={() => setEggTypeId(et.id)}>
            {String(et.params.name)}
          </span>
        ))}
      </div>

      {loading ? (
        <p style={{ color: BRAND.inkSoft }}>Đang tải...</p>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(150px, 1fr))', gap: 12 }}>
          {filtered.map((record) => (
            <DropCard key={record.id} record={record} species={speciesMap.get(String(record.params.species))} />
          ))}
        </div>
      )}
    </div>
  );
};

export default EggDropEntryList;
