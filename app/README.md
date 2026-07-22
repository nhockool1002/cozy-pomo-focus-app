# CozyPomo — Android App

Kotlin + Jetpack Compose + Hilt + Navigation Compose + Retrofit. Xem stack đầy đủ tại [`../docs/technical-spec.md`](../docs/technical-spec.md) §1.

## Build local

```bash
./gradlew :app:assembleDebug     # APK debug, trỏ API tới http://10.0.2.2:3000 (backend chạy local qua emulator)
./gradlew :app:assembleRelease   # APK release — không có keystore.properties thì tự fallback ký bằng debug key
./gradlew :app:bundleRelease     # AAB release, dùng để nộp Play Console
```

Build release ký thật cần file `keystore.properties` (copy từ `keystore.properties.example`, không commit) hoặc set biến môi trường `KEYSTORE_PATH` / `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD` (CI dùng cách này — xem `.github/workflows/android-release.yml`).

Cần Android SDK có `platforms;android-37` và `build-tools;37.0.0` trở lên (compileSdk = 37).

**Đã có (scaffold):** 4 tab Home/Forest/Shop/Stats (placeholder UI), theme theo Brand Guide, Hilt DI, Retrofit client trỏ `backend/`.
**Chưa có, còn TODO:** icon launcher thật (đang là hình học tạm), toàn bộ tính năng thật (timer, ấp trứng, khu rừng, cửa hàng), lưu trữ Room local, đăng nhập thật (network layer đã sẵn `ApiService.register/login/refresh/me`, chưa nối UI).
