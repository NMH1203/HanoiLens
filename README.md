# Hanoi Lens

Mobile-first application for exploring Hanoi's scenic spots and landmarks through photography-led journeys.

The workspace is organized into:

- `apps/hanoi-lens`: React + Vite product prototype.
- `docs`: product brief, diagrams, and project documentation.
- `prototypes`: earlier static HTML/CSS exploration kept for reference.

See [the product brief](docs/HANOI_LENS_PRODUCT_BRIEF.md) for the current direction.

Current technical documentation:

- [System architecture](docs/ARCHITECTURE.md)
- [REST API contract](docs/API.md)
- [MVP security and privacy baseline](docs/SECURITY.md)
- [Thành viên nhóm](docs/TEAM.md)

## Ý tưởng sản phẩm hiện tại

Hanoi Lens hướng người dùng tự do đi quanh Hà Nội và khám phá thành phố dưới góc nhìn của một nhiếp ảnh gia. Ứng dụng không ép người dùng đi theo tuyến đường hoặc hoàn thành một danh sách cố định.

Người dùng chọn một địa điểm, xem trang thông tin rồi quyết định có muốn đến hay không. Nếu muốn đến, ứng dụng mở bản đồ chỉ đường; GPS chỉ dùng để xác nhận khi người dùng đã vào vùng địa danh. Tại đó, người dùng có thể chủ động bắt đầu khám phá, tự do đi lại, chụp ảnh và kết thúc bất cứ lúc nào.

Mỗi trang địa điểm luôn có lối vào trợ lý Historical RAG và gợi ý chụp đẹp theo góc chụp, bố cục, ánh sáng và thời điểm. Đây là nội dung hướng dẫn tĩnh có thể dùng ngay cả khi AI tạm thời không hoạt động; ứng dụng không phân tích camera theo thời gian thực.

### Album hành trình

Ảnh, địa điểm đã ghé và tiến trình của mỗi lần khám phá được lưu thành một album hành trình. Người dùng có thể xem album sau khi kết thúc mà không bị ứng dụng tự động chuyển sang trang recap.

Album có thể bao gồm:

- Địa danh và các khu vực đã ghé.
- Ngày giờ bắt đầu và kết thúc.
- Những bức ảnh được chụp trong chuyến đi.
- Kỹ thuật chụp hoặc xử lý ảnh đã sử dụng.
- Ghi chú và cảm xúc của người dùng.
- Recap theo địa danh hoặc theo tháng.

### Dấu hành trình — Journey Stamp

Ảnh trong album có thể được gắn một lớp thông tin trực quan gọi là **Dấu hành trình**. Đây là dạng `photo watermark`, `timestamp overlay` hoặc `location stamp` được thiết kế theo phong cách Hanoi Lens.

Dấu hành trình có thể chứa:

- Tên địa danh.
- Quận hoặc khu vực.
- Ngày và giờ chụp.
- Thời tiết.
- Tọa độ hoặc vị trí gần đúng.
- Tên chuyến khám phá.
- Logo hoặc nhận diện Hanoi Lens.

Ví dụ:

```text
VĂN MIẾU – QUỐC TỬ GIÁM
Đống Đa, Hà Nội
07:42 · 01.10.2026
Hanoi Lens
```

Người dùng có thể lựa chọn nhiều phong cách dấu khác nhau:

- Minimal: chỉ tên địa danh và ngày.
- Postcard: trình bày như một tấm bưu thiếp.
- Film frame: thông tin nằm trên viền phim.
- Map stamp: có bản đồ hoặc tọa độ nhỏ.
- Heritage stamp: thiết kế riêng theo từng di tích.
- Weather stamp: thêm nhiệt độ và thời tiết.

Ảnh gốc luôn được giữ nguyên. Dấu hành trình chỉ được áp dụng trên phiên bản dùng trong album hoặc khi người dùng xuất và chia sẻ ảnh.

Vì lý do riêng tư, người dùng được lựa chọn mức độ hiển thị vị trí:

- Chỉ hiển thị tên địa danh.
- Chỉ hiển thị quận hoặc khu vực.
- Hiển thị tọa độ gần đúng.
- Hiển thị tọa độ chính xác.
- Không hiển thị vị trí.

Ngoài thông tin được hiển thị trực tiếp trên ảnh, vị trí và ngày giờ cũng có thể được lưu dưới dạng metadata để phục vụ việc sắp xếp album và tạo recap.

## Run the prototype

```bash
cd apps/hanoi-lens
pnpm install
pnpm dev
```
