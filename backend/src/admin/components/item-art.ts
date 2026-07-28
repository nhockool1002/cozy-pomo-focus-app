/**
 * Bộ sinh hình SVG cho vật phẩm bổ trợ (ShopCategory.BOOST) — cùng ngôn ngữ hình ảnh với
 * species-art.ts/egg-art.ts (hiệu ứng lơ lửng `sp-anim`, vòng hào quang mạch đập `sp-pulse`,
 * lấp lánh `sp-flicker`), 2 chủ đề riêng theo `boostType`:
 *   - FOCUS_MINUTES (Túi Thời Gian) — túi rút dây phát sáng vàng hổ phách, có tia sét bên trong,
 *     số tia sét + độ rực tăng dần theo tier (Nhỏ→Khổng Lồ) để "ngầu" hơn ở vật phẩm giá cao hơn.
 *   - HATCH_MINUTES (Giọt Sương/Ánh Trăng Ấp Trứng) — giọt sương xanh dịu; riêng Ánh Trăng
 *     (purchasable:false, chỉ phát sự kiện) đổi hẳn sang trăng lưỡi liềm + sao, tông vàng kim như
 *     trứng Huyền thoại (egg-art.ts) để nổi bật đúng vị thế "hàng hiếm".
 */
import { rndFor } from './species-art.js';

const AMBER = '#F4B942';
const AMBER_DARK = '#B9761F';
const AMBER_LIGHT = '#FFE9B8';
const MOON_BASE = '#8FA8E8';
const MOON_DARK = '#4F5FA8';
const DEW_BASE = '#6FD8D0';
const DEW_DARK = '#2E8C86';
const DEW_LIGHT = '#E0FFFC';
const GOLD = '#F4D160';

export type BoostTier = 'small' | 'medium' | 'large' | 'giant' | 'legendary';

/** Suy tier từ dữ liệu ShopItem — không thêm cột DB mới, chỉ đọc `boostType`/`boostAmount`/`purchasable` đã có sẵn. */
export function boostTierFor(boostType: string | null, boostAmount: number | null, purchasable: boolean): BoostTier {
  if (!purchasable) return 'legendary'; // Ánh Trăng Ấp Trứng — sự kiện, không bán
  if (boostType === 'HATCH_MINUTES') return 'medium'; // Giọt Sương — chỉ 1 mốc mua được
  const amt = boostAmount ?? 0;
  if (amt <= 10) return 'small';
  if (amt <= 20) return 'medium';
  if (amt <= 50) return 'large';
  return 'giant';
}

const BOLT_COUNT: Record<BoostTier, number> = { small: 1, medium: 2, large: 3, giant: 4, legendary: 4 };

function bolt(cx: number, cy: number, scale: number, rot: number, color: string): string {
  return `<path class="sp-flicker" style="animation-delay:${(-(rot / 90)).toFixed(2)}s" transform="translate(${cx} ${cy}) rotate(${rot}) scale(${scale})" d="M-3 -9 L2 -1 L-1 -1 L3 9 L-3 0 L0 0 Z" fill="${color}"/>`;
}

/** Túi rút dây (drawstring pouch) — thân bo tròn + dây rút thắt nút trên cùng, tia sét quanh túi
 * (số lượng theo tier) gợi "năng lượng tích luỹ" bên trong, không phải chỉ 1 icon đồng hồ cát tĩnh. */
function pouchBody(tier: BoostTier, seed: string): string {
  const r = rndFor(seed);
  const tilt = (r() * 6 - 3).toFixed(1);
  const boltN = BOLT_COUNT[tier];
  let bolts = '';
  for (let i = 0; i < boltN; i++) {
    const angle = (360 / boltN) * i + r() * 20;
    const rad = 34;
    const x = 50 + rad * Math.cos((angle * Math.PI) / 180);
    const y = 55 + rad * Math.sin((angle * Math.PI) / 180);
    bolts += bolt(x, y, tier === 'giant' ? 1.3 : 1, angle, AMBER_DARK);
  }
  return `<g transform="rotate(${tilt} 50 58)">
    <ellipse cx="50" cy="90" rx="20" ry="5" fill="${AMBER_DARK}" opacity="0.16"/>
    <path d="M28 50 C28 34 38 26 50 26 C62 26 72 34 72 50 L70 82 C70 90 61 94 50 94 C39 94 30 90 30 82 Z" fill="${AMBER}"/>
    <path d="M32 46 Q50 30 68 46" stroke="${AMBER_DARK}" stroke-width="4" fill="none" stroke-linecap="round"/>
    <circle cx="40" cy="42" r="3" fill="${AMBER_DARK}"/>
    <circle cx="60" cy="42" r="3" fill="${AMBER_DARK}"/>
    <ellipse cx="41" cy="58" rx="9" ry="13" fill="${AMBER_LIGHT}" opacity="0.65"/>
    <path d="M50 48 L44 62 L49 62 L45 76 L58 58 L51 58 Z" fill="${AMBER_DARK}"/>
    ${bolts}
  </g>`;
}

/** Giọt sương đơn giản, tông xanh ngọc dịu — vật phẩm mua được duy nhất trong nhóm HATCH_MINUTES. */
function dewBody(seed: string): string {
  const r = rndFor(seed);
  const tilt = (r() * 6 - 3).toFixed(1);
  return `<g transform="rotate(${tilt} 50 55)">
    <ellipse cx="50" cy="90" rx="16" ry="4.5" fill="${DEW_DARK}" opacity="0.16"/>
    <path d="M50 18 C64 40 74 56 74 68 C74 84 63 94 50 94 C37 94 26 84 26 68 C26 56 36 40 50 18 Z" fill="${DEW_BASE}"/>
    <ellipse cx="41" cy="62" rx="8" ry="12" fill="${DEW_LIGHT}" opacity="0.7"/>
  </g>`;
}

/** Ánh Trăng Ấp Trứng — trăng lưỡi liềm vàng kim + sao nhỏ, tông "huyền thoại" khác hẳn Giọt
 * Sương để phân biệt rõ ngay từ hình dạng (không chỉ qua vòng hào quang). */
function moonBody(seed: string): string {
  const r = rndFor(seed);
  const tilt = (r() * 6 - 3).toFixed(1);
  return `<g transform="rotate(${tilt} 50 55)">
    <path d="M62 24 C46 24 33 38 33 55 C33 72 46 86 62 86 C68 86 74 84 78 81 C68 80 58 68 58 55 C58 42 68 30 78 29 C74 26 68 24 62 24 Z" fill="${MOON_BASE}"/>
    <path d="M62 24 C46 24 33 38 33 55 C33 72 46 86 62 86 C68 86 74 84 78 81 C68 80 58 68 58 55 C58 42 68 30 78 29 C74 26 68 24 62 24 Z" fill="${MOON_DARK}" opacity="0.28"/>
    <path class="sp-flicker" d="M28 36 L30 41 L35 43 L30 45 L28 50 L26 45 L21 43 L26 41 Z" fill="${GOLD}"/>
    <path class="sp-flicker-hot" d="M74 60 L75.5 64 L79 65 L75.5 66 L74 70 L72.5 66 L69 65 L72.5 64 Z" fill="${GOLD}"/>
  </g>`;
}

/** SVG hoàn chỉnh (viewBox 0 0 100 100) cho 1 vật phẩm BOOST — bay bổng nhẹ qua class sp-anim. */
export function renderBoostArt(params: { boostType: string | null; tier: BoostTier; name: string }): string {
  const body =
    params.boostType === 'HATCH_MINUTES'
      ? params.tier === 'legendary'
        ? moonBody(params.name)
        : dewBody(params.name)
      : pouchBody(params.tier, params.name);
  return `<svg class="sp-anim" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">${body}</svg>`;
}

const TIER_RINGS: Record<BoostTier, Array<[number, number, number]>> = {
  small: [[38, 1, 0.16]],
  medium: [[39, 1.2, 0.2]],
  large: [[40, 1.4, 0.24], [32, 0.8, 0.16]],
  giant: [[41, 1.6, 0.3], [33, 1, 0.2]],
  legendary: [[43, 1.8, 0.32], [35, 1, 0.22]],
};

function sparkles(colorHex: string): string {
  const pts: Array<[number, number]> = [[24, 20], [76, 26], [70, 68], [20, 60]];
  return pts
    .map(([x, y]) => `<path class="sp-flicker" d="M${x} ${y - 5} L${x + 2} ${y} L${x} ${y + 5} L${x - 2} ${y} Z" fill="${colorHex}"/>`)
    .join('');
}

/** Vầng hào quang phía sau vật phẩm, màu theo chủ đề (hổ phách/xanh dương) + độ rực theo tier. */
export function renderBoostAura(boostType: string | null, tier: BoostTier): string {
  const ringColor = boostType === 'HATCH_MINUTES' ? (tier === 'legendary' ? GOLD : DEW_BASE) : AMBER;
  const circles = TIER_RINGS[tier]
    .map(([rad, w, op]) => `<circle class="sp-pulse" cx="50" cy="55" r="${rad}" fill="none" stroke="${ringColor}" stroke-width="${w}" opacity="${op}"/>`)
    .join('');
  const flair = tier === 'legendary' ? sparkles(GOLD) : tier === 'giant' ? sparkles(AMBER) : '';
  return `<svg class="sp-aura" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">${circles}${flair}</svg>`;
}

export const BOOST_TIER_LABEL: Record<BoostTier, string> = {
  small: 'Nhỏ',
  medium: 'Vừa',
  large: 'Lớn',
  giant: 'Khổng lồ',
  legendary: 'Sự kiện',
};

export const BOOST_TIER_BADGE: Record<BoostTier, { fg: string; bg: string }> = {
  small: { fg: '#7A6C5C', bg: '#EFE4C8' },
  medium: { fg: AMBER_DARK, bg: AMBER_LIGHT },
  large: { fg: '#8A6A10', bg: '#FFF3D0' },
  giant: { fg: '#8A6A10', bg: '#FFE29A' },
  legendary: { fg: GOLD, bg: '#2A1F16' },
};
