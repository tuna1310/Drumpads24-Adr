# ADR-001: Chiến lược nền tảng

- **Trạng thái:** Proposed
- **Ngày:** 2026-10-29

## Bối cảnh
Ứng dụng finger-drumming và beat-making 24 pad đòi hỏi độ trễ phản hồi từ khi chạm pad đến khi phát âm thanh (touch-to-sound latency) phải dưới 20 ms. Ngoài ra, giao diện lưới 24 pad cần phản hồi hình ảnh tức thì ở tần số quét cao (90Hz/120Hz). Bất kỳ độ trễ hay gián đoạn nào trên UI thread cũng làm hỏng cảm giác gõ nhịp của người chơi.

## Quyết định
- Phát triển Native hoàn toàn trên cả hai nền tảng:
  - **Android:** Sử dụng Kotlin và Jetpack Compose.
  - **iOS:** Sử dụng Swift và SwiftUI.
- Trực tiếp khai thác API cảm ứng và luồng âm thanh cấp thấp của từng hệ điều hành mà không thông qua bất kỳ tầng trung gian đa nền tảng nào.

## Phương án đã cân nhắc
- **Flutter hoặc React Native kết hợp native audio plugin:** Bị loại. Mặc dù giúp tái sử dụng mã nguồn UI, việc truyền sự kiện chạm và trạng thái audio qua bridge (Platform Channel/FFI) tạo thêm overhead trễ không dự đoán được, dễ gây drop khung hình và lệch nhịp giữa visual pad với audio engine.

## Hệ quả
- **Lợi ích:** Đạt độ trễ dưới 20 ms; kiểm soát tối đa pipeline render, luồng audio và bộ gom rác (GC) trên từng OS.
- **Đánh đổi:** Tốn nguồn lực bảo trì hai codebase UI riêng biệt; khối lượng công việc tăng so với giải pháp cross-platform.

## Câu hỏi mở
- Cần quyết định mức độ chia sẻ mã nguồn logic: Sequencer thuần túy nên viết riêng theo từng nền tảng hay chia sẻ bằng C++ (xem [ADR-006](file:///shared-specs/docs/adr/ADR-006-tiem-phu-thuoc-va-kien-truc-ui.md))?
