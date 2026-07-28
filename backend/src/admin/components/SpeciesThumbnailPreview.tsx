import React from 'react';
import type { EditPropertyProps } from 'adminjs';
import SpeciesThumbnail from './SpeciesThumbnail.js';
import { CARD_FX_CSS } from './species-art.js';

/** Thumbnail loài ở đầu form Sửa — property ẢO (không có cột DB thật, đăng ký qua
 * `properties.thumbnailPreview` ở resource Species trong `admin.module.ts`, KHÔNG bao giờ gửi lên
 * server khi Lưu — chỉ để Dev1002 nhìn thấy ngay hình đang sửa khớp con nào, không cần đoán qua
 * tên/Kiểu dáng/Bảng màu #. Đọc trực tiếp `record.params` hiện tại (kể cả giá trị vừa gõ, chưa lưu)
 * để cập nhật theo thời gian thực khi đổi Danh mục/Kiểu dáng/Bảng màu #/Cấp bậc. */
const SpeciesThumbnailPreview: React.FC<EditPropertyProps> = ({ record }) => {
  const category = String(record.params.category ?? 'FOREST');
  const archetype = String(record.params.archetype ?? '');
  const paletteIdx = Number(record.params.paletteIdx) || 0;
  const name = String(record.params.name ?? '');
  const rarity = String(record.params.rarity ?? 'B');

  return (
    <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 22 }}>
      <style>{CARD_FX_CSS}</style>
      <SpeciesThumbnail category={category} archetype={archetype} paletteIdx={paletteIdx} name={name} rarity={rarity} size={96} />
    </div>
  );
};

export default SpeciesThumbnailPreview;
