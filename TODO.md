# Kế hoạch Thực hiện Dự án Hanoi Lens (12 Tuần — Sắp xếp theo Thứ tự Phụ thuộc & Chống Conflict)

Tài liệu quản lý lộ trình phát triển, tích hợp và kiểm thử dự án **Hanoi Lens** (USTH Group Project 2025–2026) cho 7 thành viên trong 3 tháng (12 tuần), bám sát [Kiến trúc hệ thống](docs/02_ARCHITECTURE.md), [REST API Contract](docs/03_DATA_AND_CONTRACT.md), [Luồng nghiệp vụ](docs/04_WORKFLOW_DIAGRAM.md), [Phân công nhóm](docs/05_TEAM_ROLES_WORKLOAD.md) và [Tiêu chuẩn Bảo mật MVP](docs/07_TESTING_AND_BUG_TRIAGE.md).

---

## 1. Nguyên tắc Sắp xếp Thứ tự Công việc & Chống Xung đột (Zero-Conflict Rules)

Để không ai bị chờ đợi (blocking) và **tuyệt đối không xảy ra Git Merge Conflict** khi 7 người cùng code chung một monorepo, bảng kế hoạch dưới đây **KHÔNG xếp cơ học từ TV1 đến TV7**, mà sắp xếp theo **Thứ tự Phụ thuộc Thực thi (Topological Order — 3 Nhịp/Tuần)**:

1. **Nhịp 1 — Mở đường (`[BƯỚC 1 - NỀN TẢNG / ĐI TRƯỚC]`):** Những phần code khung, bảng CSDL gốc (cha của khóa ngoại), hoặc hợp đồng giao tiếp (DTO/Interface) phải được merge vào nhánh `main` trong **2 ngày đầu tuần**.
2. **Nhịp 2 — Phát triển Độc lập (`[BƯỚC 2 - SONG SONG]`):** Sau khi có khung/hợp đồng (hoặc làm trên nhánh độc lập `ai-service/`), các thành viên code song song trong **thư mục riêng của mình**, dùng Mock DTO khi cần mà không phải đợi nhau.
3. **Nhịp 3 — Ghép nối & Kiểm thử (`[BƯỚC 3 - TÍCH HỢP / KIỂM THỬ ĐI SAU]`):** Các task ghép nối Adapter, nối UI vào API thật và chạy bộ kiểm thử tự động/bảo mật (của TV6, TV7) thực hiện vào **cuối tuần** sau khi code nghiệp vụ đã đẩy lên.

### Bản đồ Phân vùng File Độc quyền (Ai chạm vào đâu để không bao giờ Conflict?)

| Mã TV | Thành viên | Vùng File Độc Quyền Backend (`backend/`) | Vùng File Độc Quyền Android (`android-client/`) | Vùng File AI / DB Migration |
|---|---|---|---|---|
| **TV7** | Lê Bá Tiệp | `HanoiLensApplication.java`, `shared/*`, `modules/identity/**`, `modules/profile/**`, `modules/recap/adapter/AiInferenceClientAdapter.java` | `ui/auth/**`, `data/api/AuthApi.java`, `data/api/AiApi.java`, `data/local/TokenManager.java`, `res/layout/activity_main.xml`, `res/menu/bottom_nav_menu.xml` | `V1__identity_and_profile.sql`, `docker-compose.yml` |
| **TV3** | Lê Hiền Anh | `modules/destination/**` | `ui/explore/**`, `ui/map/CombinedMapFragment.java`, `location/**`, `data/api/DestinationApi.java`, `data/repository/ExploreRepository.java`, `viewmodel/ExploreViewModel.java` | `V2__destination_and_landmarks.sql` |
| **TV2** | Lê Bá Ninh | `modules/visit/**`, `modules/event/**` | `ui/visit/**`, `ui/events/**`, `ui/map/VisitMapFragment.java`, `data/api/VisitApi.java`, `data/api/EventApi.java`, `data/repository/VisitRepository.java`, `viewmodel/VisitViewModel.java` | `V3__visit_event_and_reward.sql` |
| **TV5** | Lương Quỳnh Anh | `modules/media/**`, `modules/recap/controller/**`, `modules/recap/service/**`, `modules/recap/entity/**`, `modules/recap/repository/**` | `ui/camera/**`, `ui/journal/**`, `camera/**`, `util/JourneyStampRenderer.java`, `data/api/MediaApi.java`, `data/repository/MediaRepository.java`, `viewmodel/JournalViewModel.java` | `V4__media_and_recap.sql` |
| **TV4** | Nguyễn Minh Lương | *(Không sửa code Java Backend)* | `ui/ai/HistoricalRagChatFragment.java` *(phối hợp TV7)* | `V5__knowledge_and_rag.sql`, `ai-service/app/rag/**`, `ai-service/app/api/routes_rag.py` |
| **TV1** | Nguyễn Minh Hoàng | *(Không sửa code Java Backend)* | `ui/ai/LandmarkInfoFragment.java`, `viewmodel/AiChatViewModel.java` *(phối hợp TV7)* | `ai-service/app/vision/**`, `ai-service/app/api/routes_vision.py` |
| **TV6** | Hà Thế Duy | `src/test/java/vn/edu/usth/hanoilens/**` (`ModularityTests.java`, `security/**`) | `util/ExifPrivacySanitizer.java` | `ai-service/tests/test_inference_contracts.py`, CI Workflows |

> **Quy tắc tách file Flyway chống lỗi Khóa ngoại (Foreign Key) & chống Conflict ở Tuần 2:**
> Mọi bảng đều trỏ khóa ngoại về `users(id)` (của TV7) và `destinations(id)` / `landmarks(id)` (của TV3). Do đó hệ thống chia sẵn **5 file SQL đánh số `V1` $\rightarrow$ `V5`** theo đúng thứ tự phụ thuộc:
> `V1 (TV7: users, profiles)` $\rightarrow$ `V2 (TV3: destinations, landmarks)` $\rightarrow$ `V3 (TV2: events, visits, rewards)` $\rightarrow$ `V4 (TV5: photos, recaps)` $\rightarrow$ `V5 (TV4: knowledge_chunks, qa)`. **Không ai sửa chung 1 file SQL với người khác!**

---

## Giai đoạn 1 — Tháng 1 (Tuần 1 – Tuần 4): Nền tảng, Khung Kiến trúc, Dữ liệu & Mock Contracts

> **Mục tiêu Giai đoạn 1:** Dựng hạ tầng Docker (`postgres` PostGIS + pgvector, `minio`), khởi tạo khung dự án, chạy thành công 5 file Flyway `V1`–`V5` theo đúng thứ tự khóa ngoại, và chốt toàn bộ API Mock Contracts để Android, Backend và AI làm việc độc lập không chờ nhau.

### Tuần 1: Khởi tạo Hạ tầng & Chốt Đặc tả Kỹ thuật (Sắp xếp theo thứ tự thực thi)

- [ ] **[W1-TV7]** (Lê Bá Tiệp) — **[BƯỚC 1 - MỞ ĐƯỜNG]** (`Độc lập — Làm ngay Ngày 1–2`): Khởi tạo cấu trúc monorepo (`android-client/`, `backend/`, `ai-service/`), thiết lập `docker-compose.yml` cho PostgreSQL 16 (bật sẵn extension `postgis` và `vector`), MinIO S3 bucket `hanoilens-media`, cùng khung build Spring Boot 3.x (Java 17, `pom.xml`) và Android Native (`build.gradle`, ViewBinding).
  - *Phạm vi file:* `docker-compose.yml`, `.env.example`, `backend/pom.xml`, `android-client/build.gradle`.
  - *Đầu ra mở đường cho cả nhóm:* Môi trường local chạy thành công bằng `docker compose up -d`, khung build Maven/Gradle xanh để 6 thành viên pull về làm việc.
- [ ] **[W1-TV6]** (Hà Thế Duy) — **[BƯỚC 1 - MỞ ĐƯỜNG]** (`Phụ thuộc: W1-TV7`): Thiết lập `.gitignore`, cấu hình bảo mật nhánh Git và lập Threat Model cho 5 luồng trọng yếu (Auth Token Rotation, Geofence Arrival Check, Presigned Upload S3, Chống IDOR trên Visit/Photo/Recap, Landmark Vision & Historical RAG).
  - *Phạm vi file:* `.gitignore`, `docs/07_TESTING_AND_BUG_TRIAGE.md`.
  - *Đầu ra:* Checklist kiểm soát bảo mật bắt buộc cho từng module trước khi viết code.
- [ ] **[W1-TV3]** (Lê Hiền Anh) — **[BƯỚC 2 - SONG SONG]** (`Độc lập`): Khảo sát tọa độ GPS thực tế, bán kính vùng địa lý (Geofence polygon/radius SRID 4326) của các cụm di sản Hà Nội và danh sách các địa danh con (`landmarks`) bên trong; chuẩn hóa mã định danh (`destinationId`, `landmarkId`) để TV1, TV2, TV4 dùng chung.
  - *Đầu ra:* Bộ dữ liệu tọa độ GeoJSON và bảng mã `destinationId`/`landmarkId` chuẩn cho 5 cụm di sản trọng điểm tại Hà Nội.
- [ ] **[W1-TV1]** (Nguyễn Minh Hoàng) — **[BƯỚC 2 - SONG SONG]** (`Phối hợp danh mục nhãn với W1-TV3`): Lập danh mục nhãn phân loại (`recognitionLabel`, `landmarkId`) cho Model 1 Computer Vision và quy chuẩn thu thập ảnh dưới nhiều điều kiện ánh sáng, thời tiết, góc chụp Hà Nội.
  - *Đầu ra:* Tài liệu đặc tả nhãn Vision khớp 100% với `landmarkId` của TV3 và cấu trúc thư mục dataset chuẩn.
- [ ] **[W1-TV4]** (Nguyễn Minh Lương) — **[BƯỚC 2 - SONG SONG]** (`Phối hợp danh mục mã với W1-TV3`): Thu thập nguồn tư liệu lịch sử chính thống đã kiểm chứng về các di sản Hà Nội; định nghĩa cấu trúc trích dẫn bắt buộc (`citations`: `title`, `url`) gắn với từng `destinationId` và `landmarkId`.
  - *Đầu ra:* Kho văn bản lịch sử gốc Markdown/JSON đã làm sạch cho 5 cụm di tích đầu tiên.
- [ ] **[W1-TV2]** (Lê Bá Ninh) — **[BƯỚC 2 - SONG SONG]** (`Độc lập`): Đặc tả máy trạng thái hành trình khám phá (`ACTIVE ⇄ PAUSED → ENDED`, tối đa 1 hành trình đang mở/người dùng), cơ chế tự đánh dấu di sản (`markedHeritages`), mở khóa khi đến nơi (`unlockedHeritages`), đối soát trao thưởng (`Journey Stamp Badge` + `Random Gift`) và khảo sát nguồn sự kiện văn hóa chính thống.
  - *Đầu ra:* Bảng quy tắc chuyển trạng thái `Visit` và danh sách nguồn sự kiện văn hóa có cấu trúc JSON-LD/RSS.
- [ ] **[W1-TV5]** (Lương Quỳnh Anh) — **[BƯỚC 2 - SONG SONG]** (`Độc lập`): Đặc tả chi tiết công thức chỉnh sửa ảnh không phá hủy (`editRecipe`: crop, rotation, exposure, contrast, filter), cấu trúc 6 phong cách **Dấu hành trình (`Journey Stamp`)** (`Minimal`, `Postcard`, `Film frame`, `Map stamp`, `Heritage stamp`, `Weather stamp`) và 5 mức hiển thị vị trí (`NONE`, `DESTINATION`, `DISTRICT`, `APPROXIMATE`, `EXACT`).
  - *Đầu ra:* JSON Schema cho `editRecipe`, `watermarkConfig` và mẫu thiết kế 6 kiểu Dấu hành trình.

---

### Tuần 2: Thiết kế Schema CSDL (Tách 5 File Flyway V1–V5) & Khung Điều hướng (Sắp xếp theo thứ tự khóa ngoại)

- [ ] **[W2-TV7]** (Lê Bá Tiệp) — **[BƯỚC 1 - MỞ ĐƯỜNG CSDL & ANDROID]** (`Phụ thuộc: W1-TV7`): Viết **Flyway V1** khởi tạo extensions (`postgis`, `vector`) và các bảng gốc `users`, `refresh_tokens`, `profiles`; dựng bộ vỏ phản hồi chuẩn (`ApiResponse.java`, `ErrorResponse.java`, `GlobalExceptionHandler.java`) và khung điều hướng `activity_main.xml` + `bottom_nav_menu.xml` trên Android.
  - *Phạm vi file:* `V1__identity_and_profile.sql`, `backend/.../shared/*`, `android-client/.../res/layout/activity_main.xml`, `res/menu/bottom_nav_menu.xml`.
  - *Đầu ra mở đường:* Bảng `users(id)` sẵn sàng để các file `V2`–`V5` tham chiếu khóa ngoại; khung Bottom Navigation Android sẵn sàng để các thành viên gắn Fragment.
- [ ] **[W2-TV3]** (Lê Hiền Anh) — **[BƯỚC 1 - MỞ ĐƯỜNG CSDL ĐỊA ĐIỂM]** (`Phụ thuộc: W2-TV7 V1`): Viết **Flyway V2** tạo bảng `destinations` (cột hình học PostGIS `boundary geometry(Geometry, 4326)`) và bảng `landmarks` (`position geometry(Point, 4326)`, `recognition_label`) kèm chỉ mục không gian `GIST` và các JPA Entity `Destination.java`, `Landmark.java`.
  - *Phạm vi file:* `V2__destination_and_landmarks.sql`, `backend/.../modules/destination/entity/*`, `data/local/AppDatabase.java` (khởi tạo Room DB cơ sở).
  - *Đầu ra mở đường:* Bảng `destinations(id)` và `landmarks(id)` sẵn sàng cho `V3`, `V4`, `V5`.
- [ ] **[W2-TV2]** (Lê Bá Ninh) — **[BƯỚC 2 - SONG SONG]** (`Phụ thuộc: W2-TV7 V1 & W2-TV3 V2`): Viết **Flyway V3** tạo bảng `cultural_events`, `visits` (kèm partial unique index chỉ cho phép tối đa 1 visit `ACTIVE`/`PAUSED` mỗi `user_id`), `visit_marked_heritages`, `visit_unlocked_heritages`, `visit_landmarks` và `rewards` cùng các JPA Entity tương ứng.
  - *Phạm vi file:* `V3__visit_event_and_reward.sql`, `backend/.../modules/visit/entity/*`, `backend/.../modules/event/entity/*`.
  - *Đầu ra:* Migration `V3` chạy xanh và tầng Entity của module `visit`, `event`.
- [ ] **[W2-TV5]** (Lương Quỳnh Anh) — **[BƯỚC 2 - SONG SONG]** (`Phụ thuộc: W2-TV2 V3`): Viết **Flyway V4** tạo bảng `upload_sessions`, `photos` (`object_key`, `location_precision`, `edit_recipe jsonb`, `watermark_config jsonb`), `landmark_recognitions`, `recaps`, `recap_photos` và cấu hình `S3StorageAdapter.java` kết nối MinIO.
  - *Phạm vi file:* `V4__media_and_recap.sql`, `backend/.../modules/media/entity/*`, `backend/.../modules/recap/entity/*`, `modules/media/adapter/S3StorageAdapter.java`.
  - *Đầu ra:* Migration `V4` chạy xanh và kết nối MinIO S3 sẵn sàng.
- [ ] **[W2-TV4]** (Nguyễn Minh Lương) — **[BƯỚC 2 - SONG SONG]** (`Phụ thuộc: W2-TV3 V2`): Viết **Flyway V5** tạo bảng `knowledge_chunks` (`embedding vector(768)`, `destination_id`, `landmark_id`, `content`, `source_citation jsonb`) kèm chỉ mục `HNSW` (`vector_cosine_ops`) và bảng `qa_conversations`, `qa_messages`; viết script `ingest_corpus.py` chia nhỏ văn bản lịch sử.
  - *Phạm vi file:* `V5__knowledge_and_rag.sql`, `ai-service/app/rag/ingest_corpus.py`.
  - *Đầu ra:* Migration `V5` chạy xanh và pipeline chia đoạn văn bản lịch sử (semantic chunking).
- [ ] **[W2-TV1]** (Nguyễn Minh Hoàng) — **[BƯỚC 2 - SONG SONG]** (`Độc lập trên nhánh AI`): Thu thập, làm sạch và gán nhãn đợt 1 tập dữ liệu ảnh các công trình di sản Hà Nội (tối thiểu 150–200 ảnh/lớp + tập ảnh ngoài phân phối `OOD/Negative` như đường phố, cây cối, người) để huấn luyện khả năng trả về `UNKNOWN`.
  - *Phạm vi file:* `ai-service/app/vision/preprocess.py`.
  - *Đầu ra:* Bộ dữ liệu huấn luyện & kiểm thử v1 đã gán nhãn đúng `recognition_label` trong bảng `landmarks`.
- [ ] **[W2-TV6]** (Hà Thế Duy) — **[BƯỚC 3 - KIỂM THỬ KIẾN TRÚC ĐI SAU]** (`Phụ thuộc: W2-TV7, TV3, TV2, TV5`): Viết `ModularityTests.java` sử dụng `ApplicationModules.of(HanoiLensApplication.class).verify()` để kiểm tra tự động ranh giới 7 module (`identity`, `profile`, `destination`, `event`, `visit`, `media`, `recap`), ngăn chặn mọi import chéo trái phép ngay từ đầu dự án.
  - *Phạm vi file:* `backend/src/test/java/vn/edu/usth/hanoilens/ModularityTests.java`.
  - *Đầu ra:* Bài kiểm thử kiến trúc Spring Modulith và kiểm tra chạy liền mạch chuỗi Flyway `V1` $\rightarrow$ `V5`.

---

### Tuần 3: Xây dựng Mock API Contracts, Xác thực JWT & Khung FastAPI (Giải phóng Android/Backend/AI làm song song)

- [ ] **[W3-TV7]** (Lê Bá Tiệp) — **[BƯỚC 1 - MỞ ĐƯỜNG AUTH & FASTAPI ENTRY]** (`Phụ thuộc: W2-TV7`): Hiện thực hóa xác thực tại module `identity` (`POST /v1/auth/register`, `POST /v1/auth/login` băm mật khẩu **Argon2id**, cấp JWT 15 phút, `POST /v1/auth/refresh` xoay vòng token, `POST /v1/auth/logout`) và khởi tạo file khung `ai-service/app/main.py` gắn sẵn 2 router rỗng (`routes_vision`, `routes_rag`) để TV1 & TV4 không sửa đè vào `main.py`.
  - *Phạm vi file:* `modules/identity/**`, `shared/SecurityConfig.java`, `android-client/.../data/local/TokenManager.java`, `ai-service/app/main.py`, `ai-service/requirements.txt`.
  - *Đầu ra mở đường:* JWT Filter hoạt động cho toàn bộ Backend; `main.py` của FastAPI đã tách riêng 2 file router cho TV1 và TV4.
- [ ] **[W3-TV1]** (Nguyễn Minh Hoàng) — **[BƯỚC 2 - SONG SONG]** (`Phụ thuộc: W3-TV7 main.py`): Định nghĩa Pydantic schema và Mock endpoint `POST /internal/v1/vision/classify` trong `routes_vision.py` trả về `result` (`RECOGNIZED` | `UNKNOWN`), `landmarkId`, `recognitionLabel`, `confidence` theo đúng hợp đồng nội bộ tại `docs/03_DATA_AND_CONTRACT.md`.
  - *Phạm vi file:* `ai-service/app/api/routes_vision.py`.
  - *Đầu ra:* Endpoint FastAPI Vision Mock chạy độc lập trên cổng `8001` để Backend/Android tích hợp sớm.
- [ ] **[W3-TV4]** (Nguyễn Minh Lương) — **[BƯỚC 2 - SONG SONG]** (`Phụ thuộc: W3-TV7 main.py`): Định nghĩa Pydantic schema và Mock endpoint `POST /internal/v1/rag/answer` trong `routes_rag.py` nhận `destinationId`, `landmarkId` (tùy chọn), `question` và trả về `answer` kèm mảng `sources` (`title`, `url`).
  - *Phạm vi file:* `ai-service/app/api/routes_rag.py`.
  - *Đầu ra:* Endpoint FastAPI RAG Mock trả đúng JSON schema trích dẫn nguồn.
- [ ] **[W3-TV3]** (Lê Hiền Anh) — **[BƯỚC 2 - SONG SONG]** (`Phụ thuộc: W2-TV3`): Xây dựng Controller stub & DTO cho `/v1/destinations`, `/v1/destinations/nearby`, `/v1/destinations/{destinationId}`, `/v1/destinations/{destinationId}/landmarks`, `/v1/landmarks/{landmarkId}`, `/v1/destinations/{destinationId}/arrival-checks`; dựng giao diện `ExploreHomeFragment.java` và `HeritagePreviewFragment.java` bằng dữ liệu Mock.
  - *Phạm vi file:* `modules/destination/controller/**`, `android-client/.../data/api/DestinationApi.java`, `data/model/DestinationDto.java`, `ui/explore/ExploreHomeFragment.java`, `ui/explore/HeritagePreviewFragment.java`, `res/layout/fragment_explore_home.xml`.
  - *Đầu ra:* Màn hình Explore Hanoi (home) và Heritage Preview chạy mượt với Mock API.
- [ ] **[W3-TV2]** (Lê Bá Ninh) — **[BƯỚC 2 - SONG SONG]** (`Phụ thuộc: W2-TV2`): Xây dựng Controller stub & DTO cho toàn bộ API hành trình (`/v1/visits`, `/v1/visits/active`, `/v1/visits/{visitId}/pause`, `/resume`, `/end`) và sự kiện (`/v1/events`, `/v1/events/nearby`, `/v1/events/{eventId}`); dựng khung `EventFeedFragment.java` và `EventDetailFragment.java`.
  - *Phạm vi file:* `modules/visit/controller/**`, `modules/event/controller/**`, `android-client/.../data/api/VisitApi.java`, `data/api/EventApi.java`, `data/model/VisitDto.java`, `data/model/EventDto.java`, `ui/events/EventFeedFragment.java`, `ui/events/EventDetailFragment.java`.
  - *Đầu ra:* Bộ API stub hành trình/sự kiện và màn hình danh sách/chi tiết sự kiện trên Android.
- [ ] **[W3-TV5]** (Lương Quỳnh Anh) — **[BƯỚC 2 - SONG SONG]** (`Phụ thuộc: W2-TV5`): Xây dựng Controller stub & DTO cho luồng tải ảnh Presigned URL (`POST /v1/media/uploads`, `POST /v1/media/uploads/{uploadId}/complete`, `GET /v1/photos/{photoId}`, `POST /v1/photos/{photoId}/edits`, `POST /v1/photos/{photoId}/exports`) và Album/Recap (`/v1/albums`, `/v1/recaps`); tích hợp CameraX vào `CameraController.java` và `CameraCaptureFragment.java`.
  - *Phạm vi file:* `modules/media/controller/**`, `modules/recap/controller/**`, `android-client/.../data/api/MediaApi.java`, `data/model/PhotoDto.java`, `camera/CameraController.java`, `ui/camera/CameraCaptureFragment.java`, `res/layout/fragment_camera.xml`.
  - *Đầu ra:* API khung module `media`/`recap` và màn hình chụp ảnh CameraX cơ bản trên Android.
- [ ] **[W3-TV6]** (Hà Thế Duy) — **[BƯỚC 3 - KIỂM THỬ BẢO MẬT ĐI SAU]** (`Phụ thuộc: W3-TV7, TV1, TV4`): Viết bộ kiểm thử tích hợp xác thực trong `SecurityBaselineTests.java` (kiểm tra đăng nhập, từ chối token hết hạn/giả mạo, phát hiện tái sử dụng Refresh Token đã xoay vòng) và viết contract test trong `test_inference_contracts.py` cho 2 endpoint FastAPI của TV1 & TV4.
  - *Phạm vi file:* `backend/.../security/SecurityBaselineTests.java`, `ai-service/tests/test_inference_contracts.py`.
  - *Đầu ra:* Bộ test bảo mật Auth & hợp đồng AI tự động chạy xanh.

---

### Tuần 4: Nạp Dữ liệu Hạt giống (Seed Data), Tiền xử lý AI & Hoàn thiện Hồ sơ (Chốt Giai đoạn 1)

- [ ] **[W4-TV3]** (Lê Hiền Anh) — **[BƯỚC 1 - MỞ ĐƯỜNG DỮ LIỆU ĐỊA ĐIỂM]** (`Phụ thuộc: W3-TV3`): Nạp bộ dữ liệu hạt giống (Seed Data) chuẩn xác cho các di sản Hà Nội (Văn Miếu – Quốc Tử Giám, Hoàng Thành Thăng Long, Hồ Hoàn Kiếm...) và các `landmarks` con vào bảng `destinations`, `landmarks` với ranh giới PostGIS thực tế.
  - *Phạm vi file:* `modules/destination/service/DestinationService.java`, `modules/destination/repository/**`.
  - *Đầu ra mở đường:* Dữ liệu `destinations` và `landmarks` có sẵn trong DB để TV2 (gắn sự kiện/hành trình) và TV4 (nạp `knowledge_chunks` theo `destination_id`/`landmark_id`) sử dụng.
- [ ] **[W4-TV4]** (Nguyễn Minh Lương) — **[BƯỚC 2 - SONG SONG]** (`Phụ thuộc: W4-TV3 seed destinations/landmarks`): Chạy pipeline nhúng vector (Embedding) cho kho tư liệu lịch sử Hà Nội đợt 1 và nạp vào bảng `knowledge_chunks` trên PostgreSQL `pgvector` gắn đúng `destination_id` và `landmark_id` của TV3.
  - *Phạm vi file:* `ai-service/app/rag/ingest_corpus.py`, `ai-service/app/rag/retriever.py`.
  - *Đầu ra:* Bảng `knowledge_chunks` có đầy đủ vector embeddings để truy vấn tương đồng cosine (`<=>`).
- [ ] **[W4-TV2]** (Lê Bá Ninh) — **[BƯỚC 2 - SONG SONG]** (`Phụ thuộc: W4-TV3 seed destinations`): Xây dựng `EventSourceParserAdapter.java` bóc tách sự kiện tất định (JSON-LD, RSS, CSS Selectors, quy tắc ngày tiếng Việt) và nạp dữ liệu mẫu cho cả 2 loại sự kiện (có gắn `destinationId` và không gắn `destinationId`) cùng danh mục phần thưởng `rewards`.
  - *Phạm vi file:* `modules/event/adapter/EventSourceParserAdapter.java`, `modules/event/service/EventService.java`, `modules/event/repository/**`.
  - *Đầu ra:* Bộ parser sự kiện có unit test kèm dữ liệu sự kiện/phần thưởng trong DB.
- [ ] **[W4-TV1]** (Nguyễn Minh Hoàng) — **[BƯỚC 2 - SONG SONG]** (`Độc lập trên nhánh AI`): Xây dựng pipeline tăng cường dữ liệu ảnh (Data Augmentation: biến đổi ánh sáng, sương mù, góc nghiêng) và huấn luyện mô hình phân loại địa danh baseline đầu tiên (EfficientNet/ResNet/MobileViT) trong `classifier.py`.
  - *Phạm vi file:* `ai-service/app/vision/preprocess.py`, `ai-service/app/vision/classifier.py`.
  - *Đầu ra:* Script huấn luyện reproducible và báo cáo độ chính xác baseline trên tập validation.
- [ ] **[W4-TV5]** (Lương Quỳnh Anh) — **[BƯỚC 2 - SONG SONG]** (`Độc lập trên nhánh Android Media`): Xây dựng `ImageEditorProcessor.java` (chỉnh sửa không phá hủy: crop, rotate, brightness, contrast, filter) và khung `JourneyStampRenderer.java` vẽ lớp phủ Dấu hành trình lên bản sao `Bitmap`.
  - *Phạm vi file:* `android-client/.../camera/ImageEditorProcessor.java`, `util/JourneyStampRenderer.java`, `ui/camera/PhotoEditFragment.java`.
  - *Đầu ra:* Màn hình chỉnh sửa ảnh và kết xuất thử nghiệm mẫu dấu `Minimal`, `Postcard` trên Android.
- [ ] **[W4-TV7]** (Lê Bá Tiệp) — **[BƯỚC 2 - SONG SONG]** (`Phụ thuộc: W3-TV7`): Hiện thực hóa module `profile` (`GET /v1/me`, `PATCH /v1/me` cập nhật `displayName`, `locale`, `defaultLocationPrecision`) và hoàn thiện các màn hình Android `LoginActivity.java`, `LandingActivity.java` (chỉ mở sau khi đăng ký mới), `ProfileFragment.java`.
  - *Phạm vi file:* `modules/profile/**`, `android-client/.../ui/auth/**`, `data/api/AuthApi.java`.
  - *Đầu ra:* Luồng Đăng nhập/Đăng ký $\rightarrow$ Landing Page $\rightarrow$ Hồ sơ cá nhân hoạt động hoàn chỉnh từ Android tới Backend.
- [ ] **[W4-TV6]** (Hà Thế Duy) — **[BƯỚC 3 - KIỂM ĐỊNH FILE & BẢO MẬT]** (`Phụ thuộc: W3-TV5, W4-TV7`): Xây dựng bộ kiểm tra tính hợp lệ của tệp ảnh tải lên (kiểm tra magic bytes thực tế cho JPEG/PNG, giới hạn dung lượng $\le 25\text{ MB}$, kích thước điểm ảnh, chống decompression bomb) và kiểm thử tích hợp `/v1/me`.
  - *Phạm vi file:* `backend/.../security/SecurityBaselineTests.java`.
  - *Đầu ra:* Bộ validator kiểm định tệp ảnh an toàn sẵn sàng gắn vào luồng upload tuần 5.

---

## Giai đoạn 2 — Tháng 2 (Tuần 5 – Tuần 8): Phát triển Core Modules (Geofencing, Visits/Events, Media/Stamps, Vision & RAG)

> **Mục tiêu Giai đoạn 2:** Hiện thực hóa toàn bộ nghiệp vụ cốt lõi theo đúng 7 quy tắc trong `docs/04_WORKFLOW_DIAGRAM.md`: Khám phá tự do, GPS Geofence mở khóa di sản ngay lập tức, tách biệt 2 trang Explore, chụp ảnh nhận diện địa danh (có ngưỡng `UNKNOWN`), hỏi đáp RAG có trích dẫn và đóng 6 kiểu Dấu hành trình.

### Tuần 5: Truy vấn PostGIS, Vòng đời Hành trình, Presigned S3 & Adapter AI (Sắp xếp theo thứ tự phụ thuộc)

- [ ] **[W5-TV3]** (Lê Hiền Anh) — **[BƯỚC 1 - MỞ ĐƯỜNG API ĐỊA ĐIỂM]** (`Phụ thuộc: W4-TV3`): Hiện thực hóa truy vấn không gian PostGIS trong `DestinationRepository.java` & `DestinationService.java`: tìm di sản gần vị trí hiện tại (`GET /v1/destinations/nearby` bằng `ST_DWithin`), lọc danh sách di sản (`GET /v1/destinations`), lấy chi tiết di sản & danh sách `landmarks` (`GET /v1/destinations/{id}`, `GET /v1/landmarks/{id}`).
  - *Phạm vi file:* `backend/.../modules/destination/**`.
  - *Đầu ra mở đường:* API tra cứu `Destination` và `Landmark` thật để module `visit` (TV2) và `AiInferenceClientAdapter` (TV7) gọi qua interface công khai.
- [ ] **[W5-TV7]** (Lê Bá Tiệp) — **[BƯỚC 1 - MỞ ĐƯỜNG CỔNG KẾT NỐI AI]** (`Phụ thuộc: W3-TV1, W3-TV4`): Xây dựng `AiInferenceClientAdapter.java` kết nối HTTP nội bộ từ Spring Boot sang Python FastAPI (`:8001`) với timeout chặt chẽ (`connectTimeout = 2s`, `readTimeout = 8s`) và cơ chế **Static Fallback** khi FastAPI tạm ngưng.
  - *Phạm vi file:* `backend/.../modules/recap/adapter/AiInferenceClientAdapter.java`, `android-client/.../data/api/AiApi.java`.
  - *Đầu ra mở đường:* Cổng kết nối Backend $\leftrightarrow$ FastAPI sẵn sàng để cắm mô hình thật của TV1 và TV4 ở tuần 6.
- [ ] **[W5-TV2]** (Lê Bá Ninh) — **[BƯỚC 2 - SONG SONG]** (`Phụ thuộc: W5-TV3`): Hiện thực hóa `VisitService.java` quản lý vòng đời hành trình (`ACTIVE ⇄ PAUSED → ENDED` hoặc `CANCELLED`), lưu danh sách di sản người dùng tự đánh dấu (`markedHeritages`), và đảm bảo quy tắc tối đa 1 hành trình đang mở trên mỗi người dùng (`POST /v1/visits`, `GET /v1/visits/active`, `POST /v1/visits/{id}/pause`, `/resume`).
  - *Phạm vi file:* `backend/.../modules/visit/**`.
  - *Đầu ra:* Toàn bộ API quản lý trạng thái hành trình chạy thật trên PostgreSQL.
- [ ] **[W5-TV5]** (Lương Quỳnh Anh) — **[BƯỚC 2 - SONG SONG]** (`Phụ thuộc: W4-TV6 validator & W5-TV2 visit`): Hiện thực hóa luồng tải ảnh 2 bước qua **Presigned PUT URL** (`POST /v1/media/uploads` sinh `objectKey` từ server có hạn 5–10 phút, và `POST /v1/media/uploads/{uploadId}/complete` kiểm tra checksum/magic bytes rồi phát sự kiện `PhotoUploaded` xử lý bất đồng bộ).
  - *Phạm vi file:* `backend/.../modules/media/service/MediaService.java`, `modules/media/controller/MediaController.java`, `android-client/.../data/repository/MediaRepository.java`.
  - *Đầu ra:* Upload ảnh trực tiếp từ Android lên MinIO S3 an toàn và trả về trạng thái `PROCESSING` $\rightarrow$ `READY`.
- [ ] **[W5-TV1]** (Nguyễn Minh Hoàng) — **[BƯỚC 2 - SONG SONG]** (`Độc lập trên nhánh AI`): Tinh chỉnh (fine-tune) mô hình phân loại địa danh Hà Nội và hiệu chỉnh ngưỡng tin cậy (`confidenceThreshold = 0.75`) để phân tách chính xác giữa địa danh hợp lệ (`RECOGNIZED`) và ảnh nhiễu/ngoài tập (`UNKNOWN`).
  - *Phạm vi file:* `ai-service/app/vision/classifier.py`.
  - *Đầu ra:* Trọng số mô hình Vision v1 cùng bảng đánh giá Precision/Recall/F1 và tỷ lệ bắt đúng `UNKNOWN`.
- [ ] **[W5-TV4]** (Nguyễn Minh Lương) — **[BƯỚC 2 - SONG SONG]** (`Phụ thuộc: W4-TV4`): Hiện thực hóa `retriever.py` truy xuất tư liệu lịch sử từ `pgvector` theo 2 chế độ: lọc theo `destinationId` (khi hỏi ở *Heritage Preview* trước chuyến đi) và lọc sâu theo `destinationId + landmarkId` (khi hỏi tại *Landmark Info* sau khi nhận diện ảnh).
  - *Phạm vi file:* `ai-service/app/rag/retriever.py`.
  - *Đầu ra:* Module truy xuất vector kết hợp lọc metadata trả về top-k đoạn tư liệu xác thực dưới 100ms.
- [ ] **[W5-TV6]** (Hà Thế Duy) — **[BƯỚC 3 - KIỂM THỬ PHÂN QUYỀN ĐI SAU]** (`Phụ thuộc: W5-TV2, W5-TV5`): Viết bộ kiểm thử tự động chống **IDOR / BOLA** đợt 1 trên các API `/v1/visits/**` và `/v1/media/uploads/**` (xác nhận User B không thể xem, tạm dừng, kết thúc hoặc tải ảnh vào hành trình của User A; trả về `404`/`403` chuẩn xác).
  - *Phạm vi file:* `backend/.../security/SecurityBaselineTests.java`.
  - *Đầu ra:* Bộ test phân quyền sở hữu tài nguyên đợt 1 xanh 100%.

---

### Tuần 6: Mở khóa Di sản bằng GPS Geofence, Kết nối AI Vision-to-RAG, 6 Mẫu Dấu Hành trình & Xóa EXIF

- [ ] **[W6-TV3]** (Lê Hiền Anh) — **[BƯỚC 1 - MỞ ĐƯỜNG GEOFENCE ARRIVAL]** (`Phụ thuộc: W5-TV3`): Hiện thực hóa API kiểm tra đến nơi `POST /v1/destinations/{destinationId}/arrival-checks` bằng PostGIS (`INSIDE` / `NEARBY` / `OUTSIDE` / `INDETERMINATE`), tuyệt đối không lưu tọa độ thô sau khi kiểm tra; hoàn thiện `HeritagePreviewFragment.java` và `HeritageExploreFragment.java` (trang nội khu mở ra ngay sau khi mở khóa di sản).
  - *Phạm vi file:* `backend/.../modules/destination/service/DestinationService.java`, `android-client/.../ui/explore/HeritagePreviewFragment.java`, `ui/explore/HeritageExploreFragment.java`, `location/GeofenceManager.java`.
  - *Đầu ra mở đường:* Kiểm tra đến nơi bằng GPS hoạt động hoàn chỉnh để kích hoạt mở khóa di sản trong hành trình.
- [ ] **[W6-TV2]** (Lê Bá Ninh) — **[BƯỚC 2 - SONG SONG]** (`Phụ thuộc: W6-TV3 arrival-checks`): Hiện thực hóa logic **tự động mở khóa di sản ngay khi GPS xác nhận đến nơi** (`unlockHeritageOnArrival` trong `VisitService.java`, đúng quy tắc §1 không phụ thuộc AI) và logic `RewardService.java` đối soát danh sách `markedHeritages` với `unlockedHeritages` khi kết thúc điểm thăm để cấp **Journey Stamp Badge + Random Gift**.
  - *Phạm vi file:* `backend/.../modules/visit/service/VisitService.java`, `modules/visit/service/RewardService.java`, `modules/visit/repository/RewardRepository.java`.
  - *Đầu ra:* Luồng mở khóa di sản bằng GPS và trao thưởng hoạt động chính xác, idempotent.
- [ ] **[W6-TV1]** (Nguyễn Minh Hoàng) — **[BƯỚC 2 - SONG SONG]** (`Phụ thuộc: W5-TV1`): Tích hợp mô hình phân loại địa danh đã huấn luyện vào endpoint thật `POST /internal/v1/vision/classify` của FastAPI; kiểm tra kích thước/định dạng ảnh đầu vào và trả về `RECOGNIZED` kèm `landmarkId` hoặc `UNKNOWN` khi dưới ngưỡng `0.75`.
  - *Phạm vi file:* `ai-service/app/api/routes_vision.py`, `ai-service/app/vision/classifier.py`.
  - *Đầu ra:* Dịch vụ Vision Inference chạy mô hình thật trong container `ai-service`.
- [ ] **[W6-TV4]** (Nguyễn Minh Lương) — **[BƯỚC 2 - SONG SONG]** (`Phụ thuộc: W5-TV4`): Hiện thực hóa `generator.py` và gắn vào endpoint thật `POST /internal/v1/rag/answer`: buộc mô hình chỉ trả lời từ các `knowledge_chunks` truy xuất được, đính kèm đầy đủ trích dẫn `sources` và từ chối lịch sự nếu câu hỏi nằm ngoài tư liệu lịch sử.
  - *Phạm vi file:* `ai-service/app/rag/generator.py`, `ai-service/app/api/routes_rag.py`.
  - *Đầu ra:* Dịch vụ Historical RAG chạy thật kèm kiểm chứng trích dẫn (`verify_citations`).
- [ ] **[W6-TV7]** (Lê Bá Tiệp) — **[BƯỚC 2 - GHÉP NỐI API AI BACKEND]** (`Phụ thuộc: W5-TV7, W6-TV1, W6-TV4`): Hiện thực hóa 2 endpoint công khai `POST /v1/ai/recognize-landmark` (kiểm tra quyền sở hữu `photoId` trước khi gửi sang Vision) và `POST /v1/ai/qa` (hỗ trợ cả 2 điểm chạm: *Heritage Preview* chỉ có `destinationId` và *On-site Landmark Info* có `destinationId + landmarkId`).
  - *Phạm vi file:* `backend/.../modules/recap/controller/AlbumRecapController.java`, `modules/recap/adapter/AiInferenceClientAdapter.java`.
  - *Đầu ra:* Hai API AI phía Backend hoạt động thông suốt với FastAPI và tự động fallback về nội dung tĩnh nếu AI tắt.
- [ ] **[W6-TV5]** (Lương Quỳnh Anh) — **[BƯỚC 2 - SONG SONG]** (`Phụ thuộc: W4-TV5`): Hoàn thiện cả 6 phong cách trong `JourneyStampRenderer.java` (`Minimal`, `Postcard`, `Film frame`, `Map stamp`, `Heritage stamp`, `Weather stamp`) trên Android và `WatermarkExportService.java` trên Backend (`POST /v1/photos/{photoId}/exports`), tuân thủ 5 mức `LocationPrecision`.
  - *Phạm vi file:* `android-client/.../util/JourneyStampRenderer.java`, `backend/.../modules/media/service/WatermarkExportService.java`.
  - *Đầu ra:* Công cụ đóng Dấu hành trình đủ 6 kiểu tem và 5 mức hiển thị vị trí.
- [ ] **[W6-TV6]** (Hà Thế Duy) — **[BƯỚC 3 - BẢO MẬT EXIF ĐI SAU]** (`Phụ thuộc: W5-TV5, W6-TV5`): Hiện thực hóa `ExifPrivacySanitizer.java` trên Android và bước làm sạch EXIF trên Backend (`stripExifAndGenerateDerivatives`), đảm bảo 100% ảnh thumbnail, preview, recap và ảnh xuất có dấu đều đã chuẩn hóa hướng xoay (orientation) và xóa sạch thẻ EXIF GPS/Serial thiết bị.
  - *Phạm vi file:* `android-client/.../util/ExifPrivacySanitizer.java`, `backend/.../security/SecurityBaselineTests.java`.
  - *Đầu ra:* Tiện ích xóa EXIF GPS và bài test tự động kiểm tra không còn trường GPS trong file ảnh xuất.

---

### Tuần 7: Bản đồ Tương tác, Phân nhánh Sự kiện Văn hóa & Giao diện AI trên Android (Sắp xếp theo thứ tự phụ thuộc)

- [ ] **[W7-TV3]** (Lê Hiền Anh) — **[BƯỚC 1 - MỞ ĐƯỜNG BẢN ĐỒ]** (`Phụ thuộc: W6-TV3`): Xây dựng `CombinedMapFragment.java` (hiển thị chung cả điểm Di sản và Sự kiện văn hóa trên một bản đồ) và phần hiển thị bản đồ khu vực trong `VisitMapFragment.java` với chấm xanh vị trí trực tiếp **chỉ tính toán trên thiết bị** (`startOnDeviceVisitLocationUpdates`).
  - *Phạm vi file:* `android-client/.../ui/map/CombinedMapFragment.java`, `location/GeofenceManager.java`, `viewmodel/ExploreViewModel.java`.
  - *Đầu ra mở đường:* Hai màn hình bản đồ tương tác mượt mà, bảo mật tuyệt đối tọa độ trực tiếp trên máy.
- [ ] **[W7-TV2]** (Lê Bá Ninh) — **[BƯỚC 2 - SONG SONG]** (`Phụ thuộc: W6-TV2, W7-TV3`): Hoàn thiện các hành động điều khiển hành trình trên `VisitMapFragment.java` (Chụp ảnh, Tạm dừng `Pause` về trang chủ, Kết thúc tại đây `Finish visit` mở `StampCollectionFragment.java` $\rightarrow$ `RewardDialogFragment.java`) và hoàn thiện phân nhánh Sự kiện văn hóa: sự kiện gắn di sản chuyển sang `HeritagePreviewFragment`, sự kiện độc lập mở `EventMemoryFragment.java`.
  - *Phạm vi file:* `android-client/.../ui/map/VisitMapFragment.java`, `ui/visit/StampCollectionFragment.java`, `ui/visit/RewardDialogFragment.java`, `ui/events/EventDetailFragment.java`, `ui/events/EventMemoryFragment.java`, `viewmodel/VisitViewModel.java`, `res/layout/fragment_visit_map.xml`.
  - *Đầu ra:* Toàn bộ luồng tham quan nội khu, kết thúc nhận thưởng và trang kỷ niệm sự kiện độc lập trên Android.
- [ ] **[W7-TV7]** (Lê Bá Tiệp) — **[BƯỚC 2 - SONG SONG]** (`Phụ thuộc: W6-TV7 API AI`): Xây dựng khung kết nối `AiChatViewModel.java` và luồng điều hướng AI trên Android: từ `CameraCaptureFragment` nhận kết quả `RECOGNIZED` $\rightarrow$ mở `LandmarkInfoFragment` $\rightarrow$ bấm `Ask AI` mở `HistoricalRagChatFragment` $\rightarrow$ chọn quay về `VisitMapFragment` hoặc `HeritageExploreFragment` (nếu `UNKNOWN` thì quay thẳng về `VisitMapFragment`).
  - *Phạm vi file:* `android-client/.../viewmodel/AiChatViewModel.java`, `data/api/AiApi.java`.
  - *Đầu ra:* Luồng điều hướng Vision-to-RAG trên Android tuân thủ nghiêm ngặt quy tắc §3 (không mở chat trực tiếp từ Visit Map).
- [ ] **[W7-TV1]** (Nguyễn Minh Hoàng) — **[BƯỚC 2 - SONG SONG]** (`Phụ thuộc: W7-TV7 AiChatViewModel`): Hoàn thiện giao diện `LandmarkInfoFragment.java` hiển thị thông tin địa danh vừa nhận diện kèm độ tin cậy; đồng thời bổ sung dữ liệu hard-negatives (ảnh chụp đêm, góc khuất, trời mưa) để nâng cấp mô hình Vision v2.
  - *Phạm vi file:* `android-client/.../ui/ai/LandmarkInfoFragment.java`, `ai-service/app/vision/classifier.py`.
  - *Đầu ra:* Màn hình `LandmarkInfoFragment` hoàn chỉnh và bảng Confusion Matrix của mô hình Vision v2.
- [ ] **[W7-TV4]** (Nguyễn Minh Lương) — **[BƯỚC 2 - SONG SONG]** (`Phụ thuộc: W7-TV7 AiChatViewModel`): Hoàn thiện giao diện `HistoricalRagChatFragment.java` hiển thị câu trả lời kèm danh sách thẻ trích dẫn nguồn (`sources`) bấm xem được; mở rộng kho `knowledge_chunks` bao phủ toàn bộ các di sản và địa danh.
  - *Phạm vi file:* `android-client/.../ui/ai/HistoricalRagChatFragment.java`, `ai-service/app/rag/generator.py`.
  - *Đầu ra:* Giao diện hỏi đáp lịch sử RAG có trích dẫn trên Android và bộ lọc chống Prompt Injection.
- [ ] **[W7-TV5]** (Lương Quỳnh Anh) — **[BƯỚC 2 - SONG SONG]** (`Phụ thuộc: W6-TV5`): Hiện thực hóa cập nhật metadata ảnh (`PATCH /v1/photos/{photoId}`), tạo biến thể chỉnh sửa không phá hủy (`POST /v1/photos/{photoId}/edits`) và phối hợp lưu ảnh kỷ niệm sự kiện từ `EventMemoryFragment` vào Nhật ký hành trình.
  - *Phạm vi file:* `backend/.../modules/media/**`, `android-client/.../ui/camera/PhotoEditFragment.java`, `viewmodel/JournalViewModel.java`.
  - *Đầu ra:* Luồng chỉnh sửa ảnh không phá hủy và xuất ảnh đóng dấu đồng bộ giữa Android và Backend.
- [ ] **[W7-TV6]** (Hà Thế Duy) — **[BƯỚC 3 - KIỂM THỬ BẢO MẬT ĐỢT 2]** (`Phụ thuộc: W6-TV7, W7-TV5`): Viết bộ kiểm thử bảo mật đợt 2 cho module `media`, `recap` và các endpoint `/v1/ai/**` (kiểm tra chặn User B truyền `photoId` của User A vào `/v1/ai/recognize-landmark`, kiểm tra Rate Limiting `429 Too Many Requests` trên Auth/AI/Upload).
  - *Phạm vi file:* `backend/.../security/SecurityBaselineTests.java`.
  - *Đầu ra:* Bộ lọc Rate Limit và suite test bảo mật Media & AI xanh 100%.

---

### Tuần 8: Nhật ký Hành trình (Travel Journal), Recap Bất đồng bộ & LocationSimulator (Chốt Giai đoạn 2)

- [ ] **[W8-TV5]** (Lương Quỳnh Anh) — **[BƯỚC 1 - MỞ ĐƯỜNG JOURNAL & RECAP]** (`Phụ thuộc: W6-TV2 VisitEnded, W7-TV5`): Hiện thực hóa `RecapService.java` lắng nghe sự kiện kết thúc chuyến đi để tổng hợp bất đồng bộ `DESTINATION` Recap và `MONTHLY` Recap (`/v1/albums`, `/v1/recaps`); xây dựng màn hình `TravelJournalFragment.java` và `RecapDetailFragment.java` (xem lại chuyến đi trước, chi tiết ảnh và tải ảnh đã xóa EXIF về máy).
  - *Phạm vi file:* `backend/.../modules/recap/service/RecapService.java`, `android-client/.../ui/journal/TravelJournalFragment.java`, `ui/journal/RecapDetailFragment.java`, `res/layout/fragment_travel_journal.xml`.
  - *Đầu ra:* Tính năng Nhật ký hành trình (Album) và tạo Recap theo địa danh/tháng hoạt động hoàn chỉnh.
- [ ] **[W8-TV3]** (Lê Hiền Anh) — **[BƯỚC 2 - SONG SONG]** (`Phụ thuộc: W7-TV3`): Xây dựng công cụ `LocationSimulator.java` trên Android (cho phép bấm nút giả lập bước vào vùng Geofence của Văn Miếu, Hoàng Thành, Hồ Gươm và di chuyển giữa các địa danh con ngay trên máy chiếu phòng bảo vệ đồ án mà không cần đi thực địa).
  - *Phạm vi file:* `android-client/.../location/LocationSimulator.java`.
  - *Đầu ra:* Bộ giả lập vị trí GPS hoạt động mượt mà phục vụ kiểm thử E2E tuần 9 và demo bảo vệ đồ án.
- [ ] **[W8-TV1]** (Nguyễn Minh Hoàng) — **[BƯỚC 2 - SONG SONG]** (`Độc lập trên nhánh AI`): Tối ưu hóa tốc độ suy luận của Model 1 trong `ai-service/` (xuất mô hình sang ONNX / TorchScript, nạp sẵn mô hình vào RAM khi khởi động container — warm-up) để đạt độ trễ suy luận trung bình $< 300\text{ ms}$ trên CPU.
  - *Phạm vi file:* `ai-service/app/vision/classifier.py`.
  - *Đầu ra:* Dịch vụ Vision tối ưu hóa bộ nhớ và tốc độ phản hồi thực tế.
- [ ] **[W8-TV4]** (Nguyễn Minh Lương) — **[BƯỚC 2 - SONG SONG]** (`Độc lập trên nhánh AI`): Xây dựng bộ kiểm định chất lượng RAG định lượng (50 câu hỏi lịch sử có đáp án chuẩn + 20 câu hỏi ngoài phạm vi) để đo các chỉ số học thuật: Context Recall/Hit@K, Faithfulness (chống ảo giác) và Citation Precision.
  - *Phạm vi file:* `ai-service/tests/test_inference_contracts.py`.
  - *Đầu ra:* Bộ benchmark đánh giá chất lượng Historical RAG kèm bảng số liệu thực nghiệm.
- [ ] **[W8-TV2]** (Lê Bá Ninh) — **[BƯỚC 2 - SONG SONG]** (`Phụ thuộc: W7-TV2`): Hoàn thiện tác vụ lập lịch thu thập sự kiện tự động (`fetchAndParseScheduledSources` trong `EventService.java`, lưu trạng thái `PENDING_REVIEW` chờ Admin duyệt qua `POST /v1/admin/events/{id}/publish`) và đồng bộ bộ nhớ đệm ngoại tuyến (`AppDatabase.java`) cho hành trình đang tạm dừng.
  - *Phạm vi file:* `backend/.../modules/event/service/EventService.java`, `android-client/.../data/repository/VisitRepository.java`.
  - *Đầu ra:* Luồng duyệt xuất bản sự kiện của Admin và lưu trữ an toàn trạng thái `PAUSED` của chuyến đi.
- [ ] **[W8-TV7]** (Lê Bá Tiệp) — **[BƯỚC 2 - SONG SONG]** (`Phụ thuộc: W7-TV7`): Kiểm thử và hoàn thiện cơ chế **Static Fallback** toàn diện giữa Backend và Android: khi container `ai-service` (`:8001`) tắt hoặc quá tải, trang di sản và `/v1/ai/qa` tự động hiển thị nội dung lịch sử tĩnh (`isAiGenerated: false`) mà không làm gián đoạn trải nghiệm người dùng.
  - *Phạm vi file:* `backend/.../modules/recap/adapter/AiInferenceClientAdapter.java`, `android-client/.../viewmodel/AiChatViewModel.java`.
  - *Đầu ra:* Hệ thống vẫn hoạt động trơn tru 100% ngay cả khi tắt container AI.
- [ ] **[W8-TV6]** (Hà Thế Duy) — **[BƯỚC 3 - KIỂM TOÁN LOG ĐI SAU]** (`Phụ thuộc: Toàn bộ API Giai đoạn 2`): Rà soát và cấu hình che giấu dữ liệu nhạy cảm trên toàn bộ log của Spring Boot, FastAPI và Android Logcat (cấm ghi tọa độ `latitude`/`longitude`, `accessToken`, `refreshToken`, `presignedUrl`); viết bài test tự động kiểm tra quy tắc log redaction.
  - *Phạm vi file:* `backend/src/main/resources/application.yml`, `backend/.../security/SecurityBaselineTests.java`.
  - *Đầu ra:* Cấu hình log an toàn và bài test xác nhận không rò rỉ tọa độ/token trong log.

---

## Giai đoạn 3 — Tháng 3 (Tuần 9 – Tuần 12): Tích hợp End-to-End, Kiểm thử Bảo mật (Pentest/EXIF/IDOR), Tối ưu & Bảo vệ Đồ án

> **Mục tiêu Giai đoạn 3:** Ráp nối toàn bộ các luồng nghiệp vụ từ Android $\rightarrow$ Spring Boot Modulith $\rightarrow$ PostgreSQL/MinIO/FastAPI; thực hiện kiểm thử xâm nhập (Pentest) theo đúng checklist [`docs/07_TESTING_AND_BUG_TRIAGE.md`](docs/07_TESTING_AND_BUG_TRIAGE.md); tối ưu hiệu năng giao diện và chuẩn bị kịch bản demo trực tiếp phục vụ bảo vệ đồ án.

### Tuần 9: Tích hợp End-to-End (E2E) Toàn hệ thống theo 3 Chặng User Flow

- [ ] **[W9-TV7]** (Lê Bá Tiệp) — **[BƯỚC 1 - MỞ ĐƯỜNG MÔI TRƯỜNG TÍCH HỢP]** (`Đầu tuần 9`): Đồng bộ cấu hình `docker-compose.yml` chạy đồng thời cả PostgreSQL 16, MinIO, Backend (`:8080`) và FastAPI `ai-service` (`:8001`); rà soát cơ chế tự động làm mới JWT Access Token trên Android khi gặp `401`.
  - *Đầu ra mở đường:* Môi trường tích hợp toàn phần chạy ổn định để cả nhóm test E2E trên máy thật/Emulator.
- [ ] **[W9-TV3]** (Lê Hiền Anh) — **[BƯỚC 2 - TÍCH HỢP CHẶNG 1 & 2A]** (`Phụ thuộc: W9-TV7`): Kiểm thử và tinh chỉnh E2E luồng **Khám phá & Đến nơi**: `ExploreHomeFragment` $\rightarrow$ `CombinedMapFragment` $\rightarrow$ `HeritagePreviewFragment` $\rightarrow$ Đánh dấu di sản $\rightarrow$ Giả lập GPS bằng `LocationSimulator` $\rightarrow$ Mở khóa di sản tự động $\rightarrow$ `HeritageExploreFragment` $\rightarrow$ `VisitMapFragment`.
  - *Đầu ra:* Chặng Khám phá, Bản đồ và Mở khóa bằng GPS hoạt động liền mạch không lỗi chuyển màn hình.
- [ ] **[W9-TV1]** (Nguyễn Minh Hoàng) — **[BƯỚC 2 - TÍCH HỢP CHẶNG 2B VISION]** (`Phụ thuộc: W9-TV3`): Phối hợp kiểm thử E2E luồng chụp ảnh trên `VisitMapFragment` $\rightarrow$ `CameraCaptureFragment` $\rightarrow$ tải ảnh lên MinIO $\rightarrow$ gọi `/v1/ai/recognize-landmark` $\rightarrow$ kiểm tra cả 2 nhánh `RECOGNIZED` (mở `LandmarkInfoFragment`) và `UNKNOWN` (quay về `VisitMapFragment`).
  - *Đầu ra:* Luồng nhận diện địa danh từ ảnh chụp thực tế trên Android hoạt động chính xác.
- [ ] **[W9-TV4]** (Nguyễn Minh Lương) — **[BƯỚC 2 - TÍCH HỢP CHẶNG 2C RAG]** (`Phụ thuộc: W9-TV1`): Phối hợp kiểm thử E2E cả 2 điểm chạm của **Historical RAG**: hỏi đáp tổng quan tại `HeritagePreviewFragment` trước khi đi và hỏi đáp chuyên sâu theo `landmarkId` tại `LandmarkInfoFragment` $\rightarrow$ `HistoricalRagChatFragment` $\rightarrow$ điều hướng quay về `VisitMapFragment` hoặc `HeritageExploreFragment`.
  - *Đầu ra:* Luồng hỏi đáp lịch sử kèm trích dẫn nguồn hoạt động chuẩn xác ở cả 2 điểm chạm.
- [ ] **[W9-TV2]** (Lê Bá Ninh) — **[BƯỚC 2 - TÍCH HỢP CHẶNG 2D VISIT/EVENT/REWARD]** (`Phụ thuộc: W9-TV3`): Kiểm thử E2E luồng `Pause` $\rightarrow$ `Resume trip` từ trang chủ, luồng phân nhánh Sự kiện văn hóa (có gắn di sản vs `EventMemoryFragment`), và luồng `Finish visit` $\rightarrow$ `StampCollectionFragment` $\rightarrow$ đối soát mở khóa đủ di sản đã đánh dấu để nhận `Badge` + `Random Gift`.
  - *Đầu ra:* Vòng đời hành trình, sự kiện và trao thưởng hoạt động không lỗi trạng thái.
- [ ] **[W9-TV5]** (Lương Quỳnh Anh) — **[BƯỚC 2 - TÍCH HỢP CHẶNG 3 JOURNAL/STAMP]** (`Phụ thuộc: W9-TV2`): Kiểm thử E2E luồng chỉnh sửa ảnh không phá hủy, đóng dấu 6 phong cách `JourneyStamp`, kết thúc hành trình cập nhật vào `TravelJournalFragment`, xem `Destination Recap` / `Monthly Recap` và tải ảnh về thiết bị.
  - *Đầu ra:* Chặng Chỉnh sửa ảnh, Dấu hành trình, Nhật ký và Recap hoạt động hoàn chỉnh.
- [ ] **[W9-TV6]** (Hà Thế Duy) — **[BƯỚC 3 - KIỂM THỬ TÍCH HỢP TỰ ĐỘNG CUỐI TUẦN]** (`Phụ thuộc: W9-TV1..TV5`): Chạy bộ kiểm thử tích hợp tự động toàn luồng (Integration Tests với Testcontainers PostgreSQL + MinIO) mô phỏng xuyên suốt từ đăng ký tài khoản đến xuất ảnh có dấu và tạo Recap; phân loại mọi lỗi phát sinh theo quy trình Bug Triage (`P0`–`P3`).
  - *Đầu ra:* Báo cáo kết quả kiểm thử tích hợp E2E tuần 9 và danh sách lỗi cần đóng.

---

### Tuần 10: Kiểm thử Bảo mật Chuyên sâu (Pentest), Quyền Riêng tư Vị trí & Khả năng Chịu lỗi

- [ ] **[W10-TV6]** (Hà Thế Duy) — **[BƯỚC 1 - CHỦ TRÌ PENTEST ĐẦU TUẦN]** (`Phụ thuộc: W9 E2E`): Thực hiện đợt Pentest nội bộ toàn diện trên 5 luồng Threat Model theo đúng tiêu chí nghiệm thu tại [`docs/07_TESTING_AND_BUG_TRIAGE.md`](docs/07_TESTING_AND_BUG_TRIAGE.md) (Token replay, Geofence proof replay, Malicious upload/wrong key, Cross-user IDOR/BOLA trên mọi endpoint, EXIF GPS inspection).
  - *Đầu ra mở đường:* Biên bản kiểm thử bảo mật chi tiết chỉ rõ các điểm cần gia cố (nếu có) cho từng thành viên.
- [ ] **[W10-TV1]** (Nguyễn Minh Hoàng) — **[BƯỚC 2 - SONG SONG]**: Kiểm thử độ bền (Stress & Adversarial Testing) cho `ai-service` Vision: gửi ảnh bị cắt cụt, ảnh kích thước lớn, ảnh nhiễu để đảm bảo FastAPI xử lý ngoại lệ an toàn và không bị rò rỉ bộ nhớ (OOM).
  - *Đầu ra:* Bản vá bảo vệ tài nguyên và báo cáo độ bền dịch vụ Vision.
- [ ] **[W10-TV4]** (Nguyễn Minh Lương) — **[BƯỚC 2 - SONG SONG]**: Thực hiện Red-Teaming trên trợ lý Historical RAG với 30 kịch bản câu hỏi bẫy (Prompt Injection, yêu cầu bịa đặt thông tin lịch sử, câu hỏi gây nhầm lẫn niên đại giữa các di tích) để đảm bảo tỷ lệ từ chối/chặn ảo giác đạt 100%.
  - *Đầu ra:* Báo cáo kiểm thử Red-Team RAG và cấu hình guardrail hoàn thiện.
- [ ] **[W10-TV2]** (Lê Bá Ninh) — **[BƯỚC 2 - SONG SONG]**: Kiểm định an toàn cho `EventSourceParserAdapter`: chặn SSRF (chỉ cho phép gọi HTTPS tới danh sách tên miền chính thống đã duyệt, chặn dải IP nội bộ), làm sạch HTML/XSS trước khi lưu `PENDING_REVIEW`, và kiểm tra tính idempotent khi nhận thưởng.
  - *Đầu ra:* Bộ lọc an toàn cho trình thu thập sự kiện và bài test chống nhận thưởng trùng lặp.
- [ ] **[W10-TV3]** (Lê Hiền Anh) — **[BƯỚC 2 - SONG SONG]**: Kiểm chứng thực thi quyền riêng tư vị trí trên Android & Backend: xác nhận tọa độ chính xác chỉ dùng để tính sự hiện diện trong vùng Geofence rồi hủy ngay, và `VisitMapFragment` không phát sinh bất kỳ request mạng nào gửi vị trí trực tiếp về server.
  - *Đầu ra:* Biên bản kiểm chứng quyền riêng tư vị trí tuân thủ nguyên tắc Presence-Only.
- [ ] **[W10-TV5]** (Lương Quỳnh Anh) — **[BƯỚC 2 - SONG SONG]**: Kiểm chứng toàn diện 5 mức `LocationPrecision` (`NONE`, `DESTINATION`, `DISTRICT`, `APPROXIMATE`, `EXACT`) trên cả ảnh kết xuất từ `JourneyStampRenderer`, ảnh tải về từ `TravelJournalFragment` (kiểm tra bằng trình đọc EXIF) và JSON DTO.
  - *Đầu ra:* Bảng nghiệm thu tính năng bảo vệ vị trí và xóa sạch EXIF GPS trên mọi biến thể ảnh.
- [ ] **[W10-TV7]** (Lê Bá Tiệp) — **[BƯỚC 3 - ĐÓNG LỖ HỔNG & CÔ LẬP MẠNG CUỐI TUẦN]** (`Phụ thuộc: W10-TV6`): Phối hợp đóng toàn bộ cảnh báo từ đợt Pentest của TV6; cấu hình mạng nội bộ trong `docker-compose.yml` sao cho container `ai-service` (`:8001`) chỉ giao tiếp với `backend`, không mở cổng trực tiếp ra ngoài internet công cộng.
  - *Đầu ra:* Xác nhận không còn bất kỳ lỗi bảo mật mức `CRITICAL` hoặc `HIGH` nào tồn đọng.

---

### Tuần 11: Tối ưu Hiệu năng, Hoàn thiện UI/UX & Đóng gói Dữ liệu Trình diễn (Demo Seed)

- [ ] **[W11-TV7]** (Lê Bá Tiệp) — **[BƯỚC 1 - MỞ ĐƯỜNG DỮ LIỆU DEMO]** (`Đầu tuần 11`): Viết script khởi tạo môi trường trình diễn một chạm (`docker compose up -d`) nạp sẵn tài khoản mẫu, di sản, sự kiện văn hóa, hành trình mẫu và kho tư liệu lịch sử để bất kỳ máy tính nào trong nhóm cũng dựng lại được hệ thống demo trong 2 phút.
  - *Đầu ra mở đường:* Bộ dữ liệu trình diễn chuẩn hóa cho cả 7 thành viên luyện tập demo.
- [ ] **[W11-TV3]** (Lê Hiền Anh) — **[BƯỚC 2 - SONG SONG]**: Tối ưu hóa hiệu năng hiển thị bản đồ, tải ảnh bìa di sản (Glide caching) và thiết lập sẵn kịch bản tọa độ mẫu một chạm trên `LocationSimulator.java` đi qua Văn Miếu – Quốc Tử Giám và Hoàng Thành Thăng Long.
  - *Đầu ra:* Ứng dụng cuộn mượt 60fps và kịch bản giả lập GPS sẵn sàng cho buổi bảo vệ.
- [ ] **[W11-TV5]** (Lương Quỳnh Anh) — **[BƯỚC 2 - SONG SONG]**: Tinh chỉnh thẩm mỹ chữ viết (typography), bố cục và màu sắc của cả 6 phong cách **Dấu hành trình (`Journey Stamp`)**; tối ưu quản lý bộ nhớ `Bitmap` trên Android để không xảy ra lỗi `OutOfMemoryError`.
  - *Đầu ra:* Bộ mẫu Dấu hành trình sắc nét và trình sửa ảnh hoạt động mượt trên thiết bị thật.
- [ ] **[W11-TV2]** (Lê Bá Ninh) — **[BƯỚC 2 - SONG SONG]**: Tinh chỉnh hiệu ứng mở khóa di sản, giao diện Bộ sưu tập Tem (`StampCollectionFragment`), hộp thoại nhận Huy hiệu & Quà ngẫu nhiên (`RewardDialogFragment`) và trang Kỷ niệm sự kiện (`EventMemoryFragment`).
  - *Đầu ra:* Trải nghiệm hoàn thành hành trình và nhận thưởng trực quan, sinh động.
- [ ] **[W11-TV1]** (Nguyễn Minh Hoàng) — **[BƯỚC 2 - SONG SONG]**: Đóng gói trọng số mô hình Vision cuối cùng vào container `ai-service`, hoàn thiện chương báo cáo học thuật phần Computer Vision (kiến trúc mô hình, quá trình huấn luyện, bảng F1-score, Confusion Matrix, đồ thị ngưỡng `UNKNOWN`).
  - *Đầu ra:* Container Vision khởi chạy tức thì và chương báo cáo học thuật phần Computer Vision.
- [ ] **[W11-TV4]** (Nguyễn Minh Lương) — **[BƯỚC 2 - SONG SONG]**: Rà soát độ chính xác lịch sử của toàn bộ `knowledge_chunks`, hoàn thiện chương báo cáo học thuật phần Historical RAG (chiến lược Semantic Chunking, so sánh Embedding, chỉ mục HNSW `pgvector`, bảng điểm Faithfulness & Citation Precision).
  - *Đầu ra:* Kho tri thức lịch sử chuẩn xác và chương báo cáo học thuật phần Historical RAG.
- [ ] **[W11-TV6]** (Hà Thế Duy) — **[BƯỚC 3 - NGHIỆM THU CHẤT LƯỢNG CUỐI TUẦN]**: Chạy toàn bộ suite kiểm thử tự động (Unit + Spring Modulith `verify()` + Testcontainers Integration + Security Baseline + AI Contract Tests), xuất báo cáo nghiệm thu bảo mật & QA cuối cùng.
  - *Đầu ra:* Chứng nhận 100% bài kiểm thử đạt trạng thái Xanh (Green Build) và báo cáo Security/QA hoàn chỉnh.

---

### Tuần 12: Tổng duyệt Kịch bản Demo, Đóng băng Mã nguồn & Bảo vệ Đồ án

- [ ] **[W12-TV7]** (Lê Bá Tiệp) — **[BƯỚC 1 - ĐÓNG BĂNG MÃ NGUỒN ĐẦU TUẦN]**: Kiểm tra lần cuối toàn bộ tài liệu kỹ thuật trong `docs/`, xuất bản dựng APK ổn định, gắn thẻ phát hành (`git tag v1.0.0-mvp`) và điều phối kịch bản thuyết trình nối tiếp nhau của 7 thành viên.
  - *Đầu ra:* Bản phát hành `v1.0.0-mvp` cố định, không sửa code phút chót gây lỗi demo.
- [ ] **[W12-TV3]** (Lê Hiền Anh) — **[BƯỚC 2 - TỔNG DUYỆT PHẦN 1 DEMO]**: Tổng duyệt phần trình bày & live demo luồng **Khám phá Hà Nội (`Explore Hanoi home`), Bản đồ chung, Xem trước di sản (`Heritage preview`), Kiểm tra đến nơi bằng PostGIS Geofence (`LocationSimulator`) và Mở khóa `Heritage explore page` $\rightarrow$ `Visit map`**.
  - *Đầu ra:* Slide và phần demo trực tiếp mảng Khám phá, Bản đồ & Geofencing.
- [ ] **[W12-TV1]** (Nguyễn Minh Hoàng) — **[BƯỚC 2 - TỔNG DUYỆT PHẦN 2 DEMO]**: Tổng duyệt phần trình bày học thuật & live demo **Model 1 Computer Vision**: chụp ảnh tại di tích nhận diện đúng địa danh (`RECOGNIZED` $\rightarrow$ mở `LandmarkInfoFragment`) và thử ảnh ngoài tập trả về `UNKNOWN` an toàn.
  - *Đầu ra:* Slide học thuật và phần demo trực tiếp mảng AI Computer Vision.
- [ ] **[W12-TV4]** (Nguyễn Minh Lương) — **[BƯỚC 2 - TỔNG DUYỆT PHẦN 3 DEMO]**: Tổng duyệt phần trình bày học thuật & live demo **Model 2 Historical RAG**: hỏi đáp tại `Heritage preview` và tại `Landmark info` trả lời kèm trích dẫn nguồn xác thực, chống ảo giác và trình diễn chế độ **Static Fallback** khi tắt AI.
  - *Đầu ra:* Slide học thuật và phần demo trực tiếp mảng AI Historical RAG.
- [ ] **[W12-TV5]** (Lương Quỳnh Anh) — **[BƯỚC 2 - TỔNG DUYỆT PHẦN 4 DEMO]**: Tổng duyệt phần trình bày & live demo **Chỉnh sửa ảnh không phá hủy, đổi trực tiếp 6 phong cách Dấu hành trình (`Journey Stamp`), 5 mức hiển thị vị trí (`LocationPrecision`), xem Nhật ký hành trình (`Travel Journal`) & Recap**.
  - *Đầu ra:* Slide và phần demo trực tiếp mảng Camera, Dấu hành trình, Album & Recap.
- [ ] **[W12-TV2]** (Lê Bá Ninh) — **[BƯỚC 2 - TỔNG DUYỆT PHẦN 5 DEMO]**: Tổng duyệt phần trình bày & live demo **Vòng đời hành trình (`ACTIVE/PAUSED/ENDED`), Bộ sưu tập Tem (`StampCollection`), đối soát trao thưởng (`Badge` + `Random Gift`), trình thu thập Sự kiện văn hóa tất định và `Event Memory Page`**.
  - *Đầu ra:* Slide và phần demo trực tiếp mảng Hành trình, Sự kiện & Phần thưởng.
- [ ] **[W12-TV6]** (Hà Thế Duy) — **[BƯỚC 3 - TỔNG DUYỆT BẢO MẬT & QA CHỐT HỘI ĐỒNG]**: Tổng duyệt phần trình bày về **Kiến trúc Spring Modulith, Threat Model 5 luồng, minh chứng chống IDOR/BOLA, Presigned Upload Hardening và kiểm chứng xóa sạch EXIF GPS** trước hội đồng chấm đồ án.
  - *Đầu ra:* Slide và bộ minh chứng thực nghiệm Cyber Security & QA bảo vệ thành công đồ án.
