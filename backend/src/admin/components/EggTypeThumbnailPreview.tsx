import React from 'react';
import type { EditPropertyProps } from 'adminjs';
import { CARD_FX_CSS } from './species-art.js';
import { renderEggArt, renderEggAura } from './egg-art.js';

/** Thumbnail trứng ở đầu form Sửa — property ẢO, cùng lý do/cơ chế với `SpeciesThumbnailPreview`
 * (xem comment ở đó). Đọc `colorHex`/`priceCoin`/`name` trực tiếp từ `record.params` hiện tại nên
 * đổi màu qua color picker (`ColorHexEdit`) ở field bên dưới cũng thấy hào quang/màu cập nhật ngay
 * mà không cần Lưu trước.
 */
const EggTypeThumbnailPreview: React.FC<EditPropertyProps> = ({ record }) => {
  const name = String(record.params.name ?? '');
  const colorHex = String(record.params.colorHex ?? '#A8D08D');
  const priceCoin = Number(record.params.priceCoin) || 0;
  const icon = renderEggArt({ colorHex, name });
  const aura = renderEggAura(colorHex, priceCoin, name || 'egg');

  return (
    <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 22 }}>
      <style>{CARD_FX_CSS}</style>
      <div className="sp-icon-wrap" style={{ width: 96, height: 96 }} dangerouslySetInnerHTML={{ __html: aura + icon }} />
    </div>
  );
};

export default EggTypeThumbnailPreview;
