# 08. Hướng dẫn Cài đặt Môi trường & Khởi chạy Hệ thống (Environment Setup Guide)

Tài liệu này giúp cả 7 thành viên thiết lập môi trường phát triển đồng nhất 100% trên máy cá nhân, cấu hình hạ tầng bằng Docker Compose (PostgreSQL 16 tích hợp sẵn `PostGIS` + `pgvector`, và `MinIO`), và khởi chạy độc lập từng thành phần của **Hanoi Lens**.

---

## 1. Bảng Chuẩn hóa Phiên bản Công cụ (100% Thành viên)

Để tránh lỗi *"chạy được trên máy người này nhưng lỗi trên máy người khác"*, toàn bộ thành viên bắt buộc cài đặt đúng các phiên bản dưới đây:

| Công cụ / Nền tảng | Phiên bản chuẩn hóa | Lệnh kiểm tra nhanh | Ghi chú bắt buộc |
| :--- | :--- | :--- | :--- |
| **Java Development Kit (JDK)** | **JDK 17 (LTS)** (Eclipse Temurin / OpenJDK 17) | `java -version` | Dùng chung cho cả Spring Boot 3.x Modulith và Gradle của Android Studio. |
| **Apache Maven** | **3.9.x+** (hoặc dùng `./mvnw` đi kèm repo) | `./mvnw -v` | Luôn ưu tiên chạy `./mvnw` (Linux/macOS) hoặc `mvnw.cmd` (Windows). |
| **Android Studio** | **Ladybug (2024.2.1+)** | Kiểm tra trong *About Android Studio* | Ngôn ngữ **Java**, giao diện **XML Layouts** + **ViewBinding**. |
| **Android SDK** | **Compile/Target SDK 34**, **Min SDK 26** (Android 8.0) | Kiểm tra trong *SDK Manager* | Cài kèm *Google Play Licensing/Location Services* và máy ảo AVD API 34. |
| **Python** | **Python 3.11.x** | `python3.11 --version` | Dành cho FastAPI AI Inference Runtime (Model 1 Vision & Model 2 RAG). |
| **Docker & Docker Compose** | **Docker Engine 24+ & Compose v2** | `docker compose version` | Dùng để chạy PostgreSQL (`PostGIS` + `pgvector`), MinIO và Testcontainers. |
| **Git** | **2.40+** | `git --version` | Bật `git config core.autocrlf input` (Linux/macOS) hoặc `true` (Windows). |

---

## 2. Cấu hình Hạ tầng Cục bộ với `docker-compose.yml`

Hanoi Lens sử dụng **một cụm PostgreSQL 16 duy nhất** tích hợp đồng thời cả hai tiện ích mở rộng **`PostGIS`** (truy vấn không gian Geofence) và **`pgvector`** (tìm kiếm tương đồng vector cho Historical RAG), kết hợp với **MinIO** (lưu trữ ảnh tương thích S3).

### 2.1. File `docker-compose.yml` chuẩn

Tạo file `docker-compose.yml` tại thư mục gốc của dự án (`/home/minhluong/Documents/HanoiLens/docker-compose.yml`):

```yaml
services:
  postgres:
    image: pgvector/pgvector:pg16
    container_name: hanoilens-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${POSTGRES_DB:-hanoilens}
      POSTGRES_USER: ${POSTGRES_USER:-hanoilens}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-hanoilens_dev_secret}
      TZ: UTC
    ports:
      - "${POSTGRES_PORT:-5432}:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./docker/postgres/init-extensions.sql:/docker-entrypoint-initdb.d/01-init-extensions.sql:ro
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER:-hanoilens} -d ${POSTGRES_DB:-hanoilens}"]
      interval: 5s
      timeout: 5s
      retries: 10

  minio:
    image: minio/minio:latest
    container_name: hanoilens-minio
    restart: unless-stopped
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: ${MINIO_ROOT_USER:-hanoilens_minio}
      MINIO_ROOT_PASSWORD: ${MINIO_ROOT_PASSWORD:-hanoilens_minio_secret}
    ports:
      - "${MINIO_API_PORT:-9000}:9000"
      - "${MINIO_CONSOLE_PORT:-9001}:9001"
    volumes:
      - minio_data:/data
    healthcheck:
      test: ["CMD", "mc", "ready", "local"]
      interval: 5s
      timeout: 5s
      retries: 10

  minio-init:
    image: minio/mc:latest
    container_name: hanoilens-minio-init
    depends_on:
      minio:
        condition: service_healthy
    entrypoint: >
      /bin/sh -c "
      mc alias set local http://minio:9000 $${MINIO_ROOT_USER:-hanoilens_minio} $${MINIO_ROOT_PASSWORD:-hanoilens_minio_secret};
      mc mb --ignore-existing local/$${S3_BUCKET_NAME:-hanoilens-media};
      mc anonymous set none local/$${S3_BUCKET_NAME:-hanoilens-media};
      exit 0;
      "

volumes:
  postgres_data:
  minio_data:
```

> [!NOTE]
> Nếu sử dụng image `postgis/postgis:16-3.4`, bạn có thể cài thêm gói `postgresql-16-pgvector` qua Dockerfile mỏng, hoặc cài `postgis` lên image `pgvector/pgvector:pg16`. Cả hai extension được kích hoạt tự động qua script `docker/postgres/init-extensions.sql` dưới đây.

### 2.2. Script khởi tạo Extension (`docker/postgres/init-extensions.sql`)

```sql
-- Kích hoạt PostGIS (SRID 4326), pgvector (HNSW/IVFFlat) và sinh UUID
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
```

---

## 3. Mẫu Cấu hình Biến Môi trường (`.env.example`)

Sao chép `.env.example` thành `.env` tại thư mục gốc trước khi chạy hệ thống. **Tuyệt đối không commit file `.env` lên Git.**

```dotenv
# ==============================================================================
# 1. DATABASE CONFIGURATION (PostgreSQL 16 + PostGIS + pgvector)
# ==============================================================================
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=hanoilens
POSTGRES_USER=hanoilens
POSTGRES_PASSWORD=hanoilens_dev_secret
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/hanoilens

# ==============================================================================
# 2. OBJECT STORAGE CONFIGURATION (MinIO / S3-Compatible)
# ==============================================================================
MINIO_API_PORT=9000
MINIO_CONSOLE_PORT=9001
MINIO_ROOT_USER=hanoilens_minio
MINIO_ROOT_PASSWORD=hanoilens_minio_secret
S3_ENDPOINT=http://localhost:9000
# Endpoint dành cho máy ảo Android khi gọi Presigned PUT URL trực tiếp vào MinIO:
S3_PUBLIC_PRESIGN_ENDPOINT=http://10.0.2.2:9000
S3_REGION=ap-southeast-1
S3_BUCKET_NAME=hanoilens-media
S3_PRESIGNED_PUT_EXPIRY_MINUTES=10
S3_PRESIGNED_GET_EXPIRY_MINUTES=15

# ==============================================================================
# 3. SECURITY & JWT CONFIGURATION
# ==============================================================================
# Khóa bí mật HMAC-SHA256 tối thiểu 256-bit (32+ ký tự ngẫu nhiên)
JWT_SECRET=replace_with_a_strong_64_char_random_hex_secret_for_local_dev_only
JWT_ISSUER=https://api.hanoilens.local
JWT_AUDIENCE=hanoilens-android-client
JWT_ACCESS_TOKEN_TTL_MINUTES=15
JWT_REFRESH_TOKEN_TTL_DAYS=30

# ==============================================================================
# 4. UPLOAD & PRIVACY POLICY LIMITS
# ==============================================================================
MEDIA_MAX_UPLOAD_BYTES=26214400
MEDIA_ALLOWED_MIME_TYPES=image/jpeg,image/png
MEDIA_MAX_IMAGE_DIMENSION=8192

# ==============================================================================
# 5. PYTHON AI INFERENCE RUNTIME (FastAPI)
# ==============================================================================
AI_RUNTIME_ENABLED=true
AI_RUNTIME_BASE_URL=http://localhost:8001
AI_RUNTIME_TIMEOUT_SECONDS=15
VISION_CONFIDENCE_THRESHOLD=0.75
RAG_TOP_K_CHUNKS=5
```

---

## 4. Hướng dẫn Khởi chạy Độc lập Từng Thành phần (4 Bước)

### Bước 1: Khởi chạy Database & MinIO bằng Docker

1. Tạo file `.env` từ mẫu:
   ```bash
   cp .env.example .env
   ```
2. Khởi chạy các container nền tảng ở chế độ chạy ngầm (detached):
   ```bash
   docker compose up -d
   ```
3. Kiểm tra trạng thái hoạt động:
   ```bash
   docker compose ps
   ```
   - **PostgreSQL:** Lắng nghe tại `localhost:5432`.
   - **MinIO S3 API:** Lắng nghe tại `http://localhost:9000`.
   - **MinIO Web Console:** Mở trình duyệt tại `http://localhost:9001` (đăng nhập bằng `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD`), xác nhận bucket `hanoilens-media` đã được tạo với quyền truy cập **Private (`none`)**.

---

### Bước 2: Khởi chạy Spring Boot Modulith Backend

1. Di chuyển vào thư mục backend (ví dụ `backend/`):
   ```bash
   cd backend
   ```
2. Chạy kiểm thử xác minh kiến trúc module và khởi động ứng dụng với profile `dev`:
   ```bash
   ./mvnw test -Dtest=ModularityVerificationTest
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```
3. Kiểm tra sau khi khởi động:
   - **Health Check:** `GET http://localhost:8080/actuator/health` $\rightarrow$ trả về `{"status":"UP"}`.
   - **Swagger UI / OpenAPI Contract:** Mở `http://localhost:8080/swagger-ui.html` để xem và thử nghiệm trực tiếp các endpoint `/v1/*`.

---

### Bước 3: Khởi chạy Python FastAPI AI Runtime (Model 1 Vision & Model 2 RAG)

> [!TIP]
> Nếu bạn chỉ phát triển các tính năng khám phá bản đồ, chuyến thăm hoặc album mà chưa cần gọi AI thực tế, bạn có thể đặt `AI_RUNTIME_ENABLED=false` trong `.env`. Khi đó Backend tự động giữ nguyên nội dung văn hóa tĩnh và báo trạng thái trợ lý AI tạm nghỉ theo đúng thiết kế.

1. Di chuyển vào thư mục AI Runtime (ví dụ `ai-runtime/`), tạo và kích hoạt môi trường ảo Python 3.11:
   ```bash
   cd ai-runtime
   python3.11 -m venv .venv
   source .venv/bin/activate
   ```
2. Cài đặt các thư viện phụ thuộc:
   ```bash
   pip install --upgrade pip
   pip install -r requirements.txt
   ```
3. Khởi chạy máy chủ FastAPI tại cổng `8001`:
   ```bash
   uvicorn app.main:app --reload --host 0.0.0.0 --port 8001
   ```
4. Kiểm tra tài liệu nội bộ của AI Runtime tại `http://localhost:8001/docs`.

---

### Bước 4: Khởi chạy Android Native Client & Giả lập GPS (`LocationSimulator`)

1. **Mở dự án trong Android Studio Ladybug:**
   - Chọn **Open** $\rightarrow$ trỏ tới thư mục `android-app/` (hoặc thư mục chứa `settings.gradle`).
   - Vào **Settings $\rightarrow$ Build, Execution, Deployment $\rightarrow$ Build Tools $\rightarrow$ Gradle** và đảm bảo **Gradle JDK** đang chọn **JDK 17**.
2. **Cấu hình địa chỉ IP kết nối Backend (`10.0.2.2:8080`):**
   - Trên máy ảo Android (Android Emulator), `localhost` hoặc `127.0.0.1` trỏ về chính máy ảo đó. Để gọi tới Spring Boot và MinIO đang chạy trên máy tính host, bắt buộc sử dụng địa chỉ cầu nối đặc biệt **`10.0.2.2`**:
   ```groovy
   // Trong app/build.gradle (buildTypes.debug)
   buildConfigField "String", "API_BASE_URL", "\"http://10.0.2.2:8080/v1/\""
   ```
   - Nếu chạy trên **điện thoại Android thật** (cùng mạng Wi-Fi với máy tính): thay `10.0.2.2` bằng địa chỉ IP LAN của máy tính (ví dụ `http://192.168.1.15:8080/v1/`).
3. **Cấu hình chế độ giả lập GPS (`LocationSimulator`) để test Geofence tại chỗ:**
   - **Cách 1 (Tích hợp sẵn trong bản `debug` của ứng dụng):** Kích hoạt `LocationSimulator` trong menu Debug của ứng dụng để nạp sẵn lộ trình đi bộ giả lập qua các điểm di sản (ví dụ: từ ngoài cổng tiến vào tâm vùng Geofence *Văn Miếu - Quốc Tử Giám* tại tọa độ `21.028511, 105.835536` với `accuracyMeters = 12`).
   - **Cách 2 (Dùng Extended Controls của Android Emulator):** Nhấn biểu tượng `...` (Extended Controls) bên cạnh máy ảo $\rightarrow$ chọn **Location** $\rightarrow$ nhập tọa độ `21.028511, 105.835536` $\rightarrow$ nhấn **Set Location** để kiểm tra ngay luồng `POST /v1/destinations/{id}/arrival-checks`.

---

## 5. Xử lý Sự cố Thường gặp (Troubleshooting)

| STT | Triệu chứng lỗi | Nguyên nhân gốc rễ | Cách khắc phục nhanh |
| :--- | :--- | :--- | :--- |
| **1** | `ERROR: extension "postgis" is not available` hoặc `extension "vector" is not available` khi chạy Flyway/Liquibase | Container PostgreSQL khởi tạo từ image chuẩn không cài sẵn cả 2 extension `postgis` và `pgvector`, hoặc volume cũ chưa chạy lại script init. | Xóa volume cũ và dựng lại container DB:<br/>`docker compose down -v && docker compose up -d` |
| **2** | Máy ảo Android gọi API báo `java.net.ConnectException: Failed to connect to localhost/127.0.0.1:8080` | Dùng `localhost` trong cấu hình Retrofit trên máy ảo Android, hoặc lỗi chặn HTTP thường (Cleartext traffic) trên Android 9+. | 1. Đổi URL sang `http://10.0.2.2:8080/v1/`.<br/>2. Cho phép `10.0.2.2` trong `res/xml/network_security_config.xml` (chỉ dành cho bản `debug`). |
| **3** | Android lấy được Presigned URL từ Backend nhưng bước `PUT` ảnh lên MinIO bị lỗi `SignatureDoesNotMatch` hoặc `ConnectException` | Backend ký Presigned URL bằng host `localhost:9000` nhưng Android lại gọi qua `10.0.2.2:9000` (hoặc ngược lại), làm lệch header `Host` trong chữ ký S3 SigV4. | Cấu hình `S3_PUBLIC_PRESIGN_ENDPOINT=http://10.0.2.2:9000` khi phát triển với máy ảo, hoặc thêm `10.0.2.2` vào host alias để chữ ký khớp hoàn toàn với URL mà OkHttp gửi đi. |
| **4** | `./mvnw test` báo lỗi `Could not find a valid Docker environment` khi chạy Integration Test với Testcontainers | Dịch vụ Docker daemon chưa bật hoặc tài khoản Linux hiện tại chưa thuộc nhóm `docker`. | Kiểm tra bằng `docker info`. Nếu bị lỗi quyền trên Linux, chạy `sudo usermod -aG docker $USER` rồi đăng xuất/đăng nhập lại. |
| **5** | `ApplicationModules.verify()` thất bại với lỗi `Module 'visit' depends on non-exposed type ...` | Một class trong module này đang `import` trực tiếp Entity, Repository hoặc package nội bộ của module khác. | Xóa `import` vi phạm; chuyển sang gọi qua interface dịch vụ ở package gốc của module kia hoặc phát sự kiện `ApplicationEventPublisher`. |
| **6** | Kiểm tra đến nơi (`arrival-checks`) luôn trả về `OUTSIDE` hoặc `INDETERMINATE` trên máy ảo | Máy ảo chưa bật Location hoặc đang dùng tọa độ mặc định tại trụ sở Google (Mountain View, Mỹ) hoặc sai số `accuracyMeters` quá lớn. | Mở **Extended Controls $\rightarrow$ Location** trên Emulator hoặc bật `LocationSimulator` trong app để đặt tọa độ về Hà Nội (ví dụ: `21.028511, 105.835536`). |
