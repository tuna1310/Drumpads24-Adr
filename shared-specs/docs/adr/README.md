# Danh mục Quyết định Kiến trúc (ADR) - 24-Pad Beat Making App

Tài liệu này tổng hợp toàn bộ các Quyết định Kiến trúc (Architectural Decision Records - ADR) cho dự án ứng dụng finger-drumming / beat-making dạng lưới 24 pad trên nền tảng Android và iOS.

Quy trình quản lý, cập nhật và vòng đời của các tài liệu này tuân thủ nghiêm ngặt theo quy ước được định nghĩa tại [ADR-000](file:///shared-specs/docs/adr/ADR-000-quy-uoc.md).

---

## Bảng mục lục ADR

| Mã số | Tiêu đề | Trạng thái | Ngày | Tóm tắt quyết định |
| :--- | :--- | :--- | :--- | :--- |
| [ADR-000](file:///shared-specs/docs/adr/ADR-000-quy-uoc.md) | Quy ước viết ADR | **Accepted** | 2026-10-29 | Quy định cấu trúc ADR, mã số, và tính bất biến của ADR đã Accepted. |
| [ADR-001](file:///shared-specs/docs/adr/ADR-001-chien-luoc-nen-tang.md) | Chiến lược nền tảng | **Proposed** | 2026-10-29 | Phát triển Native độc lập (Android: Kotlin/Compose, iOS: Swift/SwiftUI) để đảm bảo độ trễ < 20 ms. |
| [ADR-002](file:///shared-specs/docs/adr/ADR-002-audio-engine.md) | Audio engine | **Proposed** | 2026-10-29 | Android dùng C++/Oboe, iOS dùng AVAudioEngine; hàng đợi lock-free, cấm cấp phát bộ nhớ/khóa/IO trên audio thread. |
| [ADR-003](file:///shared-specs/docs/adr/ADR-003-mo-hinh-thoi-gian-sequencer.md) | Mô hình thời gian của sequencer | **Proposed** | 2026-10-29 | Đơn vị tick (PPQ = 960), lập lịch bằng audio clock với lookahead 50–100 ms, không dùng Timer/sleep. |
| [ADR-004](file:///shared-specs/docs/adr/ADR-004-dinh-dang-du-lieu-json.md) | Định dạng dữ liệu JSON có version | **Proposed** | 2026-10-29 | JSON có `schemaVersion`, JSON Schema và fixture test; sample trỏ bằng `id` ổn định; parse ngoài audio thread. |
| [ADR-005](file:///shared-specs/docs/adr/ADR-005-luu-tru-cuc-bo.md) | Lưu trữ cục bộ | **Proposed** | 2026-10-29 | Room (Android) và SwiftData (iOS) cho metadata; file audio và waveform cache lưu trên filesystem. |
| [ADR-006](file:///shared-specs/docs/adr/ADR-006-tiem-phu-thuoc-va-kien-truc-ui.md) | Tiêm phụ thuộc và kiến trúc UI | **Proposed** | 2026-10-29 | MVVM + Hilt + StateFlow trên Android; @Observable + Store trên iOS; Sequencer là module thuần có adapter audio. |
| [ADR-007](file:///shared-specs/docs/adr/ADR-007-chinh-sach-noi-dung-va-ban-quyen.md) | Chính sách nội dung và bản quyền | **Accepted** | 2026-10-29 | 100% nguyên bản; không sao chép app thương mại; sample hợp lệ (CC0, CC-BY, in-house) ghi nguồn trong CREDITS.md. |
| [ADR-008](file:///shared-specs/docs/adr/ADR-008-cloud-cong-dong.md) | Đồng bộ cloud và cộng đồng | **Deferred** | 2026-10-29 | Hoãn tới giai đoạn M7; phiên bản đầu chạy offline 100%; M7 sẽ xây dựng ADR backend riêng. |

---

## Ghi chú & Đề xuất tiếp theo

Các đề xuất kiến trúc tiềm năng chưa thuộc phạm vi các ADR trên được theo dõi tại [BACKLOG.md](file:///shared-specs/docs/adr/BACKLOG.md).

