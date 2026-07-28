import React, { useEffect, useState } from 'react';
import { ApiClient } from 'adminjs';
import type { EditPropertyProps } from 'adminjs';
import { CARD_FX_CSS } from './species-art.js';
import { renderEggArt, renderEggAura } from './egg-art.js';
import { renderBoostArt, renderBoostAura, boostTierFor, renderJarArt, renderMusicArt } from './item-art.js';

const api = new ApiClient();

/** Thumbnail vật phẩm ở đầu form Sửa — property ẢO, cùng lý do/cơ chế với
 * `SpeciesThumbnailPreview`/`EggTypeThumbnailPreview` (xem comment ở đó), nhưng riêng danh mục
 * EGG cần biết `colorHex` của Loại trứng liên kết (không có sẵn trên chính record ShopItem) nên
 * phải tự fetch record `EggType` đó qua `ApiClient` — 3 danh mục còn lại (JAR_SKIN/MUSIC/BOOST) vẽ
 * được ngay từ field có sẵn trên `record.params`, không cần gọi API.
 */
const ShopItemThumbnailPreview: React.FC<EditPropertyProps> = ({ record }) => {
  const category = String(record.params.category ?? 'BOOST');
  const name = String(record.params.name ?? '');
  const priceCoin = Number(record.params.priceCoin) || 0;
  // @adminjs/prisma phơi FK quan hệ tuỳ chọn qua path TÊN QUAN HỆ ("eggType"), không phải tên cột
  // ("eggTypeId") — record.params.eggType giữ thẳng UUID (hoặc null), không phải object lồng.
  const eggTypeId = record.params.eggType ? String(record.params.eggType) : null;

  const [eggColorHex, setEggColorHex] = useState('#9CB380');

  useEffect(() => {
    if (category !== 'EGG' || !eggTypeId) return;
    api
      .recordAction({ resourceId: 'EggType', recordId: eggTypeId, actionName: 'show' } as any)
      .then((res) => {
        const colorHex = (res.data as any)?.record?.params?.colorHex;
        if (colorHex) setEggColorHex(String(colorHex));
      })
      .catch(() => undefined);
  }, [category, eggTypeId]);

  let icon = '';
  let aura = '';
  if (category === 'EGG') {
    icon = renderEggArt({ colorHex: eggColorHex, name });
    aura = renderEggAura(eggColorHex, priceCoin, name || 'egg');
  } else if (category === 'BOOST') {
    const boostType = record.params.boostType ? String(record.params.boostType) : null;
    const boostAmount = record.params.boostAmount != null ? Number(record.params.boostAmount) : null;
    const purchasable = record.params.purchasable === true || record.params.purchasable === 'true';
    const tier = boostTierFor(boostType, boostAmount, purchasable);
    icon = renderBoostArt({ boostType, tier, name });
    aura = renderBoostAura(boostType, tier);
  } else if (category === 'JAR_SKIN') {
    icon = renderJarArt(name);
  } else {
    icon = renderMusicArt();
  }

  return (
    <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 22 }}>
      <style>{CARD_FX_CSS}</style>
      <div className="sp-icon-wrap" style={{ width: 96, height: 96 }} dangerouslySetInnerHTML={{ __html: aura + icon }} />
    </div>
  );
};

export default ShopItemThumbnailPreview;
