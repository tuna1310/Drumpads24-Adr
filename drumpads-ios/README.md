# DrumPads 24 - iOS Native App

Ứng dụng finger-drumming 24 pad native trên iOS, xây dựng theo các quyết định kiến trúc đã duyệt ([ADR-001](file:///shared-specs/docs/adr/ADR-001-chien-luoc-nen-tang.md) đến [ADR-007](file:///shared-specs/docs/adr/ADR-007-chinh-sach-noi-dung-va-ban-quyen.md)).

---

## 1. Công nghệ & Kiến trúc
- **Ngôn ngữ & Giao diện:** Swift 5.9+, SwiftUI với Observation framework (`@Observable`).
- **Audio Engine:** `AVAudioEngine` kết hợp `AVAudioSourceNode` cho luồng âm thanh thời gian thực.
- **Kỷ luật Real-time Audio:**
  - Hàng đợi không khóa (SPSC Lock-Free Queue) để truyền lệnh kích hoạt pad từ UI sang Audio render block.
  - Bộ đệm voice (Voice Pool) giới hạn cứng 32 voice. Khi đầy 32 voice, chính sách Voice Stealing tự động tái sử dụng voice cũ nhất dựa trên timestamp.
  - Tuyệt đối không cấp phát bộ nhớ động (`malloc`/`Array` resizing), không dùng khóa đồng bộ chờ, không I/O đĩa trong audio callback.
- **Haptic Feedback:** Tích hợp `UIImpactFeedbackGenerator` tùy chọn bật/tắt trong Cài đặt.
- **Stress Test Debug Mode:** Tự động kích hoạt 32 voice đồng thời trong 60 giây và ghi nhận số xrun/glitch ra log.

---

## 2. Hướng dẫn Build & Kiểm thử

### Yêu cầu môi trường
- macOS 14 (Sonoma) hoặc mới hơn.
- Xcode 15.0+ hoặc Swift Toolchain 5.9+.

### Lệnh chạy kiểm thử Unit Test (Swift Package Manager)
```bash
cd drumpads-ios
swift test
```
Bộ test bao gồm:
1. `VoicePoolTests`: Cấp phát voice, giới hạn cứng 32 voice, chính sách voice stealing khi đầy, giải phóng voice khi kết thúc buffer.
2. `PadMappingTests`: Kiểm tra 24 pad chia đều 4 nhóm màu (Drum, Bass, Melody, FX), tính duy nhất của sample ID.
3. `EmptyFixtureTests`: Parse và kiểm tra tính hợp lệ của test vector rỗng (`sequencer-test-vectors.empty.json`).

### Chạy trên iOS Simulator hoặc Thiết bị thật
1. Mở thư mục `drumpads-ios` trong Xcode (hoặc mở file package `drumpads-ios/Package.swift`).
2. Chọn target simulator chuẩn: **iPhone 11** hoặc **iPhone SE (3rd generation)**.
3. Nhấn **Run** (Cmd + R).
4. Thao tác trên lưới 24 pad:
   - Chạm vào từng pad hoặc chạm đồng thời nhiều pad (multi-touch) để kiểm tra phát âm thanh lập tức.
   - Bấm nút biểu tượng Cài đặt ở góc trên phải để bật/tắt rung haptic hoặc bật chế độ Stress Test 60s.
