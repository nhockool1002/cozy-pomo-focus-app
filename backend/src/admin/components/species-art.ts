/**
 * Bộ sinh hình SVG cho từng loài — cùng thuật toán với Creature Atlas artifact
 * (archetype + palette + seed tên riêng). Dùng lại ở cả trang List (thumbnail nhỏ)
 * và Show (ảnh lớn) của resource Species trong AdminJS.
 */

export const PALETTE = [
  { base: '#E2965F', dark: '#B9713D', light: '#F6D9BB' },
  { base: '#F0D98C', dark: '#C9A94A', light: '#FBF0D0' },
  { base: '#9CB380', dark: '#6E8455', light: '#DCE6CC' },
  { base: '#9AC0D9', dark: '#5E92AE', light: '#DCEBF3' },
  { base: '#E7A8B0', dark: '#C06E7C', light: '#F8E0E3' },
  { base: '#B58BC4', dark: '#875B9C', light: '#E9DAF0' },
  { base: '#7C9A5A', dark: '#516B37', light: '#CFE0BC' },
  { base: '#E8876B', dark: '#C25A3D', light: '#F8D5C8' },
  { base: '#D9C29A', dark: '#AC8F5C', light: '#F1E7D2' },
  { base: '#6FB6A8', dark: '#3F8778', light: '#CDE9E2' },
  { base: '#C9607A', dark: '#9A3E55', light: '#F0C7D2' },
  { base: '#7E8FB0', dark: '#556487', light: '#D6DCEA' },
  { base: '#E3B04B', dark: '#B3831F', light: '#F7E2AE' },
  { base: '#8FCDB0', dark: '#5A9C80', light: '#D3EEE0' },
];
const LEAF = { base: '#8FB36B', dark: '#5F7F45' };
const INK = '#6D594E';
const MOUND = '#D9C29A';
const GOLD = '#F4D160';

export const RARITY_COLORS: Record<string, string> = {
  B: '#B7A896',
  A: '#A8D08D',
  S: '#F4D160',
  SS: '#E76F51',
  SSR: GOLD,
};

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
const rndFor = (seed: string) => mulberry32(hashStr(seed));
const n1 = (v: number) => v.toFixed(1);

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
  if (o.ear === 'pointy') ear = `<path d="M32 30 L26 10 L40 26 Z" fill="${p.dark}"/><path d="M68 30 L74 10 L60 26 Z" fill="${p.dark}"/>`;
  else if (o.ear === 'round') ear = `<circle cx="34" cy="22" r="8" fill="${p.dark}"/><circle cx="66" cy="22" r="8" fill="${p.dark}"/>`;
  else if (o.ear === 'long') ear = `<ellipse cx="38" cy="10" rx="6" ry="16" fill="${p.dark}"/><ellipse cx="62" cy="10" rx="6" ry="16" fill="${p.dark}"/>`;
  else if (o.ear === 'tiny') ear = `<circle cx="36" cy="20" r="4" fill="${p.dark}"/><circle cx="64" cy="20" r="4" fill="${p.dark}"/>`;
  else if (o.ear === 'tuft') ear = `<circle cx="34" cy="22" r="7" fill="${p.dark}"/><circle cx="66" cy="22" r="7" fill="${p.dark}"/>`;
  if (o.tail === 'fluffy') tail = `<circle cx="76" cy="58" r="13" fill="${p.base}"/><circle cx="81" cy="49" r="9" fill="${p.light}"/>`;
  else if (o.tail === 'curl') tail = `<path d="M74 66 Q94 66 90 46 Q88 32 76 40" stroke="${p.base}" stroke-width="9" fill="none" stroke-linecap="round"/>`;
  else if (o.tail === 'stub') tail = `<circle cx="75" cy="64" r="7" fill="${p.base}"/>`;
  else if (o.tail === 'ringed') tail = `<rect x="72" y="46" width="10" height="28" rx="5" fill="${p.base}"/><rect x="72" y="58" width="10" height="4" fill="${p.dark}"/>`;
  if (o.snout === 'fox') snout = `<path d="M50 40 L42 48 L58 48 Z" fill="${p.light}"/>`;
  else if (o.snout === 'bear') snout = `<ellipse cx="50" cy="42" rx="9" ry="7" fill="${p.light}"/>`;
  else if (o.snout === 'beak') snout = `<path d="M50 38 L41 44 L50 46 Z" fill="${p.dark}"/>`;
  if (o.pattern === 'spot') pat = `<circle cx="40" cy="60" r="3" fill="${p.dark}" opacity="0.5"/><circle cx="58" cy="66" r="2.4" fill="${p.dark}" opacity="0.5"/>`;
  else if (o.pattern === 'stripe') pat = `<path d="M28 56 Q50 62 72 56" stroke="${p.dark}" stroke-width="3" fill="none" opacity="0.45"/>`;
  else if (o.pattern === 'mask') pat = `<path d="M34 32 Q50 40 66 32" stroke="${p.dark}" stroke-width="7" fill="none" opacity="0.85" stroke-linecap="round"/>`;
  else if (o.pattern === 'spike') pat = `<path d="M34 46 L30 38 L38 44 Z" fill="${p.dark}"/><path d="M56 40 L58 30 L62 40 Z" fill="${p.dark}"/>`;
  else if (o.pattern === 'wing') pat = `<ellipse cx="34" cy="62" rx="8" ry="12" fill="${p.dark}" opacity="0.45" transform="rotate(-20 34 62)"/>`;
  if (o.extra === 'antler') extra = `<path d="M42 16 L38 4 M42 16 L46 6" stroke="${p.dark}" stroke-width="2.4" fill="none" stroke-linecap="round"/><path d="M58 16 L62 4 M58 16 L54 6" stroke="${p.dark}" stroke-width="2.4" fill="none" stroke-linecap="round"/>`;
  return `<g transform="rotate(${rot} 50 50)">${tail}<ellipse cx="50" cy="62" rx="26" ry="22" fill="${p.base}"/>${ear}<circle cx="50" cy="35" r="19" fill="${p.base}"/>${snout}<circle cx="43" cy="34" r="2.6" fill="${INK}"/><circle cx="57" cy="34" r="2.6" fill="${INK}"/>${pat}${extra}</g>`;
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
    if (o.shellType === 'dome') body = `<circle cx="24" cy="54" r="10" fill="${p.base}"/><ellipse cx="54" cy="56" rx="26" ry="19" fill="${p.dark}"/><circle cx="54" cy="52" r="9" fill="${p.light}" opacity="0.55"/><circle cx="24" cy="52" r="2.2" fill="${INK}"/>`;
    else if (o.shellType === 'claws') body = `<ellipse cx="50" cy="58" rx="27" ry="15" fill="${p.base}"/><circle cx="22" cy="40" r="8" fill="${p.dark}"/><circle cx="78" cy="40" r="8" fill="${p.dark}"/><circle cx="42" cy="54" r="2.2" fill="${INK}"/><circle cx="58" cy="54" r="2.2" fill="${INK}"/>`;
    else body = `<circle cx="46" cy="52" r="17" fill="${p.base}"/><circle cx="46" cy="52" r="12" fill="${p.dark}" opacity="0.5"/><ellipse cx="68" cy="66" rx="14" ry="9" fill="${p.base}"/><circle cx="78" cy="62" r="2.2" fill="${INK}"/>`;
  } else if (o.shape === 'fish') {
    body = `<path d="M78 50 L94 40 L90 50 L94 60 L78 50 Z" fill="${p.dark}"/><path d="M28 50 C28 30 72 30 76 50 C72 70 28 70 28 50 Z" fill="${p.base}"/><circle cx="42" cy="48" r="2.6" fill="${INK}"/>`;
  } else if (o.shape === 'star') {
    body = `<path d="${starPoints(50, 54, 30, 13, 5, -Math.PI / 2)}" fill="${p.base}"/><circle cx="44" cy="48" r="2.2" fill="${INK}"/><circle cx="56" cy="48" r="2.2" fill="${INK}"/>`;
  } else if (o.shape === 'blob') {
    const fin = o.fin === 'dorsal' ? `<path d="M58 34 L66 16 L68 36 Z" fill="${p.dark}"/>` : `<ellipse cx="24" cy="58" rx="8" ry="4" fill="${p.dark}"/><ellipse cx="76" cy="58" rx="8" ry="4" fill="${p.dark}"/>`;
    body = `<path d="M80 50 L94 42 L90 58 L94 68 L80 60 Z" fill="${p.dark}"/><ellipse cx="48" cy="55" rx="30" ry="21" fill="${p.base}"/>${fin}<circle cx="66" cy="48" r="2.4" fill="${INK}"/>`;
  } else if (o.shape === 'tentacle') {
    let legs = '';
    const n = o.legs ?? 6;
    for (let i = 0; i < n; i++) {
      const x = 30 + i * (40 / (n - 1));
      const sway = i % 2 === 0 ? 6 : -6;
      legs += `<path d="M${n1(x)} 60 Q${n1(x + sway)} 74 ${n1(x)} 88" stroke="${p.dark}" stroke-width="3.2" fill="none" stroke-linecap="round"/>`;
    }
    const head = o.dome ? `<path d="M24 58 A26 26 0 0 1 76 58 Z" fill="${p.base}"/>` : `<circle cx="50" cy="48" r="24" fill="${p.base}"/>`;
    body = `${legs}${head}<circle cx="42" cy="48" r="2.4" fill="${INK}"/><circle cx="58" cy="48" r="2.4" fill="${INK}"/>`;
  } else if (o.shape === 'seahorse') {
    body = `<path d="M46 82 C30 82 30 66 42 60 C54 54 40 48 42 38 C44 28 58 24 62 32" stroke="${p.base}" stroke-width="13" fill="none" stroke-linecap="round"/><circle cx="58" cy="30" r="2.4" fill="${INK}"/>`;
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
    inner += stem + leaves + petals + `<circle cx="50" cy="30" r="7" fill="${p.light}"/>`;
  } else if (archetype === 'flowerStar') {
    inner += stem + leaves + `<path d="${starPoints(50, 30, 15, 7, 6, 0)}" fill="${p.base}"/><circle cx="50" cy="30" r="4" fill="${p.light}"/>`;
  } else if (archetype === 'mushroom') {
    inner += `<rect x="44" y="52" width="12" height="32" rx="5" fill="#F1E6D2" stroke="${p.dark}" stroke-width="1.5"/><path d="M24 52 A26 20 0 0 1 76 52 Z" fill="${p.base}"/><circle cx="38" cy="42" r="3" fill="${p.light}"/><circle cx="58" cy="38" r="3.4" fill="${p.light}"/>`;
  } else if (archetype === 'fern') {
    let fronds = '';
    for (let i = 0; i < 3; i++) { const dx = (i - 1) * 16; fronds += `<path d="M50 84 Q${50 + dx} 50 ${50 + dx * 1.4} 24" stroke="${LEAF.base}" stroke-width="4" fill="none" stroke-linecap="round"/>`; }
    inner += fronds;
  } else if (archetype === 'succulent') {
    let petals = '';
    for (let i = 0; i < 7; i++) { const a = i * ((2 * Math.PI) / 7); petals += `<ellipse cx="${n1(50 + 15 * Math.cos(a))}" cy="${n1(66 + 15 * Math.sin(a) * 0.6)}" rx="9" ry="14" fill="${p.base}"/>`; }
    inner += petals + `<circle cx="50" cy="66" r="7" fill="${p.light}"/>`;
  } else if (archetype === 'cactus') {
    inner += `<rect x="38" y="30" width="24" height="54" rx="12" fill="${p.base}"/><rect x="20" y="46" width="16" height="11" rx="6" fill="${p.base}"/><rect x="64" y="40" width="16" height="11" rx="6" fill="${p.base}"/><circle cx="50" cy="26" r="6" fill="${p.light}"/>`;
  } else if (archetype === 'berry') {
    inner += `<ellipse cx="40" cy="60" rx="16" ry="14" fill="${LEAF.base}"/><ellipse cx="62" cy="56" rx="15" ry="13" fill="${LEAF.base}"/><circle cx="38" cy="58" r="3.4" fill="${p.base}"/><circle cx="52" cy="66" r="3.4" fill="${p.base}"/><circle cx="62" cy="54" r="3.4" fill="${p.base}"/>`;
  } else if (archetype === 'bamboo') {
    let stalks = '';
    for (let i = 0; i < 3; i++) { const x = 38 + i * 12; stalks += `<rect x="${x - 4}" y="20" width="8" height="64" rx="4" fill="${LEAF.base}"/><rect x="${x - 4}" y="36" width="8" height="3" fill="${LEAF.dark}"/><rect x="${x - 4}" y="54" width="8" height="3" fill="${LEAF.dark}"/>`; }
    inner += stalks;
  } else if (archetype === 'vine') {
    inner += `<path d="M28 82 Q50 60 34 44 Q20 30 40 20" stroke="${LEAF.base}" stroke-width="4" fill="none" stroke-linecap="round"/><circle cx="40" cy="20" r="6" fill="${p.base}"/>`;
  } else if (archetype === 'tree') {
    inner += `<rect x="45" y="50" width="10" height="34" rx="4" fill="#B98A5D"/><circle cx="40" cy="38" r="16" fill="${p.base}"/><circle cx="60" cy="36" r="14" fill="${p.base}"/><circle cx="50" cy="26" r="15" fill="${p.light}"/>`;
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
    body = `${plumes}${wings}<ellipse cx="50" cy="56" rx="16" ry="20" fill="${p.base}"/><circle cx="50" cy="34" r="12" fill="${p.base}"/><path d="M50 22 L45 8 L55 14 Z" fill="${p.dark}"/><circle cx="54" cy="32" r="2" fill="${INK}"/>`;
  } else if (archetype === 'qilin') {
    const legs = `<ellipse cx="36" cy="82" rx="5" ry="8" fill="${p.dark}"/><ellipse cx="64" cy="82" rx="5" ry="8" fill="${p.dark}"/>`;
    const mane = `<path d="M30 40 Q18 50 28 62" stroke="${p.dark}" stroke-width="4" fill="none" stroke-linecap="round"/><path d="M70 40 Q82 50 72 62" stroke="${p.dark}" stroke-width="4" fill="none" stroke-linecap="round"/>`;
    const horn = `<path d="M50 20 L46 4 L54 4 Z" fill="${p.light}"/>`;
    body = `${legs}<ellipse cx="50" cy="62" rx="24" ry="18" fill="${p.base}"/>${mane}<circle cx="50" cy="38" r="16" fill="${p.base}"/>${horn}<circle cx="44" cy="36" r="2.2" fill="${INK}"/><circle cx="56" cy="36" r="2.2" fill="${INK}"/>`;
  } else if (archetype === 'dragon') {
    body = `<path d="M18 74 C28 42 50 62 46 40 C42 18 66 16 76 30" stroke="${p.base}" stroke-width="14" fill="none" stroke-linecap="round"/><path d="M76 30 L86 25 L84 34 Z" fill="${p.dark}"/><circle cx="14" cy="76" r="3.6" fill="${p.light}"/><circle cx="74" cy="26" r="2.2" fill="${INK}"/>`;
  } else if (archetype === 'ninetail') {
    let tails = '';
    for (let i = 0; i < 5; i++) { const a = -50 + i * 25; tails += `<path d="M56 62 q26 6 30 ${18 + i * 2}" stroke="${i % 2 === 0 ? p.base : p.dark}" stroke-width="5" fill="none" stroke-linecap="round" transform="rotate(${a} 56 62)"/>`; }
    body = `${tails}<ellipse cx="46" cy="62" rx="18" ry="16" fill="${p.base}"/><path d="M30 34 L23 14 L38 30 Z" fill="${p.dark}"/><path d="M62 34 L71 14 L56 30 Z" fill="${p.dark}"/><circle cx="46" cy="36" r="13" fill="${p.base}"/><circle cx="40" cy="34" r="2.2" fill="${INK}"/><circle cx="52" cy="34" r="2.2" fill="${INK}"/>`;
  } else if (archetype === 'crane') {
    const legs = `<path d="M50 84 L48 97 M62 84 L64 97" stroke="${p.dark}" stroke-width="2.4" stroke-linecap="round"/>`;
    const body2 = `<ellipse cx="56" cy="72" rx="20" ry="14" fill="${p.base}"/>`;
    const wing = `<path d="M40 66 Q12 54 10 76 Q34 84 48 72 Z" fill="${p.base}"/>`;
    const neck = `<path d="M56 70 Q38 50 54 28" stroke="${p.base}" stroke-width="9" fill="none" stroke-linecap="round"/>`;
    body = `${legs}${body2}${wing}${neck}<circle cx="54" cy="25" r="7" fill="${p.base}"/><circle cx="57" cy="18" r="2.6" fill="#E76F51"/><circle cx="52" cy="20" r="2" fill="${INK}"/>`;
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

  let aura = '';
  if (category === 'MYTHIC') {
    aura = `<circle cx="50" cy="55" r="46" fill="none" stroke="${GOLD}" stroke-width="2" opacity="0.5"/><circle cx="50" cy="55" r="38" fill="none" stroke="${GOLD}" stroke-width="1.2" opacity="0.35"/>`;
  }
  return `<svg viewBox="0 0 100 100" width="100%" height="100%" xmlns="http://www.w3.org/2000/svg">${aura}${body}</svg>`;
}
