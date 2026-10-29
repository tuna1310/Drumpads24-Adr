# ADR-004: Định dạng dữ liệu JSON có version

- **Trạng thái:** Proposed
- **Ngày:** 2026-10-29

## Bối cảnh
Ứng dụng cần định dạng dữ liệu dùng chung giữa Android và iOS cho project, kit 24 pad và pattern sequencer, hỗ trợ mở rộng và tương thích ngược lâu dài.

## Quyết định
1. **Định dạng và Phiên bản:**
   - Dùng JSON làm chuẩn dữ liệu. Mọi file JSON bắt buộc có trường `schemaVersion` (số nguyên dương tăng dần).
2. **Quản lý Schema và Fixture:**
   - Định nghĩa bằng JSON Schema trong `shared-specs/schema/`.
   - Mỗi schema phải có fixture kiểm thử gồm các file JSON hợp lệ và không hợp lệ.
3. **Định danh Sample và Tách biệt luồng:**
   - Sample gắn với pad được trỏ bằng `id` ổn định (UUID hoặc hash nội dung), không dùng tên file hay đường dẫn cục bộ.
   - Parse JSON chỉ diễn ra trên background thread, tuyệt đối KHÔNG chạy trên audio thread (theo [ADR-002](file:///shared-specs/docs/adr/ADR-002-audio-engine.md)).
4. **Cơ chế Migration:** Bắt buộc có hàm migrator khi tăng `schemaVersion`.

## Phương án đã cân nhắc
- **Protocol Buffers (Protobuf) / FlatBuffers:** Bị loại tạm thời. Dù nhị phân gọn và parse nhanh, JSON dễ đọc, dễ kiểm thử fixture và không cần setup compiler phức tạp; việc parse ngoài audio thread đã đáp ứng tốt hiệu năng.

## Hệ quả
- **Lợi ích:** Dữ liệu minh bạch, chuẩn hóa hai nền tảng; an toàn cho dữ liệu người dùng khi cập nhật.
- **Đánh đổi:** Kích thước file lớn hơn dạng nhị phân; cần viết và duy trì bộ migrator qua từng phiên bản.
