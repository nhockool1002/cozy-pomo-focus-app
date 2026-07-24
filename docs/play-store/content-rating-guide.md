# Hướng dẫn điền Content Rating (Play Console)

> Play Console → App content → Content ratings. Đây là bảng câu hỏi IARC (International Age Rating Coalition) — chỉ điền được qua UI, tài liệu này ghi lại câu trả lời đúng và lý do, để Dev1002 điền nhanh và nhất quán nếu phải làm lại (VD: đổi loại app, cập nhật sau này).

## Thông tin chung

| Câu hỏi | Trả lời |
|---|---|
| Category | Apps (không phải Games — dù CozyPomo có yếu tố game-hoá/thu thập sinh vật, chức năng CHÍNH là công cụ năng suất/Pomodoro, không phải trò chơi thuần tuý. Nếu Google gợi ý xếp vào "Productivity", ưu tiên chọn category đó) |
| Email liên hệ | `nhut.nguyenminh.it@gmail.com` |

## Bảng câu hỏi nội dung (tất cả nhóm câu hỏi IARC) — trả lời **"No" / "None"** cho toàn bộ

CozyPomo không có bất kỳ nội dung nào dưới đây, nên mọi câu hỏi trong các nhóm sau đều trả lời phủ định:

| Nhóm | Trả lời | Vì sao |
|---|---|---|
| Violence (bạo lực, kể cả hoạt hình) | Không có | Không có combat/tấn công — chỉ có hoạt ảnh nở trứng, sinh vật tĩnh |
| Blood | Không có | — |
| Sexual content / Nudity | Không có | — |
| Profanity / Crude humor | Không có | Toàn bộ text đã kiểm duyệt, giọng văn "cozy" nhẹ nhàng |
| Alcohol, Tobacco, Drugs | Không có | — |
| Gambling — **real-money** (cờ bạc thật) | Không có | Không có cơ chế đặt cược tiền thật nào |
| Gambling — **simulated** (mô phỏng, VD roll ngẫu nhiên) | ⚠️ **Có, cần khai báo trung thực** — xem mục dưới |
| Scary/horror content | Không có | — |
| Controlled substances | Không có | — |
| User-generated content / chat với người lạ | Không có | Không có tính năng nhắn tin, không có mạng xã hội trong app |
| Shares location | Không có | — |
| Digital purchases | Không có | Không có in-app purchase bằng tiền thật (xem mục Data Safety) |

## ⚠️ Mục cần khai báo cẩn thận: "Simulated Gambling"

CozyPomo có cơ chế **roll ngẫu nhiên có trọng số** khi ấp trứng (species theo `rarity_weights`/`egg_drop_table`, xem `backend/prisma/seed.ts`) — về bản chất kỹ thuật đây là "loot box"/gacha nhẹ, nên câu hỏi IARC dạng *"Does the app contain simulated gambling (e.g. slot machines) that doesn't award real money or prizes?"* nên trả lời **Yes**, kèm mô tả ngắn nếu form cho phép: *"Người chơi kiếm trứng bằng cách tập trung (Pomodoro), không dùng tiền thật; khi ấp xong, trứng tự roll ra 1 loài sinh vật ngẫu nhiên theo tỉ lệ cố định — không có yếu tố cờ bạc thật, không rút tiền/đổi thưởng thật."*

Trả lời trung thực mục này **không** kéo rating lên mức cao — IARC vẫn xếp phần lớn app dạng "simulated gambling không thật, không phần thưởng thật" vào rating thấp (thường vẫn PEGI 3/Everyone hoặc chỉ nhích lên nhẹ), nhưng khai sai/bỏ sót có thể khiến Google gỡ app sau khi phát hiện qua review thủ công.

## Kết quả rating dự kiến

Với các câu trả lời trên, IARC/Google thường xếp CozyPomo vào mức **thấp nhất theo từng hệ thống rating khu vực**:

| Hệ thống | Rating dự kiến |
|---|---|
| ESRB (Mỹ) | Everyone (E) |
| PEGI (EU) | PEGI 3 |
| USK (Đức) | USK 0 |
| Google Play (toàn cầu) | Everyone / Rated for 3+ |

Kết quả CHÍNH XÁC do hệ thống IARC tự tính sau khi nộp form — bảng trên chỉ là dự đoán dựa trên câu trả lời, không phải cam kết.

## Sau khi nộp

Content rating cần làm lại (resubmit) nếu sau này thêm: chat/nhắn tin giữa người chơi, quảng cáo nội dung người lớn, hoặc bất kỳ cơ chế nào ở bảng trên chuyển từ "Không có" sang "Có".
