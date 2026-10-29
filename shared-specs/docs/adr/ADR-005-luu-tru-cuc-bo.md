# ADR-005: Lưu trữ cục bộ

- **Trạng thái:** Proposed
- **Ngày:** 2026-10-29

## Bối cảnh
Ứng dụng cần lưu metadata (kit, project, pattern, BPM) và dữ liệu nhị phân (tệp âm thanh, waveform cache). Cần phân định vai trò giữa database quan hệ và hệ thống tệp cục bộ để tối ưu hiệu năng truy vấn và tốc độ nạp audio vào RAM.

## Quyết định
1. **Quản lý Metadata bằng Database:**
   - **Android:** Dùng **Room** (SQLite).
   - **iOS:** Dùng **SwiftData** (hoặc CoreData nếu cần hỗ trợ OS cũ).
   - Database chỉ lưu metadata: thông tin kit, cấu hình 24 pad, BPM, `sample_id` và lịch sử project.
2. **Quản lý File Media & Cache:**
   - Tệp audio (WAV/FLAC) và waveform cache lưu trực tiếp thành tệp trên bộ nhớ nội bộ ứng dụng.
   - Tuyệt đối KHÔNG lưu nhị phân audio (BLOB) vào database.
3. **Quy tắc Migration:**
   - Mọi thay đổi schema database bắt buộc có script migration và bài kiểm thử di chuyển (migration test).

## Phương án đã cân nhắc
- **Lưu file audio dạng BLOB trong SQLite:** Bị loại vì làm phình database, chậm truy vấn và cản trở việc nạp audio trực tiếp vào RAM (theo [ADR-002](file:///shared-specs/docs/adr/ADR-002-audio-engine.md)).
- **Dùng Realm hoặc NoSQL nhúng:** Bị loại nhằm tránh phụ thuộc thư viện ngoài; ưu tiên giải pháp chính chủ của từng OS.

## Hệ quả
- **Lợi ích:** Database nhỏ gọn, truy vấn nhanh; đọc file âm thanh vào audio engine với hiệu năng I/O tối đa.
- **Đánh đổi:** Cần quản lý tính nhất quán giữa database và tệp trên đĩa; phải có tác vụ dọn dẹp file âm thanh mồ côi (orphaned files).
