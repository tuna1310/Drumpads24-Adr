# ADR-003: Mô hình thời gian của sequencer

- **Trạng thái:** Accepted
- **Ngày:** 2026-10-29

## Bối cảnh
Sequencer 24 pad yêu cầu lập lịch và phát lặp chính xác tuyệt đối, hỗ trợ đổi BPM, quantize và swing mà không bị trôi nhịp (timing drift) do biến thiên hệ điều hành.

## Quyết định
1. **Đơn vị thời gian nội bộ:** Dùng `tick` với độ phân giải PPQ = 960 cho toàn bộ pattern và sự kiện.
2. **Đồng hồ thời gian và Lập lịch:**
   - Chuyển từ `tick` sang `sample time` bằng audio clock phần cứng (theo [ADR-002](file:///shared-specs/docs/adr/ADR-002-audio-engine.md)). Tuyệt đối KHÔNG dùng `Timer` hay `Thread.sleep`.
   - Áp dụng cơ chế **lookahead** 50–100 ms để đẩy trước sự kiện vào audio engine buffer.
3. **Quantize, Swing và Vòng lặp:**
   - Quantize và swing tính trực tiếp trên `tick` trước khi lên lịch sang sample time.
   - Điểm lặp vòng (wrap-around) tính bằng modulo (`currentTick % loopLengthTicks`).

## Phương án đã cân nhắc
- **Dùng thời gian mili giây:** Bị loại vì trôi đồng hồ hệ thống và làm phức tạp phép tính khi người dùng đổi BPM lúc đang phát.
- **Dùng chỉ số mẫu (Sample index):** Bị loại vì khi đổi sample rate (44.1 kHz sang 48 kHz) hoặc đổi BPM, toàn bộ pattern phải tính lại từ đầu.

## Hệ quả
- **Lợi ích:** Nhịp điệu chuẩn phòng thu, không trôi nhịp khi đổi BPM, độc lập tần số lấy mẫu.
- **Đánh đổi:** Cần xử lý cẩn thận phép chuyển đổi giữa tick và sample time để tránh sai số làm tròn số học tích lũy.

## Quyết định đã duyệt
- Chốt cố định chuẩn PPQ = 960 cho toàn bộ mô hình thời gian của sequencer.
- Thời gian nội bộ sử dụng số nguyên 64-bit (`Long` trên Kotlin, `Int64` trên Swift) để đảm bảo độ mịn thời gian cao nhất cho quantize 1/16, 1/32 và micro-timing swing 50%–75% mà không gây sai số tích lũy.
