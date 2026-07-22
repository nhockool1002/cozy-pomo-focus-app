import React from 'react';
import { renderSpeciesArt, RARITY_COLORS } from './species-art.js';

type Props = {
  category: string;
  archetype: string;
  paletteIdx: number;
  name: string;
  rarity: string;
  size?: number;
};

/** Ảnh loài sinh bằng SVG (đồng bộ với Creature Atlas), kèm viền màu theo cấp bậc. */
const SpeciesThumbnail: React.FC<Props> = ({ category, archetype, paletteIdx, name, rarity, size = 64 }) => {
  const svg = renderSpeciesArt({ category, archetype, paletteIdx, name });
  const ringColor = RARITY_COLORS[rarity] ?? '#B7A896';
  return (
    <div
      style={{
        width: size,
        height: size,
        borderRadius: '50%',
        background: '#FFFBF2',
        border: `2px solid ${ringColor}`,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        overflow: 'hidden',
        flexShrink: 0,
      }}
      // eslint-disable-next-line react/no-danger
      dangerouslySetInnerHTML={{ __html: svg }}
    />
  );
};

export default SpeciesThumbnail;
