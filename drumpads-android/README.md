# DrumPads 24 - Android Native App

Ứng dụng finger-drumming 24 pad native trên nền tảng Android, xây dựng theo các quyết định kiến trúc đã duyệt ([ADR-001](file:///shared-specs/docs/adr/ADR-001-chien-luoc-nen-tang.md) đến [ADR-007](file:///shared-specs/docs/adr/ADR-007-chinh-sach-noi-dung-va-ban-quyen.md)).

---

## 1. Công nghệ & Kiến trúc
- **Ngôn ngữ & Giao diện:** Kotlin 1.9.24, Jetpack Compose (BOM 2024.06.00).
- **Kiến trúc UI:** MVVM với `StateFlow`, Dependency Injection qua **Hilt**.
- **Audio Engine:** C++17 với **Oboe** (AAudio / OpenSL ES) qua JNI.
- **Kỷ luật Real-time Audio:**
  - Hàng đợi không khóa (SPSC Lock-Free Queue) 256 phần tử để nhận NoteOn từ UI thread sang Audio callback.
  - Bộ đệm voice (Voice Pool) giới hạn cứng 32 voice. Khi đầy 32 voice, chính sách Voice Stealing tự động tái sử dụng voice cũ nhất dựa trên timestamp.
  - Tuyệt đối không cấp phát bộ nhớ động (`malloc`/`new`), không mutex/lock, không I/O đĩa trong audio thread.
- **Haptic Feedback:** Tích hợp `Vibrator` / `VibratorManager` tùy chọn bật/tắt trong Cài đặt.
- **Stress Test Debug Mode:** Tự động kích hoạt 32 voice đồng thời trong 60 giây và đếm số lần XRun / buffer underrun.

---

## 2. Hướng dẫn Build & Kiểm thử

### Yêu cầu môi trường
- JDK 17 (đã kiểm thử với Microsoft JDK 17 / OpenJDK 17).
- Android SDK (API Level 34, Build-tools 34.0.0+).
- Android NDK r25+ và CMake 3.22.1+ (cho native C++ build).

### Lệnh chạy kiểm thử Unit Test (chạy ngay trên máy dev, không cần emulator)
```bash
./gradlew test
```
Bộ test bao gồm:
1. `VoicePoolTest`: Kiểm thử cấp phát voice, đạt tối đa 32 voice, chính sách voice stealing khi đầy, và tự động thu hồi voice sau khi render xong buffer.
2. `PadMappingTest`: Kiểm thử cấu hình chuẩn 24 pad chia đều 4 nhóm (Drum, Bass, Melody, FX), tính duy nhất của sample ID.
3. `EmptyFixtureTest`: Kiểm tra nạp và parse cấu trúc JSON test vector rỗng của sequencer (`sequencer-test-vectors.empty.json`).

### Lệnh build APK
```bash
./gradlew assembleDebug
```
File APK đầu ra tại: `app/build/outputs/apk/debug/app-debug.apk`.

### Chạy trên Emulator hoặc Thiết bị thật
1. Khởi động Android Emulator (Pixel 6, Android 13/14 khuyến nghị) hoặc cắm thiết bị thật qua USB Debugging.
2. Cài đặt và khởi chạy:
   ```bash
   ./gradlew installDebug
   ```
3. Chạm vào bất kỳ pad nào trong lưới 24 pad để nghe âm thanh phát tức thì (< 20 ms).
4. Vào biểu tượng Settings (bánh răng) ở góc trên để bật/tắt Haptic hoặc kích hoạt chế độ Stress Test 60s.
