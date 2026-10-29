# ADR-000: Quy ước viết bản ghi quyết định kiến trúc (ADR)

- **Trạng thái:** Accepted
- **Ngày:** 2026-10-29

## Bối cảnh
Dự án ứng dụng 24-pad beat-maker trên Android và iOS cần cơ chế chuẩn hóa để lưu vết các quyết định kỹ thuật về audio, độ trễ và kiến trúc, đảm bảo tính minh bạch và tránh sửa đổi ngầm.

## Quyết định
- **Vị trí và định danh:** Lưu tại `shared-specs/docs/adr/` với định dạng `ADR-XXX-<ten-kebab-case>.md` (`XXX` từ 000).
- **Cấu trúc mỗi file:** Bắt buộc có Tiêu đề & Trạng thái, Ngày, Bối cảnh, Quyết định, Phương án đã cân nhắc, Hệ quả (và Câu hỏi mở nếu có).
- **Trạng thái:** `Proposed`, `Accepted`, `Deferred`, hoặc `Superseded`.
- **Quy tắc bất biến:** ADR đã `Accepted` tuyệt đối không sửa nội dung. Khi có thay đổi kiến trúc, tạo ADR mới thay thế và đánh dấu ADR cũ là `Superseded` kèm liên kết dẫn chứng.
- **Quyết định ngoài danh mục:** Ghi vào `BACKLOG.md`, không tự ý tạo ADR mới ngoài danh sách được giao.

## Phương án đã cân nhắc
- **Lưu trên Wiki/Google Docs hoặc một file chung:** Bị loại vì không gắn liền Git commit, khó truy vết lịch sử và dễ bị chỉnh sửa ngầm thiếu kiểm soát.

## Hệ quả
- **Lợi ích:** Lưu vết lịch sử minh bạch, bảo vệ toàn vẹn các cam kết kiến trúc đã thống nhất.
- **Đánh đổi:** Tốn thêm thời gian viết và review ADR mới khi có điều chỉnh thay vì sửa trực tiếp file cũ.
