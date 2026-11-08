# Hanoi Lens Product Brief

> **Project Reference:** Aligned with the USTH ICT Group Project Topic Proposal (Academic Year 2025–2026).  
> **Topic:** Hanoi Lens — Mobile exploration, cultural event discovery, and photography-led journeys across Hanoi.

---

## 1. Product overview

**Hanoi Lens** is a mobile application that allows users to freely explore Hanoi's landmarks, heritage sites, museums, cultural venues, and scenic locations through photography.

Instead of prescribing fixed tour routes or rigid checklists, the application offers an open-ended experience:
- **Presence-based arrival recognition (Privacy-First Geofencing):** The application only detects whether the user is inside or outside a destination area (zone presence). It does not track continuous movements or require knowing/storing exact coordinates, ensuring complete comfort and user privacy.
- **Account-based journeys:** Users sign in or sign up before exploring, so journeys, unlocked heritages and landmarks, collected stamps, and photos are kept in their account.
- **GPS unlock & stamp collection:** Users preview any heritage and go whenever they like; the app never prescribes routes and there is no planning or marking step. When GPS confirms arrival at a heritage, it is unlocked immediately, its heritage stamp is collected, and its **heritage explore page** opens; from there a visit map of the heritage area shows the user's live position so they can move around, explore, and take photos. Every landmark inside a heritage has its own stamp, collected when the user photographs it and it is recognized (for example Văn Miếu has stamps for the heritage itself, Khuê Văn Các, the drum tower, the bell tower, and so on).
- **Cultural event discovery:** Collects and displays ongoing and upcoming events from approved official sources using deterministic, source-specific parsers and administrator review.
- **Landmark recognition & historical Q&A:** Users can optionally photograph a scene or structure on site to identify what it is (image classification), unlock that landmark, and ask a grounded RAG system about the heritage and the landmarks they have unlocked, with verified citations.
- **Journey albums and personalized recaps:** Automatically organizes photos, visited locations, timestamps, and exploration progress into private journey albums with customizable location stamps, weather overlays, and destination-based or monthly recaps, allowing users to rediscover Hanoi through their own lens.
- **Privacy-aware design:** Operates strictly on an area-presence model; users are completely free from location tracking and retain full control over how location names are displayed on exported photos.

---

## 2. Product vision

Hanoi Lens aims to make discovering Hanoi more engaging, personal, and visually memorable. Instead of treating users as passive consumers of tourist signage or forcing them into structured itineraries, Hanoi Lens encourages self-paced exploration and connects city history, architectural heritage, and contemporary urban life directly to the creative act of photography.

---

## 3. Target users

### Primary users
- **Independent explorers & residents:** People who want to explore Hanoi at their own pace without scheduled guided tours.
- **Photography enthusiasts:** Amateurs and hobbyists who want to capture Hanoi landmarks and preserve their photos with aesthetic journey stamps.
- **Culture, heritage visitors:** Visitors interested in concise, contextual historical insights and upcoming cultural exhibitions or festivals.

### Secondary users
- **System administrators and project maintainers:** The development and operations team overseeing source-specific event parsers, reviewing imported event drafts, monitoring data reliability, and managing destination boundary datasets.

---

## 4. User needs

Users need a seamless mobile experience to:
1. **Find nearby places and events:** Easily spot heritage sites, scenic landmarks, and active cultural events nearby or across Hanoi, shown together on one map.
2. **Contextual knowledge on demand:** Read concise historical and cultural context before deciding to travel and continue using it after arrival without disrupting the physical experience.
3. **Identify what they see:** Photograph a scene or structure on site to learn what it is and ask follow-up historical questions.
4. **Capture and enhance safely:** Apply non-destructive adjustments (crop, straighten, exposure, color correction, filters) while preserving original images.
5. **Preserve meaningful memories:** Keep photos, visited landmarks, timestamps, and journey progress organized in albums with customized journey stamps.
6. **Resume unfinished explorations:** Pause an exploration session and pick it up later without losing progress.
7. **Collect stamps:** Collect a stamp for each heritage they reach and for each landmark they recognize there.
8. **Keep memories of events without a heritage site:** Capture and save photos on an event memory page that adapts to the event's details.
9. **Protect privacy & explore comfortably:** Enjoy open-ended exploration without feeling monitored or tracked; the system only needs to verify presence within a destination zone, eliminating the need to log exact coordinates or continuous travel trails.

---

## 5. Core experience lifecycle

The Hanoi Lens journey consists of five interconnected phases:

```text
[1. Discover] ---> [2. Arrive & Unlock] ---> [3. Visit & Explore] ---> [4. Capture & Edit] ---> [5. Remember & Collect]
```

1. **Discover:** Browse destinations and upcoming cultural events (also together on the map), preview a heritage, ask the AI about it, and optionally get walking directions inside the app.
2. **Arrive & Unlock:** Walk into any heritage's geographic zone where GPS geofencing automatically registers the arrival, unlocks the heritage, and collects its heritage stamp.
3. **Unlock & Explore:** Arrival unlocks the heritage and opens its heritage explore page (history, landmarks). The visit map of the heritage area shows the user's live position; users move around, take photos, and optionally identify what they photographed. When a landmark is identified, the user unlocks it (collecting its stamp) and its landmark info page opens. The AI chat is a tab of the heritage explore page and knows the heritage plus every landmark the user has unlocked; landmark info opens it focused on that landmark.
4. **Capture & Edit:** Take photos, optionally recognize supported landmarks, and perform non-destructive image edits.
5. **Remember & Collect:** When finishing a visit, view the stamp collection. Save progress to the travel journal (an automatic album per heritage, plus albums the user builds from photos of one or several heritages), decorate photos with stickers, nostalgic colour presets and handwritten captions, attach customizable location and weather stamps, and view destination or monthly recaps.

Users choose where to go themselves; the app never prescribes routes or sequential stops. Users can stop at any time. Collecting stamps is an optional goal; there are no badges or gifts (team decision 2026-11-06).

---

## 6. Key features

### 6.1 Destination discovery (Location-Based Services)
- Browse Hanoi's landmarks, heritage monuments, museums, cultural spaces, and scenic spots.
- The app has two explore pages: **Explore Hanoi (home)**, the app-wide page after sign-in (map, heritages, events, travel journal, resume trip), and the **heritage explore page** of the heritage the user is at (§6.2).
- Filter destinations by proximity to current location, district, or thematic interest.
- View detailed destination cards with cultural background and visiting windows.
- Always show the historical RAG assistant entry point on each heritage preview.
- Support continuing paused or unfinished exploration sessions from Explore Hanoi (home); resuming reopens the heritage explore page.
- Show heritages and cultural events together on the map; choosing a point opens the heritage preview or the event detail.
- Each heritage page works as a preview with optional in-app walking directions (the app never opens another map app). There is no marking step: arriving at any heritage starts its visit (team decision 2026-11-06). The app does not recommend which heritages to visit.

### 6.2 GPS-based destination detection (GPS Geofencing)
- Monitor destination boundaries solely to detect arrival and presence (binary zone check: inside/outside).
- Keep cultural content and Historical RAG access visible before travel; geofence entry at any published heritage confirms arrival, immediately unlocks it, and collects its heritage stamp.
- After unlocking, open the heritage explore page (history and landmarks of that heritage); from it the user opens the visit map. Pausing the trip returns to Explore Hanoi (home).
- After arrival, show a visit map of the heritage area with the user's live position, computed on the device only; positions inside the heritage are not sent to or stored on the server.
- **Privacy-by-Design:** Does not track continuous GPS trails or store exact coordinates; once presence is confirmed, raw location data is not retained.
- Eliminate physical QR codes or NFC tag requirements for check-ins, allowing hands-free outdoor exploration.
- Utilize circular geofence thresholds in the prototype, extensible to geographic boundary polygons.

### 6.3 Contextual cultural information & interactive historical Q&A
- Concise, high-value narratives covering history, architecture, symbolism, and cultural significance.
- **Interactive Historical Q&A (Domain RAG):** In-depth historical chat powered by a domain-specific RAG pipeline and fine-tuned LLM, strictly grounded in verified historical records of Hanoi heritage sites.
- **Visual Landmark & Relic Recognition:** Fine-tuned Vision model identifies architectural structures and artifacts directly from user photos, immediately focusing the RAG context.
- Before travel, information and Historical RAG access are available on the heritage preview.
- On site, the AI chat is a tab of the heritage explore page (team decision 2026-11-06). It answers about the heritage and every landmark the user has unlocked there; each newly unlocked landmark adds to what it can answer. Landmark info opens the same chat focused on that landmark.
- Asking a question is always optional and non-intrusive.

### 6.4 Photo capture & landmark recognition
- Capture or select a photo using the native Android camera flow.
- Submit the image to the landmark classification model and return a supported `landmarkId` with confidence.
- Return `UNKNOWN` below the configured confidence threshold instead of silently selecting a landmark.
- Recognition is optional and never affects unlocking the heritage. A confident result tells the user what the structure is and lets them unlock that landmark (collecting its stamp); the landmark then stays open, its landmark info page can be revisited freely, and the AI chat gains its knowledge. `UNKNOWN` unlocks nothing and returns the user to the visit map.
- Real-time composition overlays, lighting analysis, and camera-position guidance are outside the project scope.

### 6.5 Digital image processing & non-destructive editing
- In-app image processing tools: crop, straighten/rotate, exposure, brightness, contrast, saturation, and ready-made nostalgic colour presets.
- Non-destructive pipeline: original photo assets are preserved untouched; adjustments are stored as recipes or applied to exported copies.
- Ready-made stickers (landmark stamps the user has unlocked, postmark, date, text, heart, category badge) can be placed, scaled and rotated on a photo, and captions can be written in ready-made handwriting styles. Stickers and captions are part of the edit recipe; the edited copy can be downloaded (GPS and device data removed) or added to an album.

### 6.6 Photo watermarking & customizable journey stamps
- Apply customizable location stamps, timestamps, weather conditions, or commemorative journey badges to exported photos.
- Diverse stamp templates: Minimalist, Postcard, Film Border, Heritage Monogram, and Map Stamp.
- Dynamic metadata population based on the exploration session and user privacy settings.

### 6.7 Cultural event data aggregation & information extraction
- Ingestion of ongoing and upcoming cultural events, exhibitions, workshops, and performances from official and trusted public sources.
- Source-specific parsing using JSON-LD, RSS, CSS selectors, and deterministic Vietnamese date rules to extract event titles, schedules, venues, and descriptions.
- Validation, deduplication, and administrator review before an imported event is published.
- Seamless connection between cultural events and associated physical heritage sites on the map: an event linked to a heritage opens that heritage's preview and continues the normal heritage flow.
- **Event memory page:** An event with no linked heritage opens a template page whose layout and details adapt to the event, where users take photos and save them to the travel journal as memories.

### 6.8 Journey tracking & session lifecycle
- Track the unlocked heritage and its unlocked landmarks within a visit; one visit covers one heritage.
- Save progress and update the travel journal when the user stops exploring.
- Flexible session lifecycle: start, pause, resume, and conclude journeys at the user's discretion.
- Unfinished explorations are persisted so users can resume their journey at any time.

### 6.9 Journey albums & personalized recaps
- Every photo taken during a visit is kept in the travel journal automatically; there is no separate save step.
- Automatic structuring of photos by destination, date, and exploration session into private albums.
- Automatic journey album: when a journey ends, Hanoi Lens selects the sharpest, non-duplicate photos and arranges them into an album for that journey (for example "Hành trình Văn Miếu"). Users can turn this off in settings, edit it, save it as their own album, or ignore it.
- Users can also pick photos themselves, from one or several heritages, and build an album in a chosen layout.
- Metadata storage linking photos to timestamps, approximate locations, landmarks, and applied edit parameters.
- Generation of destination-specific summaries and monthly exploration recaps to rediscover personal journeys.
- Travel journal view: choose a past trip, see its details and photos, and download photos to the device.

### 6.11 Stamp collection
- Each heritage has one heritage stamp, collected automatically on arrival; each landmark inside it has its own stamp, collected when the user unlocks it after a confident recognition.
- Finishing a visit shows the user's stamp collection for that heritage (collected and still missing). If the user continues exploring, they stay on the heritage explore page; otherwise the visit ends and the travel journal is updated.
- Collected landmark stamps can be used as stickers in the photo editor.
- Stamps are collectibles only. Marked heritages, the Journey Stamp badge, and the random gift were removed (team decision 2026-11-06, ARCHITECTURE ADR 011); exploring, capturing, and saving memories never require stamps.

### 6.10 Privacy-aware controls & presence-only model
- **Presence-Only Geofencing:** The app operates strictly by verifying whether the user is inside a destination area; it does not require knowing or monitoring exact user coordinates.
- **No Continuous Tracking:** Zero continuous route tracking, movement logging, or background surveillance, ensuring users feel completely comfortable exploring outdoor spaces.
- **Coarse Destination Tagging:** Photos and visits associate with the destination name (e.g., "Văn Miếu – Quốc Tử Giám") rather than raw coordinates.
- **User-Controlled Export Metadata:** Users can customize exported photo stamps to display **Destination/district name**, **Approximate coordinates**, or **No location information**.
- **Private by Default:** Exploration journeys and albums remain private on the user's device/account; no content is broadcast or shared publicly without explicit user export.
- **Resource Ownership:** Authentication is followed by server-side authorization for every visit, upload, photo, album, recap, and export to prevent cross-user access.
- **Hardened Media Upload:** Server-generated object keys, short-lived single-object presigned URLs, content decoding, and file-limit checks protect the private media pipeline.
- **EXIF Protection:** Derived and shared files strip GPS/device EXIF by default; only the location representation explicitly selected by the user may be rendered into an export.

---

## 7. Primary navigation structure

- **Explore Hanoi (home):** Destination discovery, thematic collections, a nearby filter (no recommendations), travel journal, and resuming a paused trip.
- **Heritage explore page:** Page of the unlocked heritage, shown after arrival and when resuming. Its own bottom bar has five tabs (team decisions 2026-11-05 and 2026-11-06): **Thông tin** (overview, opening hours, landmark progress, stamps), **Bản đồ** (map of this heritage area = the visit map, with landmark pins), **Camera**, **Địa điểm** (the landmarks inside the heritage), and **Hỏi AI** (chat about the heritage and its unlocked landmarks). Landmarks start locked with a grey preview photo so the user can recognize them; photographing a landmark and getting a confident recognition lets the user unlock it, collect its stamp, and open its landmark info. Unlocking landmarks is optional and never affects unlocking the heritage.
- **Map:** Interactive map showing heritages and cultural events together, plus user location and destination geofences.
- **Visit map:** Map of the heritage area, opened from the heritage explore page, with the user's live position for moving around and taking photos.
- **Landmark info:** Shown after a photo is identified and the landmark unlocked; reopened any time from the landmarks tab. Opens the AI chat focused on that landmark.
- **Stamp collection:** Shown when finishing a visit and from the profile.
- **Camera:** Photo capture, landmark-recognition submission, and quick-edit interface without real-time guidance overlays.
- **Events:** Aggregated stream of ongoing and upcoming cultural events across Hanoi with venue linking.
- **Travel journal (Album):** Past trips, photo collections and downloads, journey stamp exports, the stamp collection, and destination/monthly recaps.
- **Account:** Sign in or sign up is required before exploring; a landing page introduces the app right after sign up.
- **Profile & Settings:** Privacy preferences (location precision level), saved progress, and testing simulator controls.

---

## 8. Functional flow

The detailed flow is in `docs/flows/user-flow.md` and `docs/Diagram/HanoiLens.mdj` (UserFlow).

```text
Open Hanoi Lens -> Sign in / Sign up (landing page after sign up)
     │
     ▼
Explore Hanoi (home) ──> Travel journal / Map (heritages + events) / Heritages / Events / Resume trip
     │
     ├──> Event linked to a heritage ──> continue as heritage
     ├──> Event without heritage ──> Event memory page -> Take photo -> Save memory
     │
     ▼
Heritage preview (Ask AI) -> Want to go? -> Optional directions
     │
     ▼
Arrive at any heritage -> GPS confirms arrival -> Unlock heritage + heritage stamp
     │
     ▼
Heritage explore page (history, landmarks)   <── also opened when resuming a paused trip
     │
     ▼
Visit map (area map + live position)
     ├──> Move around
     ├──> Take photo -> (optional) Identified?
     │        ├── No / UNKNOWN ──> Visit map
     │        └── Yes ──> Unlock landmark + stamp -> Landmark info -> Chat with AI -> Visit map or Heritage explore page
     ├──> Pause ──> Explore Hanoi (home)
     ▼
Finish here -> Stamp collection
     │
     ▼
Continue exploring? ── Yes ──> Heritage explore page
     │
     No ──> Save progress -> Update travel journal
     │
     ▼
Travel journal: past trip -> details & photos -> download -> Explore Hanoi (home) or End
```

---

## 9. Used methods and techniques

The product and AI methods below come from the project proposal. The security and privacy methods (Privacy-Aware Design, Security Hardening) are described in [SECURITY.md §9](SECURITY.md#9-security-and-privacy-methods).

| #  | Method / Technique                 | Category                 | Description & Purpose                                                                                                                                                                                                                                                                                                |
| -- | ---------------------------------- | ------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1  | **GPS Geofencing**                 | Geospatial Awareness     | Detects when users enter or leave the defined geographical area of a landmark (binary presence check) without tracking exact coordinates or continuous movement trails.                                                                                                                                              |
| 2  | **Location-Based Services**        | Local Discovery          | Provides nearby destinations, cultural information, and events based on the user's current location.                                                                                                                                                                                                                 |
| 3  | **Event Data Aggregation**         | Data Ingestion           | Collects event information from selected official websites and trusted public sources.                                                                                                                                                                                                                               |
| 4  | **Deterministic Event Parsing**    | Data Processing          | Uses JSON-LD, RSS, CSS selectors, and date-parsing rules to extract event names, dates, locations, and descriptions from approved sources.                                                                                                                                                                           |
| 5  | **Landmark Image Classification**  | Computer Vision          | Recognizes a supported primary landmark from a submitted photo and returns `landmarkId`, confidence, or `UNKNOWN`.                                                                                                                                                                                                   |
| 6  | **Historical RAG**                 | Generative AI (Model 2)  | Answers historical questions only from verified sources: the backend embeds the question, retrieves the closest chunks from `pgvector` filtered by `destinationId` (heritage preview) or the recognized `landmarkId` (landmark info), and the generator replies with citations or declines when evidence is missing. |
| 7  | **Digital Image Processing**       | Media Processing         | Supports cropping, straightening, exposure adjustment, color correction, filters, and non-destructive editing.                                                                                                                                                                                                       |
| 8  | **Photo Watermarking**             | Visual Identity & Export | Adds customizable location names, timestamps, weather information, or journey stamps to exported photos.                                                                                                                                                                                                             |
| 9  | **Metadata Management**            | Data Architecture        | Stores photo timestamps, approximate locations, visited landmarks, and editing information for album organization.                                                                                                                                                                                                   |
| 10 | **Journey Tracking**               | Session Lifecycle        | Records visited destinations and saves unfinished exploration sessions so users can continue later.                                                                                                                                                                                                                  |
| 11 | **Heritage Rewards**               | Engagement & Rewards     | Shows the user's stamp collection when a visit ends, then grants a Journey Stamp badge and a random gift from the heritage site once every heritage the user marked has been unlocked.                                                                                                                               |
| 12 | **Album and Recap Generation**     | Memory & Summaries       | Organizes photographs and journey information into destination-based and monthly recaps.                                                                                                                                                                                                                             |
| 13 | **Responsive Mobile UI/UX Design** | Client Interface         | Creates an accessible, mobile-first interface suitable for outdoor exploration.                                                                                                                                                                                                                                      |

---

## 10. Core data model

```text
Destination
  ├── id, name, slug, district
  ├── geographicBoundary (PostGIS polygon / circular radius)
  ├── areaMap (visit map of the heritage area)
  ├── culturalInformation (history, architecture, significance)
  ├── recognitionLabel (identifier used by the vision model)
  └── visitingWindows (opening times)

CulturalEvent
  ├── id, title, description
  ├── startDateTime, endDateTime
  ├── venueName, destinationId (optional link to Destination)
  ├── sourceUrl, extractedAt
  └── status (PENDING_REVIEW, PUBLISHED, REJECTED)

Journey (Exploration Session)
  ├── id, userId, destinationId
  ├── status (ACTIVE, PAUSED, ENDED)
  ├── startedAt, pausedAt, endedAt
  ├── unlockedLandmarks [landmarkId, recognitionId, unlockedAt]
  └── photos [photoId]

UserStamp
  ├── userId, stampId (heritage stamp or landmark stamp)
  ├── journeyId
  └── collectedAt

Photo
  ├── id, userId, journeyId, destinationId
  ├── eventId (optional, for event memory photos)
  ├── originalAssetUrl (immutable)
  ├── capturedAt, deviceMetadata
  ├── locationPrecision (NONE, DESTINATION, DISTRICT, APPROXIMATE, EXACT)
  ├── editRecipe (crop, straighten, adjustments, colorPreset, stickers, captions)
  └── appliedWatermark (stampStyle, weatherText, customLabel)

Recap
  ├── id, userId, journeyId (or monthlyPeriod)
  ├── recapType (DESTINATION, MONTHLY)
  ├── selectedPhotoIds []
  ├── summaryText
  └── generatedAt
```

---

## 11. Expected outcomes

The project delivers the 10 deliverables stated in the proposal:

1. **Mobile Application:** A responsive mobile application that enables users to freely discover Hanoi's landmarks, heritage sites, and scenic locations.
2. **GPS-Based Destination Detection:** Automated geofence detection that recognizes arrival, unlocks marked heritages, and displays location-relevant cultural insights without tracking continuous movement.
3. **Landmark Recognition & Historical RAG:** Two AI capabilities classify supported landmarks and provide source-grounded historical answers, on every heritage preview and for landmarks identified on site.
4. **Cultural Event Discovery:** An aggregated feed presenting ongoing and upcoming cultural activities, exhibitions, and festivals across Hanoi.
5. **Heritage Rewards:** Self-marked heritages with a stamp collection, and a Journey Stamp badge and random gift when every marked heritage is unlocked.
6. **Documented Security Baseline:** Threat modeling, access control, hardened photo upload, and location/EXIF privacy, with test evidence (see [SECURITY.md](SECURITY.md)).
7. **Journey Albums:** Structured digital albums preserving user photos, visited locations, timestamps, and exploration progress.
8. **Customizable Journey Stamps & Watermarks:** Flexible location names, timestamps, weather overlays, and commemorative stamps on exported photos.
9. **Personalized Destination & Monthly Recaps:** Curated visual recaps preserving exploration memories and encouraging rediscovery.
10. **Interactive Cultural Engagement:** A more engaging and interactive medium to learn about Hanoi's history, culture, and urban landscape through photography.

---

## 12. MVP scope

The initial Minimum Viable Product (MVP) validates the entire end-to-end exploration and capture loop.

### In Scope for MVP
- Android Native client implemented with Java and XML layouts.
- Interactive destination browsing and map overview.
- Detailed pilot destination (e.g., Văn Miếu – Quốc Tử Giám / Hoàn Kiếm area) with full content and landmarks.
- Always-visible historical RAG access on heritage previews, with a static fallback when AI is unavailable.
- Simulated and live GPS geofence arrival checks.
- Cultural cards and Historical RAG access on the heritage preview before travel; on site, the heritage explore page with an AI chat tab scoped to the heritage and its unlocked landmarks.
- Photo capture/upload and landmark image classification with an `UNKNOWN` confidence fallback.
- On-device digital image processing: crop, straighten, brightness, contrast, nostalgic colour presets, stickers, and handwritten captions.
- Photo watermarking engine supporting customizable Journey Stamps (Minimal, Postcard, Heritage).
- Journey session lifecycle: Start, Pause, Resume, and Complete.
- Private journey album containing captured photos, timestamps, and visited landmarks.
- Destination recap summary view.
- Curated cultural event feed with structured metadata (title, dates, venue, source).
- Combined map of heritages and events.
- Required sign in / sign up with a post-sign-up landing page.
- GPS arrival confirmation that unlocks any heritage, and an on-device visit map with the user's live position.
- Optional landmark recognition on photos taken on site, unlocking landmarks and feeding Historical RAG Q&A.
- Stamp collection: one stamp per heritage and one per landmark.
- Event memory page for events without a linked heritage.
- Travel journal with past trips and photo download.
- User-selectable location privacy preferences.
- JWT authentication with rotating refresh tokens and ownership checks for private resources.
- Hardened presigned photo upload plus automated EXIF/location-leakage verification.
- Threat model and security test evidence for authentication, geofence, media, private resources, and AI flows.
- Simulator Mode for location testing and demonstration without physical transit.

### Out of Scope / Deferred Post-MVP
- Complex live camera sensor analytics (AR framing overlays).
- Object detection with bounding boxes or recognition of multiple artifacts in one image; the MVP uses primary-landmark classification.
- Large-scale scrapers across dozens of unstructured web portals.
- Public social networks, user comments, or public feeds.
- Complex multi-device real-time sync before account stabilization.

---

## 13. Success indicators

- **Rewarding Unlocks:** Users understand that arriving unlocks a heritage and recognizing a landmark unlocks its stamp, and can find their way on the visit map.
- **Fluid Exploration:** Users can navigate from destination discovery to arrival and session completion without confusion.
- **Grounded Cultural Assistance:** Landmark recognition and historical answers support exploration without interrupting the physical experience.
- **Reliable Journey Tracking:** Explorations can be paused and resumed cleanly without losing photos or visited checkpoints.
- **Accurate Metadata & Stamps:** Exported photos accurately reflect chosen watermark styles and user-selected location privacy levels.
- **Trustworthy Event Data:** Event listings provide clear dates, locations, and source attribution.
- **Privacy Transparency:** Users understand and can change location precision at any point.
- **Security Evidence:** The MVP has no unresolved critical/high finding in the selected threat-model, authorization, media-upload, or location/EXIF scope.

---

## 14. Product principles

- **Free Exploration over Fixed Routes:** Support spontaneous curiosity instead of enforcing linear itineraries.
- **Context at the Right Moment:** Present cultural and historical knowledge exactly when the user reaches the site.
- **Assistance, Not Intrusion:** Landmark recognition runs only when the user asks to identify a photo, and cultural cards stay optional; there is no continuous camera analysis.
- **Personal Memories first, Stamps as a Bonus:** Users collect heritage and landmark stamps as they explore, but the journey and visual record remain the focus.
- **Presence over Tracking (Privacy-by-Design):** The system only checks presence within destination boundaries (`inside / outside`). It does not track continuous movements or store exact coordinates, ensuring users feel completely relaxed and free from surveillance.
- **Privacy by Choice:** Users maintain full control over how their physical location is displayed or shared on exported photo stamps.
- **Authentic and Traceable Content:** Historical data and event details remain accurate, concise, and attributed to verified sources.
