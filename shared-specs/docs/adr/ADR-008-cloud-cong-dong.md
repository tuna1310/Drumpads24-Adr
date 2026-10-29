# ADR-008: Đồng bộ cloud và cộng đồng

- **Trạng thái:** Deferred
- **Ngày:** 2026-10-29

## Bối cảnh
Trong lộ trình dài hạn, ứng dụng có thể mở rộng tính năng đám mây: chia sẻ beat, tải sound pack từ cộng đồng. Tuy nhiên, việc đưa tính năng online vào sớm sẽ gây phân tán nguồn lực khi trải nghiệm audio cốt lõi chưa hoàn thiện.

## Quyết định
1. **Trì hoãn đến Giai đoạn M7:**
   - Trì hoãn (Deferred) toàn bộ quyết định về cloud sync, tài khoản và mạng xã hội đến giai đoạn **M7**.
2. **Chiến lược Offline-First 100%:**
   - Phiên bản đầu tiên hoạt động hoàn toàn offline, không yêu cầu đăng nhập và không truyền dữ liệu ra ngoài.
3. **Kế hoạch cho M7:**
   - Khi bước vào giai đoạn M7, nhóm kiến trúc sẽ lập các ADR riêng để quyết định về:
     - Hạ tầng backend và lưu trữ.
     - Cơ chế xác thực người dùng.
     - Giao thức đồng bộ hai chiều.
     - Quy trình kiểm duyệt nội dung cộng đồng.

## Phương án đã cân nhắc
- **Tích hợp Firebase hoặc Supabase ngay từ MVP:** Bị loại vì làm phình app, phức tạp luồng dữ liệu cục bộ và tốn chi phí vận hành máy chủ khi trải nghiệm cốt lõi chưa ổn định.

## Hệ quả
- **Lợi ích:** Tập trung 100% nguồn lực cho độ trễ < 20 ms và độ hoàn thiện của 24 pad; không tốn chi phí hạ tầng ban đầu.
- **Đánh đổi:** Chưa có tính năng sao lưu hay chia sẻ trực tuyến; schema dữ liệu cục bộ ([ADR-004](file:///shared-specs/docs/adr/ADR-004-dinh-dang-du-lieu-json.md)) phải chuẩn bị sẵn cho đồng bộ sau này.
