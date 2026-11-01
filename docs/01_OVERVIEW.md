# 01. Tổng Quan Dự Án — Hanoi Lens (Product Overview & Scope)

> **Tham chiếu đề tài:** USTH ICT Group Project (Năm học 2025–2026)  
> **Tên đề tài:** **Hanoi Lens** — Ứng dụng di động khám phá địa danh, di sản văn hóa lịch sử và sự kiện tại Hà Nội qua nhiếp ảnh.  
> **Tài liệu liên quan:** [02_ARCHITECTURE.md](02_ARCHITECTURE.md) | [03_DATA_AND_CONTRACT.md](03_DATA_AND_CONTRACT.md)

---

## 1. Giới Thiệu Đề Tài & Tầm Nhìn Sản Phẩm

### 1.1 Bài toán & Mô tả hệ thống
**Hanoi Lens** là ứng dụng di động Android Native giúp người dùng tự do khám phá các địa danh (landmarks), di sản văn hóa - lịch sử (heritage sites), bảo tàng, không gian văn hóa và cảnh quan tại Hà Nội thông qua trải nghiệm nhiếp ảnh cá nhân.

Thay vì áp đặt các tuyến du lịch cố định (rigid tour routes) hay danh sách điểm đến bắt buộc, Hanoi Lens xây dựng một vòng đời trải nghiệm mở gồm 5 giai đoạn:

```text
[1. Discover & Mark] ──> [2. Arrive & Unlock] ──> [3. Visit & Explore] ──> [4. Capture & Edit] ──> [5. Remember & Reward]
```

1. **Discover & Mark:** Duyệt bản đồ tích hợp di sản và sự kiện văn hóa, xem trước thông tin (`Heritage Preview`), hỏi đáp nhanh với trợ lý lịch sử RAG và tự đánh dấu (mark) những di sản muốn ghé thăm.
2. **Arrive & Unlock:** Khi người dùng bước vào vùng địa lý của di sản, cơ chế **GPS Geofencing** xác nhận sự hiện diện (`INSIDE`) và tự động mở khóa (`unlock`) di sản mà không cần quét mã QR hay thẻ NFC vật lý.
3. **Visit & Explore:** Mở trang khám phá di sản (`Heritage Explore Page`) và bản đồ tham quan nội khu (`Visit Map`) hiển thị vị trí trực tiếp chỉ tính toán ngay trên thiết bị.
4. **Capture & Edit:** Chụp ảnh bằng `CameraX`, tùy chọn gửi ảnh để nhận diện công trình kiến trúc (**Landmark Image Classification**) nhằm mở khóa ngữ cảnh hỏi đáp chuyên sâu (**Historical RAG**), đồng thời chỉnh sửa ảnh không phá hủy bản gốc (non-destructive editing).
5. **Remember & Reward:** Lưu hành trình vào nhật ký (`Travel Journal / Journey Album`), đóng dấu ảnh nghệ thuật (`Journey Stamps / Watermarking`), xem tổng kết (`Destination & Monthly Recaps`) và nhận huy hiệu **Journey Stamp Badge** cùng phần quà ngẫu nhiên khi mở khóa trọn vẹn các di sản đã đánh dấu.

### 1.2 Tầm nhìn sản phẩm (Product Vision)
Biến việc khám phá Hà Nội trở nên sống động, mang đậm dấu ấn cá nhân và giàu giá trị thị giác. Hanoi Lens kết nối lịch sử đô thị, di sản kiến trúc và nhịp sống văn hóa đương đại trực tiếp với hành động sáng tạo nhiếp ảnh của mỗi người.

### 1.3 Đối tượng người dùng (Target Users)
- **Người dùng chính (Primary Users):**
  - *Người khám phá tự do & Cư dân địa phương:* Muốn tìm hiểu Hà Nội theo nhịp độ riêng, không phụ thuộc lịch trình tour.
  - *Người yêu thích nhiếp ảnh:* Muốn ghi lại hình ảnh di sản Hà Nội và lưu giữ kèm dấu ấn hành trình (`Journey Stamps`) thẩm mỹ.
  - *Du khách văn hóa - lịch sử:* Quan tâm đến thông tin lịch sử có kiểm chứng (grounded citations) và các triển lãm, lễ hội văn hóa đang diễn ra.
- **Người dùng thứ cấp (Secondary Users):**
  - *Quản trị viên hệ thống (System Administrators):* Quản lý dữ liệu ranh giới địa lý (`PostGIS`), vận hành bộ thu thập sự kiện (`Deterministic Event Parsers`) và kiểm duyệt nội dung trước khi xuất bản (`PENDING_REVIEW` -> `PUBLISHED`).

---

## 2. 7 Nguyên Tắc Cốt Lõi (Core Product Principles)

1. **Khám phá tự do, không ép tuyến đường (Free Exploration over Fixed Routes):** Khuyến khích sự tò mò tự nhiên; người dùng tự chọn và đánh dấu di sản muốn đến, ứng dụng không bao giờ áp đặt lộ trình hay thứ tự điểm dừng.
2. **Presence-only Geofencing — Bảo mật vị trí tuyệt đối (Presence over Tracking / Privacy-by-Design):** Hệ thống chỉ kiểm tra trạng thái hiện diện nhị phân (`INSIDE / OUTSIDE`) tại vùng ranh giới điểm đến. Tuyệt đối không theo dõi lộ trình di chuyển liên tục (no continuous GPS trail logging) và không lưu trữ tọa độ thô của người dùng làm lịch sử di chuyển.
3. **Kiến thức đúng thời điểm (Context at the Right Moment):** Cung cấp nội dung lịch sử và trợ lý RAG ngay từ bước xem trước (`Heritage Preview`) và làm giàu ngữ cảnh sâu hơn ngay khi người dùng đặt chân tới điểm di sản.
4. **AI hỗ trợ đúng lúc, không xâm lấn & Không phân tích camera thời gian thực (Assistance, Not Intrusion):** Nhận diện địa danh (`Landmark Classification`) chỉ chạy khi người dùng chủ động gửi ảnh chụp. Không phân tích luồng camera trực tiếp (no live camera stream analysis).
5. **Ký ức cá nhân là trọng tâm, phần thưởng là điểm cộng (Personal Memories First, Rewards as a Bonus):** Giá trị cốt lõi nằm ở cuốn nhật ký ảnh (`Journey Album`) và dấu ấn cá nhân; huy hiệu `Journey Stamp Badge` cùng quà tặng di sản là phần thưởng khích lệ khi hoàn thành mục tiêu tự đặt ra.
6. **Quyền riêng tư theo lựa chọn (Privacy by Choice):** Dữ liệu hành trình và album mặc định là riêng tư (`Private by Default`). Khi xuất ảnh có đóng dấu (`Watermark Export`), người dùng toàn quyền chọn mức hiển thị vị trí (`NONE`, `DISTRICT`, `DESTINATION`, `APPROXIMATE`, hoặc `EXACT`) và hệ thống tự động xóa sạch siêu dữ liệu nhạy cảm (`EXIF stripping`).
7. **Nội dung xác thực và truy xuất được nguồn gốc (Authentic & Traceable Content):** Mọi câu trả lời từ hệ thống Historical RAG đều phải bám sát kho tư liệu đã kiểm chứng kèm trích dẫn nguồn (`verified citations`); mọi sự kiện văn hóa đều ghi rõ nguồn gốc và thời점 xác thực.

---

## 3. 8 Kết Quả Đầu Ra Kỳ Vọng (Expected Outcomes)

Dự án cam kết bàn giao 8 kết quả đầu ra cụ thể theo đúng đề cương USTH ICT Group Project:

| # | Kết quả đầu ra (Deliverable) | Mô tả chi tiết |
| :--- | :--- | :--- |
| **1** | **Ứng dụng di động khám phá tự do (Mobile Application)** | Ứng dụng Android Native (`Java 17 + XML Layouts`) trực quan, hỗ trợ khám phá bản đồ di sản, danh lam thắng cảnh và không gian văn hóa tại Hà Nội. |
| **2** | **Phát hiện điểm đến qua GPS (GPS-Based Destination Detection)** | Cơ chế `GPS Geofencing` tự động nhận diện khi người dùng đến nơi (`Arrival Check`), mở khóa di sản và hiển thị thông tin văn hóa phù hợp. |
| **3** | **Nhận diện địa danh & Trợ lý lịch sử RAG (Landmark CV & Historical RAG)** | Bộ đôi năng lực AI: phân loại công trình kiến trúc từ ảnh chụp (`Landmark Image Classification`) và trả lời câu hỏi lịch sử có trích dẫn nguồn (`Domain-grounded Historical RAG`). |
| **4** | **Khám phá sự kiện văn hóa (Cultural Event Discovery)** | Luồng thông tin tổng hợp các triển lãm, lễ hội, workshop văn hóa đang và sắp diễn ra tại Hà Nội từ nguồn chính thống, hiển thị chung trên bản đồ di sản. |
| **5** | **Nền tảng bảo mật & Quyền riêng tư (Documented Security Baseline)** | Hồ sơ `Threat Modeling` cho 5 luồng trọng yếu, kiểm soát truy cập (`JWT` + `Refresh Token` xoay vòng + chống `IDOR/BOLA`), tải ảnh an toàn qua `Presigned PUT URL` và kiểm thử chống rò rỉ vị trí/`EXIF`. |
| **6** | **Album hành trình (Journey Albums)** | Hệ thống lưu trữ có cấu trúc các bức ảnh đã chụp, địa danh đã ghé thăm, mốc thời gian và tiến độ khám phá theo từng chuyến đi (`Visit / Journey`). |
| **7** | **Đóng dấu hành trình tùy biến (Customizable Journey Stamps & Watermarks)** | Công cụ kết xuất tem địa điểm, thời gian, thời tiết và khung viền kỷ niệm (`Minimalist`, `Postcard`, `Film Border`, `Heritage Monogram`, `Map Stamp`) lên ảnh xuất bản. |
| **8** | **Tổng kết hành trình cá nhân hóa (Personalized Destination & Monthly Recaps)** | Tự động tổng hợp câu chuyện và bộ sưu tập ảnh theo điểm đến (`DESTINATION`) hoặc theo tháng (`MONTHLY`), giúp người dùng sống lại ký ức khám phá Hà Nội. |

---

## 4. Bảng 14 Phương Pháp & Kỹ Thuật Sử Dụng (Used Methods & Techniques)

| # | Phương pháp / Kỹ thuật (Method / Technique) | Nhóm kỹ thuật (Category) | Mô tả ứng dụng thực tế trong Hanoi Lens |
| :--- | :--- | :--- | :--- |
| **1** | **GPS Geofencing** | Geospatial Awareness | Kiểm tra sự hiện diện nhị phân (`INSIDE / NEARBY / OUTSIDE`) của thiết bị trong vùng ranh giới địa danh (`PostGIS Circle/Polygon`) mà không lưu vết lộ trình. |
| **2** | **Location-Based Services (LBS)** | Local Discovery | Truy vấn không gian (`ST_DWithin`) để gợi ý các di sản, điểm tham quan và sự kiện văn hóa gần vị trí hiện tại của người dùng. |
| **3** | **Event Data Aggregation** | Data Ingestion | Thu thập định kỳ thông tin sự kiện văn hóa từ danh sách các cổng thông tin chính thống (allowlisted official websites & portals). |
| **4** | **Deterministic Event Parsing** | Data Processing | Sử dụng `JSON-LD`, `RSS`, `CSS Selectors` và quy tắc phân tích ngày tháng tiếng Việt xác định (deterministic rules) để trích xuất sự kiện đưa vào hàng đợi `PENDING_REVIEW` cho Admin duyệt. |
| **5** | **Landmark Computer Vision (Image Classification)** | Computer Vision (Model 1) | Phân loại công trình/di vật chính trong ảnh chụp của người dùng, trả về `landmarkId` kèm độ tin cậy (`confidence`) hoặc `UNKNOWN` nếu dưới ngưỡng. |
| **6** | **Historical RAG (Retrieval-Augmented Generation)** | Generative AI & NLP (Model 2) | Nhúng câu hỏi (`vector embeddings`), truy xuất đoạn tư liệu lịch sử chuẩn xác qua `pgvector` (lọc theo `destinationId`/`landmarkId`) và sinh câu trả lời kèm trích dẫn (`citations`). |
| **7** | **Digital Image Processing** | Media Processing | Xử lý ảnh không phá hủy bản gốc (non-destructive editing): cắt (`crop`), xoay thẳng (`straighten`), chỉnh sáng (`exposure`), tương phản, nhiệt độ màu và bộ lọc màu (`filters`). |
| **8** | **Photo Watermarking** | Visual Identity & Export | Đóng dấu thông tin điểm đến, quận/huyện, thời gian chụp, thời tiết hoặc huy hiệu hành trình lên bản sao ảnh xuất ra theo mẫu thiết kế. |
| **9** | **Metadata Management** | Data Architecture | Quản lý siêu dữ liệu ảnh chụp, mốc thời gian UTC (`timestamptz`), mức độ chính xác vị trí (`LocationPrecision`), danh sách địa danh đã qua và công thức chỉnh sửa (`editRecipe`). |
| **10** | **Journey Tracking** | Session Lifecycle | Quản lý trạng thái phiên khám phá (`ACTIVE`, `PAUSED`, `ENDED`, `CANCELLED`), cho phép tạm dừng và tiếp tục hành trình dang dở bất cứ lúc nào. |
| **11** | **Album and Recap Generation** | Memory & Summaries | Tự động nhóm ảnh và nhật ký chuyến đi thành các Album theo điểm đến và tạo bản tổng kết (`Destination / Monthly Recaps`) bất đồng bộ. |
| **12** | **Responsive Mobile UI/UX Design** | Client Interface | Thiết kế giao diện Android Native (`Material Design 3`, `XML Layouts`, `ViewBinding`) tối ưu cho thao tác ngoài trời và sử dụng một tay. |
| **13** | **Privacy-Aware Design** | Privacy Engineering | Áp dụng chính sách độ chính xác vị trí (`NONE`, `DISTRICT`, `DESTINATION`, `APPROXIMATE`, `EXACT`) ngay tại tầng Backend Domain và loại bỏ `GPS/Device EXIF` trên mọi ảnh dẫn xuất. |
| **14** | **Security Hardening** | Cyber Security | Xác thực `Argon2id` + `Access JWT` ngắn hạn (≤ 15 phút) + `Rotating Opaque Refresh Token`, kiểm tra quyền sở hữu tài nguyên chống `IDOR/BOLA`, ràng buộc `Presigned PUT URL` một lần và kiểm tra magic bytes/decoded image. |

---

## 5. Ranh Giới Phạm Vi: In-Scope (MVP) vs. Out-of-Scope (Hậu MVP)

### 5.1 Trong phạm vi MVP (In-Scope)
- **Android Native Client (`Java 17 + XML Layouts`):** Kiến trúc `MVVM`, `Retrofit 2 + OkHttp 3`, `Room` cache, `EncryptedSharedPreferences` (Android Keystore), `CameraX` chụp ảnh tiêu chuẩn và chế độ giả lập tọa độ (**Location Simulator Mode**) phục vụ kiểm thử/demo.
- **Spring Boot 3.x Modulith Backend:** 1 tiến trình JVM duy nhất gồm **đúng 7 module nghiệp vụ** (`identity`, `profile`, `destination`, `event`, `visit`, `media`, `recap`) dùng chung cơ sở dữ liệu `PostgreSQL 16` (`PostGIS` + `pgvector`) và `S3-compatible Object Storage` (`MinIO` / `AWS S3`).
- **Trải nghiệm khám phá & Mở khóa di sản:**
  - Bản đồ gộp chung Di sản (`Destinations`) và Sự kiện (`Cultural Events`).
  - Điểm đến thí điểm đầy đủ dữ liệu chuyên sâu (ví dụ: Khu di tích **Văn Miếu – Quốc Tử Giám** / khu vực **Hoàn Kiếm**).
  - Tự đánh dấu di sản (`Self-marked heritages`), xác nhận đến nơi bằng `GPS Geofencing` (`POST /v1/destinations/{id}/arrival-checks`), mở khóa trang `Heritage Explore Page` và bản đồ nội khu `Visit Map`.
  - Trang lưu giữ ký ức sự kiện (`Event Memory Page`) dành cho các sự kiện văn hóa không gắn cố định với một di sản.
  - Hệ thống huy hiệu `Journey Stamp Badge` và phần quà ngẫu nhiên khi mở khóa hết các di sản đã đánh dấu trong chuyến đi.
- **Phạm vi AI chuẩn hóa theo ADR 008 (Đúng 2 Model AI):**
  - **Model 1 — Landmark Image Classification:** Nhận diện 1 công trình/địa danh chính trong ảnh chụp (`POST /v1/ai/recognize-landmark`), trả về `RECOGNIZED` kèm `landmarkId` hoặc `UNKNOWN` nếu độ tin cậy thấp.
  - **Model 2 — Domain-grounded Historical RAG:** Hỏi đáp lịch sử có trích dẫn nguồn (`POST /v1/ai/qa`), hoạt động ở cả 2 ngữ cảnh: hỏi tổng quan trên `Heritage Preview` (chỉ cần `destinationId`) và hỏi chuyên sâu tại chỗ từ trang `Landmark Info` sau khi nhận diện ảnh thành công (`destinationId` + `landmarkId`). Có cơ chế fallback về nội dung tĩnh khi dịch vụ AI tạm ngưng.
- **Xử lý ảnh, Đóng dấu & Nhật ký:** Chỉnh sửa ảnh không phá hủy bản gốc, xuất ảnh kèm `Journey Stamps`, quản lý `Albums` và sinh `Recaps`.
- **Bảo mật & Quyền riêng tư theo ADR 009:** Hoàn thiện kiểm thử tự động cho 5 luồng đe dọa chính (Auth, Geofence, Media Upload, Private Resource IDOR/BOLA, AI Inputs) và kiểm chứng xóa sạch `EXIF`.

### 5.2 Ngoài phạm vi MVP / Đưa vào giai đoạn sau (Out-of-Scope & Explicit Exclusions)
- **Loại bỏ hoàn toàn tính năng gợi ý góc chụp & module `guidance` (theo ADR 010):** Không xây dựng `PhotoPrompt`, không có API `/destinations/{id}/prompts`, không gợi ý thời점 hay góc máy chụp ảnh và không tồn tại module `guidance` trong Backend.
- **Không phân tích camera thời gian thực / Không làm AR Live Overlay (theo ADR 008):** Không xử lý khung hình trực tiếp trên luồng `CameraX Preview`, không vẽ lưới bố cục AR hay phân tích ánh sáng thời gian thực trên kính ngắm.
- **Không làm Object Detection đa vật thể (theo ADR 008):** MVP không yêu cầu vẽ nhiều `bounding boxes` hay nhận diện đồng thời nhiều cổ vật trong cùng một bức ảnh; chỉ sử dụng **Primary-Landmark Image Classification**.
- **Không dùng mô hình NLP/LLM để cào sự kiện tự do (theo ADR 008):** Không xây dựng hệ thống cào dữ liệu quy mô lớn trên các trang web phi cấu trúc hay dùng AI tự động xuất bản sự kiện; chỉ dùng **Deterministic Event Parsing** trên các nguồn chính thống đã duyệt kèm bước kiểm duyệt của Admin.
- **Không làm mạng xã hội công khai:** Không có bảng tin công khai (public feed), bình luận hay chia sẻ tự động; mọi dữ liệu hành trình đều riêng tư mặc định.
