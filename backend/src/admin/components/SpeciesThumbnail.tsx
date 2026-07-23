import React from 'react';
import { renderSpeciesArt, renderAura } from './species-art.js';

type Props = {
  category: string;
  archetype: string;
  paletteIdx: number;
  name: string;
  rarity: string;
  size?: number;
};

/**
 * Ảnh loài sinh bằng SVG (đồng bộ với Creature Atlas / wireframe), có vầng hào quang
 * theo cấp bậc phía sau và hiệu ứng lơ lửng nhẹ — y hệt thẻ loài trong wireframe.
 * Trang cha cần bơm sẵn `CARD_FX_CSS` (SpeciesList/SpeciesShow làm việc này).
 */
const SpeciesThumbnail: React.FC<Props> = ({ category, archetype, paletteIdx, name, rarity, size = 64 }) => {
  const icon = renderSpeciesArt({ category, archetype, paletteIdx, name });
  const aura = renderAura(rarity, name);
  return (
    <div
      className="sp-icon-wrap"
      style={{ width: size, height: size, flexShrink: 0 }}
      // eslint-disable-next-line react/no-danger
      dangerouslySetInnerHTML={{ __html: aura + icon }}
    />
  );
};

export default SpeciesThumbnail;
