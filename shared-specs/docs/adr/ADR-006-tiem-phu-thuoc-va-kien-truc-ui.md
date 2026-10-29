# ADR-006: Tiêm phụ thuộc và kiến trúc UI

- **Trạng thái:** Proposed
- **Ngày:** 2026-10-29

## Bối cảnh
Lưới 24 pad và màn hình sequencer đòi hỏi phản hồi giao diện tức thì, đồng thời phải tách biệt với audio engine để dễ bảo trì và kiểm thử. Nếu gắn chặt logic nhịp với phần cứng hay UI, việc kiểm thử sẽ bị phụ thuộc vào thiết bị thật.

## Quyết định
1. **Kiến trúc UI và Tiêm phụ thuộc:**
   - **Android:** Mô hình **MVVM** với Jetpack Compose, **StateFlow** quản lý state, **Hilt** tiêm phụ thuộc (DI).
   - **iOS:** **SwiftUI** kết hợp macro **`@Observable`** và lớp Store; cân nhắc **`swift-dependencies`** để quản lý dependency.
2. **Module Sequencer thuần túy (Pure Domain):**
   - Sequencer là module nghiệp vụ thuần túy, tuyệt đối KHÔNG import framework UI (Compose/SwiftUI) hay audio (Oboe/AVAudioEngine).
   - Audio chỉ đóng vai trò là adapter kết nối qua interface trừu tượng.
3. **Mục tiêu Kiểm thử:**
   - Cho phép chạy unit test toàn bộ sequencer (theo [ADR-003](file:///shared-specs/docs/adr/ADR-003-mo-hinh-thoi-gian-sequencer.md)) trực tiếp trên máy dev (JVM/macOS host) không cần emulator hay thiết bị thật.

## Phương án đã cân nhắc
- **Gắn chặt Sequencer với Audio Engine trong một lớp nguyên khối:** Bị loại vì không thể test độc lập; việc kiểm tra nhịp buộc phải phụ thuộc phần cứng âm thanh thật.

## Hệ quả
- **Lợi ích:** Tách bạch ranh giới nghiệp vụ; test suite chạy nhanh trên CI; UI phản hồi mượt theo state stream.
- **Đánh đổi:** Cần viết thêm các lớp interface và adapter trung gian ban đầu.
