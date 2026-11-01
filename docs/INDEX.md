# Mục lục tài liệu — Hanoi Lens

Mỗi chủ đề chỉ có **một** tài liệu nguồn. Khi sửa một nội dung, sửa đúng file sở hữu nó và chỉ dẫn link từ các file khác.

## 1. Bộ tài liệu chuẩn

| # | Tài liệu | Nội dung (nguồn duy nhất cho) |
|---|---|---|
| 01 | [01_OVERVIEW.md](01_OVERVIEW.md) | Bài toán, mục tiêu, kết quả kỳ vọng, phương pháp, In-Scope / Out-of-Scope |
| 02 | [02_ARCHITECTURE.md](02_ARCHITECTURE.md) | Kiến trúc tổng thể (Mermaid), 7 module Spring Modulith, MVVM Android, ADR 001–010, cây thư mục |
| 03 | [03_DATA_AND_CONTRACT.md](03_DATA_AND_CONTRACT.md) | ERD, quy tắc DB, REST API `/v1`, contract nội bộ Spring Boot ↔ FastAPI |
| 04 | [04_WORKFLOW_DIAGRAM.md](04_WORKFLOW_DIAGRAM.md) | User flow, system flow, sequence diagrams |
| 05 | [05_TEAM_ROLES_WORKLOAD.md](05_TEAM_ROLES_WORKLOAD.md) | 7 thành viên, vai trò, RACI, ma trận khối lượng 3 tháng |
| 06 | [06_GIT_AND_CODING_CONVENTION.md](06_GIT_AND_CODING_CONVENTION.md) | Git flow, commit, PR, chống conflict, quy chuẩn đặt tên |
| 07 | [07_TESTING_AND_BUG_TRIAGE.md](07_TESTING_AND_BUG_TRIAGE.md) | Chiến lược test, baseline bảo mật & quyền riêng tư, bug triage |
| 08 | [08_ENVIRONMENT_SETUP.md](08_ENVIRONMENT_SETUP.md) | Phiên bản công cụ, Docker, `.env`, cách chạy từng phần |
| — | [../TODO.md](../TODO.md) | Kế hoạch 12 tuần, 84 task `[Wx-TVy]` |

## 2. Tài liệu gốc & sơ đồ

- [Group project proposal.md](Group%20project%20proposal.md) (+ bản `.docx`): đề bài gốc đã nộp — **không sửa**.
- [Diagram/](Diagram/): ảnh PNG user/system flow và DB (`hanoi-lens-db.png`); nguồn HTML trong `Diagram/HTML/`.

## 3. Lộ trình đọc theo vai trò

Tất cả thành viên đọc **01 → 08** (cài máy) → **06** trước khi code. Sau đó:

| TV | Thành viên | Vai trò | Đọc tiếp | Trọng tâm |
|---|---|---|---|---|
| TV1 | Nguyễn Minh Hoàng | AI: Computer Vision | 03 → 04 → 07 | Contract `/internal/v1/vision/classify`, ngưỡng `UNKNOWN` |
| TV2 | Lê Bá Ninh | Hành trình, Sự kiện & Phần thưởng | 02 → 03 → 04 | Vòng đời `ACTIVE/PAUSED/ENDED`, parser sự kiện `PENDING_REVIEW`, reward |
| TV3 | Lê Hiền Anh | Khám phá, Bản đồ & Đến nơi | 02 → 03 → 04 | PostGIS, `arrival-checks`, không lưu tọa độ thô, `LocationSimulator` |
| TV4 | Nguyễn Minh Lương | AI: RAG lịch sử | 03 → 04 → 07 | `knowledge_chunks` (`pgvector`), 2 điểm vào RAG, bắt buộc citation |
| TV5 | Lương Quỳnh Anh | Camera, Ảnh, Album & Recap | 03 → 04 → 07 | Presigned upload, non-destructive edit, Journey Stamp, xóa EXIF |
| TV6 | Hà Thế Duy | Cyber Security + QA | 07 → 02 → 03 | Threat model 5 luồng, IDOR/BOLA, `ModularityTests`, triage |
| TV7 | Lê Bá Tiệp | Tài khoản, Hồ sơ & Tích hợp AI | 02 → 03 → 07 | JWT + refresh rotation, `LocationPrecision`, `AiInferenceClientAdapter` |
