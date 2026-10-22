# Hanoi Lens Product Brief

> **Project Reference:** Aligned with the USTH ICT Group Project Topic Proposal (Academic Year 2025–2026).  
> **Topic:** Hanoi Lens — Mobile exploration, cultural event discovery, and photography-led journeys across Hanoi.

---

## 1. Product overview

**Hanoi Lens** is a mobile application that allows users to freely explore Hanoi's landmarks, heritage sites, museums, cultural venues, and scenic locations through photography.

Instead of prescribing fixed tour routes or rigid checklists, the application offers an open-ended experience:
- **Presence-based arrival recognition (Privacy-First Geofencing):** The application only detects whether the user is inside or outside a destination area (zone presence). It does not track continuous movements or require knowing/storing exact coordinates, ensuring complete comfort and user privacy.
- **Account-based journeys:** Users sign in or sign up before exploring, so journeys, unlocked heritages, photos, and rewards are kept in their account.
- **Self-marked heritages & GPS unlock:** Users preview a heritage and mark it if they want to go; the app never prescribes routes. When GPS confirms arrival, the heritage is unlocked immediately and its **heritage explore page** (history, landmarks) opens; from there a visit map of the heritage area shows the user's live position so they can move around, explore, and take photos. Unlocking every marked heritage earns an in-app Journey Stamp badge and a random gift from that heritage site.
- **Cultural event discovery:** Collects and displays ongoing and upcoming events from approved official sources using deterministic, source-specific parsers and administrator review.
- **Landmark recognition & historical Q&A:** Users can optionally photograph a scene or structure on site to identify what it is (image classification), then, from the landmark info page, ask a grounded RAG system about that landmark and get answers with verified citations.
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
7. **Earn rewards for completed journeys:** Receive a Journey Stamp badge and a random gift from the heritage site after unlocking every heritage they marked.
8. **Keep memories of events without a heritage site:** Capture and save photos on an event memory page that adapts to the event's details.
9. **Protect privacy & explore comfortably:** Enjoy open-ended exploration without feeling monitored or tracked; the system only needs to verify presence within a destination zone, eliminating the need to log exact coordinates or continuous travel trails.

---

## 5. Core experience lifecycle

The Hanoi Lens journey consists of five interconnected phases:

```text
[1. Discover & Mark] ---> [2. Arrive & Unlock] ---> [3. Visit & Explore] ---> [4. Capture & Edit] ---> [5. Remember & Reward]
```

1. **Discover & Mark:** Browse destinations and upcoming cultural events (also together on the map), preview a heritage, and mark it if the user wants to go.
2. **Arrive & Unlock:** Walk into a destination's geographic zone where GPS geofencing automatically registers the arrival and unlocks the heritage.
3. **Unlock & Explore:** Arrival unlocks the heritage and opens its heritage explore page (history, landmarks). The visit map of the heritage area shows the user's live position; users move around, take photos, and optionally identify what they photographed. When a landmark is identified, its landmark info page opens and the user can chat with the AI assistant about it, then return to the visit map or the heritage explore page.
4. **Capture & Edit:** Take photos, optionally recognize supported landmarks, and perform non-destructive image edits.
5. **Remember & Reward:** When finishing a visit, view the stamp collection, then collect a Journey Stamp badge and a random gift once every marked heritage is unlocked. Save progress to the travel journal (journey albums), attach customizable location and weather stamps, and view destination or monthly recaps.

Users choose and mark heritages themselves; the app never prescribes routes or sequential stops. Users can stop at any time. Unlocking every marked heritage is an optional goal that earns a reward.

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
- Each heritage page works as a preview. If the user wants to go, the heritage is marked in the current trip. The app does not recommend which heritages to mark.

### 6.2 GPS-based destination detection (GPS Geofencing)
- Monitor destination boundaries solely to detect arrival and presence (binary zone check: inside/outside).
- Keep cultural content and Historical RAG access visible before travel; geofence entry confirms arrival and immediately unlocks the marked heritage.
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
- On site, the AI chat is reached from the landmark info page after a photo is identified, and is scoped to that landmark. After chatting, the user returns to the visit map or the heritage explore page.
- Asking a question is always optional and non-intrusive.

### 6.4 Photo capture & landmark recognition
- Capture or select a photo using the native Android camera flow.
- Submit the image to the landmark classification model and return a supported `landmarkId` with confidence.
- Return `UNKNOWN` below the configured confidence threshold instead of silently selecting a landmark.
- Recognition is optional and does not affect unlocking: it tells the user what the photographed scene or structure is, opens the landmark info page, and supplies the `landmarkId` as context for Historical RAG questions. `UNKNOWN` returns the user to the visit map.
- Real-time composition overlays, lighting analysis, and camera-position guidance are outside the project scope.

### 6.5 Digital image processing & non-destructive editing
- In-app image processing tools: crop, straighten/rotate, exposure, brightness, contrast, saturation, and stylistic filters.
- Non-destructive pipeline: original photo assets are preserved untouched; adjustments are stored as recipes or applied to exported copies.

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
- Track marked, visited, and unlocked heritages within an active session.
- Save progress and update the travel journal when the user stops exploring.
- Flexible session lifecycle: start, pause, resume, and conclude journeys at the user's discretion.
- Unfinished explorations are persisted so users can resume their journey at any time.

### 6.9 Journey albums & personalized recaps
- Automatic structuring of photos by destination, date, and exploration session into private albums.
- Metadata storage linking photos to timestamps, approximate locations, landmarks, and applied edit parameters.
- Generation of destination-specific summaries and monthly exploration recaps to rediscover personal journeys.
- Travel journal view: choose a past trip, see its details and photos, and download photos to the device.

### 6.11 Heritage rewards
- Finishing a visit first shows the user's stamp collection, then checks whether every marked heritage is unlocked.
- If the user continues exploring, they stay on the heritage explore page; otherwise the trip ends and the travel journal is updated.
- When every heritage marked in a trip is unlocked, the user collects an in-app Journey Stamp badge and a random gift from that heritage site.
- Rewards are a bonus for finished journeys; exploring, capturing, and saving memories never require them.

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

- **Explore Hanoi (home):** Destination discovery, thematic collections, nearby recommendations, travel journal, and resuming a paused trip.
- **Heritage explore page:** Page of the unlocked heritage (history, landmarks), shown after arrival and when resuming; opens the visit map.
- **Map:** Interactive map showing heritages and cultural events together, plus user location and destination geofences.
- **Visit map:** Map of the heritage area, opened from the heritage explore page, with the user's live position for moving around and taking photos.
- **Landmark info:** Shown after a photo is identified; the only on-site entry to the AI chat for that landmark.
- **Stamp collection:** Shown when finishing a visit, before the reward check.
- **Camera:** Photo capture, landmark-recognition submission, and quick-edit interface without real-time guidance overlays.
- **Events:** Aggregated stream of ongoing and upcoming cultural events across Hanoi with venue linking.
- **Travel journal (Album):** Past trips, photo collections and downloads, journey stamp exports, earned badges, and destination/monthly recaps.
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
Heritage preview -> Want to go? -> Mark heritage in trip
     │
     ▼
Optional directions -> Arrive -> GPS confirms arrival -> Unlock heritage
     │
     ▼
Heritage explore page (history, landmarks)   <── also opened when resuming a paused trip
     │
     ▼
Visit map (area map + live position)
     ├──> Move around
     ├──> Take photo -> (optional) Identified?
     │        ├── No / UNKNOWN ──> Visit map
     │        └── Yes ──> Landmark info -> Chat with AI -> Visit map or Heritage explore page
     ├──> Pause ──> Explore Hanoi (home)
     ▼
Finish here -> Stamp collection
     │
     ▼
All marked heritages unlocked? ── Yes ──> Journey Stamp badge + random gift
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

The system directly implements the 12 methods and techniques defined in the project proposal:

| #   | Method / Technique                 | Category                 | Description & Purpose                                                                                                                                                   |
| --- | ---------------------------------- | ------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | **GPS Geofencing**                 | Geospatial Awareness     | Detects when users enter or leave the defined geographical area of a landmark (binary presence check) without tracking exact coordinates or continuous movement trails. |
| 2   | **Location-Based Services**        | Local Discovery          | Provides nearby destinations, cultural information, and events based on the user's current location.                                                                    |
| 3   | **Event Data Aggregation**         | Data Ingestion           | Collects event information from selected official websites and trusted public sources.                                                                                  |
| 4   | **Deterministic Event Parsing**    | Data Processing          | Uses JSON-LD, RSS, CSS selectors, and date-parsing rules to extract event names, dates, locations, and descriptions from approved sources.                              |
| 5   | **Landmark Image Classification**  | Computer Vision          | Recognizes a supported primary landmark from a submitted photo and returns `landmarkId`, confidence, or `UNKNOWN`.                                                      |
| 6   | **Digital Image Processing**       | Media Processing         | Supports cropping, straightening, exposure adjustment, color correction, filters, and non-destructive editing.                                                          |
| 7   | **Photo Watermarking**             | Visual Identity & Export | Adds customizable location names, timestamps, weather information, or journey stamps to exported photos.                                                                |
| 8   | **Metadata Management**            | Data Architecture        | Stores photo timestamps, approximate locations, visited landmarks, and editing information for album organization.                                                      |
| 9   | **Journey Tracking**               | Session Lifecycle        | Records visited destinations and saves unfinished exploration sessions so users can continue later.                                                                     |
| 10  | **Album and Recap Generation**     | Memory & Summaries       | Organizes photographs and journey information into destination-based and monthly recaps.                                                                                |
| 11  | **Responsive Mobile UI/UX Design** | Client Interface         | Creates an accessible, mobile-first interface suitable for outdoor exploration.                                                                                         |
| 12  | **Privacy-Aware Design**           | Security & Privacy       | Maximizes user comfort by operating on area presence only; avoids storing exact coordinates or monitoring travel paths.                                                 |

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
  ├── markedHeritages [destinationId, markedAt]
  ├── unlockedHeritages [destinationId, unlockedAt]
  ├── visitedLandmarks [landmarkId, visitedAt]
  └── photos [photoId]

Reward
  ├── id, userId, journeyId, destinationId
  ├── journeyStampBadgeId
  ├── giftDescription (random gift from the heritage site)
  └── awardedAt

Photo
  ├── id, userId, journeyId, destinationId
  ├── eventId (optional, for event memory photos)
  ├── originalAssetUrl (immutable)
  ├── capturedAt, deviceMetadata
  ├── locationPrecision (EXACT, APPROXIMATE, NONE)
  ├── editRecipe (crop, straighten, brightness, contrast, filter)
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

The project delivers the 8 concrete deliverables stated in the proposal:

1. **Mobile Application:** A responsive mobile application that enables users to freely discover Hanoi's landmarks, heritage sites, and scenic locations.
2. **GPS-Based Destination Detection:** Automated geofence detection that recognizes arrival and displays location-relevant cultural insights.
3. **Landmark Recognition & Historical RAG:** Two AI capabilities classify supported landmarks and provide source-grounded historical answers.
4. **Cultural Event Discovery:** An aggregated feed presenting ongoing and upcoming cultural activities, exhibitions, and festivals across Hanoi.
5. **Journey Albums:** Structured digital albums preserving user photos, visited locations, timestamps, and exploration progress.
6. **Customizable Journey Stamps & Watermarks:** Flexible location names, timestamps, weather overlays, and commemorative stamps on exported photos.
7. **Personalized Destination & Monthly Recaps:** Curated visual recaps preserving exploration memories and encouraging rediscovery.
8. **Interactive Cultural Engagement:** A more engaging and interactive medium to learn about Hanoi's history, culture, and urban landscape through photography.

---

## 12. MVP scope

The initial Minimum Viable Product (MVP) validates the entire end-to-end exploration and capture loop.

### In Scope for MVP
- Android Native client implemented with Java and XML layouts.
- Interactive destination browsing and map overview.
- Detailed pilot destination (e.g., Văn Miếu – Quốc Tử Giám / Hoàn Kiếm area) with full content and landmarks.
- Always-visible historical RAG access on heritage previews, with a static fallback when AI is unavailable.
- Simulated and live GPS geofence arrival checks.
- Cultural cards and Historical RAG access on the heritage preview before travel; on site, the heritage explore page (history, landmarks) and AI chat from landmark info after identification.
- Photo capture/upload and landmark image classification with an `UNKNOWN` confidence fallback.
- In-browser digital image processing: crop, straighten, brightness, contrast, and color filters.
- Photo watermarking engine supporting customizable Journey Stamps (Minimal, Postcard, Heritage).
- Journey session lifecycle: Start, Pause, Resume, and Complete.
- Private journey album containing captured photos, timestamps, and visited landmarks.
- Destination recap summary view.
- Curated cultural event feed with structured metadata (title, dates, venue, source).
- Combined map of heritages and events.
- Required sign in / sign up with a post-sign-up landing page.
- Self-marked heritages, GPS arrival confirmation that unlocks the heritage, and an on-device visit map with the user's live position.
- Optional landmark recognition on photos taken on site, feeding Historical RAG Q&A.
- Journey Stamp badge and random heritage gift after all marked heritages are unlocked.
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

- **Rewarding Unlocks:** Users understand that arriving unlocks a marked heritage, can find their way on the visit map, and receive their badge and gift.
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
- **Personal Memories first, Rewards as a Bonus:** Users mark their own heritages; unlocking them all earns a Journey Stamp badge and a random gift, but the journey and visual record remain the focus.
- **Presence over Tracking (Privacy-by-Design):** The system only checks presence within destination boundaries (`inside / outside`). It does not track continuous movements or store exact coordinates, ensuring users feel completely relaxed and free from surveillance.
- **Privacy by Choice:** Users maintain full control over how their physical location is displayed or shared on exported photo stamps.
- **Authentic and Traceable Content:** Historical data and event details remain accurate, concise, and attributed to verified sources.
