# CeLeHub (Nhóm 4)

```
celehub/
├─ backend/   Spring Boot 3 (Java 17) – VS Code
├─ android/   Android Studio (Java, MVVM) – tạo project mới vào thư mục này
├─ docs/      Đặc tả, SQL cũ, API notes
└─ docker-compose.yml   PostgreSQL cho dev
```

## Chạy backend
1. `docker compose up -d`            (bật PostgreSQL)
2. `cd backend && mvn spring-boot:run`
3. Flyway tự tạo bảng. Admin mặc định: xem `app.bootstrap` trong `application.yml`
4. Swagger UI: http://localhost:8080/swagger-ui.html

Emulator Android gọi backend qua `http://10.0.2.2:8080/`.
