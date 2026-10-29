# Báo cáo Đo Độ Trễ & Kiểm Thử Áp Lực (Milestone M1)

Tài liệu này ghi nhận phương pháp, quy trình và biểu mẫu đo lường độ trễ touch-to-sound cùng tỷ lệ dropped buffer (xrun) theo chuẩn đã duyệt tại [ADR-002](file:///shared-specs/docs/adr/ADR-002-audio-engine.md).

> **Lưu ý:** Môi trường Agent không có thiết bị phần cứng thật. Toàn bộ công cụ đo đạc (Stress Mode, telemetry logging) đã được tích hợp sẵn trong mã nguồn. Bảng số liệu dưới đây để trống, đánh dấu `[Chờ đo trên thiết bị thật]` để chủ dự án điền kết quả sau khi chạy thử nghiệm.

---

## 1. Biểu mẫu Kết quả Đo lường

### Android (Mục tiêu: Touch-to-Sound < 20 ms, XRun Rate < 0.1%)
| Trường thông tin | Thông số thực tế |
| :--- | :--- |
| **Thiết bị thử nghiệm** | *[Điền: ví dụ Google Pixel 6 / Samsung S22]* |
| **Phiên bản Android** | *[Điền: ví dụ Android 13 / 14]* |
| **Audio Engine** | C++ Oboe (AAudio backend, Low Latency, Exclusive mode) |
| **Tần số lấy mẫu (Sample Rate)** | *[Điền: 48000 Hz / 44100 Hz]* |
| **Kích thước Buffer (Frames)** | *[Điền: ví dụ 96 / 192 / 240 frames]* |
| **Burst Size** | *[Điền: ví dụ 96 frames]* |
| **Phương pháp đo** | *[Loopback Audio Cable / High-Speed Camera 240fps]* |
| **Touch-to-Sound Latency** | **[Chờ đo trên thiết bị thật]** ms |
| **Số Voice đồng thời trong Stress Test** | 32 voice polyphony liên tục trong 60 giây |
| **Tổng số Audio Callback Buffer** | *[Chờ đo trên thiết bị thật]* |
| **Số lần phát hiện XRun (Buffer Underrun)** | **[Chờ đo trên thiết bị thật]** |
| **Tỷ lệ XRun (%)** | **[Chờ đo trên thiết bị thật]** % |

### iOS (Mục tiêu: Touch-to-Sound < 20 ms, XRun Rate < 0.1%)
| Trường thông tin | Thông số thực tế |
| :--- | :--- |
| **Thiết bị thử nghiệm chuẩn** | iPhone 11 (A13 Bionic) hoặc iPhone SE (2020) |
| **Phiên bản iOS** | *[Điền: ví dụ iOS 17.x / 18.x]* |
| **Audio Engine** | AVAudioEngine (Manual Rendering / Real-time I/O Unit) |
| **Tần số lấy mẫu (Sample Rate)** | *[Điền: 44100 Hz / 48000 Hz]* |
| **I/O Buffer Duration** | *[Điền: ví dụ 0.005s (5.8 ms / 256 frames)]* |
| **Phương pháp đo** | *[Loopback Audio Cable / High-Speed Camera 240fps]* |
| **Touch-to-Sound Latency** | **[Chờ đo trên thiết bị thật]** ms |
| **Số Voice đồng thời trong Stress Test** | 32 voice polyphony liên tục trong 60 giây |
| **Số lần phát hiện XRun / Glitch** | **[Chờ đo trên thiết bị thật]** |
| **Tỷ lệ XRun (%)** | **[Chờ đo trên thiết bị thật]** % |
| **Đánh giá quyết định AVAudioEngine** | *[Giữ nguyên AVAudioEngine / Cần hạ xuống AudioUnit]* |

---

## 2. Hướng dẫn Từng Bước Đo Độ Trễ & Kiểm Thử Áp Lực

### Phương pháp 1: Kích hoạt Chế độ Stress Test trong App (Đo XRun & Dropped Buffer)
1. Mở app trên thiết bị thật ở chế độ **Debug**.
2. Nhấn nút biểu tượng bánh răng **Settings** ở góc trên màn hình.
3. Bật mục **"Stress Test Mode (32 Voice, 60s)"**.
4. Ứng dụng sẽ tự động kích hoạt liên tục 32 voice âm thanh đồng thời qua lock-free queue trong 60 giây.
5. Quan sát log hệ thống (Logcat trên Android với tag `DrumPadsAudio`, Console trên Xcode với subsystem `com.drumpads.audio`):
   - Tổng số frames được render.
   - Số sự kiện underrun / xrun phát hiện bởi audio driver.
   - Tỷ lệ lỗi tính theo phần trăm.

### Phương pháp 2: Đo Độ trễ Touch-to-Sound bằng Camera Tốc độ cao (240 fps)
1. Sử dụng smartphone thứ hai có tính năng quay video Slow-motion 240 khung hình/giây (mỗi khung hình tương đương $\approx 4.16$ ms).
2. Đặt thiết bị quay sao cho nhìn rõ cả ngón tay người chơi chạm vào bề mặt pad và đồng thời nghe được âm thanh qua micro của máy quay.
3. Gõ dứt khoát vào một pad bất kỳ.
4. Mở video đã quay trong phần mềm chỉnh sửa video (như CapCut, DaVinci Resolve hoặc QuickTime):
   - Đánh dấu khung hình $F_1$: Thời điểm ngón tay vừa chạm đáy mặt kính màn hình (pad bắt đầu đổi màu visual feedback).
   - Đánh dấu khung hình $F_2$: Thời điểm sóng âm thanh đầu tiên bắt đầu xuất hiện trên thanh audio waveform.
   - Độ trễ phản hồi tính theo công thức: 
     $$\text{Touch-to-Sound Latency (ms)} = (F_2 - F_1) \times 4.166\text{ ms}$$
5. Ghi lại kết quả trung bình của 10 lần gõ vào bảng biểu trên.
