**Team members:**

| No. | Full name | Student ID | Role |
|---|---|---|---|
| 1 | Nguyễn Minh Hoàng | 2410315 | AI: Computer Vision |
| 2 | Lê Bá Ninh | 23BA14224 | Journeys, Events & Rewards |
| 3 | Lê Hiền Anh | 2410042 | Discovery, Map & Arrival |
| 4 | Nguyễn Minh Lương | 23BA14184 | AI: Historical RAG |
| 5 | Lương Quỳnh Anh | 23BA14008 | Camera, Photos, Album & Recap |
| 6 | Hà Thế Duy | 2410207 | Cyber Security & QA |
| 7 | Lê Bá Tiệp | 23BA14282 | Accounts, Profile & AI Integration |

+-----------------------------------------------------------------------+
| **Description:** Hanoi Lens is a mobile application that allows users |
| to freely explore Hanoi's landmarks, heritage sites, and scenic       |
| locations through photography. Users sign in, preview heritages, and  |
| mark the ones they want to visit; when GPS confirms arrival, the      |
| heritage is unlocked and its explore page and visit map open. Every   |
| heritage preview offers a historical RAG assistant, and on site users |
| can photograph a landmark to identify it and ask about its history    |
| with cited sources. The app also helps users discover ongoing and     |
| upcoming events at museums, heritage sites, and cultural venues       |
| across Hanoi. After each exploration, photos and progress are saved   |
| in a journey album with location stamps, timestamps, a stamp          |
| collection, and personalized recaps, and unlocking every marked       |
| heritage earns a Journey Stamp badge and a gift, allowing users to    |
| rediscover Hanoi through their own lens.                              |
+-----------------------------------------------------------------------+
| **Expected outcomes:**                                                |
|                                                                       |
| - A mobile application that enables users to freely discover Hanoi's  |
|   landmarks, heritage sites, and scenic locations.                    |
|                                                                       |
| - GPS-based destination detection that unlocks marked heritages on    |
|   arrival and displays relevant information, without tracking         |
|   continuous movement.                                                |
|                                                                       |
| - Landmark classification and a historical RAG assistant, shown on    |
|   every heritage preview and for landmarks identified on site.        |
|                                                                       |
| - An event discovery feature that collects and displays ongoing or    |
|   upcoming cultural events in Hanoi.                                  |
|                                                                       |
| - Self-marked heritages with a stamp collection and a reward (Journey |
|   Stamp badge and random gift) when every marked heritage is          |
|   unlocked.                                                           |
|                                                                       |
| - A documented security baseline covering threat modeling, access     |
|   control, hardened photo upload, and location/EXIF privacy.          |
|                                                                       |
| - A journey album that stores users' photos, visited locations,       |
|   timestamps, and exploration progress.                               |
|                                                                       |
| - Customizable location and time stamps that users can add to their   |
|   photos.                                                             |
|                                                                       |
| - Personalized destination and monthly recaps that help users         |
|   preserve and revisit their exploration memories.                    |
|                                                                       |
| - A more engaging and interactive way to learn about Hanoi's history, |
|   culture, and urban landscape through photography.                   |
+-----------------------------------------------------------------------+
| **Used Methods and Techniques:**                                      |
|                                                                       |
| - GPS Geofencing: Detects when users enter or leave the defined       |
|   geographical area of a landmark (presence check only) and unlocks   |
|   marked heritages, without storing exact coordinates or movement     |
|   trails.                                                             |
|                                                                       |
| - Location-Based Services: Provides nearby destinations, cultural     |
|   information, and events based on the user's current location.       |
|                                                                       |
| - Event Data Aggregation: Collects event information from selected    |
|   official websites and trusted public sources.                       |
|                                                                       |
| - Event Parsing: Uses source-specific JSON-LD, RSS, CSS selectors,    |
|   and deterministic date rules to extract event names, dates,         |
|   locations, and descriptions for administrator review.               |
|                                                                       |
| - Landmark Computer Vision: Classifies a supported primary landmark   |
|   from a submitted photo and returns a confidence score, or UNKNOWN   |
|   when unsure.                                                        |
|                                                                       |
| - Historical RAG: Retrieves verified historical evidence and returns  |
|   grounded answers with source citations, scoped to the previewed     |
|   heritage or the identified landmark.                                |
|                                                                       |
| - Digital Image Processing: Supports cropping, straightening,         |
|   exposure adjustment, color correction, filters, and non-destructive |
|   editing.                                                            |
|                                                                       |
| - Photo Watermarking: Adds customizable location names, timestamps,   |
|   weather information, or journey stamps to exported photos.          |
|                                                                       |
| - Metadata Management: Stores photo timestamps, approximate           |
|   locations, visited landmarks, and editing information for album     |
|   organization.                                                       |
|                                                                       |
| - Journey Tracking: Records marked, unlocked, and visited heritages   |
|   and saves unfinished exploration sessions so users can continue     |
|   later.                                                              |
|                                                                       |
| - Heritage Rewards: Shows a stamp collection when a visit ends and    |
|   grants a Journey Stamp badge and a random gift once every marked    |
|   heritage is unlocked.                                               |
|                                                                       |
| - Album and Recap Generation: Organizes photographs and journey       |
|   information into destination-based and monthly recaps.              |
|                                                                       |
| - Responsive Mobile UI/UX Design: Creates an accessible, mobile-first |
|   interface suitable for outdoor exploration.                         |
|                                                                       |
| - Privacy-Aware Design: Allows users to control whether exact,        |
|   approximate, or no location information is stored and displayed.    |
|                                                                       |
| - Security Hardening: Uses short-lived access tokens, rotating        |
|   refresh tokens, resource ownership checks, constrained upload URLs, |
|   and tests for location and EXIF leakage.                            |
+-----------------------------------------------------------------------+
