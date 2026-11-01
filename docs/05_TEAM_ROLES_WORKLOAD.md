# 05 — Phân công Vai trò & Cân bằng Khối lượng Công việc (Team Roles & Equal Workload)

Tài liệu này chuẩn hóa cơ cấu tổ chức của nhóm dự án **Hanoi Lens** (7 thành viên chính thức) theo mô hình phòng phát triển sản phẩm tại doanh nghiệp CNTT, ánh xạ trực tiếp từng thành viên vào các module trong kiến trúc **Spring Boot Modulith + Android Native (Java/XML) + Python FastAPI AI** ([02_ARCHITECTURE.md](02_ARCHITECTURE.md)), đồng thời thiết lập ma trận RACI và bảng cân bằng khối lượng công việc trong 3 tháng.

---

## 1. Danh sách Thành viên & Ánh xạ Kiến trúc (Team Roster & Module Mapping)

| Mã TV | Họ và tên | Mã SV | Chức danh chuẩn hóa (IT Role) | Module Backend sở hữu | Thành phần Android / AI sở hữu |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **TV1** | **Nguyễn Minh Hoàng** | `2410315` | **AI Engineer (Computer Vision)** | Phối hợp `media` / AI contract | **Model 1 (Landmark Classifier)**, FastAPI `/internal/v1/vision/classify`, UI nhận diện trên Android |
| **TV2** | **Lê Bá Ninh** | `23BA14224` | **Backend & Mobile Engineer (Visits, Events & Rewards)** | `modules.visit`, `modules.event` | Vòng đời hành trình, Đánh dấu/Mở khóa, Trao thưởng Badge+Gift, Crawler sự kiện, UI Sự kiện & Stamp/Reward |
| **TV3** | **Lê Hiền Anh** | `2410042` | **Geospatial Backend & Mobile Engineer (Discovery, Map & Arrival)** | `modules.destination` | PostGIS spatial queries, Geofence arrival checks, `ExploreHomeFragment`, `HeritagePreviewFragment`, `HeritageExploreFragment`, `CombinedMapFragment`, `VisitMapFragment`, `GeofenceManager`, `LocationSimulator` |
| **TV4** | **Nguyễn Minh Lương** | `23BA14184` | **AI Engineer (Historical RAG & Knowledge Base)** | Kho tri thức `KnowledgeChunk` (`pgvector`) | **Model 2 (Historical RAG)**, Pipeline chunking + embedding, FastAPI `/internal/v1/rag/answer`, kịch bản fallback tĩnh |
| **TV5** | **Lương Quỳnh Anh** | `23BA14008` | **Media & Mobile Engineer (Camera, Album, Stamp & Recap)** | `modules.media`, `modules.recap` | Presigned S3 upload, xử lý ảnh không phá hủy, xuất ảnh gắn watermark, Album & Recap, Android CameraX, `JourneyStampRenderer`, `TravelJournalFragment` |
| **TV6** | **Hà Thế Duy** | `2410207` | **Cyber Security & QA Lead** | Kiểm định toàn bộ 7 modules & `shared` | Threat Modeling 5 luồng, chống IDOR/BOLA, kiểm định Presigned Upload, tự động xóa EXIF GPS, `ModularityTests` & CI pipeline |
| **TV7** | **Lê Bá Tiệp** | `23BA14282` | **Platform/Identity Lead & AI Integration Engineer** | `modules.identity`, `modules.profile`, AI Adapter | Argon2id, JWT + Rotating Refresh Token, `LocationPrecision`, `AiInferenceClientAdapter`, API `/v1/ai/*`, Android Auth/Profile/AI Chat UI |

---

## 2. Đặc tả Trách nhiệm Chi tiết Từng Thành viên (Detailed Role Specifications)

### 2.1. TV1 — Nguyễn Minh Hoàng (`2410315`) — *AI Engineer (Computer Vision)*
- **Trách nhiệm cốt lõi:** Phụ trách toàn diện **Model 1 — Landmark Image Classification** trong hệ thống AI.
- **Công việc cụ thể:**
  - Thu thập, làm sạch, tăng cường dữ liệu (data augmentation) và gán nhãn bộ dữ liệu ảnh thực tế các di sản và hạng mục kiến trúc (`landmarks`) tiêu biểu tại Hà Nội (các góc chụp ngày/đêm, bị che khuất một phần, thời tiết khác nhau).
  - Huấn luyện, tinh chỉnh (fine-tune) mô hình phân lớp ảnh (PyTorch / ViT / ConvNeXt / YOLO-cls) và hiệu chỉnh ngưỡng tin cậy (`confidence threshold`) để trả về `UNKNOWN` an toàn khi ảnh mờ, lạc đề hoặc không thuộc di sản, tuyệt đối tránh nhận diện sai.
  - Xây dựng và tối ưu hóa endpoint nội bộ `POST /internal/v1/vision/classify` trên **Python FastAPI**, đảm bảo thời gian phản hồi nhanh và định dạng JSON chuẩn.
  - Phối hợp phát triển giao diện kết quả nhận diện địa danh (`LandmarkInfoFragment` / màn hình hiển thị kết quả nhận diện và trạng thái `UNKNOWN`) trên ứng dụng Android.

### 2.2. TV2 — Lê Bá Ninh (`23BA14224`) — *Backend & Mobile Engineer (Visits, Events & Rewards)*
- **Trách nhiệm cốt lõi:** Phụ trách 2 module nghiệp vụ trung tâm trên Spring Boot là **`modules.visit`** và **`modules.event`**, cùng giao diện Sự kiện & Phần thưởng trên Android.
- **Công việc cụ thể:**
  - **Module `visit`:** Thiết kế và hiện thực hóa máy trạng thái hành trình (`ACTIVE ⇄ PAUSED → ENDED`), ràng buộc duy nhất 1 hành trình `ACTIVE` mỗi người dùng, chức năng đánh dấu di sản (`mark`), mở khóa tự động khi nhận kết quả đến nơi (`unlock`), xem bộ sưu tập Tem (`Stamp collection`) và thuật toán đối soát `Marked vs Unlocked` để trao **Huy hiệu Dấu hành trình (`Journey Stamp badge`) + quà ngẫu nhiên (`random gift`)** có chống trùng lặp (`Idempotency-Key`).
  - **Module `event`:** Xây dựng bộ thu thập dữ liệu sự kiện văn hóa theo lịch trình (`@Scheduled`) từ các cổng thông tin chính thống được phép (allowlisted), bóc tách dữ liệu tất định (**deterministic parsers** bằng JSON-LD, RSS, CSS Selector và quy tắc ngày tháng tiếng Việt — không dùng AI), khử trùng lặp và lưu ở trạng thái `PENDING_REVIEW` chờ duyệt.
  - **Android UI:** Phát triển màn hình danh sách/chi tiết sự kiện văn hóa (`EventsFragment`, `EventDetailFragment`), trang kỷ niệm sự kiện không gắn di sản (`EventMemoryFragment`) và màn hình Bộ sưu tập Tem / Nhận thưởng (`StampCollectionFragment`, `RewardDialogFragment`).

### 2.3. TV3 — Lê Hiền Anh (`2410042`) — *Geospatial Backend & Mobile Engineer (Discovery, Map & Arrival)*
- **Trách nhiệm cốt lõi:** Phụ trách **`modules.destination`** (PostGIS) trên Backend và toàn bộ luồng Khám phá, Bản đồ, Định vị GPS trên Android.
- **Công việc cụ thể:**
  - **Module `destination`:** Thiết kế lược đồ không gian PostGIS (SRID 4326) cho ranh giới di sản (`Polygon`/`MultiPolygon`) và tọa độ địa danh (`Point`), tối ưu chỉ mục không gian GiST, xây dựng API tìm kiếm di sản gần đây (`ST_DWithin`) và API kiểm tra đến nơi (`POST /v1/destinations/{id}/arrival-checks` dùng `ST_Contains`/`ST_Distance`) đảm bảo không lưu trữ tọa độ thô của người dùng.
  - **Dữ liệu di sản:** Chuẩn hóa dữ liệu tọa độ ranh giới thực tế và nội dung giới thiệu cho các khu di sản & địa danh Hà Nội.
  - **Android UI & Location:** Xây dựng rõ ràng 2 trang khám phá khác nhau: **`ExploreHomeFragment`** (trang chủ toàn ứng dụng) và **`HeritageExploreFragment`** (trang khám phá nội khu di sản khi đã đến nơi), cùng **`HeritagePreviewFragment`**, **`CombinedMapFragment`** (bản đồ tổng hợp di sản & sự kiện), **`VisitMapFragment`** (hiển thị vị trí trực tiếp hoàn toàn trên thiết bị), **`GeofenceManager`** và công cụ giả lập đường đi **`LocationSimulator`** phục vụ kiểm thử và demo trực tiếp.

### 2.4. TV4 — Nguyễn Minh Lương (`23BA14184`) — *AI Engineer (Historical RAG & Knowledge Base)*
- **Trách nhiệm cốt lõi:** Phụ trách toàn diện **Model 2 — Historical RAG** và kho tư liệu lịch sử Hà Nội có kiểm chứng trên `pgvector`.
- **Công việc cụ thể:**
  - Biên soạn, thẩm định và chuẩn hóa kho tư liệu lịch sử – văn hóa Hà Nội từ các nguồn chính thống, gắn siêu dữ liệu (`destinationId`, `landmarkId`, `sourceCitation`).
  - Xây dựng pipeline phân đoạn văn bản (semantic/hierarchical chunking) và nhúng vector (embedding model hỗ trợ tiếng Việt/đa ngôn ngữ) lưu vào bảng `KnowledgeChunk` trên PostgreSQL 16 với phần mở rộng **`pgvector`** (chỉ mục HNSW/IVFFlat).
  - Phát triển dịch vụ truy xuất lai (hybrid retrieval lọc theo `destinationId` cho trang `Heritage preview` và lọc sâu theo `landmarkId` cho màn hình `Landmark info`) kết hợp LLM sinh câu trả lời bám sát bằng chứng (grounded generation) kèm danh sách trích dẫn bắt buộc (`citations`).
  - Xây dựng endpoint `POST /internal/v1/rag/answer` trên **Python FastAPI**, cơ chế từ chối trả lời khi câu hỏi nằm ngoài tư liệu để chống ảo giác (hallucination), và kịch bản **fallback nội dung tĩnh** khi dịch vụ AI ngoại tuyến.

### 2.5. TV5 — Lương Quỳnh Anh (`23BA14008`) — *Media & Mobile Engineer (Camera, Album, Stamp & Recap)*
- **Trách nhiệm cốt lõi:** Phụ trách **`modules.media`** và **`modules.recap`** trên Backend cùng hệ thống CameraX, đóng dấu ảnh và Nhật ký hành trình trên Android.
- **Công việc cụ thể:**
  - **Module `media`:** Xây dựng luồng cấp phát Presigned S3 PUT/GET URLs, quản lý phiên tải ảnh (`UploadSession`), xử lý ảnh bất đồng bộ qua sự kiện Spring Modulith (`PhotoUploadedEvent`), giữ nguyên bản ảnh gốc bất biến (`immutable original`), tạo ảnh thu nhỏ (thumbnails) và xuất ảnh gắn watermark.
  - **Module `recap`:** Tự động tổng hợp Album theo điểm đến/tháng và lắng nghe sự kiện `VisitEndedEvent` để tạo bản kể chuyện hành trình (`Journey Recap`) bất đồng bộ.
  - **Android Camera, Stamp & Journal:** Tích hợp **CameraX** chụp ảnh tại di sản và tại sự kiện (`Event memory page`), xây dựng trình chỉnh sửa không phá hủy và bộ kết xuất **`JourneyStampRenderer`** (vẽ khung Dấu hành trình, tên di sản, ngày chụp lên Bitmap), cùng màn hình **`TravelJournalFragment`** xem lại lịch sử chuyến đi, phần thưởng và tải ảnh về máy.

### 2.6. TV6 — Hà Thế Duy (`2410207`) — *Cyber Security & QA Lead*
- **Trách nhiệm cốt lõi:** Đảm bảo an toàn thông tin, quyền riêng tư dữ liệu và chất lượng kiến trúc trên toàn bộ hệ thống theo tiêu chuẩn tại [07_TESTING_AND_BUG_TRIAGE.md](07_TESTING_AND_BUG_TRIAGE.md).
- **Công việc cụ thể:**
  - Lập mô hình mối đe dọa (**Threat Modeling** theo STRIDE) cho 5 luồng trọng yếu: (1) Xác thực & xoay vòng token, (2) Kiểm tra đến nơi & quyền riêng tư vị trí, (3) Tải ảnh Presigned URL & xử lý media, (4) Nhận diện AI & hỏi đáp RAG, (5) Thu thập sự kiện văn hóa từ nguồn ngoài (chống SSRF/XSS).
  - Viết bộ kiểm thử tự động chống **IDOR/BOLA** (đảm bảo người dùng A không thể đọc/sửa/xóa hành trình, bộ sưu tập tem, ảnh hay recap của người dùng B).
  - Kiểm định bảo mật luồng **Presigned Upload**: kiểm tra magic bytes thực tế, giới hạn kích thước byte/pixel, thời hạn URL ngắn (5–10 phút), khóa cứng `objectKey` chống ghi đè tệp.
  - Xây dựng cơ chế tự động **quét và xóa sạch siêu dữ liệu EXIF GPS/thiết bị** trên mọi ảnh phái sinh và kiểm chứng nguyên tắc không lưu tọa độ thô.
  - Duy trì bộ kiểm thử kiến trúc Spring Modulith (`ApplicationModules.of(HanoiLensApplication.class).verify()`), cấu hình CI pipeline và điều phối kiểm thử tích hợp đầu-cuối (E2E).

### 2.7. TV7 — Lê Bá Tiệp (`23BA14282`) — *Platform/Identity Lead & AI Integration Engineer*
- **Trách nhiệm cốt lõi:** Phụ trách nền tảng xác thực **`modules.identity`**, hồ sơ quyền riêng tư **`modules.profile`**, cổng tích hợp AI trên Spring Boot và giao diện Auth/Profile/AI Chat trên Android.
- **Công việc cụ thể:**
  - **Module `identity` & `profile`:** Hiện thực hóa đăng ký/đăng nhập với băm mật khẩu **Argon2id**, cấp phát Access JWT ngắn hạn (tối đa 15 phút) và **Rotating Refresh Token** có phát hiện phát lại (replay detection) & thu hồi phiên; quản lý cấu hình quyền riêng tư vị trí (`LocationPrecision`: `EXACT`, `COARSE`, `HIDDEN`).
  - **Cầu nối AI (`AiInferenceClientAdapter`):** Xây dựng adapter chịu lỗi (timeouts, circuit-breaker/fallback, Chuẩn hóa DTO) kết nối Spring Boot Modulith với Python FastAPI runtime cho cả Model 1 (Vision) và Model 2 (RAG), cung cấp các REST API công khai `/v1/ai/recognize-landmark` và `/v1/ai/qa`.
  - **Android Auth, Profile & AI Chat UI:** Xây dựng màn hình Đăng nhập, Đăng ký, `LandingActivity` (chỉ hiện cho tài khoản mới), lưu trữ token an toàn bằng `EncryptedSharedPreferences`/Keystore, `AuthInterceptor` tự động làm mới token trên OkHttp, và giao diện hội thoại **`HistoricalRagChatFragment`** hiển thị câu trả lời kèm thẻ trích dẫn nguồn (`citations`).

---

## 3. Ma trận Trách nhiệm RACI (RACI Matrix)

Quy ước:
- **R (Responsible):** Trực tiếp thực thi và viết code chính.
- **A (Accountable):** Chịu trách nhiệm nghiệm thu cuối cùng về chất lượng module/hạng mục.
- **C (Consulted):** Tham vấn kỹ thuật, phối hợp thiết kế hợp đồng API/dữ liệu.
- **I (Informed):** Nắm thông tin để đồng bộ tiến độ.

| Hạng mục / Module hệ thống | TV1 (Hoàng) | TV2 (Ninh) | TV3 (H.Anh) | TV4 (Lương) | TV5 (Q.Anh) | TV6 (Duy) | TV7 (Tiệp) |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **1. `identity` & `profile` (Auth, JWT, Privacy Policy)** | I | I | I | I | I | C | **A/R** |
| **2. `destination` (PostGIS, Geofence Arrival Check)** | C | C | **A/R** | C | I | C | I |
| **3. `visit` (Journey Lifecycle, Mark/Unlock, Rewards)** | I | **A/R** | C | I | C | C | I |
| **4. `event` (Deterministic Crawler, Event Memory)** | I | **A/R** | C | I | C | C | I |
| **5. `media` & `recap` (Presigned S3, Stamp, Recap)** | C | C | I | C | **A/R** | C | I |
| **6. Model 1 — Landmark Vision (FastAPI + Dataset)** | **A/R** | I | C | C | C | C | R |
| **7. Model 2 — Historical RAG & `pgvector` Corpus** | C | I | C | **A/R** | C | C | R |
| **8. Spring Boot AI Adapter & `/v1/ai/*` Endpoints** | C | I | I | C | I | C | **A/R** |
| **9. Android Map, Discovery & On-device GPS** | I | C | **A/R** | I | I | C | C |
| **10. Android CameraX, Stamp Editor & Travel Journal** | C | C | I | I | **A/R** | C | I |
| **11. Bảo mật (IDOR, EXIF Strip, Upload Hardening) & QA** | C | C | C | C | C | **A/R** | C |

---

## 4. Bảng Cân bằng Khối lượng Công việc trong 3 Tháng (3-Month Equal Workload Matrix)

Lộ trình 12 tuần (3 tháng) được phân bổ đồng đều (~100% tải công việc chuẩn mỗi thành viên trong từng tháng), đảm bảo **không có thành viên nào bị quá tải hay nhàn rỗi** ở bất kỳ giai đoạn nào.

| Thành viên | Tháng 1: Nền tảng, Dữ liệu & Khung Kiến trúc (Tuần 1–4) | Tháng 2: Nghiệp vụ Cốt lõi, Модели AI & Tích hợp (Tuần 5–8) | Tháng 3: Hoàn thiện UI, Đóng dấu, Bảo mật & Nghiệm thu E2E (Tuần 9–12) |
| :--- | :--- | :--- | :--- |
| **TV1 — Nguyễn Minh Hoàng**<br/>*(AI Vision)* | • Xây dựng danh mục địa danh (`landmarks`) cùng TV3.<br/>• Thu thập, chụp thực địa & làm sạch bộ ảnh huấn luyện tại Hà Nội.<br/>• Thiết lập khung dự án Python FastAPI dùng chung với TV4. | • Huấn luyện & fine-tune Model 1 (Landmark Classifier).<br/>• Đánh giá độ chính xác, tìm ngưỡng `confidence threshold` tối ưu cho `UNKNOWN`.<br/>• Xây dựng endpoint `/internal/v1/vision/classify`. | • Tối ưu tốc độ suy luận (inference latency) & kiểm thử ảnh thực tế.<br/>• Phát triển UI hiển thị kết quả nhận diện (`Landmark info` & trạng thái `UNKNOWN`) trên Android.<br/>• Hỗ trợ kịch bản demo nhận diện trực tiếp. |
| **TV2 — Lê Bá Ninh**<br/>*(Visits, Events & Rewards)* | • Thiết kế Flyway migration cho bảng `visits`, `visit_marks`, `rewards`, `cultural_events`.<br/>• Xây dựng khung module `visit` và `event` trên Spring Boot.<br/>• Khảo sát cấu trúc HTML/JSON-LD/RSS của các nguồn sự kiện chính thống. | • Hiện thực hóa đầy đủ vòng đời `ACTIVE/PAUSED/ENDED`, đánh dấu & mở khóa di sản.<br/>• Xây dựng bộ parser sự kiện tất định (JSON-LD/RSS/CSS) + khử trùng lặp.<br/>• Viết logic đối soát `Marked vs Unlocked` & cấp Badge + Gift. | • Xây dựng UI Android cho Sự kiện, `EventMemoryFragment`, `StampCollectionFragment` và màn hình nhận thưởng.<br/>• Phối hợp TV6 kiểm thử chống gian lận phần thưởng & SSRF ở crawler.<br/>• Hoàn thiện dữ liệu sự kiện mẫu phục vụ demo. |
| **TV3 — Lê Hiền Anh**<br/>*(Geospatial, Map & Arrival)* | • Thiết kế Flyway migration PostGIS (`destinations`, `landmarks`, GiST index).<br/>• Thu thập tọa độ ranh giới Polygon thực tế của các khu di sản Hà Nội.<br/>• Dựng khung bản đồ trên Android (`CombinedMapFragment`). | • Xây dựng API tìm kiếm lân cận (`ST_DWithin`) & kiểm tra đến nơi (`arrival-checks`) trên Backend.<br/>• Phát triển `ExploreHomeFragment`, `HeritagePreviewFragment`, `HeritageExploreFragment`.<br/>• Xây dựng `GeofenceManager` trên Android. | • Hoàn thiện `VisitMapFragment` (hiển thị vị trí trực tiếp chỉ trên thiết bị).<br/>• Xây dựng công cụ `LocationSimulator` giả lập lộ trình GPS mượt mà khi báo cáo/demo.<br/>• Tinh chỉnh hiệu năng bản đồ & trải nghiệm chuyển trang. |
| **TV4 — Nguyễn Minh Lương**<br/>*(Historical RAG & Knowledge Base)* | • Thu thập, biên tập & chuẩn hóa bộ tư liệu lịch sử Hà Nội có nguồn trích dẫn.<br/>• Thiết kế bảng `knowledge_chunks` với `pgvector` và chỉ mục HNSW.<br/>• Xây dựng script chunking & nhúng vector hàng loạt. | • Phát triển pipeline truy xuất ngữ cảnh theo `destinationId` (Preview) và `landmarkId` (On-site).<br/>• Tích hợp LLM sinh câu trả lời bám sát tư liệu + trích dẫn (`citations`).<br/>• Xây dựng endpoint `/internal/v1/rag/answer`. | • Xây dựng cơ chế chống ảo giác (từ chối câu hỏi lạc đề) & fallback nội dung tĩnh khi mất kết nối AI.<br/>• Phối hợp TV5 cung cấp tóm tắt kể chuyện cho `Journey Recap`.<br/>• Đánh giá chất lượng câu trả lời RAG trên bộ câu hỏi kiểm thử. |
| **TV5 — Lương Quỳnh Anh**<br/>*(Camera, Media, Stamp & Recap)* | • Thiết kế Flyway migration cho `photos`, `upload_sessions`, `recaps`.<br/>• Cấu hình MinIO / S3 local & xây dựng API tạo Presigned PUT/GET URLs.<br/>• Tích hợp khung CameraX chụp ảnh cơ bản trên Android. | • Xây dựng xử lý ảnh bất đồng bộ (`PhotoUploadedEvent`), tạo thumbnail & giữ nguyên ảnh gốc.<br/>• Phát triển `JourneyStampRenderer` đóng dấu khung kỷ niệm lên ảnh trên Android.<br/>• Xây dựng module `recap` xử lý `VisitEndedEvent`. | • Phát triển hoàn chỉnh `TravelJournalFragment`, trình xem chi tiết chuyến đi & tải ảnh đóng dấu về máy.<br/>• Tích hợp chụp ảnh kỷ niệm tại `EventMemoryFragment` cùng TV2.<br/>• Kiểm thử hiệu năng bộ nhớ Bitmap trên thiết bị thật. |
| **TV6 — Hà Thế Duy**<br/>*(Cyber Security & QA Lead)* | • Hoàn thành tài liệu Threat Modeling cho 5 luồng trọng yếu.<br/>• Thiết lập `ModularityTests` kiểm tra ranh giới Spring Modulith & CI workflow.<br/>• Thiết kế bộ test case bảo mật & quyền riêng tư. | • Viết bộ Integration Tests kiểm tra phân quyền chống IDOR/BOLA trên mọi module.<br/>• Xây dựng bộ kiểm tra tệp ảnh tải lên (magic bytes, kích thước, xóa sạch EXIF GPS).<br/>• Kiểm định thời hạn & khóa `objectKey` của Presigned URL. | • Thực hiện kiểm thử xâm nhập (pentest nội bộ) bằng OWASP ZAP & kiểm tra rò rỉ tọa độ trong log/DTO.<br/>• Điều phối kiểm thử tích hợp đầu-cuối (E2E) toàn nhóm.<br/>• Nghiệm thu danh mục Security DoD trước khi đóng gói phát hành. |
| **TV7 — Lê Bá Tiệp**<br/>*(Platform, Identity & AI Integration)* | • Khởi tạo bộ khung monorepo Spring Boot Modulith & Android MVVM base (Retrofit, OkHttp, Room).<br/>• Xây dựng module `identity` (Argon2id, JWT, Rotating Refresh Token) & `profile` (`LocationPrecision`). | • Xây dựng `AiInferenceClientAdapter` kết nối Spring Boot với FastAPI của TV1 & TV4.<br/>• Phát triển các endpoint `/v1/ai/recognize-landmark` và `/v1/ai/qa`.<br/>• Xây dựng màn hình Login, Sign up, `LandingActivity` trên Android. | • Phát triển giao diện hội thoại `HistoricalRagChatFragment` hiển thị câu trả lời & thẻ `citations` tại cả `Heritage preview` và `Landmark info`.<br/>• Hoàn thiện trang cài đặt quyền riêng tư `ProfileFragment`.<br/>• Quản lý cấu hình Docker Compose & đóng gói bản dựng APK/Backend. |
