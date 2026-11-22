-- =============================================================================
-- V9 — Seed: Hanoi heritage destinations (historical relics) and their landmarks
-- Owner : Lê Hiền Anh (destination)
--
-- * boundary_geom holds the site footprint (POLYGON, or MULTIPOLYGON for a split site), SRID 4326 lon/lat.
-- * The geofence stays CIRCLE, as required by docs/API.md §5 and openapi.yaml (Geofence.type = CIRCLE) and by
--   the on-device Android geofence (ADR 013). The circle is the smallest one around the boundary:
--   centre = centre of ST_MinimumBoundingCircle, radius = farthest boundary vertex in metres, rounded up.
--   Văn Miếu keeps the centre and 250 m radius used in docs/API.md and VisitFlowTest (it covers the boundary).
-- * Boundaries are simplified site footprints (a few vertices, roughly +/- 20-50 m); landmark points are placed
--   to within a few tens of metres. Refine both against a survey or OpenStreetMap before relying on them.
-- * District names are the pre-2025 urban districts (quận/huyện), as used elsewhere in the project.
-- * Fixed UUIDv7-shaped ids (prefix 0193b6a0-) so other seeds and tests can reference these rows. A destination
--   whose slug already exists is left untouched, and landmarks attach to it by slug.
-- * 15 sites in total: 9 historical relics (ids ...0001-0009) + 6 more (ids ...000a-000f): Nhà hát Lớn, Nhà thờ Lớn,
--   Lăng Chủ tịch Hồ Chí Minh - Quảng trường Ba Đình, Khu di tích Phủ Chủ tịch, Đền Bạch Mã, Chùa Láng.
-- * Content (summary, history, ...) is English; proper names stay in Vietnamese.
-- =============================================================================

INSERT INTO destinations (id, slug, name, district, area, status, is_featured, summary, history_text,
                          architecture_text, significance_text, best_time, estimated_minutes,
                          geofence_type, center_lat, center_lng, radius_m, boundary_geom)
SELECT v.id, v.slug, v.name, v.district, v.area, 'PUBLISHED', v.is_featured, v.summary, v.history_text,
       v.architecture_text, v.significance_text, v.best_time, v.estimated_minutes,
       'CIRCLE', c.lat, c.lng, COALESCE(v.radius_m, r.radius_m), g.geom
FROM (VALUES
    -- 1. Văn Miếu - Quốc Tử Giám
    (CAST('0193b6a0-0000-7000-8000-000000000001' AS uuid), 'van-mieu-quoc-tu-giam', 'Văn Miếu - Quốc Tử Giám', 'Đống Đa', 'CENTRAL',
     TRUE, 'Temple of Literature: Hanoi''s temple to Confucius (1070) and Vietnam''s first national university (1076).',
     'Founded in 1070 under Emperor Lý Thánh Tông to honour Confucius; in 1076 Emperor Lý Nhân Tông opened the Quốc Tử Giám, the first national academy. The 82 doctoral steles record the laureates of the royal examinations held from 1442 to 1779.',
     'A walled complex laid out along a north-south axis in five successive courtyards, from Văn Miếu Gate through Khuê Văn Các and the Well of Heavenly Clarity to the main sanctuary and the Thái Học house.',
     'Special National Relic. The 82 doctoral steles are inscribed in UNESCO''s Memory of the World Register (2011).',
     'EARLY_MORNING', 90,
     CAST(21.028511 AS numeric), CAST(105.835536 AS numeric), CAST(250 AS integer),
     'POLYGON((105.834800 21.027300, 105.836400 21.027300, 105.836400 21.030600, 105.834800 21.030600, 105.834800 21.027300))'),
    -- 2. Hoàng thành Thăng Long
    (CAST('0193b6a0-0000-7000-8000-000000000002' AS uuid), 'hoang-thanh-thang-long', 'Hoàng thành Thăng Long', 'Ba Đình', 'CENTRAL',
     TRUE, 'Imperial Citadel of Thăng Long, political centre of Vietnam for thirteen centuries and a UNESCO World Heritage Site.',
     'Built in 1010 when Emperor Lý Thái Tổ moved the capital to Thăng Long on the foundations of the 7th-century Chinese citadel. It remained the seat of power under the Lý, Trần and Lê dynasties; the Nguyễn rebuilt it as a provincial citadel, and in the 20th century it hosted the French army and then the headquarters of the Vietnam People''s Army.',
     'Central sector along the north-south axis: Đoan Môn gate, the stone dragon steps of Kính Thiên palace, Hậu Lâu and Bắc Môn. Across Hoàng Diệu street, the 18 Hoàng Diệu excavation shows layered foundations from the 7th to the 19th century.',
     'UNESCO World Heritage Site (2010) and Special National Relic.',
     'MORNING', 120,
     CAST(NULL AS numeric), CAST(NULL AS numeric), CAST(NULL AS integer),
     'MULTIPOLYGON(((105.838800 21.032000, 105.842500 21.032000, 105.842500 21.040000, 105.838800 21.040000, 105.838800 21.032000)), ((105.836800 21.034000, 105.838300 21.034000, 105.838300 21.036600, 105.836800 21.036600, 105.836800 21.034000)))'),
    -- 3. Hồ Hoàn Kiếm - Đền Ngọc Sơn
    (CAST('0193b6a0-0000-7000-8000-000000000003' AS uuid), 'ho-hoan-kiem-den-ngoc-son', 'Hồ Hoàn Kiếm - Đền Ngọc Sơn', 'Hoàn Kiếm', 'CENTRAL',
     TRUE, 'Lake of the Returned Sword and the Temple of the Jade Mountain, the historic heart of Hanoi.',
     'The lake takes its name from the legend of Emperor Lê Lợi returning the heavenly sword to the Golden Turtle God after defeating the Ming in the 15th century. Ngọc Sơn temple was rebuilt in its present form in the 19th century, largely through the scholar Nguyễn Văn Siêu.',
     'The temple stands on Jade Islet, reached by the red wooden Thê Húc bridge; Pen Tower and Ink Slab gate stand on the shore. Turtle Tower stands on a smaller islet in the middle of the lake.',
     'Special National Relic (Hồ Gươm cluster, including Ngọc Sơn temple).',
     'EARLY_MORNING', 60,
     CAST(NULL AS numeric), CAST(NULL AS numeric), CAST(NULL AS integer),
     'POLYGON((105.851900 21.031600, 105.853000 21.031400, 105.853800 21.030600, 105.854200 21.029000, 105.854200 21.026800, 105.853600 21.025100, 105.852600 21.024500, 105.851500 21.024800, 105.850900 21.026200, 105.850800 21.028000, 105.851100 21.030000, 105.851900 21.031600))'),
    -- 4. Chùa Một Cột
    (CAST('0193b6a0-0000-7000-8000-000000000004' AS uuid), 'chua-mot-cot', 'Chùa Một Cột', 'Ba Đình', 'CENTRAL',
     FALSE, 'One Pillar Pagoda, a lotus-shaped shrine on a single pillar first built in 1049.',
     'Built in 1049 by Emperor Lý Thái Tông after a dream of the bodhisattva Avalokiteśvara seated on a lotus. Destroyed by French troops in 1954, it was rebuilt in 1955 following the original design.',
     'A small wooden shrine on a single stone pillar rising from a square lotus pond, next to the Diên Hựu pagoda.',
     'Special National Relic.',
     'MORNING', 30,
     CAST(NULL AS numeric), CAST(NULL AS numeric), CAST(NULL AS integer),
     'POLYGON((105.832900 21.035200, 105.834100 21.035200, 105.834100 21.036400, 105.832900 21.036400, 105.832900 21.035200))'),
    -- 5. Chùa Trấn Quốc
    (CAST('0193b6a0-0000-7000-8000-000000000005' AS uuid), 'chua-tran-quoc', 'Chùa Trấn Quốc', 'Tây Hồ', 'NORTH',
     TRUE, 'Trấn Quốc Pagoda, Hanoi''s oldest pagoda, on an islet of West Lake.',
     'Founded in the 6th century under Emperor Lý Nam Đế on the bank of the Red River and moved to its present islet on West Lake in 1615.',
     'Main hall, ancestor hall and bell tower around a courtyard; the 15-metre Lục Độ Đài Sen stupa (1998) holds sixty-six Buddha statues.',
     'National Relic.',
     'LATE_AFTERNOON', 45,
     CAST(NULL AS numeric), CAST(NULL AS numeric), CAST(NULL AS integer),
     'POLYGON((105.835800 21.047300, 105.837300 21.047300, 105.837300 21.048700, 105.835800 21.048700, 105.835800 21.047300))'),
    -- 6. Đền Quán Thánh
    (CAST('0193b6a0-0000-7000-8000-000000000006' AS uuid), 'den-quan-thanh', 'Đền Quán Thánh', 'Ba Đình', 'CENTRAL',
     FALSE, 'Quán Thánh Temple, the northern guardian temple of Thăng Long, dedicated to Huyền Thiên Trấn Vũ.',
     'One of the Four Sacred Temples (Thăng Long Tứ Trấn) guarding the capital, founded under Emperor Lý Thái Tổ. The bronze statue of Huyền Thiên Trấn Vũ was cast in 1677.',
     'Triple gate, front hall and sanctuary with a 3.96-metre black bronze statue weighing about four tonnes.',
     'National Relic.',
     'MORNING', 30,
     CAST(NULL AS numeric), CAST(NULL AS numeric), CAST(NULL AS integer),
     'POLYGON((105.836100 21.042500, 105.837100 21.042500, 105.837100 21.043300, 105.836100 21.043300, 105.836100 21.042500))'),
    -- 7. Di tích Nhà tù Hỏa Lò
    (CAST('0193b6a0-0000-7000-8000-000000000007' AS uuid), 'nha-tu-hoa-lo', 'Di tích Nhà tù Hỏa Lò', 'Hoàn Kiếm', 'CENTRAL',
     FALSE, 'Hỏa Lò Prison, built by the French colonial government in 1896, now a museum.',
     'Built in 1896 as Maison Centrale to detain Vietnamese revolutionaries. During the Vietnam War it held American airmen, who nicknamed it the ''Hanoi Hilton''. Most of the prison was demolished in the 1990s; the remaining part became a museum.',
     'The preserved south-east part of the original prison, with its main gate, cell blocks and the drain through which prisoners escaped in 1945.',
     'National Relic of revolutionary history.',
     'MORNING', 60,
     CAST(NULL AS numeric), CAST(NULL AS numeric), CAST(NULL AS integer),
     'POLYGON((105.846000 21.024900, 105.846900 21.024900, 105.846900 21.025700, 105.846000 21.025700, 105.846000 21.024900))'),
    -- 8. Ô Quan Chưởng
    (CAST('0193b6a0-0000-7000-8000-000000000008' AS uuid), 'o-quan-chuong', 'Ô Quan Chưởng', 'Hoàn Kiếm', 'CENTRAL',
     FALSE, 'Quan Chưởng Gate, the last surviving gate of the old Hanoi city wall (1817).',
     'Rebuilt in 1817 as the Đông Hà gate of the eastern wall. It takes its name from the officer who commanded its defence against the French in 1873.',
     'A two-storey brick gate with three arched passages and a watchtower above.',
     'National Relic.',
     'AFTERNOON', 15,
     CAST(NULL AS numeric), CAST(NULL AS numeric), CAST(NULL AS integer),
     'POLYGON((105.852600 21.036700, 105.853300 21.036700, 105.853300 21.037300, 105.852600 21.037300, 105.852600 21.036700))'),
    -- 9. Thành Cổ Loa
    (CAST('0193b6a0-0000-7000-8000-000000000009' AS uuid), 'thanh-co-loa', 'Thành Cổ Loa', 'Đông Anh', 'OUTSKIRTS',
     FALSE, 'Cổ Loa Citadel, the spiral-walled capital of King An Dương Vương (3rd century BC).',
     'Capital of the Âu Lạc kingdom under King An Dương Vương in the 3rd century BC, and again the capital of Ngô Quyền in 939. The legend of the magic crossbow and Princess Mỵ Châu is tied to the site.',
     'Remains of three concentric earthen ramparts in a spiral; inside the inner citadel stand the Thượng temple, the Ngự Triều Di Quy communal house, the Mỵ Châu shrine and the Ngọc well.',
     'Special National Relic.',
     'MORNING', 120,
     CAST(NULL AS numeric), CAST(NULL AS numeric), CAST(NULL AS integer),
     'POLYGON((105.869000 21.112000, 105.878000 21.112000, 105.878000 21.119000, 105.869000 21.119000, 105.869000 21.112000))'),
    -- 9b. Nhà hát Lớn Hà Nội
    (CAST('0193b6a0-0000-7000-8000-00000000000a' AS uuid), 'nha-hat-lon-ha-noi', 'Nhà hát Lớn Hà Nội', 'Hoàn Kiếm', 'CENTRAL',
     TRUE, 'Hanoi Opera House, a French colonial landmark opened in 1911 and the stage of the 1945 August Revolution.',
     'Built between 1901 and 1911 by the French colonial government on the model of the Palais Garnier in Paris. In August 1945 the square in front of it was a gathering place of the August Revolution, and the building itself was occupied by revolutionary forces.',
     'A neoclassical building with a colonnaded facade, a domed foyer and a 900-seat horseshoe auditorium, set at the end of Tràng Tiền street with the Revolution Square (Quảng trường Cách mạng Tháng Tám) in front.',
     'Architectural landmark of the French colonial period and a symbol of the 1945 August Revolution.',
     'EVENING', 45,
     CAST(NULL AS numeric), CAST(NULL AS numeric), CAST(NULL AS integer),
     'POLYGON((105.856800 21.023600, 105.858600 21.023600, 105.858600 21.025100, 105.856800 21.025100, 105.856800 21.023600))'),
    -- 10. Nhà thờ Lớn Hà Nội
    (CAST('0193b6a0-0000-7000-8000-00000000000b' AS uuid), 'nha-tho-lon-ha-noi', 'Nhà thờ Lớn Hà Nội', 'Hoàn Kiếm', 'CENTRAL',
     FALSE, 'St. Joseph''s Cathedral, the neo-Gothic cathedral of Hanoi, inaugurated in 1887.',
     'Built on the site of the Báo Thiên pagoda, one of the Four Great Works of the Lý dynasty, and inaugurated at Christmas 1887 as the seat of the Archdiocese of Hanoi.',
     'A neo-Gothic church inspired by Notre-Dame de Paris, with two 31-metre bell towers, a stone-grey facade, pointed arches and stained-glass windows, standing on a plaza at the end of Nhà Thờ street.',
     'Architectural landmark of the French colonial period and the main Catholic church of Hanoi.',
     'MORNING', 30,
     CAST(NULL AS numeric), CAST(NULL AS numeric), CAST(NULL AS integer),
     'POLYGON((105.848200 21.028200, 105.849600 21.028200, 105.849600 21.029200, 105.848200 21.029200, 105.848200 21.028200))'),
    -- 11. Lăng Chủ tịch Hồ Chí Minh - Quảng trường Ba Đình
    (CAST('0193b6a0-0000-7000-8000-00000000000c' AS uuid), 'lang-chu-tich-ho-chi-minh-quang-truong-ba-dinh', 'Lăng Chủ tịch Hồ Chí Minh - Quảng trường Ba Đình', 'Ba Đình', 'CENTRAL',
     TRUE, 'Ho Chi Minh Mausoleum and Ba Đình Square, where President Hồ Chí Minh read the Declaration of Independence on 2 September 1945.',
     'On 2 September 1945, Hồ Chí Minh read the Declaration of Independence of the Democratic Republic of Vietnam on Ba Đình Square. The mausoleum was built from 1973 to 1975 and opened on 2 September 1975.',
     'A granite mausoleum with a colonnade and a tiered roof, facing the square on its west side; the square is marked by a grid of grass plots, with the Hanoi flag staff and the Monument to War Heroes nearby.',
     'National political and historical site of modern Vietnam.',
     'EARLY_MORNING', 60,
     CAST(NULL AS numeric), CAST(NULL AS numeric), CAST(NULL AS integer),
     'POLYGON((105.834000 21.036500, 105.836500 21.036500, 105.836500 21.039000, 105.834000 21.039000, 105.834000 21.036500))'),
    -- 12. Khu di tích Chủ tịch Hồ Chí Minh tại Phủ Chủ tịch
    (CAST('0193b6a0-0000-7000-8000-00000000000d' AS uuid), 'khu-di-tich-phu-chu-tich', 'Khu di tích Chủ tịch Hồ Chí Minh tại Phủ Chủ tịch', 'Ba Đình', 'CENTRAL',
     FALSE, 'Presidential Palace relic site, with the stilt house where Hồ Chí Minh lived and worked from 1958 to 1969.',
     'The palace was built between 1901 and 1906 as the residence of the Governor-General of French Indochina. Hồ Chí Minh chose not to live in it; from 1958 he lived and worked in a simple stilt house in the garden until his death in 1969.',
     'The yellow French colonial palace stands in a park of fruit trees and a carp pond; the stilt house of wood and bamboo overlooks the pond.',
     'Special National Relic (2012).',
     'MORNING', 75,
     CAST(NULL AS numeric), CAST(NULL AS numeric), CAST(NULL AS integer),
     'POLYGON((105.831000 21.037500, 105.833800 21.037500, 105.833800 21.040500, 105.831000 21.040500, 105.831000 21.037500))'),
    -- 13. Đền Bạch Mã
    (CAST('0193b6a0-0000-7000-8000-00000000000e' AS uuid), 'den-bach-ma', 'Đền Bạch Mã', 'Hoàn Kiếm', 'CENTRAL',
     FALSE, 'Bạch Mã Temple, the eastern guardian of Thăng Long among the Four Sacred Temples, dedicated to the White Horse deity.',
     'Founded in the 9th century according to tradition, the temple is linked to the legend that King Lý Thái Tổ saw a white horse show him the line of the citadel walls when he founded Thăng Long in 1010. The current buildings date mostly from the 18th and 19th centuries.',
     'A small temple in the old quarter on Hàng Buồm street, with a front gate, a ceremonial hall and a sanctuary holding a bronze statue of the guardian deity and a white horse.',
     'National Relic; one of the Four Sacred Temples (Thăng Long Tứ Trấn).',
     'MORNING', 20,
     CAST(NULL AS numeric), CAST(NULL AS numeric), CAST(NULL AS integer),
     'POLYGON((105.850100 21.034700, 105.850700 21.034700, 105.850700 21.035200, 105.850100 21.035200, 105.850100 21.034700))'),
    -- 14. Chùa Láng
    (CAST('0193b6a0-0000-7000-8000-00000000000f' AS uuid), 'chua-lang', 'Chùa Láng', 'Đống Đa', 'WEST',
     FALSE, 'Láng Pagoda (Chiêu Thiền), a 12th-century temple dedicated to the monk Từ Đạo Hạnh.',
     'Founded in the 12th century under Emperor Lý Anh Tông, and dedicated to the Zen master Từ Đạo Hạnh, regarded as a patron of the Láng village. It was rebuilt several times, notably in the 17th century.',
     'A walled courtyard with a three-entrance gate, a main hall with carved wooden beams, an ancestor hall, and the shrine of Từ Đạo Hạnh in a quiet garden.',
     'National Relic (1962).',
     'MORNING', 40,
     CAST(NULL AS numeric), CAST(NULL AS numeric), CAST(NULL AS integer),
     'POLYGON((105.799500 21.023000, 105.800800 21.023000, 105.800800 21.024200, 105.799500 21.024200, 105.799500 21.023000))')
) AS v (id, slug, name, district, area, is_featured, summary, history_text, architecture_text,
         significance_text, best_time, estimated_minutes, center_lat, center_lng, radius_m, boundary_wkt)
CROSS JOIN LATERAL (SELECT ST_GeomFromText(v.boundary_wkt, 4326) AS geom) g
CROSS JOIN LATERAL (SELECT ST_Centroid(ST_MinimumBoundingCircle(g.geom)) AS pt) m
CROSS JOIN LATERAL (
    SELECT COALESCE(v.center_lat, round(CAST(ST_Y(m.pt) AS numeric), 6)) AS lat,
           COALESCE(v.center_lng, round(CAST(ST_X(m.pt) AS numeric), 6)) AS lng) c
CROSS JOIN LATERAL (
    SELECT CAST(ceil(max(ST_Distance(
               CAST(ST_SetSRID(ST_MakePoint(CAST(c.lng AS double precision), CAST(c.lat AS double precision)), 4326)
                    AS geography),
               CAST((dp).geom AS geography)))) AS integer) AS radius_m
    FROM ST_DumpPoints(g.geom) dp) r
ON CONFLICT (slug) DO NOTHING;

-- Landmarks are attached by slug, so they follow an existing destination row with the same slug.
INSERT INTO landmarks (id, destination_id, name, description, is_checkpoint, location_geom)
SELECT v.id, d.id, v.name, v.description, v.is_checkpoint, ST_SetSRID(ST_MakePoint(v.lng, v.lat), 4326)
FROM (VALUES
    -- Văn Miếu - Quốc Tử Giám
    (CAST('0193b6a0-0001-7000-8000-000001000001' AS uuid), 'van-mieu-quoc-tu-giam', 'Văn Miếu Môn',
     'The main gate on Quốc Tử Giám street, entrance to the first courtyard.', FALSE, 21.027550, 105.835620),
    (CAST('0193b6a0-0001-7000-8000-000001000002' AS uuid), 'van-mieu-quoc-tu-giam', 'Đại Trung Môn',
     'Great Middle Gate between the first and second courtyards.', FALSE, 21.028100, 105.835600),
    (CAST('0193b6a0-0001-7000-8000-000001000003' AS uuid), 'van-mieu-quoc-tu-giam', 'Khuê Văn Các',
     'Pavilion of the Constellation of Literature (1805), the symbol of Hanoi.', TRUE, 21.028550, 105.835570),
    (CAST('0193b6a0-0001-7000-8000-000001000004' AS uuid), 'van-mieu-quoc-tu-giam', 'Thiên Quang Tỉnh',
     'Well of Heavenly Clarity, the square pond of the third courtyard.', FALSE, 21.028850, 105.835550),
    (CAST('0193b6a0-0001-7000-8000-000001000005' AS uuid), 'van-mieu-quoc-tu-giam', 'Bia Tiến sĩ',
     'The 82 doctoral steles on stone turtles flanking the Well of Heavenly Clarity.', TRUE, 21.028880, 105.835300),
    (CAST('0193b6a0-0001-7000-8000-000001000006' AS uuid), 'van-mieu-quoc-tu-giam', 'Đại Thành Môn',
     'Gate of Great Success leading to the sanctuary courtyard.', FALSE, 21.029250, 105.835520),
    (CAST('0193b6a0-0001-7000-8000-000001000007' AS uuid), 'van-mieu-quoc-tu-giam', 'Đại Bái Đường',
     'House of Ceremonies in front of the sanctuary of Confucius.', TRUE, 21.029600, 105.835500),
    (CAST('0193b6a0-0001-7000-8000-000001000008' AS uuid), 'van-mieu-quoc-tu-giam', 'Nhà Thái Học',
     'Rebuilt hall of the former Imperial Academy, in the fifth courtyard.', FALSE, 21.030100, 105.835480),
    -- Hoàng thành Thăng Long
    (CAST('0193b6a0-0001-7000-8000-000002000001' AS uuid), 'hoang-thanh-thang-long', 'Cột cờ Hà Nội',
     'Hanoi Flag Tower (1812), built under the Nguyễn dynasty.', FALSE, 21.032500, 105.839680),
    (CAST('0193b6a0-0001-7000-8000-000002000002' AS uuid), 'hoang-thanh-thang-long', 'Đoan Môn',
     'Main southern gate of the Forbidden City.', TRUE, 21.033950, 105.840000),
    (CAST('0193b6a0-0001-7000-8000-000002000003' AS uuid), 'hoang-thanh-thang-long', 'Điện Kính Thiên',
     'Foundation and dragon steps of the Kính Thiên palace, the main throne hall.', TRUE, 21.035300, 105.840100),
    (CAST('0193b6a0-0001-7000-8000-000002000004' AS uuid), 'hoang-thanh-thang-long', 'Nhà và hầm D67',
     'House and bunker D67, wartime command post of the General Staff.', FALSE, 21.036400, 105.839750),
    (CAST('0193b6a0-0001-7000-8000-000002000005' AS uuid), 'hoang-thanh-thang-long', 'Hậu Lâu',
     'Rear pavilion of the princesses, at the northern end of the axis.', FALSE, 21.036900, 105.840200),
    (CAST('0193b6a0-0001-7000-8000-000002000006' AS uuid), 'hoang-thanh-thang-long', 'Bắc Môn',
     'North Gate (1805), still marked by French artillery shells from 1882.', FALSE, 21.039720, 105.841940),
    (CAST('0193b6a0-0001-7000-8000-000002000007' AS uuid), 'hoang-thanh-thang-long', 'Khu khảo cổ 18 Hoàng Diệu',
     'Archaeological site with foundations from the Đại La to the Nguyễn period.', TRUE, 21.035300, 105.837600),
    -- Hồ Hoàn Kiếm - Đền Ngọc Sơn
    (CAST('0193b6a0-0001-7000-8000-000003000001' AS uuid), 'ho-hoan-kiem-den-ngoc-son', 'Tháp Bút',
     'Pen Tower on Độc Tôn hill, inscribed ''writing on the blue sky''.', FALSE, 21.030450, 105.853100),
    (CAST('0193b6a0-0001-7000-8000-000003000002' AS uuid), 'ho-hoan-kiem-den-ngoc-son', 'Cầu Thê Húc',
     'The red wooden ''Morning Sunlight'' bridge to Jade Islet.', TRUE, 21.030300, 105.852750),
    (CAST('0193b6a0-0001-7000-8000-000003000003' AS uuid), 'ho-hoan-kiem-den-ngoc-son', 'Đền Ngọc Sơn',
     'Temple of the Jade Mountain on Jade Islet.', TRUE, 21.030700, 105.852400),
    (CAST('0193b6a0-0001-7000-8000-000003000004' AS uuid), 'ho-hoan-kiem-den-ngoc-son', 'Tháp Rùa',
     'Turtle Tower on its islet in the middle of the lake.', TRUE, 21.027900, 105.852300),
    (CAST('0193b6a0-0001-7000-8000-000003000005' AS uuid), 'ho-hoan-kiem-den-ngoc-son', 'Tháp Hòa Phong',
     'Hòa Phong tower, last remnant of the Báo Ân pagoda, on the east shore.', FALSE, 21.026800, 105.853850),
    -- Chùa Một Cột
    (CAST('0193b6a0-0001-7000-8000-000004000001' AS uuid), 'chua-mot-cot', 'Chùa Một Cột',
     'The lotus shrine on its single pillar in the Linh Chiểu pond.', TRUE, 21.035850, 105.833600),
    (CAST('0193b6a0-0001-7000-8000-000004000002' AS uuid), 'chua-mot-cot', 'Chùa Diên Hựu',
     'Diên Hựu pagoda behind the One Pillar shrine.', FALSE, 21.035920, 105.833350),
    (CAST('0193b6a0-0001-7000-8000-000004000003' AS uuid), 'chua-mot-cot', 'Cây bồ đề',
     'Bodhi tree given by India in 1958.', FALSE, 21.035650, 105.833500),
    -- Chùa Trấn Quốc
    (CAST('0193b6a0-0001-7000-8000-000005000001' AS uuid), 'chua-tran-quoc', 'Cổng Tam Quan',
     'Three-entrance gate on Thanh Niên road.', FALSE, 21.047750, 105.837080),
    (CAST('0193b6a0-0001-7000-8000-000005000002' AS uuid), 'chua-tran-quoc', 'Chính điện',
     'Main hall of the pagoda.', TRUE, 21.047750, 105.836780),
    (CAST('0193b6a0-0001-7000-8000-000005000003' AS uuid), 'chua-tran-quoc', 'Bảo tháp Lục Độ Đài Sen',
     'Eleven-storey lotus stupa with sixty-six Buddha statues.', TRUE, 21.047950, 105.836500),
    -- Đền Quán Thánh
    (CAST('0193b6a0-0001-7000-8000-000006000001' AS uuid), 'den-quan-thanh', 'Cổng Tam Quan',
     'Triple gate on Quán Thánh street.', FALSE, 21.042750, 105.836700),
    (CAST('0193b6a0-0001-7000-8000-000006000002' AS uuid), 'den-quan-thanh', 'Tượng Huyền Thiên Trấn Vũ',
     'Black bronze statue of the guardian deity (1677).', TRUE, 21.042980, 105.836520),
    -- Di tích Nhà tù Hỏa Lò
    (CAST('0193b6a0-0001-7000-8000-000007000001' AS uuid), 'nha-tu-hoa-lo', 'Cổng Maison Centrale',
     'Original prison gate with the ''Maison Centrale'' inscription.', TRUE, 21.025400, 105.846600),
    (CAST('0193b6a0-0001-7000-8000-000007000002' AS uuid), 'nha-tu-hoa-lo', 'Khu biệt giam',
     'Solitary confinement cells and the death-row area.', FALSE, 21.025250, 105.846400),
    -- Ô Quan Chưởng
    (CAST('0193b6a0-0001-7000-8000-000008000001' AS uuid), 'o-quan-chuong', 'Cổng Ô Quan Chưởng',
     'The central arched passage of the gate.', TRUE, 21.037000, 105.852950),
    -- Thành Cổ Loa
    (CAST('0193b6a0-0001-7000-8000-000009000001' AS uuid), 'thanh-co-loa', 'Đền Thượng',
     'Temple of King An Dương Vương.', TRUE, 21.116000, 105.873300),
    (CAST('0193b6a0-0001-7000-8000-000009000002' AS uuid), 'thanh-co-loa', 'Giếng Ngọc',
     'Jade Well in front of the Thượng temple.', FALSE, 21.115850, 105.873650),
    (CAST('0193b6a0-0001-7000-8000-000009000003' AS uuid), 'thanh-co-loa', 'Đình Ngự Triều Di Quy',
     'Communal house of Cổ Loa village.', FALSE, 21.115200, 105.873500),
    (CAST('0193b6a0-0001-7000-8000-000009000004' AS uuid), 'thanh-co-loa', 'Am Bà Chúa',
     'Shrine of Princess Mỵ Châu.', TRUE, 21.115450, 105.872850),
    -- Nhà thờ lớn Hà Nội
    (CAST('0193b6a0-0001-7000-8000-00000a000001' AS uuid), 'nha-hat-lon-ha-noi', 'Quảng trường Cách mạng Tháng Tám',
     'Revolution Square in front of the Opera House, a gathering place in August 1945.', FALSE, 21.024800, 105.857750),
    (CAST('0193b6a0-0001-7000-8000-00000a000002' AS uuid), 'nha-hat-lon-ha-noi', 'Mặt tiền Nhà hát Lớn',
     'The colonnaded facade and entrance steps of the Opera House.', TRUE, 21.024350, 105.857700),
    (CAST('0193b6a0-0001-7000-8000-00000a000003' AS uuid), 'nha-hat-lon-ha-noi', 'Phòng khán giả',
     'The horseshoe-shaped main auditorium.', FALSE, 21.023950, 105.857700),
    (CAST('0193b6a0-0001-7000-8000-00000b000001' AS uuid), 'nha-tho-lon-ha-noi', 'Sân Nhà thờ',
     'The plaza in front of the cathedral.', FALSE, 21.028900, 105.848950),
    (CAST('0193b6a0-0001-7000-8000-00000b000002' AS uuid), 'nha-tho-lon-ha-noi', 'Mặt tiền Nhà thờ Lớn',
     'The neo-Gothic facade with its two bell towers.', TRUE, 21.028700, 105.848900),
    -- Lăng Chủ tịch Hồ Chí Minh - Quảng trường Ba Đình
    (CAST('0193b6a0-0001-7000-8000-00000c000001' AS uuid), 'lang-chu-tich-ho-chi-minh-quang-truong-ba-dinh', 'Lăng Chủ tịch Hồ Chí Minh',
     'The granite mausoleum on the west side of Ba Đình Square.', TRUE, 21.036850, 105.834700),
    (CAST('0193b6a0-0001-7000-8000-00000c000002' AS uuid), 'lang-chu-tich-ho-chi-minh-quang-truong-ba-dinh', 'Quảng trường Ba Đình',
     'The square where the Declaration of Independence was read on 2 September 1945.', TRUE, 21.037600, 105.835600),
    -- Khu di tích Chủ tịch Hồ Chí Minh tại Phủ Chủ tịch
    (CAST('0193b6a0-0001-7000-8000-00000d000001' AS uuid), 'khu-di-tich-phu-chu-tich', 'Phủ Chủ tịch',
     'The yellow French colonial palace (1901-1906).', TRUE, 21.039000, 105.832800),
    (CAST('0193b6a0-0001-7000-8000-00000d000002' AS uuid), 'khu-di-tich-phu-chu-tich', 'Nhà sàn Bác Hồ',
     'The stilt house where Hồ Chí Minh lived from 1958 to 1969.', TRUE, 21.038300, 105.832200),
    (CAST('0193b6a0-0001-7000-8000-00000d000003' AS uuid), 'khu-di-tich-phu-chu-tich', 'Ao cá',
     'The carp pond between the palace and the stilt house.', FALSE, 21.038600, 105.832500),
    -- Đền Bạch Mã
    (CAST('0193b6a0-0001-7000-8000-00000e000001' AS uuid), 'den-bach-ma', 'Cổng đền Bạch Mã',
     'The front gate of the temple on Hàng Buồm street.', FALSE, 21.034800, 105.850400),
    (CAST('0193b6a0-0001-7000-8000-00000e000002' AS uuid), 'den-bach-ma', 'Chính điện đền Bạch Mã',
     'The sanctuary with the statue of the White Horse deity.', TRUE, 21.035000, 105.850400),
    -- Chùa Láng
    (CAST('0193b6a0-0001-7000-8000-00000f000001' AS uuid), 'chua-lang', 'Cổng Tam Quan chùa Láng',
     'The three-entrance gate of the pagoda.', FALSE, 21.023200, 105.800100),
    (CAST('0193b6a0-0001-7000-8000-00000f000002' AS uuid), 'chua-lang', 'Chính điện chùa Láng',
     'The main hall with carved wooden beams.', TRUE, 21.023600, 105.800150)
) AS v (id, destination_slug, name, description, is_checkpoint, lat, lng)
JOIN destinations d ON d.slug = v.destination_slug
ON CONFLICT (id) DO NOTHING;