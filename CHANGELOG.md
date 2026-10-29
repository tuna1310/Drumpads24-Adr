# Changelog

Tất cả các thay đổi đáng chú ý của dự án sẽ được ghi nhận tại đây theo định dạng [Keep a Changelog](https://keepachangelog.com/en/1.0.0/) và tuân thủ [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [M1 - Milestone 1: 24-Pad Core Engine & Multi-Touch] - 2026-10-29

### Đã thêm (Added)
- **Tài liệu Kiến trúc (ADR):**
  - Hoàn thiện bộ ADR từ ADR-000 đến ADR-008.
  - Cập nhật trạng thái `Accepted` cho ADR-001 (Native 100%), ADR-002 (Oboe C++ & AVAudioEngine, ngưỡng AudioUnit), ADR-003 (PPQ 960, 64-bit int), ADR-004 (JSON schema có version), ADR-005 (Lưu trữ Room & SwiftData, audio trên disk), ADR-006 (MVVM & @Observable, Sequencer pure domain) và ADR-007 (Bản quyền nguyên bản 100%).
  - Ghi nhận `BACKLOG.md` cho các quyết định kiến trúc trong tương lai (Audio Export, DSP chain, Hardware MIDI).
- **Bộ âm thanh Starter 24 Pad:**
  - 24 tệp âm thanh WAV (16-bit 44.1kHz PCM mono) được tổng hợp thuật toán in-house tại `shared-specs/sample-packs/starter/samples/`.
  - Cấu hình kit chuẩn `kit.json`, tài liệu cấp phép `LICENSE` (CC0 1.0) và nguồn gốc chi tiết trong `CREDITS.md`.
- **Nền tảng Android (`drumpads-android`):**
  - Lưới 6x4 = 24 pad bằng Jetpack Compose với 4 nhóm màu chuẩn: Drum (Đỏ), Bass (Tím), Melody (Xanh mòng két), FX (Cam).
  - Multi-touch mượt mà cho phép chạm đồng thời nhiều pad với visual feedback tức thì.
  - C++ Audio Engine với thư viện Oboe qua JNI, quản lý Voice Pool cứng 32 voice với chính sách Voice Stealing (tái sử dụng voice cũ nhất khi đầy).
  - Hàng đợi không khóa (SPSC Lock-Free Queue) để nhận NoteOn từ UI sang audio thread.
  - Cơ chế rung haptic tùy chọn bật/tắt trong màn hình Cài đặt.
  - Chế độ Stress Test tự động kích hoạt 32 voice liên tục trong 60 giây và đếm xrun.
  - Bộ kiểm thử đơn vị: `VoicePoolTest`, `PadMappingTest`, `EmptyFixtureTest`.
- **Nền tảng iOS (`drumpads-ios`):**
  - Lưới 24 pad tương tự bằng SwiftUI với `@Observable` Store.
  - Audio Engine thời gian thực với `AVAudioEngine` và `AVAudioSourceNode`, Voice Pool 32 voice và lock-free queue.
  - Haptic feedback qua `UIImpactFeedbackGenerator`.
  - Chế độ Stress Test 32 voice trong 60 giây.
  - Bộ kiểm thử đơn vị XCTest: `VoicePoolTests`, `PadMappingTests`, `EmptyFixtureTests`.
- **Đo lường & Tiêu chuẩn:**
  - Tài liệu đo độ trễ và kiểm thử áp lực `shared-specs/benchmarks/m1-latency.md` kèm hướng dẫn đo touch-to-sound bằng camera 240 fps và loopback.
  - Fixture rỗng có cấu trúc `shared-specs/fixtures/test-vectors/sequencer-test-vectors.empty.json` chuẩn bị cho Sequencer ở M4.
