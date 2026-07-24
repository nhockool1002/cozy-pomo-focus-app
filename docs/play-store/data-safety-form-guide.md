# Hướng dẫn điền Data Safety form (Play Console)

> Đây là tài liệu tham khảo để Dev1002 tự điền form "Data safety" trong Play Console (Play Console → App content → Data safety) — form này chỉ điền được qua UI, không có API để tự động hoá. Câu trả lời dưới đây khớp với nội dung thật của [`privacy-policy.html`](../../backend/legal/privacy-policy.html) — đừng trả lời khác đi giữa 2 nơi, Google đối chiếu và có thể từ chối app nếu lệch nhau.

## 1. "Does your app collect or share any of the required user data types?"

**Có** — app có thu thập dữ liệu (tài khoản + tiến trình chơi), nhưng **không chia sẻ** với bên thứ ba.

## 2. Data types — điền đúng theo bảng sau

| Data type (Play Console) | Thu thập? | Chia sẻ với bên thứ 3? | Bắt buộc? | Mục đích khai báo |
|---|---|---|---|---|
| **Personal info → Email address** | ✅ Có | ❌ Không | ✅ Có (để tạo tài khoản) | Account management |
| **Personal info → Name / khác** | ❌ Không | — | — | — |
| **Financial info** (mọi mục con) | ❌ Không | — | — | Xu Lá/Giờ tích luỹ là tiền tệ ảo trong game, không map vào "Financial info" của Google (mục này chỉ tính tiền thật/thẻ ngân hàng) |
| **App activity → App interactions** | ✅ Có | ❌ Không | ❌ Không | Lịch sử phiên tập trung, dùng để tính thống kê/streak |
| **App activity → In-app search history** | ❌ Không | — | — | — |
| **App info and performance → Crash logs** | ❌ Không | — | — | Chưa tích hợp Crashlytics/công cụ crash-report nào |
| **App info and performance → Diagnostics** | ❌ Không | — | — | — |
| **Device or other IDs** | ❌ Không | — | — | Không đọc Advertising ID/Android ID |
| **Location** (mọi mục con) | ❌ Không | — | — | — |
| **Photos/Videos/Audio/Files** | ❌ Không | — | — | — |
| **Contacts** | ❌ Không | — | — | — |

## 3. Với mỗi data type đã tick "Có thu thập" — 4 câu hỏi phụ

Áp dụng cho **Email address** và **App interactions** (2 mục duy nhất = Có):

- **Is this data collected, shared, or both?** → *Collected* (không share).
- **Is this data processing ephemeral?** → *No* (lưu lại lâu dài trong DB, không phải xử lý tức thời rồi xoá).
- **Is this data required or optional?** → Email: *Required*. App interactions (lịch sử phiên): *Required* (cốt lõi tính năng, không thể tắt).
- **Why is this user data collected?** → Email: tick **Account management**. App interactions: tick **App functionality**.

## 4. Security practices (mục riêng, không thuộc bảng data type)

| Câu hỏi | Trả lời |
|---|---|
| Is all of the user data collected by your app encrypted in transit? | ✅ Yes — toàn bộ API qua HTTPS/TLS (`https://cozyapi.nhutnm.id.vn`) |
| Do you provide a way for users to request that their data is deleted? | ✅ Yes — qua email liên hệ trong Privacy Policy (mục 6) |

## 5. Data deletion request URL / email

Dùng chính link Privacy Policy (`https://cozyapi.nhutnm.id.vn/legal/privacy-policy.html`, mục "6. Quyền của bạn") hoặc email `nhut.nguyenminh.it@gmail.com` trực tiếp trong ô "Account and data deletion" nếu Play Console tách riêng mục này.

## 6. Vì sao đơn giản hơn dự kiến ban đầu

Bản kế hoạch gốc (`docs/technical-spec.md`) có tính năng Sao lưu & Đồng bộ qua Google Drive (S-07a) — nếu làm, sẽ phải khai thêm mục chia sẻ dữ liệu với Google Drive API (yêu cầu OAuth scope riêng, thêm mục "Shared with 3rd party" phức tạp hơn). Dev1002 đã quyết định **bỏ hẳn tính năng này khỏi phạm vi** (xem `plan.md` T-095) trước khi làm Data Safety form — nên form hiện tại không cần khai bất kỳ mục chia sẻ bên thứ ba nào.

---

**Trước khi nộp:** đối chiếu lại 1 lần với `privacy-policy.html` — nếu sau này thêm tính năng mới có thu thập dữ liệu (VD: Sign in with Google, quảng cáo), phải cập nhật **cả 2 nơi** cùng lúc.
