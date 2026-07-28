/**
 * Bộ sinh hình SVG + vầng hào quang cho thẻ Trứng (EggType) trong AdminJS —
 * cùng ngôn ngữ hình ảnh với Creature Atlas / species-art.ts (hiệu ứng lơ lửng
 * `sp-anim`, vòng hào quang mạch đập `sp-pulse`), nhưng màu lấy trực tiếp từ
 * `colorHex` của từng loại trứng thay vì bảng màu 14 mục của loài.
 */
import { rndFor } from './species-art.js';

const GOLD = '#F4D160';
const DEFAULT_COLOR = '#A8D08D';
const HEX_RE = /^#([0-9a-f]{3}|[0-9a-f]{6})$/i;

function hexToRgb(hex: string): [number, number, number] {
  const h = hex.slice(1);
  const full = h.length === 3 ? h.split('').map((c) => c + c).join('') : h;
  const int = parseInt(full, 16);
  return [(int >> 16) & 255, (int >> 8) & 255, int & 255];
}
function rgbToHex(r: number, g: number, b: number): string {
  const c = (v: number) => Math.max(0, Math.min(255, Math.round(v))).toString(16).padStart(2, '0');
  return `#${c(r)}${c(g)}${c(b)}`;
}
/** amt > 0: pha trắng (sáng hơn); amt < 0: pha đen (tối hơn). */
function shade(hex: string, amt: number): string {
  const [r, g, b] = hexToRgb(hex);
  const mix = (v: number) => (amt >= 0 ? v + (255 - v) * amt : v * (1 + amt));
  return rgbToHex(mix(r), mix(g), mix(b));
}

function eggBody(base: string, dark: string, light: string, seed: string): string {
  const r = rndFor(seed);
  const tilt = (r() * 6 - 3).toFixed(1);
  const spots: Array<[number, number]> = [[42, 64], [58, 50], [47, 45], [55, 73]];
  let speckles = '';
  for (let i = 0; i < 3; i++) {
    const [x, y] = spots[i];
    const rr = (2 + r() * 1.4).toFixed(1);
    speckles += `<circle cx="${x}" cy="${y}" r="${rr}" fill="${dark}" opacity="0.35"/>`;
  }
  return `<g transform="rotate(${tilt} 50 55)">
    <ellipse cx="50" cy="90" rx="19" ry="5" fill="${dark}" opacity="0.16"/>
    <path d="M50 16 C69 16 79 45 79 63 C79 83 66 94 50 94 C34 94 21 83 21 63 C21 45 31 16 50 16 Z" fill="${base}"/>
    <ellipse cx="39" cy="38" rx="9" ry="12" fill="${light}" opacity="0.65"/>
    ${speckles}
  </g>`;
}

/** SVG hoàn chỉnh (viewBox 0 0 100 100) cho 1 loại trứng — bay bổng nhẹ qua class sp-anim. */
export function renderEggArt(params: { colorHex: string; name: string }): string {
  const base = HEX_RE.test(params.colorHex) ? params.colorHex : DEFAULT_COLOR;
  const dark = shade(base, -0.3);
  const light = shade(base, 0.55);
  return `<svg class="sp-anim" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">${eggBody(base, dark, light, params.name)}</svg>`;
}

export type EggTier = 'common' | 'rare' | 'legendary';

/**
 * Ngưỡng giá cũ (150/60) được viết trước T-116 khi giá trứng còn ở thang thấp — sau khi T-116
 * tăng giá 4 loại trứng mua được lên 300–1500 Xu, MỌI loại đều rơi vào nhánh `legendary` theo
 * ngưỡng cũ. Trong khi đó 3 Trứng Truyền Thuyết (T-116, chỉ Admin phát qua `OwnedEgg`, không
 * bán qua Cửa hàng) luôn có `priceCoin = 0` nên lại rơi vào `common` — ngược hoàn toàn với thiết
 * kế thật (Truyền Thuyết mới là Huyền thoại). Tín hiệu đúng không phải "giá cao/thấp" mà là
 * "có bán hay không": `priceCoin = 0` = Admin-only = Huyền thoại, còn lại đều là trứng mua được
 * bình thường trong Cửa hàng nên xếp chung `common`.
 */
export function eggTierForPrice(priceCoin: number): EggTier {
  if (priceCoin === 0) return 'legendary';
  return 'common';
}

const TIER_RINGS: Record<EggTier, Array<[number, number, number]>> = {
  common: [[40, 1.2, 0.18]],
  rare: [[41, 1.5, 0.26], [33, 0.9, 0.18]],
  legendary: [[43, 1.8, 0.32], [35, 1, 0.22]],
};

function sparkles(colorHex: string): string {
  const pts: Array<[number, number]> = [[24, 20], [76, 26], [70, 68], [20, 60]];
  return pts
    .map(([x, y]) => `<path class="sp-flicker" d="M${x} ${y - 5} L${x + 2} ${y} L${x} ${y + 5} L${x - 2} ${y} Z" fill="${colorHex}"/>`)
    .join('');
}

function hashStr(s: string): number {
  let h = 1779033703;
  for (let i = 0; i < s.length; i++) {
    h = Math.imul(h ^ s.charCodeAt(i), 3432918353);
    h = (h << 13) | (h >>> 19);
  }
  return h >>> 0;
}

/**
 * Hào quang Huyền thoại (T-128, Dev1002 yêu cầu tối hơn/cổ hơn/mãn nhãn hơn) — port 1:1 cùng cấu
 * trúc với hào quang SSR loài (`species-art.ts#renderAura`, xem comment ở đó: quầng glow mix-blend
 * screen + lõi sáng + vòng nét đứt xoay 2 chiều + sparkle) nhưng KHÔNG dùng vàng-hồng chung cho mọi
 * loài — quầng sáng lấy từ chính `colorHex` (đã tối/cổ) của từng quả trứng pha sáng lên (`glow`),
 * mỗi Trứng Truyền Thuyết phát ra 1 màu hào quang riêng theo "chất liệu" của nó (Rừng xanh rêu,
 * Biển xanh lam, Hoa hồng cổ) — chỉ giữ lại `GOLD` làm 1 vòng nhấn nhỏ chung, gợi "cổ vật dát vàng"
 * thay vì mọi Trứng Truyền Thuyết phát sáng cùng 1 màu vàng-hồng như SSR loài (mờ nhạt bản sắc).
 */
function legendaryEggAura(base: string, seed: string): string {
  const glow = shade(base, 0.45);
  const gid = `eggBlaze${Math.abs(hashStr(seed)).toString(36)}`;
  const gid2 = `eggCore${Math.abs(hashStr(seed + 'core')).toString(36)}`;
  const defs = `<defs>
      <radialGradient id="${gid}" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stop-color="${glow}" stop-opacity="0.9"/>
        <stop offset="100%" stop-color="${glow}" stop-opacity="0"/>
      </radialGradient>
      <radialGradient id="${gid2}" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stop-color="#FFFDF0" stop-opacity="0.9"/>
        <stop offset="100%" stop-color="${glow}" stop-opacity="0"/>
      </radialGradient>
    </defs>`;
  const glowLayer = `<circle class="sp-blaze" cx="50" cy="55" r="42" fill="url(#${gid})" style="mix-blend-mode:screen"/>
    <circle class="sp-core" cx="50" cy="55" r="15" fill="url(#${gid2})" style="mix-blend-mode:screen"/>`;
  const rings = `<circle class="sp-ring-hot" cx="50" cy="55" r="39" fill="none" stroke="${glow}" stroke-width="3.2" opacity="0.9"/>
    <g class="sp-aura-ring gold">
      <circle cx="50" cy="55" r="34" fill="none" stroke="${GOLD}" stroke-width="2" opacity="0.8" stroke-dasharray="7 6" stroke-linecap="round"/>
    </g>
    <g class="sp-aura-ring gold-rev">
      <circle cx="50" cy="55" r="25" fill="none" stroke="${glow}" stroke-width="1.6" opacity="0.65" stroke-dasharray="5 5" stroke-linecap="round"/>
    </g>`;
  const spark = sparkles(GOLD);
  return `${defs}${glowLayer}${rings}${spark}`;
}

/** Vầng hào quang phía sau trứng, màu theo `colorHex` riêng và độ rực theo mốc giá (thường/hiếm/huyền thoại). */
export function renderEggAura(colorHex: string, priceCoin: number, seed = 'egg'): string {
  const base = HEX_RE.test(colorHex) ? colorHex : DEFAULT_COLOR;
  const tier = eggTierForPrice(priceCoin);
  if (tier === 'legendary') {
    return `<svg class="sp-aura ssr" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">${legendaryEggAura(base, seed)}</svg>`;
  }
  const circles = TIER_RINGS[tier]
    .map(([rad, w, op]) => `<circle class="sp-pulse" cx="50" cy="55" r="${rad}" fill="none" stroke="${base}" stroke-width="${w}" opacity="${op}"/>`)
    .join('');
  return `<svg class="sp-aura" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">${circles}</svg>`;
}

export const EGG_TIER_LABEL: Record<EggTier, string> = {
  common: 'Thường',
  rare: 'Hiếm',
  legendary: 'Huyền thoại',
};

export const EGG_TIER_BADGE: Record<EggTier, { fg: string; bg: string }> = {
  common: { fg: '#7A6C5C', bg: '#EFE4C8' },
  rare: { fg: '#3F5C2E', bg: '#E9F2E0' },
  legendary: { fg: GOLD, bg: '#2A1F16' },
};
