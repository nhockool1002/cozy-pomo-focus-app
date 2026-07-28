import React, { useState } from 'react';
import { useTranslation } from 'adminjs';
import type { EditPropertyProps } from 'adminjs';

const HEX_RE = /^#([0-9a-f]{6})$/i;

/** Ô "Mã màu" (EggType.colorHex) hiện thêm color picker `<input type="color">` cạnh ô text, thay
 * vì chỉ gõ tay mã hex — Dev1002 yêu cầu. Override CHỈ component `edit` của riêng property này
 * (`admin.module.ts`: `properties.colorHex.components.edit`) nên tự vẽ label/khung, AdminJS không
 * bọc sẵn như property mặc định — dùng `useTranslation().translateProperty` để lấy đúng nhãn tiếng
 * Việt ("Mã màu", khai báo ở `admin-i18n.ts`) thay vì `property.label` thô (khoá field DB gốc,
 * AdminJS chỉ tự dịch khi qua `PropertyLabel` mặc định mà component tự vẽ này đã thay thế). Cả 2
 * input (picker + text) cùng ghi vào 1 giá trị, đồng bộ 2 chiều — gõ tay 1 mã hex hợp lệ cũng tự
 * cập nhật màu hiện trên picker.
 */
const ColorHexEdit: React.FC<EditPropertyProps> = ({ property, record, resource, onChange }) => {
  const { translateProperty } = useTranslation();
  const initial = String(record.params[property.path] ?? '#A8D08D');
  const [value, setValue] = useState(initial);

  const commit = (next: string) => onChange(property.path, next);

  return (
    <div style={{ marginBottom: 22 }}>
      <label style={{ display: 'block', fontSize: 12.5, fontWeight: 600, color: '#6D594E', marginBottom: 6 }}>
        {translateProperty(property.path, resource.id)}
        {property.isRequired ? ' *' : ''}
      </label>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        <input
          type="color"
          value={HEX_RE.test(value) ? value : '#A8D08D'}
          onChange={(e) => {
            setValue(e.target.value);
            commit(e.target.value);
          }}
          style={{ width: 44, height: 38, padding: 0, border: '1px solid rgba(109,89,78,0.16)', borderRadius: 8, cursor: 'pointer', background: 'none' }}
        />
        <input
          type="text"
          value={value}
          onChange={(e) => setValue(e.target.value)}
          onBlur={() => commit(value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') commit(value);
          }}
          placeholder="#A8D08D"
          style={{ flex: 1, maxWidth: 160, padding: '8px 10px', borderRadius: 8, border: '1px solid rgba(109,89,78,0.16)', fontSize: 13, fontFamily: 'monospace' }}
        />
      </div>
    </div>
  );
};

export default ColorHexEdit;
