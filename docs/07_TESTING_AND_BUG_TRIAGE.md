# 07. Chiến lược Kiểm thử, Bảo mật & Quy trình Xử lý Lỗi (Testing, Security Baseline & Bug Triage)

Tài liệu này quy định tiêu chuẩn kiểm thử 7 cấp độ, tích hợp đầy đủ **Baseline Bảo mật & Quyền riêng tư (MVP Security & Privacy Baseline)** và quy trình phân loại, xử lý lỗi (Bug Triage & SLA) cho dự án **Hanoi Lens**.

---

## 1. Chiến lược Kiểm thử Toàn diện (7 Cấp độ)

Toàn bộ các bài kiểm thử sử dụng tọa độ và thời gian (`Clock`) cố định (deterministic), **tuyệt đối không gọi trực tiếp các dịch vụ bên thứ ba thật** trong bộ test tự động tiêu chuẩn.

| Cấp độ | Tên cấp độ kiểm thử | Trọng tâm kiểm thử | Công cụ & Framework | Người phụ trách chính |
| :--- | :--- | :--- | :--- | :--- |
| **1** | **Backend Unit Test** | Chuyển đổi trạng thái Domain (`Visit`: `ACTIVE -> PAUSED -> ENDED`), làm tròn quyền riêng tư vị trí (`LocationPrecision`), công thức chỉnh ảnh, chuẩn hóa dữ liệu sự kiện từ HTML/JSON-LD/RSS. | JUnit 5, Mockito, AssertJ | TV2, TV3, TV5, TV7 |
| **2** | **Spring Modulith Architecture Verification** | Kiểm tra ranh giới 7 module (`identity`, `profile`, `destination`, `event`, `visit`, `media`, `recap`), phát hiện phụ thuộc vòng (cyclic dependencies), cấm truy cập trực tiếp package nội bộ hoặc Repository chéo module, xác minh phát/nhận sự kiện nội bộ. | Spring Modulith Test (`ApplicationModules.of(HanoiLensApplication.class).verify()`) | TV7, TV6 |
| **3** | **API / Integration Test** | Hợp đồng REST Controller (`/v1/*`), truy vấn không gian PostGIS (`ST_DWithin`, `ST_Contains`), tìm kiếm vector `pgvector` (`<=>`), tích hợp S3/MinIO presigned URL, kiểm tra `Idempotency-Key`. | `@SpringBootTest`, MockMvc / RestAssured, **Testcontainers** (PostgreSQL 16 + PostGIS + pgvector, MinIO) | Tất cả Backend + TV6 |
| **4** | **Android Client Unit Test** | Logic trong `ViewModel`, biến đổi `LiveData`, `Repository` kết hợp Room cache và Retrofit API được giả lập (mock), xử lý xoay vòng JWT interceptor. | JUnit 4/5, Mockito, Robolectric | TV2, TV3, TV5, TV7 |
| **5** | **Android UI / Instrumented Test** | Điều hướng Activity/Fragment, hiển thị layout XML (ViewBinding), luồng vòng đời CameraX, trình chỉnh sửa tem dấu hành trình (Journey Stamp Canvas). | Espresso, AndroidX TestRunner | TV2, TV3, TV5, TV7 |
| **6** | **AI Model Evaluation & Contract Test** | Đánh giá độ chính xác phân loại di tích (Model 1: Top-1 Accuracy, ngưỡng `UNKNOWN`), chất lượng truy xuất & trích dẫn nguồn của Historical RAG (Model 2: Groundedness, Citation Accuracy), kiểm thử hợp đồng JSON schema giữa Spring Boot Adapter và FastAPI. | Pytest, FastAPI TestClient, WireMock (phía Spring Boot) | TV1, TV4, TV7 |
| **7** | **End-to-End (E2E) GPS Simulator Test** | Kịch bản xuyên suốt từ đầu đến cuối: Khám phá điểm đến $\rightarrow$ Di chuyển giả lập vào vùng Geofence $\rightarrow$ Xác nhận đến nơi (`arrival-checks`) $\rightarrow$ Bắt đầu chuyến thăm $\rightarrow$ Chụp & tải ảnh lên MinIO $\rightarrow$ Nhận diện di tích & hỏi đáp RAG $\rightarrow$ Kết thúc chuyến thăm $\rightarrow$ Tạo Recap & xuất ảnh có đóng dấu. | Kịch bản điều phối E2E, Android `LocationSimulator` / Mock GPS Provider | TV6 (Chủ trì) + Cả nhóm |

### Mẫu kiểm thử kiến trúc Spring Modulith bắt buộc (Cấp độ 2)

```java
package vn.edu.usth.hanoilens;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularityVerificationTest {

    private final ApplicationModules modules = ApplicationModules.of(HanoiLensApplication.class);

    @Test
    void verifyModularStructure() {
        // Thất bại ngay lập tức nếu có module gọi trực tiếp repository/internal package của module khác
        modules.verify();
    }

    @Test
    void generateModuleDocumentation() {
        new Documenter(modules).writeDocumentation();
    }
}
```

---

## 2. Baseline Bảo mật & Quyền riêng tư (MVP Security & Privacy Baseline)

Phần này quy định các yêu cầu bảo mật bắt buộc phải hoàn thành trong phạm vi MVP. Mọi phát hiện lỗ hổng được theo dõi theo mức độ (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`) kèm bằng chứng, tác động, hướng khắc phục và trạng thái kiểm thử lại (retest).

### 2.1. Tài sản cần bảo vệ (Protected Assets) & Ranh giới tin cậy (Trust Boundaries)

#### Tài sản cần bảo vệ (Protected Assets)
1. **Định danh & Đặc quyền:** Mật khẩu băm (Argon2id/bcrypt), Access Token (JWT), Refresh Token (opaque hash), và quyền quản trị (`ADMIN`).
2. **Dữ liệu cá nhân người dùng:** Hồ sơ (`Profile`), lịch sử chuyến đi (`Visit`), album, ảnh gốc riêng tư và siêu dữ liệu ảnh (`Photo`).
3. **Dữ liệu vị trí:** Tọa độ GPS chính xác hoặc xấp xỉ, kết quả kiểm tra hiện diện tại điểm đến (`arrival-checks`).
4. **Hạ tầng lưu trữ đối tượng:** Object keys trên S3/MinIO, Presigned URLs (PUT/GET), và các biến thể ảnh riêng tư.
5. **Tri thức văn hóa & AI:** Tài liệu lịch sử đã xác minh, trích dẫn RAG (`KnowledgeChunk`), và luồng truy vấn/kết quả suy luận mô hình.

#### Ranh giới tin cậy (Trust Boundaries)
1. **Ứng dụng Android $\leftrightarrow$ Spring Boot REST API:** Giao tiếp qua HTTPS JSON.
2. **Spring Boot $\leftrightarrow$ PostgreSQL (PostGIS/pgvector) & Object Storage (MinIO/S3):** Giao tiếp nội bộ qua mạng backend.
3. **Ứng dụng Android $\leftrightarrow$ Object Storage:** Tải ảnh trực tiếp bằng phương thức `PUT` thông qua Presigned URL ngắn hạn.
4. **Spring Boot $\leftrightarrow$ Python FastAPI AI Runtime:** Giao tiếp qua HTTP nội bộ thông qua Backend AI Adapter (Mobile không bao giờ gọi trực tiếp AI Runtime).
5. **Spring Boot Event Parser $\leftrightarrow$ Website nguồn sự kiện chính thống:** Thu thập dữ liệu một chiều từ danh sách tên miền được phê duyệt (allowlist).

> [!IMPORTANT]
> Mọi dữ liệu đi qua các ranh giới tin cậy trên đều được coi là **không đáng tin cậy (untrusted)** — bao gồm mảng byte tải lên, các claim trong JWT trước khi xác thực chữ ký, metadata từ kho lưu trữ, kết quả đầu ra của mô hình AI, và nội dung HTML/JSON-LD/RSS bên ngoài.

---

### 2.2. Mô hình Đe dọa cho 5 Luồng Trọng yếu (Threat Model for 5 Critical Flows)

| STT | Luồng trọng yếu | Mối đe dọa chính (Main Threats) | Biện pháp kiểm soát bắt buộc (Required Controls) | Bằng chứng kiểm thử bắt buộc (Required Evidence) |
| :--- | :--- | :--- | :--- | :--- |
| **1** | **Xác thực & Làm mới Token** (`Auth & Token Refresh`) | Nhồi thông tin đăng nhập (Credential stuffing), đánh cắp token, phát lại (replay) refresh token, leo thang đặc quyền (`USER` $\rightarrow$ `ADMIN`). | Băm mật khẩu bằng **Argon2id** (hoặc bcrypt cost $\ge 12$); Access JWT có chữ ký sống tối đa **15 phút**; Refresh Token dạng opaque ngẫu nhiên mật mã (sống 30 ngày), lưu dạng băm trong DB, **xoay vòng (rotate)** mỗi lần làm mới, nhóm theo *token family*, thu hồi toàn bộ họ token khi phát hiện tái sử dụng (reuse detection); giới hạn tần suất (rate limit) nghiêm ngặt; thông báo lỗi chung chung. | Integration test cho đăng nhập/làm mới/đăng xuất; test với token hết hạn, bị sửa đổi chữ ký, bị dùng lại (replayed), và đã bị thu hồi. |
| **2** | **Kiểm tra Đến nơi Geofence** (`Geofence Arrival Check`) | Giả mạo tọa độ (Spoofed coordinates), phát lại gói tin kiểm tra (replay), lưu trữ vị trí chính xác trái phép, rò rỉ tọa độ trong log. | Xác thực phía server bằng PostGIS; kiểm tra giới hạn độ trễ thời gian (`observedAt`) và sai số (`accuracyMeters`); bằng chứng đến nơi (`arrivalCheckId`) có thời hạn ngắn và **chỉ dùng một lần** để bắt đầu chuyến thăm; **không lưu trữ mẫu tọa độ thô** làm lịch sử di chuyển; che giấu (redact) `latitude`/`longitude` khỏi log. | Test giá trị biên (boundary-value tests) tại ranh giới Geofence; test dùng lại hoặc dùng `arrivalCheckId` hết hạn; kiểm tra bảng DB và file log. |
| **3** | **Tải ảnh lên & Cấp Presigned URL** (`Photo Upload & Presigned URL`) | Lỗ hổng IDOR/BOLA, tải lên file độc hại/polyglot, file quá dung lượng, ghi đè object key, lộ Presigned URL. | Kiểm tra quyền sở hữu `visitId`/`uploadId`; server tự sinh object key không thể đoán (tên file của client chỉ là metadata); Presigned URL chỉ cho phép `PUT` vào đúng 1 key và hết hạn sau **5–10 phút**; kiểm soát kích thước ($\le 25\text{ MB}$), định dạng (`image/jpeg`, `image/png`), mã băm `sha256`, kiểm tra **magic bytes** và giải mã ảnh thực tế; bucket ở chế độ riêng tư (private). | Test từ chối truy cập chéo giữa 2 người dùng; bộ mẫu file lỗi/độc hại (invalid-file corpus); test URL hết hạn/sai key/sai HTTP method; rà soát chính sách bucket. |
| **4** | **Truy xuất Chuyến thăm, Album & Ảnh** (`Visit, Album & Photo Retrieval`) | Truy cập chéo tài khoản (IDOR/BOLA), gán hàng loạt (Mass Assignment), lộ dữ liệu nhạy cảm trong phản hồi, thao tác quyền Admin trái phép. | Chính sách kiểm tra quyền sở hữu trên từng tài nguyên; truy vấn Repository luôn gắn điều kiện `userId` của người đang đăng nhập; dùng DTO allowlist (cấm client truyền `ownerId`, `role`, `objectKey`, `status`); trả về `404 Not Found` khi truy cập tài nguyên riêng tư của người khác để chống dò quét; ghi audit log cho mọi thao tác sửa đổi của `ADMIN`. | Parameterized test truy cập chéo tài khoản cho **mọi endpoint** tài nguyên riêng tư; kiểm tra các trường trong JSON trả về. |
| **5** | **Nhận diện Di tích & Hỏi đáp Lịch sử RAG** (`Landmark Recognition & Historical RAG`) | Lạm dụng mô hình (Model abuse), đầu ra AI sai lệch/bịa đặt trích dẫn (hallucination/citation fabrication), cạn kiệt tài nguyên, gửi `photoId` của người khác để nhận diện. | Kiểm tra quyền sở hữu `photoId` trước khi gọi Vision; giới hạn kích thước ảnh trước suy luận; áp dụng rate limit theo người dùng/token; xác thực JSON schema đầu ra của mô hình; truy xuất vector lọc chặt theo ngữ cảnh được duyệt (`destinationId`/`landmarkId`); xác minh mã trích dẫn (`citedChunkIds`); trả về `UNKNOWN` nếu độ tự tin thấp. | Test gửi `photoId` không thuộc sở hữu; test xử lý đầu ra AI sai định dạng; test từ chối trả lời khi thiếu trích dẫn; test vượt ngưỡng rate limit. |

---

### 2.3. Ma trận Kiểm thử Bảo mật Chi tiết

#### A. Ma trận Kiểm thử Phân quyền Tối thiểu (Minimum Authorization Test Matrix)
Đối với **mọi endpoint tài nguyên riêng tư** (`Visit`, `UploadSession`, `Photo`, `PhotoVariant`, `Album`, `Recap`, `Export`, `AI Recognition/QA`), bộ test tự động phải chứng minh đủ 6 trường hợp:

| Kịch bản kiểm thử | Kết quả kỳ vọng |
| :--- | :--- |
| **1. Chủ sở hữu hợp lệ (`Owner`)** | Thực hiện thành công thao tác theo tài liệu (`200 OK`, `201 Created`, `202 Accepted`, `204 No Content`). |
| **2. Người dùng khác đã đăng nhập (`User B` truy cập tài nguyên của `User A`)** | Bị từ chối hoàn toàn; trả về `404 Not Found` (hoặc `403 Forbidden` tùy chính sách không tiết lộ sự tồn tại của ID). |
| **3. Yêu cầu chưa xác thực (`Unauthenticated` / thiếu hoặc hết hạn JWT)** | Trả về `401 Unauthorized`. |
| **4. Người dùng thường (`USER`) gọi endpoint Quản trị (`/v1/admin/**`)** | Trả về `403 Forbidden`; không có thay đổi nào trong cơ sở dữ liệu. |
| **5. Thay đổi ID trên đường dẫn hoặc ID cha lồng nhau (ví dụ: ghép `visitId` của User A với `photoId` của User B)** | Bị chặn tại tầng Service/Repository; không thể vượt qua kiểm tra quyền sở hữu. |
| **6. Kiểm tra cấu trúc DTO phản hồi (`Response DTO Assertion`)** | Không bao giờ chứa `passwordHash`, `refreshToken`, `objectKey` nội bộ, thông tin xác thực S3, hoặc tọa độ chi tiết vượt quá mức `locationPrecision` cho phép. |

#### B. Kiểm thử Bảo mật Tải ảnh & Presigned URL (Upload Security Tests)
1. **Kiểm tra nội dung tệp nghiêm ngặt:**
   - Chỉ chấp nhận `image/jpeg` và `image/png` với dung lượng gốc tối đa **25 MB**.
   - Từ chối các tệp giả mạo phần mở rộng/MIME type (ví dụ: đổi tên `.exe` hoặc `.html` thành `.jpg`), tệp ảnh bị cắt cụt (truncated), tệp đa hình (polyglot payload), và ứng viên bom giải nén (decompression bomb có số chiều/pixel vượt ngưỡng).
   - Bắt buộc kiểm tra **Magic Bytes** đầu tệp (`FF D8 FF` cho JPEG, `89 50 4E 47` cho PNG) và giải mã thử ảnh thành công trước khi chuyển trạng thái từ `PROCESSING` sang `READY`.
2. **Ràng buộc khóa và thời hạn Presigned URL:**
   - Presigned URL chỉ có hiệu lực trong **5–10 phút**, chỉ cho phép phương thức `PUT` vào đúng 1 object key do server sinh ra, kèm điều kiện `Content-Type` và `x-amz-checksum-sha256`.
   - Kiểm thử xác nhận URL không thể dùng để tải lên key khác, không thể dùng phương thức `GET`/`POST`/`DELETE`, và bị từ chối ngay khi hết hạn.
3. **Tính lũy đẳng khi hoàn tất (`Idempotency`):**
   - Gọi lặp lại `POST /v1/media/uploads/{uploadId}/complete` với cùng `Idempotency-Key` chỉ trả về kết quả ban đầu và **không đẩy trùng tác vụ xử lý ảnh** vào hàng đợi sự kiện.

#### C. Kiểm thử Chống Rò rỉ Vị trí & Siêu dữ liệu EXIF (Location & EXIF Privacy Tests)
1. **Loại bỏ tự động EXIF trên mọi biến thể ảnh:**
   - Ảnh gốc được giữ nguyên vẹn và hoàn toàn riêng tư.
   - **Tất cả** ảnh thu nhỏ (thumbnails), ảnh xem trước (previews), ảnh đóng dấu hành trình (watermarked/stamped images), ảnh trong Recap và tệp xuất chia sẻ (`Export`) phải **tự động xóa sạch toàn bộ EXIF mặc định** — đặc biệt là khối thẻ GPS (`GPSLatitude`, `GPSLongitude`, `GPSAltitude`), mã định danh thiết bị, số sê-ri máy ảnh và thumbnail nhúng sẵn.
   - Chuẩn hóa hướng xoay ảnh (Orientation normalization) trong quá trình giải mã trước khi xóa metadata để ảnh xuất ra không bị xoay ngược.
   - **Bài test bắt buộc:** Dùng thư viện đọc EXIF (như `metadata-extractor` trong Java) quét trực tiếp các tệp ảnh sinh ra và khẳng định (`assertThat`) không tồn tại bất kỳ trường GPS hay định danh thiết bị nào.
2. **Bảo vệ quyền riêng tư vị trí trên API và Log:**
   - Kiểm tra cả 5 mức `LocationPrecision` (`NONE`, `DESTINATION`, `DISTRICT`, `APPROXIMATE`, `EXACT`) trên API phản hồi, tem dấu ảnh và bản xuất. Nếu người dùng chọn gắn vị trí lên ảnh xuất, server chỉ vẽ thông tin theo mức độ đã chọn, tuyệt đối không sao chép khối EXIF GPS gốc.
   - Quét log có cấu trúc (SLF4J/JSON logs) trong suốt các bài test tìm kiếm lân cận, kiểm tra đến nơi, tải ảnh và xuất ảnh để đảm bảo không xuất hiện tọa độ thô (`latitude`, `longitude`), JWT hay Presigned URL.
   - Đảm bảo tọa độ chính xác không bao giờ được gửi sang Python AI Inference Runtime.

---

### 2.4. Tiêu chí Nghiệm thu Bảo mật MVP (Security Acceptance Criteria)

Hệ thống MVP **không được phép nghiệm thu phát hành** cho đến khi thỏa mãn toàn bộ 6 tiêu chí sau:
1. Cả 5 luồng trong Threat Model đều có người chịu trách nhiệm rõ ràng, có bằng chứng kiểm thử tự động, và **không còn bất kỳ lỗi bảo mật nào ở mức `CRITICAL` hoặc `HIGH` chưa được khắc phục**.
2. Toàn bộ luồng đăng nhập, xoay vòng refresh token, đăng xuất/thu hồi, hết hạn và phát hiện tái sử dụng refresh token đều được bao phủ bởi Integration Test.
3. Bộ kiểm thử phân quyền tự động (Authorization Test Matrix) bao phủ 100% loại tài nguyên riêng tư và các endpoint quản trị sự kiện/điểm đến.
4. Kiểm thử tải ảnh và Presigned URL vượt qua toàn bộ bộ mẫu tệp độc hại, hết hạn URL, sai key/method, tính lũy đẳng và truy cập chéo tài khoản.
5. Mọi ảnh thumbnail, ảnh đóng dấu và ảnh xuất đều vượt qua bài kiểm tra tự động chống rò rỉ EXIF và vị trí.
6. Các bài kiểm thử bảo mật chạy tự động trong pipeline CI; báo cáo cuối kỳ của **TV6 (Hà Thế Duy — Cyber Security + QA)** ghi nhận đầy đủ danh sách rủi ro `MEDIUM`/`LOW` còn lại cùng phương án kiểm soát.

---

## 3. Quy trình Phân loại & Xử lý Lỗi (Bug Triage & SLA)

### 3.1. Phân cấp Mức độ Nghiêm trọng & Cam kết Thời gian (Severity & SLA)

| Cấp độ | Định nghĩa & Ví dụ điển hình | Thời gian phản hồi (Acknowledge) | Thời gian khắc phục (Fix SLA) | Quy tắc phát hành |
| :--- | :--- | :--- | :--- | :--- |
| **`P0 / Critical`** | - Lỗ hổng bảo mật mức `CRITICAL` (rò rỉ dữ liệu chéo tài khoản IDOR, lộ JWT/Presigned URL, rò rỉ tọa độ GPS/EXIF trái phép).<br/>- Crash ứng dụng Android hoặc sập tiến trình Spring Boot / FastAPI.<br/>- Lỗi hỏng kiến trúc `ApplicationModules.verify()` hoặc làm gãy build nhánh `main`. | **$\le 2\text{ giờ}$** | **$\le 12\text{ giờ}$** (Ưu tiên số 1, dừng việc tính năng mới để fix) | **Chặn tuyệt đối (Release Blocker)** |
| **`P1 / High`** | - Lỗ hổng bảo mật mức `HIGH` (thiếu kiểm tra magic bytes, không thu hồi refresh token khi reuse).<br/>- Luồng nghiệp vụ chính không hoạt động (không thể bắt đầu/kết thúc chuyến thăm, không tải được ảnh lên MinIO, Vision/RAG trả lỗi `500`). | **$\le 4\text{ giờ}$** | **$\le 24\text{ giờ}$** | **Chặn nghiệm thu MVP** |
| **`P2 / Medium`** | - Chức năng hoạt động sai ở các trường hợp biên (hiển thị sai khoảng cách làm tròn, bộ lọc sự kiện theo quận chưa chuẩn, giao diện lệch trên một số kích thước màn hình).<br/>- Lỗi bảo mật mức `MEDIUM` có biện pháp giảm thiểu tạm thời. | **$\le 12\text{ giờ}$** | **$\le 72\text{ giờ}$** (Trong cùng Sprint/Tuần) | Cho phép merge nếu có kế hoạch fix rõ ràng |
| **`P3 / Low`** | - Lỗi thẩm mỹ giao diện nhỏ (sai chính tả tiếng Việt, màu sắc/khoảng cách padding chưa đều, tối ưu hiệu năng nhẹ). | **$\le 24\text{ giờ}$** | Xếp vào Backlog tuần kế tiếp | Không chặn phát hành |

---

### 3.2. Quy tắc Xác định Người Chịu trách nhiệm khi Ghép nhánh (`main`) Bị lỗi

Khi tích hợp code lên nhánh `main` xảy ra lỗi giữa **Android Client**, **Spring Boot Modulith Backend** và **Python FastAPI AI Runtime**, nhóm áp dụng quy tắc **"Đối chiếu Hợp đồng & Mã định danh Truy vết (`X-Request-Id`)"** theo 4 bước sau:

1. **Bước 1 — Tái hiện và thu thập bằng chứng chuẩn (Do TV6 điều phối):**
   - Mọi báo cáo lỗi tích hợp phải đính kèm: mã `X-Request-Id`, HTTP Status Code, Request/Response JSON thực tế, Logcat (phía Android) hoặc JSON log (phía Spring Boot / FastAPI).
2. **Bước 2 — Đối chiếu với Hợp đồng Chuẩn (`docs/03_DATA_AND_CONTRACT.md` & OpenAPI Schema):**
   - **Trường hợp A — Lỗi thuộc về Android Client:** Nếu Android gửi thiếu header bắt buộc (`Authorization`, `Idempotency-Key`), sai tên trường `camelCase`, sai kiểu dữ liệu/enum `UPPER_SNAKE_CASE`, hoặc xử lý sai trạng thái bất đồng bộ (`202 Accepted` khi tải ảnh/kết thúc chuyến đi) $\rightarrow$ **Thành viên phụ trách màn hình Android tương ứng (TV2, TV3, TV5, TV7) chịu trách nhiệm sửa.**
   - **Trường hợp B — Lỗi thuộc về Spring Boot Backend:** Nếu Request từ Android đã đúng hợp đồng trong `03_DATA_AND_CONTRACT.md` nhưng Backend trả về `500 Internal Server Error`, sai cấu trúc Error/Success Envelope, sai truy vấn PostGIS/JPA, lỗi phân quyền hoặc làm gãy `ApplicationModules.verify()` $\rightarrow$ **Thành viên sở hữu module Spring Modulith tương ứng chịu trách nhiệm sửa:**
     - `identity`, `profile` & AI Adapter: **TV7 (Lê Bá Tiệp)**
     - `destination`: **TV3 (Lê Hiền Anh)**
     - `visit`, `event`: **TV2 (Lê Bá Ninh)**
     - `media`, `recap`: **TV5 (Lương Quỳnh Anh)**
   - **Trường hợp C — Lỗi giữa Spring Boot và Python AI Runtime:**
     - Nếu Python FastAPI trả về sai JSON schema, quá thời gian chờ (timeout), lỗi nạp trọng số mô hình hoặc bịa đặt trích dẫn không có trong `pgvector` $\rightarrow$ **TV1 (Nguyễn Minh Hoàng — Vision Model 1)** hoặc **TV4 (Nguyễn Minh Lương — Historical RAG Model 2)** chịu trách nhiệm sửa.
     - Nếu Python AI Runtime trả về đúng hợp đồng nội bộ (hoặc đang tắt theo cấu hình) nhưng Spring Boot không xử lý fallback mượt mà (làm sập luồng xem nội dung văn hóa tĩnh) $\rightarrow$ **TV7 (Lê Bá Tiệp)** chịu trách nhiệm sửa tại tầng AI Adapter.
3. **Bước 3 — Quy tắc "Revert trước, Sửa sau" cho nhánh `main`:**
   - Nếu một Pull Request sau khi merge làm gãy build hoặc làm trượt bộ test tự động trên `main` mà không thể sửa nóng (hotfix) trong vòng **30 phút**, **Nguyễn Minh Hoàng (TV1)**, **Lê Bá Tiệp (TV7)** hoặc **Hà Thế Duy (TV6)** có quyền revert commit đó ngay lập tức để giải phóng nhánh cho các thành viên còn lại làm việc.
4. **Bước 4 — Bổ sung Regression Test trước khi đóng lỗi:**
   - Mọi lỗi `P0` và `P1` sau khi sửa xong **bắt buộc phải kèm theo ít nhất 1 bài test tự động** (Unit/Integration/Security Test) tái hiện lại đúng lỗi đó để đảm bảo lỗi không bao giờ lặp lại. **TV6** là người kiểm tra cuối cùng (Verify & Close).
