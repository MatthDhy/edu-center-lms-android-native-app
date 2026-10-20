import React from 'react';
import {
  Database, Globe, Smartphone, Shield, ArrowRight,
  Key, Lock, Server, Layers, GitBranch, CheckCircle,
  AlertTriangle, Zap
} from 'lucide-react';

function CodeBlock({ children, lang = '' }) {
  return (
    <pre className="text-xs bg-slate-900 text-slate-200 p-4 rounded-xl overflow-x-auto border border-slate-700 leading-relaxed mt-2">
      <code>{children}</code>
    </pre>
  );
}

function SectionTitle({ icon: Icon, title, color = 'brand' }) {
  const colors = {
    brand: 'bg-brand-50 text-brand-700 border-brand-200',
    emerald: 'bg-emerald-50 text-emerald-700 border-emerald-200',
    purple: 'bg-purple-50 text-purple-700 border-purple-200',
    amber: 'bg-amber-50 text-amber-700 border-amber-200',
  };
  return (
    <div className={`inline-flex items-center gap-2 px-3.5 py-2 rounded-xl border font-bold text-sm mb-4 ${colors[color]}`}>
      <Icon className="w-4 h-4" />
      {title}
    </div>
  );
}

export default function ArchitecturePage() {
  return (
    <div className="space-y-8 max-w-5xl animate-in fade-in duration-150">
      {/* Intro Banner */}
      <div className="p-6 rounded-2xl bg-gradient-to-r from-slate-800 to-slate-900 text-white border border-slate-700">
        <h3 className="text-xl font-extrabold mb-1">Kiến trúc Hệ thống CeLeHub</h3>
        <p className="text-slate-400 text-sm">
          Giải thích cách Web Admin, App Android và Backend Spring Boot kết nối chung một Database, và cách phân quyền Role hoạt động từ đầu đến cuối.
        </p>
      </div>

      {/* Section 1: Overall Architecture */}
      <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
        <SectionTitle icon={Layers} title="1. Sơ đồ Tổng thể (Big Picture)" color="brand" />

        <div className="bg-slate-50 p-5 rounded-xl border border-slate-200 font-mono text-xs text-slate-700 leading-loose">
          <pre>{`┌─────────────────────────────┐   ┌───────────────────────────────┐
│   Web Admin (React + Vite)  │   │  Android App (Java MVVM)      │
│   Chạy trên trình duyệt     │   │  STUDENT App  │  TEACHER App  │
│   http://localhost:5173     │   │  (Retrofit HTTP Client)       │
└─────────────┬───────────────┘   └──────────────┬────────────────┘
              │  HTTPS · JSON · JWT              │  HTTPS · JSON · JWT
              │  Header: Authorization: Bearer   │  Header: Authorization: Bearer
              └────────────────┬─────────────────┘
                               ▼
              ┌────────────────────────────────────┐
              │   Spring Boot REST API  :8080      │
              │   SecurityConfig → JwtAuthFilter   │
              │   Kiểm tra ROLE trong JWT Claims   │
              └────────────────┬───────────────────┘
                     ┌─────────┴──────────┐
                     ▼                    ▼
              PostgreSQL 16        LocalStorage / FCM
              (Docker :5432)       (uploads/, push)
              celehub_db
              ← Schema Flyway V1
`}</pre>
        </div>

        <div className="mt-4 grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="p-4 rounded-xl bg-brand-50 border border-brand-200 text-center">
            <Globe className="w-6 h-6 text-brand-600 mx-auto mb-2" />
            <p className="font-bold text-brand-800 text-sm">Web Admin</p>
            <p className="text-xs text-brand-600 mt-1">React + Vite, port 5173</p>
            <p className="text-[11px] text-brand-500 mt-0.5">ROLE_ADMIN only</p>
          </div>
          <div className="p-4 rounded-xl bg-purple-50 border border-purple-200 text-center">
            <Server className="w-6 h-6 text-purple-600 mx-auto mb-2" />
            <p className="font-bold text-purple-800 text-sm">Spring Boot</p>
            <p className="text-xs text-purple-600 mt-1">REST API, port 8080</p>
            <p className="text-[11px] text-purple-500 mt-0.5">JWT + BCrypt + Flyway</p>
          </div>
          <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-center">
            <Database className="w-6 h-6 text-emerald-600 mx-auto mb-2" />
            <p className="font-bold text-emerald-800 text-sm">PostgreSQL 16</p>
            <p className="text-xs text-emerald-600 mt-1">Docker, port 5432</p>
            <p className="text-[11px] text-emerald-500 mt-0.5">DB chung cho tất cả</p>
          </div>
        </div>
      </div>

      {/* Section 2: DB Connection */}
      <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
        <SectionTitle icon={Database} title="2. Làm sao Web Admin nối được DB chung?" color="emerald" />

        <p className="text-sm text-slate-600 mb-4 leading-relaxed">
          Web Admin <strong>không bao giờ kết nối trực tiếp</strong> vào PostgreSQL. Thay vào đó, nó gọi HTTP tới Spring Boot, và Spring Boot sẽ là người duy nhất giữ kết nối JDBC với database.
        </p>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <p className="text-xs font-bold text-slate-600 uppercase tracking-wider mb-2">
              ① Cấu hình DB trong <code className="bg-slate-100 px-1 rounded">application.yml</code>
            </p>
            <CodeBlock>{`spring:
  datasource:
    url: \${DB_URL:jdbc:postgresql://localhost:5432/celehub_db}
    username: \${DB_USER:celehub_user}
    password: \${DB_PASSWORD:celehub_dev}
  jpa:
    hibernate:
      ddl-auto: validate   # Flyway quản lý schema
  flyway:
    enabled: true          # Tự chạy V1__init_schema.sql`}</CodeBlock>
          </div>

          <div>
            <p className="text-xs font-bold text-slate-600 uppercase tracking-wider mb-2">
              ② Web Admin gọi API — <code className="bg-slate-100 px-1 rounded">src/api/client.js</code>
            </p>
            <CodeBlock>{`// Axios tự thêm JWT vào mỗi request:
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('celehub_token');
  if (token) config.headers.Authorization = \`Bearer \${token}\`;
  return config;
});

// Gọi API admin (chỉ ROLE_ADMIN mới được phép):
GET  /api/admin/users         → Danh sách users
POST /api/admin/users         → Tạo user mới
PATCH /api/admin/users/:id/status → Khoá/mở khoá
POST /api/admin/users/:id/reset-password`}</CodeBlock>
          </div>
        </div>

        <div className="mt-4 p-4 rounded-xl bg-amber-50 border border-amber-200 text-sm text-amber-800 flex items-start gap-3">
          <AlertTriangle className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
          <div>
            <p className="font-bold">Tại sao Web Admin không kết nối DB trực tiếp?</p>
            <p className="text-xs text-amber-700 mt-1">
              Bảo mật! Nếu Web Admin kết nối thẳng vào PostgreSQL, kẻ tấn công có thể chặn traffic và lấy credentials database. Thay vào đó, chỉ Spring Boot (chạy trong Docker) mới biết thông tin DB. Web Admin chỉ giao tiếp qua JWT-protected REST API.
            </p>
          </div>
        </div>
      </div>

      {/* Section 3: Role-based Auth */}
      <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
        <SectionTitle icon={Shield} title="3. Phân quyền Role hoạt động thế nào?" color="purple" />

        <div className="space-y-6">
          {/* Step 1: Login */}
          <div>
            <div className="flex items-center gap-2 mb-2">
              <div className="w-7 h-7 rounded-full bg-brand-600 text-white text-xs font-black flex items-center justify-center shrink-0">1</div>
              <p className="font-bold text-slate-800 text-sm">Đăng nhập → Spring Boot trả về JWT chứa Role</p>
            </div>
            <CodeBlock>{`// POST /api/auth/login { "email": "...", "password": "..." }
// JwtService.generateToken(user):
Jwts.builder()
    .subject(String.valueOf(user.getId()))  // userId
    .claim("role", user.getRole().name())  // "ADMIN" | "TEACHER" | "STUDENT"
    .expiration(...)
    .signWith(hmacKey)
    .compact();   // → "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZSI6IkFETUlOIn0..."`}</CodeBlock>
          </div>

          {/* Step 2: Filter */}
          <div>
            <div className="flex items-center gap-2 mb-2">
              <div className="w-7 h-7 rounded-full bg-brand-600 text-white text-xs font-black flex items-center justify-center shrink-0">2</div>
              <p className="font-bold text-slate-800 text-sm">Mỗi request → JwtAuthFilter kiểm tra token</p>
            </div>
            <CodeBlock>{`// JwtAuthFilter.doFilterInternal(...)
// 1. Đọc header: Authorization: Bearer <token>
// 2. Giải mã token → lấy userId
// 3. Load user từ DB → kiểm tra status (ACTIVE/LOCKED)
// 4. Kiểm tra mustChangePassword
// 5. Set SecurityContext với ROLE_ prefix:
var principal = new AuthUser(user.getId(), user.getEmail(), user.getRole());
var auth = new UsernamePasswordAuthenticationToken(
    principal, null,
    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
    // → "ROLE_ADMIN" | "ROLE_TEACHER" | "ROLE_STUDENT"
);
SecurityContextHolder.getContext().setAuthentication(auth);`}</CodeBlock>
          </div>

          {/* Step 3: SecurityConfig */}
          <div>
            <div className="flex items-center gap-2 mb-2">
              <div className="w-7 h-7 rounded-full bg-brand-600 text-white text-xs font-black flex items-center justify-center shrink-0">3</div>
              <p className="font-bold text-slate-800 text-sm">SecurityConfig định nghĩa quy tắc truy cập</p>
            </div>
            <CodeBlock>{`// SecurityConfig.java — Chỉ ROLE_ADMIN mới vào được /api/admin/**
.authorizeHttpRequests(a -> a
    .requestMatchers("/api/auth/login").permitAll()     // Mọi người đăng nhập được
    .requestMatchers("/api/admin/**").hasRole("ADMIN")  // Chỉ ADMIN
    .anyRequest().authenticated()                       // Còn lại: phải đăng nhập
)`}</CodeBlock>
          </div>

          {/* Role Matrix */}
          <div>
            <div className="flex items-center gap-2 mb-3">
              <div className="w-7 h-7 rounded-full bg-brand-600 text-white text-xs font-black flex items-center justify-center shrink-0">4</div>
              <p className="font-bold text-slate-800 text-sm">Ma trận quyền hạn theo Role</p>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-xs border border-slate-200 rounded-xl overflow-hidden">
                <thead className="bg-slate-50 text-slate-600 font-bold uppercase tracking-wider">
                  <tr>
                    <th className="px-4 py-3 text-left">Tính năng / API</th>
                    <th className="px-4 py-3 text-center">ADMIN (Web)</th>
                    <th className="px-4 py-3 text-center">TEACHER (Android)</th>
                    <th className="px-4 py-3 text-center">STUDENT (Android)</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 bg-white">
                  {[
                    ['POST /api/auth/login', '✅', '✅', '✅'],
                    ['GET /api/users/me', '✅', '✅', '✅'],
                    ['GET /api/admin/users', '✅', '❌ 403', '❌ 403'],
                    ['POST /api/admin/users', '✅', '❌ 403', '❌ 403'],
                    ['PATCH /api/admin/users/:id/status', '✅', '❌ 403', '❌ 403'],
                    ['POST /api/admin/users/:id/reset-password', '✅', '❌ 403', '❌ 403'],
                    ['GET /api/courses/** (GĐ2)', '⬜', '✅', '✅'],
                    ['POST /api/courses (tạo lớp)', '⬜', '✅', '❌'],
                    ['POST /api/attendance (điểm danh)', '⬜', '✅', '❌'],
                    ['POST /api/submissions (nộp bài)', '⬜', '❌', '✅'],
                  ].map(([api, admin, teacher, student], i) => (
                    <tr key={i} className="hover:bg-slate-50/60">
                      <td className="px-4 py-2.5 font-mono text-slate-700">{api}</td>
                      <td className="px-4 py-2.5 text-center">{admin}</td>
                      <td className="px-4 py-2.5 text-center">{teacher}</td>
                      <td className="px-4 py-2.5 text-center">{student}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      {/* Section 4: CORS */}
      <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
        <SectionTitle icon={Globe} title="4. CORS — Tại sao trình duyệt không bị block?" color="amber" />

        <p className="text-sm text-slate-600 mb-4 leading-relaxed">
          Trình duyệt thực thi chính sách <strong>Same-Origin Policy</strong>: React (localhost:5173) sẽ bị block khi gọi Spring Boot (localhost:8080) nếu không cấu hình CORS đúng. Spring Boot đã cấu hình sẵn:
        </p>

        <CodeBlock>{`// SecurityConfig.java
@Bean
CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration c = new CorsConfiguration();
    c.setAllowedOriginPatterns(List.of(
        "http://localhost:*",          // Web Admin dev
        "https://*.celehub.edu.vn"     // Production domain
    ));
    c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
    c.setAllowedHeaders(List.of("*")); // Bao gồm cả Authorization header
    // allowCredentials không set → app dùng Bearer token trong header (không phải cookie)
    UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
    src.registerCorsConfiguration("/**", c);
    return src;
}
// ⚠ Mobile Android KHÔNG bị CORS (không chạy qua trình duyệt — Retrofit gọi HTTP trực tiếp)`}</CodeBlock>
      </div>

      {/* Section 5: Token Flow */}
      <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
        <SectionTitle icon={Key} title="5. Luồng Đăng nhập & Lưu Token đầy đủ" color="brand" />

        <div className="relative">
          {[
            {
              step: 1, icon: Smartphone, color: 'blue',
              title: 'Client gửi credentials',
              desc: 'Web Admin (Axios) hoặc Android (Retrofit) gửi POST /api/auth/login với { email, password }.'
            },
            {
              step: 2, icon: Lock, color: 'purple',
              title: 'Backend xác thực',
              desc: 'AuthService tìm user trong DB, kiểm tra BCrypt password, kiểm tra status ACTIVE, kiểm tra cờ mustChangePassword.'
            },
            {
              step: 3, icon: Key, color: 'amber',
              title: 'Backend trả JWT',
              desc: 'JwtService tạo token chứa userId + role, hết hạn sau 7 ngày. AuthResponse = { token, role, mustChangePassword, user }.'
            },
            {
              step: 4, icon: Database, color: 'emerald',
              title: 'Client lưu & dùng token',
              desc: 'Web Admin lưu vào localStorage("celehub_token"). Mọi request sau đó tự gắn Authorization: Bearer <token>. Android lưu trong SharedPreferences.'
            },
            {
              step: 5, icon: CheckCircle, color: 'brand',
              title: 'Backend validate mỗi request',
              desc: 'JwtAuthFilter chạy trước mỗi API: giải mã token → load user từ DB → kiểm tra role → set SecurityContext → Spring Security áp quy tắc hasRole().'
            },
          ].map(({ step, icon: Icon, color, title, desc }) => {
            const cs = {
              blue: 'bg-blue-50 text-blue-700 border-blue-200',
              purple: 'bg-purple-50 text-purple-700 border-purple-200',
              amber: 'bg-amber-50 text-amber-700 border-amber-200',
              emerald: 'bg-emerald-50 text-emerald-700 border-emerald-200',
              brand: 'bg-brand-50 text-brand-700 border-brand-200',
            }[color];
            return (
              <div key={step} className="flex gap-4 pb-6 last:pb-0 relative">
                {step < 5 && <div className="absolute left-5 top-10 bottom-0 w-0.5 bg-slate-200"></div>}
                <div className={`w-10 h-10 rounded-xl border flex items-center justify-center shrink-0 ${cs}`}>
                  <Icon className="w-5 h-5" />
                </div>
                <div className="flex-1 pt-0.5">
                  <p className="text-xs font-bold text-slate-400 uppercase tracking-wider">Bước {step}</p>
                  <p className="font-bold text-slate-800 text-sm mt-0.5">{title}</p>
                  <p className="text-xs text-slate-500 mt-1 leading-relaxed">{desc}</p>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Section 6: Quick Start */}
      <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
        <SectionTitle icon={Zap} title="6. Chạy hệ thống trong 3 lệnh" color="emerald" />
        <CodeBlock>{`# Bước 1: Khởi động PostgreSQL (Docker)
cd celehub/
docker compose up postgres -d

# Bước 2: Chạy Spring Boot backend (port 8080)
cd backend/
mvn spring-boot:run
# → Flyway tự tạo schema, DataInitializer tạo admin@celehub.edu.vn / Admin@123456

# Bước 3: Chạy Web Admin (port 5173)
cd ../web-admin/
npm run dev
# → Mở http://localhost:5173 → Đăng nhập với tài khoản Admin`}</CodeBlock>
      </div>
    </div>
  );
}
