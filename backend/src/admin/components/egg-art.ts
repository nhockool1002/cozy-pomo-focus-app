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

export function eggTierForPrice(priceCoin: number): EggTier {
  if (priceCoin >= 150) return 'legendary';
  if (priceCoin >= 60) return 'rare';
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

/** Vầng hào quang phía sau trứng, màu theo `colorHex` riêng và độ rực theo mốc giá (thường/hiếm/huyền thoại). */
export function renderEggAura(colorHex: string, priceCoin: number): string {
  const base = HEX_RE.test(colorHex) ? colorHex : DEFAULT_COLOR;
  const tier = eggTierForPrice(priceCoin);
  const circles = TIER_RINGS[tier]
    .map(([rad, w, op]) => `<circle class="sp-pulse" cx="50" cy="55" r="${rad}" fill="none" stroke="${base}" stroke-width="${w}" opacity="${op}"/>`)
    .join('');
  const flair = tier === 'legendary' ? sparkles(GOLD) : '';
  return `<svg class="sp-aura" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">${circles}${flair}</svg>`;
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
