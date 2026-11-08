# Hướng dẫn dùng Firebase (FCM) để đẩy thông báo cho CeLeHub

Luồng: **Android lấy FCM token → gửi lên backend (`POST /api/devices`) → backend lưu `device_tokens` → mỗi khi có thông báo mới, backend gọi Firebase để đẩy tới máy.** Backend đã viết xong (`PushService`); bạn chỉ cần cấu hình + thêm code Android.

## A. Tạo project Firebase (làm 1 lần)
1. Vào https://console.firebase.google.com → **Add project** → đặt tên `celehub` (tắt Google Analytics cũng được).
2. **Project settings (⚙) → General → Your apps → Android**. Package name **phải trùng** `applicationId` trong `app/build.gradle.kts`: `com.example.celehub`. Bấm Register.
3. Tải **`google-services.json`** → đặt vào `android/celehub/app/google-services.json` (file này đã nằm trong `.gitignore`).
4. **Project settings → Service accounts → Generate new private key** → tải file JSON (khóa cho **backend**, tuyệt đối không commit). Đặt vào VPS: `~/edu-center-lms-android-native-app/secrets/firebase-service-account.json`, `chmod 644`.

## B. Backend (VPS)
1. Thêm `firebase-admin` 9.4.1 vào `pom.xml` và các khóa `app.firebase.*` vào `application.yml` (xem `BACKEND_PATCH_NOTES.md`).
2. `.env`:
```
FIREBASE_ENABLED=true
FIREBASE_CREDENTIALS_PATH=/app/firebase/service-account.json
```
3. `docker-compose.yml` → service `backend`:
```yaml
    volumes:
      - ./secrets/firebase-service-account.json:/app/firebase/service-account.json:ro
```
4. `docker compose up -d --build`. Không báo lỗi `Bật Firebase nhưng thiếu...` là OK. Không có khóa thì để `FIREBASE_ENABLED=false`, app vẫn chạy (chỉ không có push).

## C. Android
**1. Gradle** — `android/celehub/build.gradle.kts` (root) thêm plugin:
```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}
```
`app/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}
dependencies {
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-messaging")
}
```
**2. Manifest** (`AndroidManifest.xml`):
```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
...
<application ...>
    <service android:name=".Service.CeLeHubMessagingService" android:exported="false">
        <intent-filter><action android:name="com.google.firebase.MESSAGING_EVENT" /></intent-filter>
    </service>
    <meta-data android:name="com.google.firebase.messaging.default_notification_channel_id"
               android:value="celehub_default" />
```
**3. Model + API** (`ApiService`):
```java
@POST("api/devices") Call<ApiResponse<Void>> registerDevice(@Body RegisterDeviceRequest r);
@DELETE("api/devices") Call<ApiResponse<Void>> unregisterDevice(@Query("token") String token);
```
```java
public class RegisterDeviceRequest {          // Model/RegisterDeviceRequest.java
    private final String fcmToken;
    public RegisterDeviceRequest(String fcmToken) { this.fcmToken = fcmToken; }
}
```
**4. Service nhận push** — `Service/CeLeHubMessagingService.java`:
```java
public class CeLeHubMessagingService extends FirebaseMessagingService {
    public static final String CHANNEL_ID = "celehub_default";

    @Override public void onNewToken(@NonNull String token) {
        if (SessionManager.get(this).isLoggedIn()) registerToken(this, token);   // token đổi -> gửi lại
    }

    @Override public void onMessageReceived(@NonNull RemoteMessage m) {
        // App ĐANG MỞ thì FCM không tự hiện thông báo -> tự hiện
        RemoteMessage.Notification n = m.getNotification();
        if (n == null) return;
        ensureChannel(this);
        Intent open = new Intent(this, MainActivity.class);
        for (Map.Entry<String, String> e : m.getData().entrySet()) open.putExtra(e.getKey(), e.getValue());
        PendingIntent pi = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_nav_notifications).setContentTitle(n.getTitle())
                .setContentText(n.getBody()).setAutoCancel(true).setContentIntent(pi);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                || Build.VERSION.SDK_INT < 33) {
            NotificationManagerCompat.from(this).notify((int) System.currentTimeMillis(), b.build());
        }
    }

    public static void ensureChannel(Context c) {      // Android 8+ bắt buộc có channel
        if (Build.VERSION.SDK_INT >= 26) {
            c.getSystemService(NotificationManager.class).createNotificationChannel(
                    new NotificationChannel(CHANNEL_ID, "Thông báo CeLeHub", NotificationManager.IMPORTANCE_HIGH));
        }
    }

    /** Gọi sau khi đăng nhập thành công (và trong onNewToken). */
    public static void registerCurrentToken(Context c) {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(t -> registerToken(c, t));
    }
    private static void registerToken(Context c, String token) {
        ApiClient.get(c).registerDevice(new RegisterDeviceRequest(token)).enqueue(new Callback<ApiResponse<Void>>() {
            @Override public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> r) { }
            @Override public void onFailure(Call<ApiResponse<Void>> call, Throwable t) { }
        });
    }
}
```
**5. Khi nào gọi**
- `MainActivity.onCreate`: `CeLeHubMessagingService.ensureChannel(this); CeLeHubMessagingService.registerCurrentToken(this);` và xin quyền (Android 13+):
```java
registerForActivityResult(new ActivityResultContracts.RequestPermission(), ok -> {})
    .launch(Manifest.permission.POST_NOTIFICATIONS);   // chỉ gọi khi Build.VERSION.SDK_INT >= 33
```
- Đăng xuất (`ProfileFragment.logout`): lấy token → `unregisterDevice(token)` rồi mới `session.clear()` (để máy dùng chung không nhận thông báo của người cũ).
- Bấm thông báo khi app đóng: `MainActivity` đọc `getIntent().getStringExtra("refType")` / `"refId"` (backend gửi kèm `type, courseId, refType, refId`) rồi mở đúng màn (ASSIGNMENT → AssignmentSubmitFragment, QUIZ, LEAVE, TUITION...).

## D. Kiểm tra
1. Đăng nhập app (máy thật hoặc emulator **có Google Play**) → bảng `device_tokens` có 1 dòng.
2. Web Admin → Gửi thông báo → máy nhận push (cả khi app đóng).
3. Không có push? Kiểm tra theo thứ tự: `FIREBASE_ENABLED=true` và log backend không có `Gửi push lỗi` · package name khớp `google-services.json` · đã cấp quyền thông báo (Android 13+) · máy không tắt thông báo/tiết kiệm pin · thử "Send test message" trong Firebase Console với token lấy từ DB để tách lỗi app/backend.
4. Token chết (gỡ app) được backend tự dọn khỏi `device_tokens`.

## E. Lưu ý
- Push chỉ là "chuông reo"; dữ liệu thật vẫn lấy từ `GET /api/notifications`. Push lỗi không bao giờ làm hỏng nghiệp vụ.
- Miễn phí, không giới hạn số tin. Không cần cấu hình SHA-1 cho FCM.
