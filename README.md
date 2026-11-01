# Hanoi Lens

Ứng dụng Android giúp người dùng tự do khám phá di sản, danh thắng và sự kiện văn hóa Hà Nội qua nhiếp ảnh — **USTH ICT Group Project 2025–2026**.

- **Khám phá tự do:** người dùng tự đánh dấu di sản; GPS geofence (chỉ kiểm tra trong/ngoài vùng) mở khóa di sản khi đến nơi.
- **2 model AI:** nhận diện địa danh từ ảnh (trả `UNKNOWN` khi độ tin cậy thấp) và Historical RAG trả lời có trích dẫn.
- **Album & Dấu hành trình:** ảnh gốc bất biến, chỉnh sửa không phá hủy, Journey Stamp, recap theo địa điểm/tháng.
- **Quyền riêng tư:** không theo dõi liên tục, người dùng chọn mức hiển thị vị trí, xóa EXIF GPS khi xuất ảnh.

## Tech stack

Android Native (Java 17 + XML, MVVM) · Spring Boot 3.x Modulith · PostgreSQL 16 (PostGIS + pgvector) · MinIO (S3) · Python FastAPI (Vision + RAG). Chi tiết: [docs/02_ARCHITECTURE.md](docs/02_ARCHITECTURE.md).

## Cấu trúc repo

```text
android-client/   Ứng dụng Android (Java/XML)
backend/          Spring Boot Modulith: identity, profile, destination, event, visit, media, recap
ai-service/       FastAPI nội bộ: vision/ và rag/
docs/             Tài liệu dự án (bắt đầu từ docs/INDEX.md)
TODO.md           Kế hoạch 12 tuần
```

Các file trong `android-client/`, `backend/`, `ai-service/` hiện chỉ là **file khung** chứa comment `TODO` (mục đích, người phụ trách, các hàm cần viết).

## Thành viên

| TV | Họ và tên | MSSV | Vai trò |
|---|---|---|---|
| TV1 | Nguyễn Minh Hoàng | 2410315 | AI: Computer Vision |
| TV2 | Lê Bá Ninh | 23BA14224 | Hành trình, Sự kiện & Phần thưởng |
| TV3 | Lê Hiền Anh | 2410042 | Khám phá, Bản đồ & Đến nơi |
| TV4 | Nguyễn Minh Lương | 23BA14184 | AI: RAG lịch sử |
| TV5 | Lương Quỳnh Anh | 23BA14008 | Camera, Ảnh, Album & Recap |
| TV6 | Hà Thế Duy | 2410207 | Cyber Security + QA |
| TV7 | Lê Bá Tiệp | 23BA14282 | Tài khoản, Hồ sơ & Tích hợp AI |

Phân công chi tiết: [docs/05_TEAM_ROLES_WORKLOAD.md](docs/05_TEAM_ROLES_WORKLOAD.md).

## Tài liệu

Mục lục và lộ trình đọc theo vai trò: **[docs/INDEX.md](docs/INDEX.md)**. Kế hoạch: **[TODO.md](TODO.md)**.

## Quick start

```bash
cp .env.example .env
docker compose up -d                                   # PostgreSQL + MinIO
cd backend && ./mvnw spring-boot:run                   # API :8080
cd ai-service && uvicorn app.main:app --reload --port 8001
```

Android: mở `android-client/` bằng Android Studio. Hướng dẫn đầy đủ: [docs/08_ENVIRONMENT_SETUP.md](docs/08_ENVIRONMENT_SETUP.md).
