# ADR-002: Audio engine

- **Trạng thái:** Proposed
- **Ngày:** 2026-10-29

## Bối cảnh
Để đạt độ trễ touch-to-sound dưới 20 ms và tránh hiện tượng giật tiếng (xrun), engine âm thanh phải chạy trên luồng thời gian thực độc lập, hỗ trợ đa âm tối thiểu 32 voice và cách ly khỏi các tác vụ UI hay đĩa.

## Quyết định
1. **Công nghệ nền tảng:**
   - **Android:** Dùng C++ với thư viện **Oboe** kết nối qua JNI.
   - **iOS:** Dùng **AVAudioEngine**; chỉ chuyển sang **AudioUnit** nếu đo đạc thực tế vượt ngưỡng 20 ms.
2. **Kỷ luật trên Audio Thread:**
   - Tuyệt đối KHÔNG cấp phát/giải phóng bộ nhớ (`malloc`, `free`, `new`).
   - Tuyệt đối KHÔNG dùng khóa (mutex, lock, condition variable).
   - Tuyệt đối KHÔNG thực hiện I/O đĩa hay mạng.
3. **Giao tiếp và Quản lý Sample:**
   - Giao tiếp từ UI sang audio thread qua hàng đợi không khóa (lock-free queue) hoặc ring buffer.
   - Sample âm thanh phải được nạp sẵn (preloaded) vào RAM trước khi phát; hỗ trợ đa âm tối thiểu 32 voice.

## Phương án đã cân nhắc
- **Android dùng SoundPool/AudioTrack:** Bị loại vì độ trễ lớn, jitter cao và hạn chế mix/DSP.
- **iOS dùng AudioUnit C API ngay từ đầu:** Bị loại vì phức tạp; AVAudioEngine đủ đáp ứng mục tiêu độ trễ ban đầu.

## Hệ quả
- **Lợi ích:** Đạt độ trễ dưới 20 ms, loại bỏ xrun, âm thanh phát ổn định khi gõ 24 pad nhanh.
- **Đánh đổi:** Tăng chi phí bảo trì mã C++/JNI trên Android; đòi hỏi kỷ luật lập trình audio khắt khe.
