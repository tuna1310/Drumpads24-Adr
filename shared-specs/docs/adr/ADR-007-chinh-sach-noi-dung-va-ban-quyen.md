# ADR-007: Chính sách nội dung và bản quyền

- **Trạng thái:** Accepted
- **Ngày:** 2026-10-29

## Bối cảnh
Sản phẩm là ứng dụng finger-drumming độc lập và nguyên bản 100%. Dự án phải triệt tiêu mọi nguy cơ vi phạm quyền sở hữu trí tuệ, nhãn hiệu thương mại, bản quyền âm thanh và mã nguồn trên App Store và Google Play.

## Quyết định
1. **Tính Nguyên bản Thương hiệu và Giao diện:**
   - Tuyệt đối KHÔNG sử dụng tên, logo, màu thương hiệu, icon, font, sample hay giao diện mô phỏng bất kỳ app thương mại nào trên thị trường.
2. **Quy định về Mã nguồn:**
   - Tuyệt đối KHÔNG đọc, phân tích, trích xuất hay sao chép code từ repo bên thứ ba không có giấy phép cho phép hoặc từ app dịch ngược.
3. **Bản quyền Sample Âm thanh:**
   - Mọi sample phải có giấy phép hợp lệ: thuộc phạm vi công cộng (CC0), có bản quyền ghi nguồn (CC-BY), hoặc do đội ngũ tự tạo (in-house).
   - Toàn bộ nguồn gốc, tác giả và giấy phép của từng sample bắt buộc phải được ghi rõ trong tệp `CREDITS.md` tại thư mục gốc.

## Phương án đã cân nhắc
- **Dùng các bộ sample miễn phí trôi nổi trên internet:** Bị loại bỏ hoàn toàn do rủi ro tranh chấp bản quyền và nguy cơ bị gỡ bỏ ứng dụng khỏi chợ ứng dụng.

## Hệ quả
- **Lợi ích:** Đảm bảo an toàn pháp lý tuyệt đối; xây dựng thư viện âm thanh và thương hiệu độc quyền bền vững.
- **Đánh đổi:** Tốn thời gian xác minh pháp lý của từng sample hoặc chi phí tự sản xuất nội dung âm thanh ban đầu.
