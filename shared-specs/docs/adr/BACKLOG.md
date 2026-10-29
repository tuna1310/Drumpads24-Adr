# Sổ tồn đọng Quyết định Kiến trúc (ADR Backlog)

Tài liệu này ghi nhận các chủ đề kiến trúc tiềm năng cần được thẩm định và lập thành ADR chính thức trong các giai đoạn phát triển tiếp theo của dự án. 

> **Quy tắc:** Tuyệt đối không tự ý tạo thêm ADR mới vào danh sách chính thức khi chưa có sự thống nhất của kiến trúc sư trưởng. Toàn bộ các đề xuất mới phải được ghi nhận tại đây trước khi xem xét đưa vào quy trình thẩm định.

---

## Danh sách các chủ đề kiến trúc dự kiến

### 1. ADR-B01: Kiến trúc Xuất âm thanh và Dựng hình ngoại tuyến (Audio Export & Offline Rendering)
- **Mô tả:** Cơ chế render bản thu của 24 pad và sequencer thành tệp âm thanh (WAV, MP3, AAC) với chất lượng phòng thu.
- **Vấn đề cần quyết định:**
  - Dựng âm thanh bằng luồng xử lý nền (offline non-realtime rendering) độc lập với luồng âm thanh thời gian thực của [ADR-002](file:///shared-specs/docs/adr/ADR-002-audio-engine.md) để tránh drop sample.
  - Sử dụng thư viện mã hóa âm thanh nào (MediaCodec trên Android, AVAssetWriter trên iOS, hay thư viện C/C++ dùng chung như libmp3lame/libvorbis)?
- **Giai đoạn dự kiến:** M3 (Audio Recording & Export).

---

### 2. ADR-B02: Pipeline Xử lý Hiệu ứng Âm thanh (Audio Effects & DSP Chain)
- **Mô tả:** Thiết kế chuỗi hiệu ứng (Effects Chain) như Filter (Low-pass, High-pass), Reverb, Delay, Bitcrusher và Compressor cho từng pad hoặc master bus.
- **Vấn đề cần quyết định:**
  - Cài đặt DSP thuần trong tầng C++ (Oboe trên Android) và AudioUnit DSP kernel trên iOS, hay trừu tượng hóa một thư viện DSP C++ dùng chung cho cả hai nền tảng?
  - Giới hạn tải CPU để đảm bảo độ trễ tổng thể luôn dưới 20 ms.
- **Giai đoạn dự kiến:** M4 (Audio Effects & Master Processing).

---

### 3. ADR-B03: Hỗ trợ Thiết bị Ngoại vi MIDI (USB / Bluetooth LE MIDI)
- **Mô tả:** Cho phép người dùng kết nối bàn phím MIDI hoặc MIDI pad controller ngoài qua cổng USB-C hoặc Bluetooth Low Energy.
- **Vấn đề cần quyết định:**
  - Kiến trúc adapter MIDI input ánh xạ 24 nốt MIDI tới 24 pad trong UI và Sequencer ([ADR-006](file:///shared-specs/docs/adr/ADR-006-tiem-phu-thuoc-va-kien-truc-ui.md)).
  - Bù trừ độ trễ (latency jitter) đặc thù của Bluetooth LE MIDI.
- **Giai đoạn dự kiến:** M5 (External Hardware Integration).

---

### 4. ADR-B04: Thu âm Mẫu trực tiếp từ Microphone (Microphone Sampling)
- **Mô tả:** Tính năng ghi âm trực tiếp qua mic của điện thoại để tạo sample tùy biến gán ngay vào một pad bất kỳ.
- **Vấn đề cần quyết định:**
  - Quản lý quyền truy cập Microphone và cấu hình input buffer với độ phân giải cao.
  - Pipeline chuẩn hóa âm lượng (normalization), cắt đầu cuối (trimming) và trích xuất waveform cache theo [ADR-005](file:///shared-specs/docs/adr/ADR-005-luu-tru-cuc-bo.md).
- **Giai đoạn dự kiến:** M4 (Sampler & Pad Editor).

---

### 5. ADR-B05: Báo cáo Lỗi và Thống kê bảo vệ Quyền riêng tư (Privacy-first Crash Reporting & Telemetry)
- **Mô tả:** Cơ chế ghi nhận crash và đo lường số liệu kỹ thuật (audio xrun count, touch-to-sound latency benchmark).
- **Vấn đề cần quyết định:**
  - Giải pháp crash reporting cục bộ hoặc công cụ mã nguồn mở tôn trọng quyền riêng tư, không thu thập định danh cá nhân và hoạt động phù hợp với nguyên tắc offline của [ADR-008](file:///shared-specs/docs/adr/ADR-008-cloud-cong-dong.md).
- **Giai đoạn dự kiến:** M6 (Hardening & Beta Release).

