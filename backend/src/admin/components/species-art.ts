/**
 * Bộ sinh hình SVG cho từng loài — cùng thuật toán với Creature Atlas artifact
 * (archetype + palette + seed tên riêng). Dùng lại ở cả trang List (thumbnail nhỏ)
 * và Show (ảnh lớn) của resource Species trong AdminJS.
 *
 * Bảng màu + hình vẽ (T-124, 2026-07-27) — bản "hầm hố dễ thương": base là màu kẹo bão hoà, dark là
 * 1 tông đậm hơn CÙNG gam màu (không phải đen), "light" đổi vai trò thành glow (điểm nhấn sáng vui ở
 * mắt/sparkle) thay vì pastel nhạt như bản gốc. Port 1:1 từ SpeciesArt.kt (Android/Compose) — xem
 * comment ở đó để hiểu lý do đổi hướng.
 */

export const PALETTE = [
  { base: '#FF9466', dark: '#B24E32', light: '#FFD166' },
  { base: '#FFDD59', dark: '#C9A227', light: '#FF8FD1' },
  { base: '#8BD17E', dark: '#4F8C45', light: '#FFEA8A' },
  { base: '#6C9EFF', dark: '#3D5AC2', light: '#7CF2E0' },
  { base: '#FF8FAE', dark: '#C24E72', light: '#FFF07C' },
  { base: '#B98FE8', dark: '#7A4FB8', light: '#7CF2E0' },
  { base: '#5FBF52', dark: '#357A2C', light: '#FFD166' },
  { base: '#FF8A65', dark: '#C2502E', light: '#7CF2E0' },
  { base: '#E8C97A', dark: '#B8933D', light: '#FF8FD1' },
  { base: '#2FD9C4', dark: '#1C8C94', light: '#FF8FD1' },
  { base: '#E8559C', dark: '#A8306E', light: '#7CFFC4' },
  { base: '#8FA8E8', dark: '#4F5FA8', light: '#FFD166' },
  { base: '#FFC94A', dark: '#C9860F', light: '#7CFFC4' },
  { base: '#6FE0B8', dark: '#359C78', light: '#FFD166' },
];
const LEAF = { base: '#6FCB5A', dark: '#3F8C32' };
const INK = '#6D594E';
const MOUND = '#E8D4A8';
const SHINE = '#FFFBF3';
const GLOW_GOLD = '#FFD166';
const GLOW_PINK = '#FF8FD1';
const GLOW_MINT = '#7CFFC4';

export const RARITY_COLORS: Record<string, string> = {
  B: '#FFE9B8',
  A: '#7CF2E0',
  S: GLOW_GOLD,
  SS: GLOW_PINK,
  SSR: GLOW_GOLD,
};

export const RARITY_BADGE: Record<string, { fg: string; bg: string }> = {
  B: { fg: '#8A7A5E', bg: '#FFF3D8' },
  A: { fg: '#2E7A5E', bg: '#DFF7EE' },
  S: { fg: '#8A6A10', bg: '#FFF3D0' },
  SS: { fg: '#C23E8A', bg: '#FFE3F3' },
  SSR: { fg: '#C9860F', bg: '#FFF7DD' },
};

const n1 = (v: number) => v.toFixed(1);

/** Hoạ tiết sparkle (✦) dùng chung — 1 điểm nhấn nhỏ trên mỗi loài + vòng hào quang rarity. */
function sparkleMark(cx: number, cy: number, r: number): string {
  const r2 = r * 0.35;
  return `M${n1(cx)} ${n1(cy - r)} L${n1(cx + r2)} ${n1(cy - r2)} L${n1(cx + r)} ${n1(cy)} L${n1(cx + r2)} ${n1(cy + r2)} L${n1(cx)} ${n1(cy + r)} L${n1(cx - r2)} ${n1(cy + r2)} L${n1(cx - r)} ${n1(cy)} L${n1(cx - r2)} ${n1(cy - r2)} Z`;
}

/** Mắt hí nhỏ dạng hạt sáng — dùng cho sinh vật biển + thần thú, màu lấy từ `p.light` (glow). */
function eyeMark(cx: number, cy: number, r: number): string {
  return `M${n1(cx - r)} ${n1(cy)} Q${n1(cx)} ${n1(cy - r)} ${n1(cx + r)} ${n1(cy)} Q${n1(cx)} ${n1(cy + r)} ${n1(cx - r)} ${n1(cy)} Z`;
}

/** CSS dùng chung cho hiệu ứng thẻ loài (float + vầng hào quang) — bơm 1 lần / trang qua thẻ <style>. */
export const CARD_FX_CSS = `
.sp-icon-wrap { position: relative; overflow: hidden; }
.sp-icon-wrap svg { position: absolute; top: 0; left: 0; width: 100%; height: 100%; }
.sp-aura { z-index: 0; }
.sp-aura.ssr { filter: drop-shadow(0 0 3px rgba(255,209,102,0.55)) drop-shadow(0 0 7px rgba(255,143,209,0.4)); }
.sp-icon-wrap svg.sp-anim { z-index: 1; }
.sp-badge { position: absolute; top: -4px; left: -4px; font-size: 9.5px; font-weight: 700; padding: 2px 6px; border-radius: 999px; z-index: 2; letter-spacing: 0.02em; }
@media (prefers-reduced-motion: no-preference) {
  .sp-anim { animation: sp-float 3.4s ease-in-out infinite; }
  .sp-aura-ring.gold, .sp-aura-ring.gold-rev, .sp-pulse, .sp-pulse-strong, .sp-flicker, .sp-flicker-hot, .sp-blaze, .sp-core, .sp-ring-hot { transform-box: fill-box; transform-origin: center; }
  .sp-aura-ring.gold { animation: sp-spin 3.2s linear infinite; }
  .sp-aura-ring.gold-rev { animation: sp-spin-rev 4.2s linear infinite; }
  .sp-pulse { animation: sp-breathe 2.6s ease-in-out infinite; }
  .sp-pulse-strong { animation: sp-breathe-strong 1.7s ease-in-out infinite; }
  .sp-ring-hot { animation: sp-breathe-hot 1.15s ease-in-out infinite; }
  .sp-flicker { animation: sp-flicker 0.9s ease-in-out infinite; }
  .sp-flicker-hot { animation: sp-flicker-hot 0.6s ease-in-out infinite; }
  .sp-blaze { animation: sp-blaze 1.3s ease-in-out infinite; filter: blur(2.5px); }
  .sp-core { animation: sp-core-pulse 0.7s ease-in-out infinite; filter: blur(1px); }
}
@keyframes sp-float { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-3px); } }
@keyframes sp-spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
@keyframes sp-spin-rev { from { transform: rotate(360deg); } to { transform: rotate(0deg); } }
@keyframes sp-breathe { 0%, 100% { opacity: 0.55; transform: scale(0.96); } 50% { opacity: 1; transform: scale(1.05); } }
@keyframes sp-breathe-strong { 0%, 100% { opacity: 0.55; transform: scale(0.92); } 50% { opacity: 1; transform: scale(1.1); } }
@keyframes sp-breathe-hot { 0%, 100% { opacity: 0.7; transform: scale(0.88); } 50% { opacity: 1; transform: scale(1.16); } }
@keyframes sp-flicker { 0%, 100% { opacity: 0.5; transform: scale(0.85); } 50% { opacity: 1; transform: scale(1.15); } }
@keyframes sp-flicker-hot { 0%, 100% { opacity: 0.55; transform: scale(0.75) rotate(0deg); } 50% { opacity: 1; transform: scale(1.35) rotate(12deg); } }
@keyframes sp-blaze { 0%, 100% { opacity: 0.65; transform: scale(0.92); } 50% { opacity: 1; transform: scale(1.18); } }
@keyframes sp-core-pulse { 0%, 100% { opacity: 0.75; transform: scale(0.8); } 50% { opacity: 1; transform: scale(1.35); } }
`;

// 8 điểm sparkle dùng chung cho S/SS/SSR — cùng bộ toạ độ ở cả 2 nền tảng, chỉ đổi màu/số lượng
// theo cấp bậc để "leo thang" rõ (T-124: đổi ngôn ngữ hình ảnh từ khói/sét/lửa sang lấp lánh).
const AURA_SPARK_PTS: Array<[number, number]> = [[26, 22], [74, 26], [70, 74], [22, 70], [50, 14], [50, 86], [12, 46], [88, 54]];

function sparkles(n: number, colors: string[], opts: { hot?: boolean; scale?: number; group?: string } = {}): string {
  const cls = opts.hot ? 'sp-flicker-hot' : 'sp-flicker';
  const scale = opts.scale ?? 1;
  let s = '';
  for (let i = 0; i < n; i++) {
    const [x, y] = AURA_SPARK_PTS[i % AURA_SPARK_PTS.length];
    const color = colors[i % colors.length];
    const r = (3.4 + (i % 3) * 0.7) * scale;
    const delay = (-(i * 0.23)).toFixed(2);
    s += `<path class="${cls}" style="animation-delay:${delay}s" d="${sparkleMark(x, y, r)}" fill="${color}"/>`;
  }
  return opts.group ? `<g class="sp-aura-ring ${opts.group}">${s}</g>` : s;
}

/**
 * Vầng hào quang phía sau ảnh loài — B/A chỉ vòng nhẹ, S/SS thêm sparkle xoay quanh (4 rồi 8
 * điểm), SSR thêm quầng sáng vàng-hồng (mix-blend screen) + lõi trắng + 2 vòng nét đứt xoay ngược
 * chiều nhau + sparkle lớn nhiều màu. Toàn bộ hình học nằm gọn trong viewBox kể cả ở đỉnh hiệu ứng
 * phồng (breathe, tối đa x1.16).
 */
export function renderAura(rarity: string, seed = 'x'): string {
  if (rarity === 'SSR') {
    const gid = `ssrBlaze${Math.abs(hashStr(seed)).toString(36)}`;
    const gid2 = `ssrCore${Math.abs(hashStr(seed + 'core')).toString(36)}`;
    const glow = `<defs>
        <radialGradient id="${gid}" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stop-color="${GLOW_GOLD}" stop-opacity="1"/>
          <stop offset="35%" stop-color="${GLOW_PINK}" stop-opacity="0.6"/>
          <stop offset="100%" stop-color="${GLOW_PINK}" stop-opacity="0"/>
        </radialGradient>
        <radialGradient id="${gid2}" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stop-color="#FFFDF0" stop-opacity="1"/>
          <stop offset="100%" stop-color="${GLOW_GOLD}" stop-opacity="0"/>
        </radialGradient>
      </defs>
      <circle class="sp-blaze" cx="50" cy="50" r="42" fill="url(#${gid})" style="mix-blend-mode:screen"/>
      <circle class="sp-core" cx="50" cy="50" r="16" fill="url(#${gid2})" style="mix-blend-mode:screen"/>`;
    const rings = `<circle class="sp-ring-hot" cx="50" cy="50" r="39" fill="none" stroke="${GLOW_GOLD}" stroke-width="3.6" opacity="0.95"/>
      <g class="sp-aura-ring gold">
        <circle cx="50" cy="50" r="34" fill="none" stroke="${GLOW_GOLD}" stroke-width="2.2" opacity="0.85" stroke-dasharray="7 6" stroke-linecap="round"/>
      </g>
      <g class="sp-aura-ring gold-rev">
        <circle cx="50" cy="50" r="25" fill="none" stroke="${GLOW_PINK}" stroke-width="1.8" opacity="0.7" stroke-dasharray="5 5" stroke-linecap="round"/>
      </g>`;
    const spark = sparkles(7, [GLOW_GOLD, GLOW_PINK, GLOW_MINT, '#FFFDF0'], { hot: true, scale: 1.3 });
    const core = `<circle class="sp-pulse" cx="50" cy="50" r="15" fill="#FFFDF0" opacity="0.7"/>`;
    return `<svg class="sp-aura ssr" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">${glow}${rings}${spark}${core}</svg>`;
  }
  if (rarity === 'SS') {
    const ring = `<circle class="sp-pulse-strong" cx="50" cy="50" r="39" fill="none" stroke="${GLOW_PINK}" stroke-width="2" opacity="0.4"/>`;
    const stars = sparkles(8, [GLOW_PINK, GLOW_GOLD], { group: 'gold-rev' });
    return `<svg class="sp-aura" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">${ring}${stars}</svg>`;
  }
  if (rarity === 'S') {
    const ring = `<circle class="sp-pulse" cx="50" cy="50" r="38" fill="none" stroke="${GLOW_GOLD}" stroke-width="1.5" opacity="0.28"/>`;
    const pts = [AURA_SPARK_PTS[0], AURA_SPARK_PTS[2], AURA_SPARK_PTS[4], AURA_SPARK_PTS[6]];
    const stars = pts.map(([x, y], i) => `<path class="sp-flicker" style="animation-delay:${(-(i * 0.3)).toFixed(2)}s" d="${sparkleMark(x, y, 3.2)}" fill="${GLOW_GOLD}"/>`).join('');
    return `<svg class="sp-aura" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">${ring}<g class="sp-aura-ring gold">${stars}</g></svg>`;
  }
  if (rarity === 'A') {
    const c = RARITY_COLORS.A;
    const rings = `<circle class="sp-pulse" cx="50" cy="50" r="30" fill="none" stroke="${c}" stroke-width="2" opacity="0.5"/><circle class="sp-pulse" style="animation-delay:-0.3s" cx="50" cy="50" r="24" fill="none" stroke="${c}" stroke-width="1" opacity="0.3"/>`;
    return `<svg class="sp-aura" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">${rings}</svg>`;
  }
  if (rarity === 'B') {
    const c = RARITY_COLORS.B;
    const ring = `<circle class="sp-pulse" cx="50" cy="50" r="34" fill="none" stroke="${c}" stroke-width="1.5" opacity="0.4" stroke-dasharray="2 4"/>`;
    return `<svg class="sp-aura" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">${ring}</svg>`;
  }
  return '';
}

function hashStr(s: string): number {
  let h = 1779033703;
  for (let i = 0; i < s.length; i++) {
    h = Math.imul(h ^ s.charCodeAt(i), 3432918353);
    h = (h << 13) | (h >>> 19);
  }
  return h >>> 0;
}
function mulberry32(a: number) {
  return function () {
    a |= 0;
    a = (a + 0x6d2b79f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}
export const rndFor = (seed: string) => mulberry32(hashStr(seed));

function starPoints(cx: number, cy: number, rOuter: number, rInner: number, points: number, rot: number) {
  const pts: string[] = [];
  for (let i = 0; i < points * 2; i++) {
    const r = i % 2 === 0 ? rOuter : rInner;
    const a = rot + (i * Math.PI) / points;
    pts.push(`${n1(cx + r * Math.cos(a))},${n1(cy + r * Math.sin(a))}`);
  }
  return `M${pts.join('L')}Z`;
}

type LandOpts = { ear: string; tail: string; snout: string; pattern: string; extra?: string };
const LAND_ARCH: Record<string, LandOpts> = {
  fox: { ear: 'pointy', tail: 'curl', snout: 'fox', pattern: 'none' },
  rabbit: { ear: 'long', tail: 'fluffy', snout: 'none', pattern: 'none' },
  bear: { ear: 'tiny', tail: 'stub', snout: 'bear', pattern: 'none' },
  cat: { ear: 'pointy', tail: 'curl', snout: 'none', pattern: 'stripe' },
  bird: { ear: 'none', tail: 'none', snout: 'beak', pattern: 'wing' },
  hedgehog: { ear: 'tiny', tail: 'none', snout: 'bear', pattern: 'spike' },
  squirrel: { ear: 'round', tail: 'fluffy', snout: 'none', pattern: 'none' },
  raccoon: { ear: 'round', tail: 'ringed', snout: 'bear', pattern: 'mask' },
  deer: { ear: 'tiny', tail: 'stub', snout: 'none', pattern: 'none', extra: 'antler' },
  owl: { ear: 'tuft', tail: 'none', snout: 'beak', pattern: 'none' },
};

function landSvg(archetype: string, paletteIdx: number, seed: string): string {
  const o = LAND_ARCH[archetype] ?? LAND_ARCH.fox;
  const p = PALETTE[paletteIdx % PALETTE.length];
  const r = rndFor(seed);
  const rot = (r() * 8 - 4).toFixed(1);
  let ear = '', tail = '', snout = '', pat = '', extra = '';
  if (o.ear === 'pointy') {
    ear = `<path d="M32 30 L26 10 L40 26 Z" fill="${p.dark}"/><path d="M68 30 L74 10 L60 26 Z" fill="${p.dark}"/>`
      + `<path d="M33 27 L29 15 L37 27 Z" fill="${p.light}" opacity="0.55"/><path d="M67 27 L71 15 L63 27 Z" fill="${p.light}" opacity="0.55"/>`;
  } else if (o.ear === 'round') ear = `<circle cx="34" cy="22" r="8" fill="${p.dark}"/><circle cx="66" cy="22" r="8" fill="${p.dark}"/>`;
  else if (o.ear === 'long') ear = `<ellipse cx="38" cy="10" rx="6" ry="16" fill="${p.dark}"/><ellipse cx="62" cy="10" rx="6" ry="16" fill="${p.dark}"/>`;
  else if (o.ear === 'tiny') ear = `<circle cx="36" cy="20" r="4" fill="${p.dark}"/><circle cx="64" cy="20" r="4" fill="${p.dark}"/>`;
  else if (o.ear === 'tuft') {
    ear = `<circle cx="34" cy="22" r="7" fill="${p.dark}"/><circle cx="66" cy="22" r="7" fill="${p.dark}"/>`
      + `<circle cx="34" cy="20" r="2.6" fill="${p.light}" opacity="0.55"/><circle cx="66" cy="20" r="2.6" fill="${p.light}" opacity="0.55"/>`;
  }
  if (o.tail === 'fluffy') tail = `<circle cx="76" cy="58" r="13" fill="${p.base}"/><circle cx="81" cy="49" r="9" fill="${p.light}"/>`;
  else if (o.tail === 'curl') tail = `<path d="M74 66 Q94 66 90 46 Q88 32 76 40" stroke="${p.base}" stroke-width="9" fill="none" stroke-linecap="round"/>`;
  else if (o.tail === 'stub') tail = `<circle cx="75" cy="64" r="7" fill="${p.base}"/>`;
  else if (o.tail === 'ringed') tail = `<rect x="72" y="46" width="10" height="28" rx="5" fill="${p.base}"/><rect x="72" y="58" width="10" height="4" fill="${p.dark}"/>`;
  if (o.snout === 'fox') snout = `<path d="M50 40 L42 48 L58 48 Z" fill="${SHINE}"/>`;
  else if (o.snout === 'bear') snout = `<ellipse cx="50" cy="42" rx="9" ry="7" fill="${SHINE}"/>`;
  else if (o.snout === 'beak') snout = `<path d="M50 38 L41 44 L50 46 Z" fill="${p.dark}"/>`;
  if (o.pattern === 'spot') pat = `<circle cx="40" cy="60" r="3" fill="${p.dark}" opacity="0.5"/><circle cx="58" cy="66" r="2.4" fill="${p.dark}" opacity="0.5"/>`;
  else if (o.pattern === 'stripe') pat = `<path d="M28 56 Q50 62 72 56" stroke="${p.dark}" stroke-width="3" fill="none" opacity="0.45"/>`;
  else if (o.pattern === 'mask') pat = `<path d="M34 32 Q50 40 66 32" stroke="${p.dark}" stroke-width="7" fill="none" opacity="0.85" stroke-linecap="round"/>`;
  else if (o.pattern === 'spike') pat = `<path d="M34 46 L30 38 L38 44 Z" fill="${p.dark}"/><path d="M56 40 L58 30 L62 40 Z" fill="${p.dark}"/>`;
  else if (o.pattern === 'wing') pat = `<ellipse cx="34" cy="62" rx="8" ry="12" fill="${p.dark}" opacity="0.45" transform="rotate(-20 34 62)"/>`;
  if (o.extra === 'antler') extra = `<path d="M42 16 L38 4 M42 16 L46 6" stroke="${p.dark}" stroke-width="2.4" fill="none" stroke-linecap="round"/><path d="M58 16 L62 4 M58 16 L54 6" stroke="${p.dark}" stroke-width="2.4" fill="none" stroke-linecap="round"/>`;
  const body = `<ellipse cx="50" cy="62" rx="26" ry="22" fill="${p.base}"/><ellipse cx="44" cy="68" rx="8" ry="11" fill="${SHINE}" opacity="0.85"/>`;
  const eyes = `<circle cx="43" cy="33.5" r="3.2" fill="${INK}"/><circle cx="57" cy="33.5" r="3.2" fill="${INK}"/><circle cx="41.8" cy="32.3" r="1" fill="${SHINE}"/><circle cx="55.8" cy="32.3" r="1" fill="${SHINE}"/>`;
  const sparkle = `<path d="${sparkleMark(50, 8, 4)}" fill="${p.light}" opacity="0.9"/>`;
  return `<g transform="rotate(${rot} 50 50)">${tail}${body}${ear}<circle cx="50" cy="35" r="19" fill="${p.base}"/>${snout}${eyes}${sparkle}${pat}${extra}</g>`;
}

type SeaOpts = { shape: string; shellType?: string; fin?: string; legs?: number; dome?: boolean };
const SEA_ARCH: Record<string, SeaOpts> = {
  turtle: { shape: 'shell', shellType: 'dome' },
  crab: { shape: 'shell', shellType: 'claws' },
  snail: { shape: 'shell', shellType: 'spiral' },
  fish: { shape: 'fish' },
  starfish: { shape: 'star' },
  seal: { shape: 'blob', fin: 'flipper' },
  dolphin: { shape: 'blob', fin: 'dorsal' },
  jellyfish: { shape: 'tentacle', legs: 6, dome: true },
  octopus: { shape: 'tentacle', legs: 6, dome: false },
  seahorse: { shape: 'seahorse' },
};

function seaSvg(archetype: string, paletteIdx: number, seed: string): string {
  const o = SEA_ARCH[archetype] ?? SEA_ARCH.fish;
  const p = PALETTE[paletteIdx % PALETTE.length];
  const r = rndFor(seed);
  const rot = (r() * 8 - 4).toFixed(1);
  let body = '';
  if (o.shape === 'shell') {
    if (o.shellType === 'dome') {
      body = `<circle cx="24" cy="54" r="10" fill="${p.base}"/><ellipse cx="54" cy="56" rx="26" ry="19" fill="${p.dark}"/>`
        + `<ellipse cx="54" cy="50" rx="9" ry="6" fill="${SHINE}" opacity="0.6"/>`
        + `<path d="${eyeMark(24, 52, 2.2)}" fill="${p.light}"/>`
        + `<path d="${sparkleMark(70, 40, 3.6)}" fill="${p.light}" opacity="0.85"/>`;
    } else if (o.shellType === 'claws') {
      body = `<ellipse cx="50" cy="58" rx="27" ry="15" fill="${p.base}"/><circle cx="22" cy="40" r="8" fill="${p.dark}"/><circle cx="78" cy="40" r="8" fill="${p.dark}"/>`
        + `<ellipse cx="44" cy="52" rx="8" ry="4" fill="${SHINE}" opacity="0.6"/>`
        + `<path d="${eyeMark(42, 54, 2.2)}" fill="${p.light}"/><path d="${eyeMark(58, 54, 2.2)}" fill="${p.light}"/>`
        + `<path d="${sparkleMark(50, 36, 3.6)}" fill="${p.light}" opacity="0.85"/>`;
    } else {
      body = `<circle cx="46" cy="52" r="17" fill="${p.base}"/><circle cx="46" cy="52" r="12" fill="${p.dark}" opacity="0.5"/><ellipse cx="68" cy="66" rx="14" ry="9" fill="${p.base}"/>`
        + `<path d="${eyeMark(78, 62, 2.2)}" fill="${p.light}"/>`
        + `<path d="${sparkleMark(38, 40, 3.6)}" fill="${p.light}" opacity="0.85"/>`;
    }
  } else if (o.shape === 'fish') {
    body = `<path d="M78 50 L94 40 L90 50 L94 60 L78 50 Z" fill="${p.dark}"/><path d="M28 50 C28 30 72 30 76 50 C72 70 28 70 28 50 Z" fill="${p.base}"/>`
      + `<ellipse cx="38" cy="58" rx="8" ry="5" fill="${SHINE}" opacity="0.55"/>`
      + `<path d="${eyeMark(42, 48, 2.8)}" fill="${p.light}"/>`
      + `<path d="${sparkleMark(58, 34, 3.6)}" fill="${p.light}" opacity="0.85"/>`;
  } else if (o.shape === 'star') {
    body = `<path d="${starPoints(50, 54, 30, 13, 5, -Math.PI / 2)}" fill="${p.base}"/>`
      + `<circle cx="50" cy="44" r="4" fill="${SHINE}" opacity="0.5"/>`
      + `<path d="${eyeMark(44, 48, 2.2)}" fill="${p.dark}"/><path d="${eyeMark(56, 48, 2.2)}" fill="${p.dark}"/>`
      + `<path d="${sparkleMark(50, 14, 3.6)}" fill="${p.light}" opacity="0.9"/>`;
  } else if (o.shape === 'blob') {
    const fin = o.fin === 'dorsal' ? `<path d="M58 34 L66 16 L68 36 Z" fill="${p.dark}"/>` : `<ellipse cx="24" cy="58" rx="8" ry="4" fill="${p.dark}"/><ellipse cx="76" cy="58" rx="8" ry="4" fill="${p.dark}"/>`;
    body = `<path d="M80 50 L94 42 L90 58 L94 68 L80 60 Z" fill="${p.dark}"/><ellipse cx="48" cy="55" rx="30" ry="21" fill="${p.base}"/>`
      + `<ellipse cx="38" cy="62" rx="9" ry="6" fill="${SHINE}" opacity="0.6"/>${fin}`
      + `<path d="${eyeMark(66, 48, 2.6)}" fill="${p.light}"/>`
      + `<path d="${sparkleMark(56, 30, 3.6)}" fill="${p.light}" opacity="0.85"/>`;
  } else if (o.shape === 'tentacle') {
    let legs = '';
    const n = o.legs ?? 6;
    for (let i = 0; i < n; i++) {
      const x = 30 + i * (40 / (n - 1));
      const sway = i % 2 === 0 ? 6 : -6;
      legs += `<path d="M${n1(x)} 60 Q${n1(x + sway)} 74 ${n1(x)} 88" stroke="${p.dark}" stroke-width="3.2" fill="none" stroke-linecap="round"/>`;
    }
    const head = o.dome ? `<path d="M24 58 A26 26 0 0 1 76 58 Z" fill="${p.base}"/>` : `<circle cx="50" cy="48" r="24" fill="${p.base}"/>`;
    body = `${legs}${head}<ellipse cx="40" cy="38" rx="8" ry="5" fill="${SHINE}" opacity="0.55"/>`
      + `<path d="${eyeMark(42, 48, 2.6)}" fill="${p.light}"/><path d="${eyeMark(58, 48, 2.6)}" fill="${p.light}"/>`
      + `<path d="${sparkleMark(50, 20, 3.8)}" fill="${p.light}" opacity="0.9"/>`;
  } else if (o.shape === 'seahorse') {
    body = `<path d="M46 82 C30 82 30 66 42 60 C54 54 40 48 42 38 C44 28 58 24 62 32" stroke="${p.base}" stroke-width="13" fill="none" stroke-linecap="round"/>`
      + `<ellipse cx="40" cy="66" rx="4" ry="6" fill="${SHINE}" opacity="0.55"/>`
      + `<path d="${eyeMark(58, 30, 2.4)}" fill="${p.light}"/>`
      + `<path d="${sparkleMark(34, 44, 3.4)}" fill="${p.light}" opacity="0.85"/>`;
  }
  return `<g transform="rotate(${rot} 50 50)">${body}</g>`;
}

function plantSvg(archetype: string, paletteIdx: number, seed: string): string {
  const p = PALETTE[paletteIdx % PALETTE.length];
  const r = rndFor(seed);
  const lean = (r() * 6 - 3).toFixed(1);
  let inner = `<ellipse cx="50" cy="86" rx="22" ry="6" fill="${MOUND}"/>`;
  const stem = `<path d="M50 84 L50 46" stroke="${LEAF.base}" stroke-width="5" stroke-linecap="round"/>`;
  const leaves = `<ellipse cx="38" cy="66" rx="10" ry="5" fill="${LEAF.base}"/><ellipse cx="62" cy="58" rx="10" ry="5" fill="${LEAF.base}"/>`;
  if (archetype === 'flowerRound') {
    let petals = '';
    for (let i = 0; i < 6; i++) { const a = (i * Math.PI) / 3; petals += `<circle cx="${n1(50 + 16 * Math.cos(a))}" cy="${n1(30 + 16 * Math.sin(a))}" r="9" fill="${p.base}"/>`; }
    inner += stem + leaves + petals + `<circle cx="50" cy="30" r="7" fill="${p.light}"/><path d="${sparkleMark(70, 18, 3.6)}" fill="${p.light}" opacity="0.85"/>`;
  } else if (archetype === 'flowerStar') {
    inner += stem + leaves + `<path d="${starPoints(50, 30, 15, 7, 6, 0)}" fill="${p.base}"/><circle cx="50" cy="30" r="4" fill="${p.light}"/><path d="${sparkleMark(72, 20, 3.6)}" fill="${p.light}" opacity="0.85"/>`;
  } else if (archetype === 'mushroom') {
    inner += `<rect x="44" y="52" width="12" height="32" rx="5" fill="#F1E6D2" stroke="${p.dark}" stroke-width="1.5"/><path d="M24 52 A26 20 0 0 1 76 52 Z" fill="${p.base}"/><circle cx="38" cy="42" r="3" fill="${p.light}"/><circle cx="58" cy="38" r="3.4" fill="${p.light}"/><path d="${sparkleMark(70, 30, 3.4)}" fill="${p.light}" opacity="0.85"/>`;
  } else if (archetype === 'fern') {
    let fronds = '';
    for (let i = 0; i < 3; i++) { const dx = (i - 1) * 16; fronds += `<path d="M50 84 Q${50 + dx} 50 ${50 + dx * 1.4} 24" stroke="${p.base}" stroke-width="4" fill="none" stroke-linecap="round"/><circle cx="${n1(50 + dx * 1.4)}" cy="24" r="2.6" fill="${p.dark}"/>`; }
    inner += fronds + `<path d="${sparkleMark(68, 30, 3.6)}" fill="${p.light}" opacity="0.85"/>`;
  } else if (archetype === 'succulent') {
    let petals = '';
    for (let i = 0; i < 7; i++) { const a = i * ((2 * Math.PI) / 7); petals += `<ellipse cx="${n1(50 + 15 * Math.cos(a))}" cy="${n1(66 + 15 * Math.sin(a) * 0.6)}" rx="9" ry="14" fill="${p.base}"/>`; }
    inner += petals + `<circle cx="50" cy="66" r="7" fill="${p.light}"/><path d="${sparkleMark(70, 40, 3.6)}" fill="${p.light}" opacity="0.85"/>`;
  } else if (archetype === 'cactus') {
    inner += `<path d="M36 86 L34 50 L40 36 L60 36 L66 50 L64 86 Z" fill="${p.base}"/>`
      + `<path d="M42 42 L42 80" stroke="${p.dark}" stroke-width="1.4" opacity="0.4"/><path d="M50 38 L50 82" stroke="${p.dark}" stroke-width="1.4" opacity="0.4"/><path d="M58 42 L58 80" stroke="${p.dark}" stroke-width="1.4" opacity="0.4"/>`
      + `<path d="M34 46 L25 44 L34 50 Z" fill="${p.dark}"/><path d="M66 46 L75 44 L66 50 Z" fill="${p.dark}"/>`
      + `<path d="M34 62 L25 62 L34 66 Z" fill="${p.dark}"/><path d="M66 62 L75 62 L66 66 Z" fill="${p.dark}"/>`
      + `<path d="M34 74 L25 72 L34 78 Z" fill="${p.dark}"/><path d="M66 74 L75 72 L66 78 Z" fill="${p.dark}"/>`
      + `<path d="${starPoints(50, 26, 11, 4.5, 5, -Math.PI / 2)}" fill="${p.light}"/><circle cx="55" cy="24" r="1.6" fill="${SHINE}" opacity="0.9"/>`;
  } else if (archetype === 'berry') {
    inner += `<ellipse cx="40" cy="60" rx="16" ry="14" fill="${LEAF.base}"/><ellipse cx="62" cy="56" rx="15" ry="13" fill="${LEAF.base}"/><circle cx="38" cy="58" r="3.4" fill="${p.base}"/><circle cx="52" cy="66" r="3.4" fill="${p.base}"/><circle cx="62" cy="54" r="3.4" fill="${p.base}"/><circle cx="37" cy="56.5" r="1" fill="${SHINE}" opacity="0.8"/><path d="${sparkleMark(68, 34, 3.6)}" fill="${p.light}" opacity="0.85"/>`;
  } else if (archetype === 'bamboo') {
    let stalks = '';
    for (let i = 0; i < 3; i++) { const x = 38 + i * 12; stalks += `<rect x="${x - 4}" y="20" width="8" height="64" rx="4" fill="${p.base}"/><rect x="${x - 4}" y="36" width="8" height="3" fill="${p.dark}"/><rect x="${x - 4}" y="54" width="8" height="3" fill="${p.dark}"/>`; }
    inner += stalks + `<path d="${sparkleMark(66, 26, 3.6)}" fill="${p.light}" opacity="0.85"/>`;
  } else if (archetype === 'vine') {
    inner += `<path d="M28 82 Q50 60 34 44 Q20 30 40 20" stroke="${LEAF.base}" stroke-width="4" fill="none" stroke-linecap="round"/><circle cx="40" cy="20" r="6" fill="${p.base}"/><path d="${sparkleMark(64, 34, 3.6)}" fill="${p.light}" opacity="0.85"/>`;
  } else if (archetype === 'tree') {
    inner += `<rect x="45" y="50" width="10" height="34" rx="4" fill="#B98A5D"/><circle cx="40" cy="38" r="16" fill="${p.base}"/><circle cx="60" cy="36" r="14" fill="${p.base}"/><circle cx="50" cy="26" r="15" fill="${p.light}"/><path d="${sparkleMark(74, 20, 3.6)}" fill="${p.light}" opacity="0.85"/>`;
  }
  return `<g transform="rotate(${lean} 50 78)">${inner}</g>`;
}

function mythicSvg(archetype: string, paletteIdx: number, seed: string): string {
  const p = PALETTE[paletteIdx % PALETTE.length];
  const r = rndFor(seed);
  const rot = (r() * 6 - 3).toFixed(1);
  let body = '';
  if (archetype === 'phoenix') {
    let plumes = '';
    const plumeColors = [p.dark, p.base, p.light];
    for (let i = 0; i < 3; i++) { const dx = -12 + i * 12; plumes += `<path d="M50 62 Q${40 + dx} 80 ${28 + dx} 96" stroke="${plumeColors[i]}" stroke-width="6" fill="none" stroke-linecap="round"/>`; }
    const wings = `<path d="M34 50 Q8 38 6 60 Q24 68 38 58 Z" fill="${p.base}"/><path d="M66 50 Q92 38 94 60 Q76 68 62 58 Z" fill="${p.base}"/>`;
    body = `${plumes}${wings}<ellipse cx="50" cy="56" rx="16" ry="20" fill="${p.base}"/><ellipse cx="46" cy="60" rx="5" ry="8" fill="${SHINE}" opacity="0.5"/><circle cx="50" cy="34" r="12" fill="${p.base}"/><path d="M50 22 L45 8 L55 14 Z" fill="${p.dark}"/><path d="${eyeMark(54, 32, 2)}" fill="${p.light}"/><path d="${sparkleMark(74, 44, 3.6)}" fill="${p.light}" opacity="0.85"/>`;
  } else if (archetype === 'qilin') {
    const legs = `<ellipse cx="36" cy="82" rx="5" ry="8" fill="${p.dark}"/><ellipse cx="64" cy="82" rx="5" ry="8" fill="${p.dark}"/>`;
    const mane = `<path d="M30 40 Q18 50 28 62" stroke="${p.dark}" stroke-width="4" fill="none" stroke-linecap="round"/><path d="M70 40 Q82 50 72 62" stroke="${p.dark}" stroke-width="4" fill="none" stroke-linecap="round"/>`;
    const horn = `<path d="M50 20 L46 4 L54 4 Z" fill="${p.light}"/>`;
    body = `${legs}<ellipse cx="50" cy="62" rx="24" ry="18" fill="${p.base}"/><ellipse cx="42" cy="68" rx="6" ry="8" fill="${SHINE}" opacity="0.55"/>${mane}<circle cx="50" cy="38" r="16" fill="${p.base}"/>${horn}<path d="${eyeMark(44, 36, 2.2)}" fill="${p.light}"/><path d="${eyeMark(56, 36, 2.2)}" fill="${p.light}"/><path d="${sparkleMark(74, 24, 3.6)}" fill="${p.light}" opacity="0.85"/>`;
  } else if (archetype === 'dragon') {
    body = `<path d="M18 74 C28 42 50 62 46 40 C42 18 66 16 76 30" stroke="${p.base}" stroke-width="13" fill="none" stroke-linecap="round"/>`
      + `<ellipse cx="28" cy="58" rx="3.6" ry="5.4" fill="${SHINE}" opacity="0.55"/><ellipse cx="40" cy="38" rx="3.2" ry="4.6" fill="${SHINE}" opacity="0.55"/>`
      + `<path d="M40 42 L36 28 L46 36 Z" fill="${p.dark}"/><path d="M58 24 L56 10 L66 20 Z" fill="${p.dark}"/>`
      + `<path d="M74 26 L86 19 L82 32 Z" fill="${p.dark}"/><path d="M72 22 L80 11 L78 24 Z" fill="${p.dark}"/>`
      + `<path d="M70 32 L78 34 L70 40 Z" fill="${SHINE}"/>`
      + `<path d="${eyeMark(74, 27, 2.4)}" fill="${p.light}"/><path d="${sparkleMark(58, 46, 3.6)}" fill="${p.light}" opacity="0.85"/>`
      + `<path d="M10 80 L18 72 L20 84 Z" fill="${p.base}"/>`;
  } else if (archetype === 'ninetail') {
    let tails = '';
    for (let i = 0; i < 5; i++) { const a = -50 + i * 25; tails += `<path d="M56 62 q26 6 30 ${18 + i * 2}" stroke="${i % 2 === 0 ? p.base : p.dark}" stroke-width="5" fill="none" stroke-linecap="round" transform="rotate(${a} 56 62)"/>`; }
    body = `${tails}<ellipse cx="46" cy="62" rx="18" ry="16" fill="${p.base}"/><ellipse cx="40" cy="68" rx="5" ry="7" fill="${SHINE}" opacity="0.5"/><path d="M30 34 L23 14 L38 30 Z" fill="${p.dark}"/><path d="M62 34 L71 14 L56 30 Z" fill="${p.dark}"/><circle cx="46" cy="36" r="13" fill="${p.base}"/><path d="${eyeMark(40, 34, 2.2)}" fill="${p.light}"/><path d="${eyeMark(52, 34, 2.2)}" fill="${p.light}"/><path d="${sparkleMark(70, 24, 3.6)}" fill="${p.light}" opacity="0.85"/>`;
  } else if (archetype === 'crane') {
    const legs = `<path d="M50 84 L48 97 M62 84 L64 97" stroke="${p.dark}" stroke-width="2.4" stroke-linecap="round"/>`;
    const body2 = `<ellipse cx="56" cy="72" rx="20" ry="14" fill="${p.base}"/><ellipse cx="50" cy="76" rx="6" ry="8" fill="${SHINE}" opacity="0.5"/>`;
    const wing = `<path d="M40 66 Q12 54 10 76 Q34 84 48 72 Z" fill="${p.base}"/>`;
    const neck = `<path d="M56 70 Q38 50 54 28" stroke="${p.base}" stroke-width="9" fill="none" stroke-linecap="round"/>`;
    body = `${legs}${body2}${wing}${neck}<circle cx="54" cy="25" r="7" fill="${p.base}"/><path d="${eyeMark(57, 18, 2.6)}" fill="${p.light}"/><circle cx="52" cy="20" r="1.6" fill="${INK}"/><path d="${sparkleMark(76, 40, 3.6)}" fill="${p.light}" opacity="0.85"/>`;
  }
  return `<g transform="rotate(${rot} 50 55)">${body}</g>`;
}

/** SVG hoàn chỉnh (viewBox 0 0 100 100) cho 1 loài — dùng render trực tiếp qua dangerouslySetInnerHTML. */
export function renderSpeciesArt(params: {
  category: string;
  archetype: string;
  paletteIdx: number;
  name: string;
}): string {
  const { category, archetype, paletteIdx, name } = params;
  let body = '';
  if (category === 'FOREST') body = landSvg(archetype, paletteIdx, name);
  else if (category === 'SEA') body = seaSvg(archetype, paletteIdx, name);
  else if (category === 'PLANT') body = plantSvg(archetype, paletteIdx, name);
  else body = mythicSvg(archetype, paletteIdx, name);

  return `<svg class="sp-anim" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">${body}</svg>`;
}
