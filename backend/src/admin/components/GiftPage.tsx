import React, { useEffect, useMemo, useState } from 'react';
import { ApiClient } from 'adminjs';
import type { RecordJSON } from 'adminjs';
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

type Tab = 'FOREST' | 'SEA' | 'PLANT' | 'MYTHIC' | 'EGG';
const TABS: Array<{ value: Tab; label: string }> = [
  { value: 'FOREST', label: 'Thú rừng' },
  { value: 'SEA', label: 'Sinh vật biển' },
  { value: 'PLANT', label: 'Thực vật' },
  { value: 'MYTHIC', label: 'Thần thú' },
  { value: 'EGG', label: 'Trứng' },
];

type SelectedItem = { kind: 'SPECIES' | 'EGG'; id: string; name: string; quantity: number };

type SubmitResult = {
  ok: boolean;
  error?: string;
  recipientsResolved?: number;
  recipientsNotFound?: string[];
  grantsApplied?: number;
  errors?: string[];
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

const GiftPage: React.FC = () => {
  const [speciesRecords, setSpeciesRecords] = useState<RecordJSON[]>([]);
  const [eggRecords, setEggRecords] = useState<RecordJSON[]>([]);
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState<Tab>('FOREST');
  const [search, setSearch] = useState('');
  const [selected, setSelected] = useState<Record<string, SelectedItem>>({});
  const [recipientsText, setRecipientsText] = useState('');
  const [csvFile, setCsvFile] = useState<File | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<SubmitResult | null>(null);

  useEffect(() => {
    Promise.all([
      api.resourceAction({ resourceId: 'Species', actionName: 'list', params: { perPage: 250, sortBy: 'name', direction: 'asc' } } as any),
      api.resourceAction({ resourceId: 'EggType', actionName: 'list', params: { perPage: 250, sortBy: 'name', direction: 'asc' } } as any),
    ])
      .then(([speciesRes, eggRes]) => {
        setSpeciesRecords((speciesRes.data as any).records ?? []);
        setEggRecords((eggRes.data as any).records ?? []);
      })
      .finally(() => setLoading(false));
  }, []);

  const filteredSpecies = useMemo(() => {
    if (tab === 'EGG') return [];
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

  const toggleSelect = (kind: 'SPECIES' | 'EGG', id: string, name: string) => {
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

  const handleSubmit = async () => {
    setSubmitting(true);
    setResult(null);
    try {
      const fd = new FormData();
      fd.append('recipientsText', recipientsText);
      fd.append('grants', JSON.stringify(selectedList.map(([, v]) => ({ kind: v.kind, id: v.id, quantity: v.quantity }))));
      if (csvFile) fd.append('csvFile', csvFile);
      const res = await api.getPage({ pageName: 'gift-items', method: 'post', data: fd } as any);
      setResult(res.data as SubmitResult);
      if ((res.data as SubmitResult).ok) {
        setSelected({});
        setRecipientsText('');
        setCsvFile(null);
      }
    } catch (err: any) {
      setResult({ ok: false, error: err?.message ?? 'Có lỗi xảy ra, thử lại sau' });
    } finally {
      setSubmitting(false);
    }
  };

  const canSubmit = selectedList.length > 0 && (recipientsText.trim().length > 0 || csvFile) && !submitting;

  return (
    <div style={{ background: BRAND.bg, padding: '24px', fontFamily: 'inherit' }}>
      <style>{CARD_FX_CSS}</style>
      <h1 style={{ fontSize: 22, fontWeight: 700, color: BRAND.ink, margin: '0 0 6px' }}>Phát quà</h1>
      <p style={{ fontSize: 13, color: BRAND.inkSoft, margin: '0 0 20px', maxWidth: 640 }}>
        Chọn 1 hoặc nhiều vật phẩm (thú/sinh vật biển/thực vật/thần thú/trứng) bên dưới, nhập người
        nhận (ID hoặc email, danh sách hoặc file CSV), rồi bấm Phát quà. Mỗi vật phẩm được cấp cho
        TẤT CẢ người nhận đã nhập.
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
              {tab !== 'EGG' && filteredSpecies.map((record) => {
                const id = String(record.id);
                const name = String(record.params.name);
                const rarity = String(record.params.rarity);
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
                      category={String(record.params.category)}
                      archetype={String(record.params.archetype)}
                      paletteIdx={Number(record.params.paletteIdx)}
                      name={name}
                      rarity={rarity}
                      size={64}
                    />
                    <span style={{ fontSize: 12, fontWeight: 600, color: BRAND.ink, textAlign: 'center', lineHeight: 1.3 }}>{name}</span>
                  </div>
                );
              })}
              {tab === 'EGG' && filteredEggs.map((record) => {
                const id = String(record.id);
                const name = String(record.params.name);
                const colorHex = String(record.params.colorHex);
                const priceCoin = Number(record.params.priceCoin) || 0;
                const eggTier = eggTierForPrice(priceCoin);
                const badge = EGG_TIER_BADGE[eggTier];
                const key = `EGG:${id}`;
                const isSelected = Boolean(selected[key]);
                const icon = renderEggArt({ colorHex, name });
                const aura = renderEggAura(colorHex, priceCoin);
                return (
                  <div
                    key={key}
                    onClick={() => toggleSelect('EGG', id, name)}
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
            </div>
          )}
        </div>

        <div style={{ background: BRAND.surface, border: `1px solid ${BRAND.border}`, borderRadius: 14, padding: 16, position: 'sticky', top: 16 }}>
          <h3 style={{ fontSize: 14, fontWeight: 700, color: BRAND.ink, margin: '0 0 10px' }}>
            Đã chọn ({selectedList.length})
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

          <h3 style={{ fontSize: 14, fontWeight: 700, color: BRAND.ink, margin: '0 0 8px' }}>Người nhận</h3>
          <textarea
            placeholder={'Dán ID hoặc email — mỗi dòng 1 người, hoặc phân cách bằng dấu phẩy'}
            value={recipientsText}
            onChange={(e) => setRecipientsText(e.target.value)}
            rows={4}
            style={{ width: '100%', padding: '8px 10px', borderRadius: 10, border: `1px solid ${BRAND.border}`, fontSize: 12.5, marginBottom: 8, resize: 'vertical' }}
          />
          <div style={{ marginBottom: 14 }}>
            <label style={{ fontSize: 12, color: BRAND.inkSoft }}>
              Hoặc tải file CSV (1 cột ID/email, mỗi dòng 1 người):
              <input
                type="file"
                accept=".csv,text/csv"
                onChange={(e) => setCsvFile(e.target.files?.[0] ?? null)}
                style={{ display: 'block', marginTop: 6, fontSize: 12 }}
              />
            </label>
          </div>

          <button
            onClick={handleSubmit}
            disabled={!canSubmit}
            style={{
              width: '100%', padding: '10px 16px', borderRadius: 999, border: 'none',
              background: canSubmit ? BRAND.primary : BRAND.border,
              color: canSubmit ? BRAND.primaryInk : BRAND.inkSoft,
              fontWeight: 700, fontSize: 13.5, cursor: canSubmit ? 'pointer' : 'not-allowed',
            }}
          >
            {submitting ? 'Đang phát...' : 'Phát quà'}
          </button>

          {result ? (
            <div
              style={{
                marginTop: 14, padding: 10, borderRadius: 10, fontSize: 12,
                background: result.ok ? BRAND.primary : BRAND.warnBg,
                color: result.ok ? BRAND.primaryInk : BRAND.warnInk,
              }}
            >
              {result.ok ? (
                <>
                  <div>Đã phát cho {result.recipientsResolved} người nhận, {result.grantsApplied} lượt cấp thành công.</div>
                  {result.recipientsNotFound && result.recipientsNotFound.length > 0 ? (
                    <div style={{ marginTop: 6 }}>Không tìm thấy: {result.recipientsNotFound.join(', ')}</div>
                  ) : null}
                  {result.errors && result.errors.length > 0 ? (
                    <div style={{ marginTop: 6 }}>Lỗi: {result.errors.join('; ')}</div>
                  ) : null}
                </>
              ) : (
                <div>{result.error || 'Có lỗi xảy ra'}</div>
              )}
            </div>
          ) : null}
        </div>
      </div>
    </div>
  );
};

export default GiftPage;
