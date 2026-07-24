# CozyPomo — Chuẩn bị phát hành Play Store (Nhóm E)

Thư mục này gom mọi nội dung/hình ảnh cần cho Play Console — hạ tầng release (keystore, CI/CD, ký app) đã xong từ trước (xem `plan.md` T-053/T-054), phần còn lại chỉ là nội dung/thủ tục.

## Đã chuẩn bị trong thư mục này

| Việc | File | Ghi chú |
|---|---|---|
| Feature graphic | [`feature-graphic-1024x500.png`](feature-graphic-1024x500.png) | Đúng 1024×500, PNG 24-bit không alpha (Google yêu cầu) |
| Ảnh chụp màn hình | [`screenshots/`](screenshots/) | 5 ảnh, 1080×2130 (tỉ lệ ~1.97:1, trong giới hạn max/min ≤ 2 của Google), chụp thật từ emulator |
| Icon độ phân giải cao | [`../branding/exports/icon-512.png`](../branding/exports/icon-512.png) | Đã có sẵn từ trước (T-021), 512×512 |
| Chính sách bảo mật | [`../../backend/legal/privacy-policy.html`](../../backend/legal/privacy-policy.html) | Host live tại `https://cozyapi.nhutnm.id.vn/legal/privacy-policy.html` sau khi release backend (xem bên dưới) |
| Hướng dẫn điền Data Safety form | [`data-safety-form-guide.md`](data-safety-form-guide.md) | Form chỉ điền qua UI Play Console, đây là đáp án tham khảo khớp với Privacy Policy |
| Hướng dẫn điền Content Rating | [`content-rating-guide.md`](content-rating-guide.md) | Bảng câu hỏi IARC + lưu ý riêng về mục "simulated gambling" (cơ chế roll trứng) |

## Quy cách ảnh đã dùng (đúng yêu cầu kỹ thuật của Google Play, 2026)

- **Feature graphic**: chính xác 1024 × 500 px, PNG không có kênh alpha (`-alpha off` khi xuất — đã verify bằng `identify -verbose` cho `Type: TrueColor`, không phải `TrueColorAlpha`).
- **Ảnh chụp màn hình điện thoại**: JPEG/PNG 24-bit, cạnh ngắn nhất ≥ 320px, cạnh dài nhất ≤ 3840px, và **tỉ lệ cạnh dài:cạnh ngắn không vượt quá 2:1**. Ảnh chụp thật từ emulator là 1080×2340 (tỉ lệ 2.167:1) — **vi phạm** giới hạn 2:1 vì màn hình hiện đại quá dài; đã cắt bớt thanh điều hướng hệ thống ở đáy (không phải nội dung app) còn 1080×2130 (tỉ lệ 1.972:1, trong giới hạn) bằng `magick <ảnh gốc> -crop 1080x2130+0+0 <ảnh ra>`.
- **Icon độ phân giải cao**: 512×512 — đã có sẵn từ T-021, không cần làm lại.

## Việc còn cần làm thủ công trong Play Console (không tự động hoá được)

1. **Release backend** để `/legal/privacy-policy.html` thật sự online (đã viết code — `backend/src/main.ts` mount `/legal`, `Dockerfile` đã `COPY legal`, nhưng **chưa release** lên production, cần tag `backend-v...` mới như các lần trước).
2. Dán URL Privacy Policy vào Play Console → App content → Privacy policy.
3. Điền Data Safety form theo [`data-safety-form-guide.md`](data-safety-form-guide.md).
4. Điền Content Rating theo [`content-rating-guide.md`](content-rating-guide.md).
5. Upload feature graphic + 5 ảnh chụp màn hình + mô tả ngắn/dài (có thể tái dùng đoạn mô tả trong `ui/about/AboutScreen.kt` làm mô tả ngắn) vào Play Console → Store listing.
6. Tạo app trong Play Console (nếu chưa), upload `.aab` từ GitHub Release mới nhất (`app-v1.20260724.002` hoặc bản mới hơn), chọn track (khuyến nghị Internal testing trước khi Production).

## Mô tả ứng dụng (gợi ý dùng cho Store listing)

**Mô tả ngắn (tối đa 80 ký tự):**
> Ấp trứng, nuôi cả khu rừng của riêng bạn khi bạn tập trung — Pomodoro dễ thương.

**Mô tả dài** (tái dùng từ [`AboutScreen.kt`](../../app/app/src/main/java/com/cozypomo/app/ui/about/AboutScreen.kt), có thể mở rộng thêm):
> CozyPomo là ứng dụng Pomodoro nhẹ nhàng — mỗi phiên tập trung là một bước ấp nở, giúp bạn dần mở khoá và nuôi lớn một khu rừng sinh vật của riêng mình. Tập trung càng đều, khu rừng càng thêm nhiều loài mới để khám phá.
>
> - Đếm giờ Pomodoro chính xác, hoạt động cả khi khoá màn hình
> - Chọn ấp 1 trong các loại trứng đang sở hữu mỗi phiên, hoặc không ấp trứng nào
> - 175 loài sinh vật với 5 cấp độ hiếm (B/A/S/SS/SSR), mỗi loài một câu chuyện riêng
> - Cửa hàng: mua trứng mới, đổi bình ấp, đổi nhạc nền bằng Xu Lá hoặc Giờ tích luỹ kiếm được
> - Thống kê streak và tổng thời gian tập trung theo ngày
> - Không quảng cáo, không theo dõi bên thứ ba, không giao dịch tiền thật
