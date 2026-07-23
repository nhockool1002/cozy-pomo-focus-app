import React, { useEffect, useMemo, useState } from 'react';

const BRAND = {
  bg: '#F9F6F0',
  surface: '#FFFFFF',
  ink: '#6D594E',
  inkSoft: '#95816F',
  primary: '#A8D08D',
  primaryInk: '#3F5C2E',
  accent: '#F4D160',
  warn: '#E76F51',
  border: 'rgba(109,89,78,0.16)',
};

const METHOD_COLOR: Record<string, { bg: string; fg: string }> = {
  get: { bg: '#E9F2E0', fg: '#3F5C2E' },
  post: { bg: '#DCEACB', fg: '#3F5C2E' },
  patch: { bg: '#FBF0CE', fg: '#8A6A10' },
  put: { bg: '#E9DAF0', fg: '#6B3FA0' },
  delete: { bg: '#FBE0D7', fg: '#B23F22' },
};

const TAG_LABEL: Record<string, string> = {
  auth: 'Xác thực',
  species: 'Loài',
  eggs: 'Trứng',
  sessions: 'Phiên tập trung',
  currency: 'Xu Lá',
  collection: 'Bộ sưu tập',
  shop: 'Cửa hàng',
  settings: 'Cài đặt',
  sync: 'Đồng bộ',
  stats: 'Thống kê',
  health: 'Health check',
};

type Endpoint = {
  method: string;
  path: string;
  tag: string;
  summary: string;
  secured: boolean;
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

const METHOD_ORDER = ['get', 'post', 'patch', 'put', 'delete'];

const ApiExplorer: React.FC = () => {
  const [endpoints, setEndpoints] = useState<Endpoint[]>([]);
  const [basePath, setBasePath] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [tag, setTag] = useState('');
  const [search, setSearch] = useState('');

  useEffect(() => {
    fetch('/docs-json')
      .then((res) => {
        if (!res.ok) throw new Error('fetch failed');
        return res.json();
      })
      .then((doc) => {
        const list: Endpoint[] = [];
        Object.entries(doc.paths ?? {}).forEach(([rawPath, methods]: [string, any]) => {
          Object.entries(methods).forEach(([method, op]: [string, any]) => {
            if (!METHOD_ORDER.includes(method)) return;
            list.push({
              method,
              path: rawPath,
              tag: op.tags?.[0] ?? 'khác',
              summary: op.summary || op.operationId || '',
              secured: Array.isArray(op.security) && op.security.length > 0,
            });
          });
        });
        list.sort((a, b) => a.path.localeCompare(b.path) || METHOD_ORDER.indexOf(a.method) - METHOD_ORDER.indexOf(b.method));
        setEndpoints(list);
        setBasePath(doc.servers?.[0]?.url ?? '');
      })
      .catch(() => setError(true))
      .finally(() => setLoading(false));
  }, []);

  const tags = useMemo(() => {
    const seen = new Map<string, number>();
    endpoints.forEach((e) => seen.set(e.tag, (seen.get(e.tag) ?? 0) + 1));
    return [...seen.keys()].sort();
  }, [endpoints]);

  const filtered = useMemo(() => {
    return endpoints.filter((e) => {
      if (tag && e.tag !== tag) return false;
      if (search) {
        const q = search.toLowerCase();
        if (!e.path.toLowerCase().includes(q) && !e.summary.toLowerCase().includes(q)) return false;
      }
      return true;
    });
  }, [endpoints, tag, search]);

  return (
    <div style={{ background: BRAND.bg, padding: '24px', fontFamily: 'inherit', minHeight: '100%' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12, marginBottom: 16 }}>
        <h1 style={{ fontSize: 22, fontWeight: 700, color: BRAND.ink, margin: 0 }}>
          API cho App <span style={{ color: BRAND.inkSoft, fontWeight: 400 }}>({filtered.length}/{endpoints.length})</span>
        </h1>
        <a
          href="/docs"
          target="_blank"
          rel="noreferrer"
          style={{
            fontSize: 13, fontWeight: 700, padding: '9px 16px', borderRadius: 999,
            background: BRAND.primary, color: BRAND.primaryInk, textDecoration: 'none',
          }}
        >
          Mở Swagger UI (thử request) ↗
        </a>
      </div>

      <p style={{ fontSize: 13, color: BRAND.inkSoft, marginTop: 0, marginBottom: 16 }}>
        Danh sách endpoint đã sẵn sàng để app Android tích hợp qua Retrofit. Sinh trực tiếp từ OpenAPI spec của backend,
        luôn khớp với code hiện tại.{basePath ? ` Base path: ${basePath}` : ''}
      </p>

      <input
        type="text"
        placeholder="Tìm theo đường dẫn hoặc mô tả..."
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        style={{
          width: '100%', maxWidth: 360, padding: '9px 12px', marginBottom: 12,
          borderRadius: 10, border: `1px solid ${BRAND.border}`, fontSize: 13,
        }}
      />

      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 20 }}>
        <span style={pillStyle(tag === '')} onClick={() => setTag('')}>Tất cả</span>
        {tags.map((t) => (
          <span key={t} style={pillStyle(tag === t)} onClick={() => setTag(t)}>
            {TAG_LABEL[t] ?? t}
          </span>
        ))}
      </div>

      {loading ? (
        <p style={{ color: BRAND.inkSoft }}>Đang tải...</p>
      ) : error ? (
        <p style={{ color: BRAND.warn }}>Không tải được OpenAPI spec từ /docs-json. Kiểm tra Swagger đã bật ở main.ts chưa.</p>
      ) : (
        <div style={{ background: BRAND.surface, border: `1px solid ${BRAND.border}`, borderRadius: 14, overflow: 'hidden' }}>
          {filtered.map((e, i) => {
            const mc = METHOD_COLOR[e.method] ?? { bg: BRAND.border, fg: BRAND.ink };
            return (
              <div
                key={`${e.method}-${e.path}-${i}`}
                style={{
                  display: 'flex', alignItems: 'center', gap: 14, padding: '12px 16px',
                  borderTop: i === 0 ? 'none' : `1px solid ${BRAND.border}`,
                }}
              >
                <span
                  style={{
                    fontSize: 11, fontWeight: 700, padding: '4px 10px', borderRadius: 999,
                    background: mc.bg, color: mc.fg, textTransform: 'uppercase', minWidth: 56, textAlign: 'center',
                  }}
                >
                  {e.method}
                </span>
                <span style={{ fontFamily: 'ui-monospace, monospace', fontSize: 13, color: BRAND.ink, minWidth: 260 }}>
                  {e.path}
                </span>
                <span style={{ fontSize: 12.5, color: BRAND.inkSoft, flex: 1 }}>{e.summary}</span>
                {e.secured ? (
                  <span
                    title="Yêu cầu đăng nhập (Bearer token)"
                    style={{ fontSize: 10.5, fontWeight: 700, padding: '3px 8px', borderRadius: 999, background: '#FBF0CE', color: '#8A6A10' }}
                  >
                    🔒 Cần đăng nhập
                  </span>
                ) : (
                  <span
                    style={{ fontSize: 10.5, fontWeight: 700, padding: '3px 8px', borderRadius: 999, background: '#F1EADA', color: BRAND.inkSoft }}
                  >
                    Công khai
                  </span>
                )}
              </div>
            );
          })}
          {filtered.length === 0 ? (
            <p style={{ padding: 16, color: BRAND.inkSoft, margin: 0 }}>Không tìm thấy endpoint phù hợp.</p>
          ) : null}
        </div>
      )}
    </div>
  );
};

export default ApiExplorer;
