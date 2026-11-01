# 06 — Quy chuẩn Git & Quy tắc Viết Code (Git Workflow & Coding Conventions)

Tài liệu này quy định quy trình thao tác Git từng bước gắn với mã công việc trong [`TODO.md`](../TODO.md), chuẩn thông điệp Commit, quy tắc Pull Request (PR), 5 nguyên tắc chống xung đột code (**Git Conflict**) cho 7 thành viên trên cùng một kho mã nguồn (**Monorepo**), và quy chuẩn đặt tên / viết code cho cả 3 thành phần: **Backend Java Spring Boot Modulith**, **Android Native (Java & XML)** và **Python FastAPI AI Service**.

---

## 1. Cấu trúc Kho mã nguồn (Monorepo Layout)

Dự án **Hanoi Lens** được quản lý trong một kho lưu trữ Git duy nhất với ranh giới thư mục tách biệt rõ ràng:

```text
HanoiLens/
├── backend/            # Java 17 — Spring Boot 3.x Modulith (7 modules trong 1 JVM duy nhất)
├── android-client/     # Java 17 — Android Native App (MVVM + XML Layouts + ViewBinding)
├── ai-service/         # Python 3.11+ — FastAPI Runtime (Model 1 Vision & Model 2 RAG)
├── docs/               # Tài liệu kiến trúc (01 -> 08), UI_SIMULATOR.html & sơ đồ Diagram/
├── TODO.md             # Bảng kế hoạch 12 tuần (84 mã công việc từ W1-TV1 đến W12-TV7)
└── docker-compose.yml  # Hạ tầng phát triển cục bộ (PostgreSQL 16 PostGIS/pgvector + MinIO)
```

---

## 2. Quy trình Quản lý Nhánh Git & 4 Bước Làm Việc Chuẩn Cho 7 Thành Viên

### 2.1. Nhánh chính mặc định trên GitHub (`main` — Protected Default Branch)
- **`main` (Default Branch):** Là nhánh tích hợp chính và mặc định trên GitHub của nhóm.
- **Quy tắc bắt buộc:**
  - **Tuyệt đối cấm `git push origin main` trực tiếp** từ máy cá nhân lên nhánh `main` và **cấm force-push (`git push -f`)**.
  - Mọi thành viên bắt buộc tách nhánh riêng `feature/<tên-nhánh>-<ID-TODO>` từ `main`, sau khi code xong thì tạo **Pull Request (PR)** trên GitHub để gộp vào `main` sau khi đã được Reviewer duyệt.

### 2.2. Quy tắc đặt tên nhánh làm việc (Gắn trực tiếp mã `<ID-TODO>`)
Mỗi công việc trong [`TODO.md`](../TODO.md) đều có mã định danh duy nhất (ví dụ: `W1-TV7`, `W3-TV3`, `W5-TV1`, `W6-TV5`). Khi làm task nào, thành viên đặt tên nhánh gắn đúng `<ID-TODO>` đó:

```text
feature/<tên-nhánh>-<ID-TODO>
```

*Ví dụ thực tế cho 7 thành viên:*
- **TV1 (Nguyễn Minh Hoàng):** `feature/vision-classifier-W5-TV1`
- **TV2 (Lê Bá Ninh):** `feature/visit-reward-check-W6-TV2`
- **TV3 (Lê Hiền Anh):** `feature/postgis-arrival-W6-TV3`
- **TV4 (Nguyễn Minh Lương):** `feature/rag-pgvector-retriever-W5-TV4`
- **TV5 (Lương Quỳnh Anh):** `feature/journey-stamp-renderer-W6-TV5`
- **TV6 (Hà Thế Duy):** `feature/idor-security-tests-W5-TV6`
- **TV7 (Lê Bá Tiệp):** `feature/jwt-refresh-rotation-W3-TV7`

*(Trường hợp nhánh sửa lỗi đột xuất dùng tiền tố `fix/<tên-lỗi>-<ID-TODO>`).*

---

### 2.3. Hướng dẫn 4 Bước Thao tác Git Chuẩn (Copy & Chạy Theo Từng Task)

Mỗi thành viên khi bắt đầu một đầu việc trong [`TODO.md`](../TODO.md) bắt buộc thực hiện đúng **4 bước** sau với nhánh mặc định **`main`**:

- **Bước 1 — Trước khi bắt đầu làm Feature mới (Luôn tách nhánh từ `main` mới nhất):**
  ```bash
  git checkout main
  git pull origin main
  git checkout -b feature/<tên-nhánh>-<ID-TODO>
  ```
  *(Ví dụ: `git checkout -b feature/postgis-arrival-W6-TV3`)*

- **Bước 2 — Khi đang code xong từng file (Chỉ `git add` đúng file của mình & Commit theo chuẩn Conventional Commits):**
  ```bash
  git status
  git add <đường-dẫn-file-của-bạn>
  git commit -m "feat(<module>): <mô tả ngắn gọn> (<ID-TODO>)"
  ```
  *(Lưu ý: Không dùng `git add .` bừa bãi để tránh đẩy nhầm file cấu hình máy cá nhân. Ví dụ: `git commit -m "feat(destination): implement PostGIS arrival check (W6-TV3)"`)*

- **Bước 3 — Trước khi Push lên GitHub (Kéo `main` về kiểm tra & tick `[x]` trong `TODO.md`):**
  ```bash
  git fetch origin
  git merge origin/main
  # Mở file TODO.md, đổi [ ] thành [x] ở đúng dòng [<ID-TODO>] của bạn, sau đó:
  git add TODO.md
  git commit -m "docs: check [<ID-TODO>] completed"
  git push -u origin feature/<tên-nhánh>-<ID-TODO>
  ```

- **Bước 4 — Tạo Pull Request (PR) & Báo nhóm:**
  1. Vào GitHub tạo Pull Request từ nhánh `feature/...` của bạn vào nhánh mặc định **`main`** (**Tuyệt đối không push thẳng vào `main` mà không qua PR**).
  2. Chọn Reviewer là **Nguyễn Minh Hoàng (TV1)** và **Lê Bá Tiệp (TV7)**.
  3. Nhắn tin lên nhóm Zalo/Discord báo mọi người chạy `git pull origin main` về sau khi PR được merge.

---

## 3. Quy chuẩn Thông điệp Commit (Conventional Commits)

Tất cả commit phải tuân theo chuẩn **Conventional Commits** kèm mã `<ID-TODO>`:

```text
<type>(<module>): <mô tả ngắn gọn không quá 72 ký tự> (<ID-TODO>)
```

- **`<type>` hợp lệ:**
  - `feat`: Thêm tính năng mới.
  - `fix`: Sửa lỗi logic, bảo mật hoặc giao diện.
  - `refactor`: Tái cấu trúc code nhưng không làm thay đổi hành vi nghiệp vụ.
  - `test`: Thêm hoặc cập nhật unit test, integration test, architecture test.
  - `db`: Thêm hoặc cập nhật file migration cơ sở dữ liệu (Flyway).
  - `docs`: Cập nhật tài liệu thiết kế, `TODO.md` hoặc sơ đồ.
  - `chore`: Cập nhật cấu hình Gradle, Maven, Docker, CI.

- **`<module>` hợp lệ:**
  - Backend modules: `identity`, `profile`, `destination`, `event`, `visit`, `media`, `recap`, `shared`.
  - Android scopes: `android-explore`, `android-map`, `android-camera`, `android-events`, `android-journal`, `android-auth`.
  - AI & Security scopes: `ai-vision`, `ai-rag`, `security`, `ci`.

- **Ví dụ chuẩn:**
  - `feat(destination): implement PostGIS geofence arrival check endpoint (W6-TV3)`
  - `feat(android-map): add on-device live location marker in VisitMapFragment (W7-TV3)`
  - `fix(security): strip GPS EXIF metadata from exported images (W6-TV6)`
  - `feat(ai-rag): enforce citation grounding in landmark QA response (W6-TV4)`
  - `docs: check [W6-TV3] completed`

---

## 4. Năm (05) Nguyên tắc Vàng Chống Xung đột Code (Zero-Conflict Rules)

1. **Nguyên tắc 1 — Tôn trọng Ranh giới 7 Module trong Spring Modulith:**
   - Thành viên nào sở hữu module nào (`identity`, `profile`, `destination`, `event`, `visit`, `media`, `recap`) chỉ tạo và sửa file bên trong package `vn.edu.usth.hanoilens.modules.<tên_module>` của mình (xem bảng phân vùng file độc quyền tại Mục 1 của [`TODO.md`](../TODO.md)).
   - Khi cần dữ liệu hoặc kích hoạt hành vi của module khác, chỉ gọi qua **Interface dịch vụ công khai** hoặc phát **Spring Modulith Application Event**, tuyệt đối không tự ý sửa Repository hay Entity của thành viên khác.
2. **Nguyên tắc 2 — Mỗi Thành viên Sở hữu File Flyway SQL Riêng biệt (`V1` $\rightarrow$ `V5`):**
   - Để tránh xung đột file SQL và đảm bảo thứ tự khóa ngoại (Foreign Key), thư mục `backend/src/main/resources/db/migration/` đã được tách sẵn 5 file:
     - `V1__identity_and_profile.sql` — Phụ trách: **TV7 (Lê Bá Tiệp)** (`users`, `refresh_tokens`, `profiles`).
     - `V2__destination_and_landmarks.sql` — Phụ trách: **TV3 (Lê Hiền Anh)** (`destinations`, `landmarks`).
     - `V3__visit_event_and_reward.sql` — Phụ trách: **TV2 (Lê Bá Ninh)** (`cultural_events`, `visits`, `rewards`).
     - `V4__media_and_recap.sql` — Phụ trách: **TV5 (Lương Quỳnh Anh)** (`upload_sessions`, `photos`, `recaps`).
     - `V5__knowledge_and_rag.sql` — Phụ trách: **TV4 (Nguyễn Minh Lương)** (`knowledge_chunks`, `qa_conversations`, `qa_messages`).
   - Nếu cần bổ sung cột hoặc bảng sau khi `V1`–`V5` đã merge vào `main`, hãy tạo file migration mới (`V6__<tv>_<mô_tả>.sql`), tuyệt đối không sửa lại file đã merge.
3. **Nguyên tắc 3 — Tách biệt File Tài nguyên & Layout trên Android:**
   - Mỗi màn hình sử dụng Class Fragment và file layout XML riêng biệt (`fragment_explore_home.xml`, `fragment_visit_map.xml`, `fragment_camera.xml`, `fragment_travel_journal.xml`).
   - Trong `res/values/strings.xml`, mỗi thành viên chỉ thêm chuỗi vào đúng khối comment phân vùng của mình (`<!-- [TV1] -->` .. `<!-- [TV7] -->`) để không bị conflict ở dòng cuối file.
   - Khi tick `[x]` trong `TODO.md` ở Bước 3, luôn chạy `git fetch origin && git merge origin/main` **trước** khi sửa `TODO.md` để lấy trạng thái tick mới nhất của các bạn khác.
4. **Nguyên tắc 4 — Khóa Hợp đồng API (Contract-First) trước khi Code Song song:**
   - Khi Backend/AI và Android cùng làm một tính năng, hai bên tuân thủ đúng cấu trúc JSON Request/Response trong [03_DATA_AND_CONTRACT.md](03_DATA_AND_CONTRACT.md). Nhờ đó Android dùng Mock DTO và Backend/AI viết logic độc lập mà không phải chờ nhau.
5. **Nguyên tắc 5 — Tuyệt đối Không Commit File Sinh tự động & Dữ liệu Nhạy cảm:**
   - Không bao giờ commit thư mục `build/`, `target/`, `.gradle/`, `.idea/`, `__pycache__/`, `.venv/`, trọng số mô hình nặng (`.pt`, `.onnx`), tệp `.env`, khóa bí mật hay ảnh cá nhân vào Git.

---

## 5. Quy chuẩn Đặt tên & Viết Code theo Từng Nền tảng

### 5.1. Backend: Java Spring Boot Modulith (`backend/`)

#### Cấu trúc Package trong mỗi Module
```text
vn.edu.usth.hanoilens.modules.<module_name>
├── controller/                // *Controller.java (@RestController)
├── service/                   // *Service.java (@Service, @Transactional)
├── entity/                    // JPA Entities (@Entity)
├── repository/                // *Repository.java (Spring Data JPA)
└── adapter/                   // *Adapter.java (S3StorageAdapter, EventSourceParserAdapter, AiInferenceClientAdapter)
```

#### Quy tắc Đặt tên & Coding Rules (Backend)
- **Class / Interface / Record:** `PascalCase` (`DestinationController`, `VisitService`, `S3StorageAdapter`).
- **Method / Biến / JSON field:** `camelCase` (`checkArrival`, `distanceMeters`, `defaultLocationPrecision`).
- **Hằng số & Giá trị Enum:** `UPPER_SNAKE_CASE` (`PENDING_REVIEW`, `RECOGNIZED`, `UNKNOWN`).
- **Bảng & Cột PostgreSQL:** `snake_case` số nhiều cho tên bảng (`destinations`, `cultural_events`, `knowledge_chunks`), `snake_case` số ít cho tên cột (`destination_id`, `started_at`, `location_precision`).
- **Quy tắc Bảo mật & Kiến trúc bắt buộc:**
  - Controller **không bao giờ** trả trực tiếp JPA `@Entity` ra ngoài HTTP; bắt buộc ánh xạ sang DTO đã lọc quyền riêng tư (`LocationPrecision`).
  - Mọi truy vấn đọc/sửa/xóa tài nguyên cá nhân (`Visit`, `Photo`, `Reward`, `Recap`) trong Repository/Service phải kèm điều kiện `user_id = :authenticatedUserId` để chống lỗi **IDOR/BOLA**.
  - Không ghi log chứa Access Token, Refresh Token, Presigned URL hay tọa độ GPS chính xác (`latitude`, `longitude`) của người dùng.
  - Luôn giữ bài kiểm thử `ModularityTests.java` ở trạng thái xanh (PASS).

---

### 5.2. Mobile Client: Android Native — Java & XML (`android-client/`)

#### Quy tắc Đặt tên Class Java (Android)
- **Activity / Fragment:** `PascalCase` khớp 100% với tên file khung đã tạo trong dự án:
  - `LoginActivity`, `LandingActivity`, `ProfileFragment` (`ui/auth/`) — **TV7**
  - `ExploreHomeFragment`, `HeritagePreviewFragment`, `HeritageExploreFragment` (`ui/explore/`) — **TV3**
  - `CombinedMapFragment`, `VisitMapFragment` (`ui/map/`) — **TV3 & TV2**
  - `StampCollectionFragment`, `RewardDialogFragment` (`ui/visit/`) — **TV2**
  - `EventFeedFragment`, `EventDetailFragment`, `EventMemoryFragment` (`ui/events/`) — **TV2 & TV5**
  - `LandmarkInfoFragment`, `HistoricalRagChatFragment` (`ui/ai/`) — **TV1, TV4 & TV7**
  - `CameraCaptureFragment`, `PhotoEditFragment` (`ui/camera/`) — **TV5 & TV1**
  - `TravelJournalFragment`, `RecapDetailFragment` (`ui/journal/`) — **TV5**
- **ViewModel / Repository:** `ExploreViewModel`, `VisitViewModel`, `JournalViewModel`, `AiChatViewModel`.

#### Quy tắc Đặt tên File XML & View ID (`snake_case`)
- **File Layout XML:** `activity_main.xml`, `fragment_explore_home.xml`, `fragment_visit_map.xml`, `fragment_camera.xml`, `fragment_travel_journal.xml`.
- **Đặt tên `android:id` trong XML (`<tiền_tố_view>_<màn_hình>_<mô_tả>`):**
  - `btn_` (Button): `@+id/btn_preview_ask_ai`, `@+id/btn_visit_finish_here`
  - `tv_` (TextView): `@+id/tv_landmark_title`, `@+id/tv_arrival_distance`
  - `iv_` (ImageView): `@+id/iv_photo_stamped_preview`
  - `rv_` (RecyclerView): `@+id/rv_heritage_landmarks`
  - `edt_` (EditText): `@+id/edt_ai_question`

#### Quy tắc Viết Code Android Bắt buộc
1. **Sử dụng ViewBinding 100%:** Cấm sử dụng `findViewById()`. Trong `Fragment`, luôn gán `binding = null` bên trong `onDestroyView()` để chống rò rỉ bộ nhớ.
2. **Kiến trúc MVVM:** `Activity`/`Fragment` chỉ lắng nghe `LiveData` từ `ViewModel`; tuyệt đối không gọi trực tiếp Retrofit hay truy vấn Room Database trên UI Thread.
3. **Bảo vệ Quyền riêng tư Vị trí trên Thiết bị:** Tọa độ đọc từ `FusedLocationProviderClient` phục vụ `VisitMapFragment` và kiểm tra đến nơi chỉ giữ trên RAM, không ghi xuống `SharedPreferences`, file log hay SQLite cục bộ.

---

### 5.3. Dịch vụ AI: Python FastAPI (`ai-service/`)

#### Cấu trúc Thư mục Chuẩn
```text
ai-service/
├── requirements.txt
├── app/
│   ├── main.py                  # Điểm khởi chạy FastAPI & gắn 2 router (TV7 khởi tạo)
│   ├── api/
│   │   ├── routes_vision.py     # POST /internal/v1/vision/classify (TV1 phụ trách)
│   │   └── routes_rag.py        # POST /internal/v1/rag/answer (TV4 phụ trách)
│   ├── vision/
│   │   ├── preprocess.py        # Tiền xử lý & chuẩn hóa ảnh đầu vào (TV1)
│   │   └── classifier.py        # Model 1 inference & ngưỡng UNKNOWN (TV1)
│   └── rag/
│       ├── ingest_corpus.py     # Chia nhỏ tư liệu lịch sử & lưu vào pgvector (TV4)
│       ├── retriever.py         # Truy xuất vector theo destinationId / landmarkId (TV4)
│       └── generator.py         # Model 2 sinh câu trả lời kèm kiểm chứng citations (TV4)
└── tests/
    └── test_inference_contracts.py # Kiểm thử hợp đồng JSON, ngưỡng UNKNOWN & citations (TV1, TV4, TV6)
```

#### Quy tắc Viết Code Python (PEP 8 & Type Hints)
- **Tên file, module, hàm, biến:** `snake_case` (`predict_landmark`, `retrieve_grounded_chunks`, `generate_grounded_answer`).
- **Tên Class & Pydantic Model:** `PascalCase` (`VisionClassifyRequest`, `VisionClassifyResponse`, `RagAnswerRequest`).
- **Hằng số:** `UPPER_SNAKE_CASE` (`DEFAULT_CONFIDENCE_THRESHOLD = 0.75`, `UNKNOWN_RESULT = "UNKNOWN"`).
- **Quy tắc Bắt buộc đối với AI Service:**
  - Mọi hàm xử lý và endpoint FastAPI **bắt buộc có đầy đủ Type Hints** và xác thực đầu vào/đầu ra bằng **Pydantic**.
  - **Model 1 (Vision — TV1):** Khi `confidence < 0.75`, bắt buộc trả về `result = "UNKNOWN"` và `landmarkId = None`.
  - **Model 2 (RAG — TV4):** Mọi câu trả lời sinh ra phải đính kèm mảng `sources` trích dẫn từ các `knowledge_chunks` truy xuất được trong `pgvector`; nếu không tìm thấy tư liệu phù hợp, trả về thông báo từ chối lịch sự, tuyệt đối không tự suy diễn thông tin lịch sử.
