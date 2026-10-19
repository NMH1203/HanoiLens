
+-----------------------------------------------------------------------+
| **Description:** Hanoi Lens is a mobile application that allows users |
| to freely explore Hanoi's landmarks, heritage sites, and scenic       |
| locations through photography. It uses GPS to recognize when users    |
| arrive at a destination and provides cultural information through a   |
| historical RAG system. A landmark image-classification model can use  |
| a submitted photo to focus that historical context. The app also      |
| helps users discover ongoing                                          |
| and upcoming events at museums, heritage sites, and cultural venues   |
| across Hanoi. After each exploration, photos and progress are saved   |
| in a journey album with location stamps, timestamps, and personalized |
| recaps, allowing users to rediscover Hanoi through their own lens     |
|                                                                       |
| > []{.mark}                                                           |
+-----------------------------------------------------------------------+
| **Expected outcomes:**                                                |
|                                                                       |
| - A mobile application that enables users to freely discover Hanoi's  |
|   landmarks, heritage sites, and scenic locations.                    |
|                                                                       |
| - GPS-based destination detection that displays relevant information  |
|   when users arrive at a location.                                    |
|                                                                       |
| - Landmark classification, an always-visible historical RAG assistant |
|   and curated tips for composition, lighting, angle, and timing.      |
|                                                                       |
| - An event discovery feature that collects and displays ongoing or    |
|   upcoming cultural events in Hanoi.                                  |
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
| - Personalized destination that help users preserve and revisit their |
|   exploration memories.                                               |
|                                                                       |
| - A more engaging and interactive way to learn about Hanoi's history, |
|   culture, and urban landscape through photography.                   |
+-----------------------------------------------------------------------+
| **Used Methods and Techniques:**                                      |
|                                                                       |
| - GPS Geofencing: Detects when users enter or leave the defined       |
|   geographical area of a landmark or scenic location.                 |
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
|   from a submitted photo and returns a confidence score.              |
|                                                                       |
| - Historical RAG: Retrieves verified historical evidence and returns  |
|   grounded answers with source citations.                             |
|                                                                       |
| - Photography Guidance: Provides editorial suggestions for angle,     |
|   composition, lighting, and suitable shooting time at each place.    |
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
| - Journey Tracking: Records visited destinations and saves unfinished |
|   exploration sessions so users can continue later.                   |
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
| - Security Hardening: Uses short-lived access tokens, rotating refresh|
|   tokens, resource ownership checks, constrained upload URLs, and     |
|   tests for location and EXIF leakage.                                |
+-----------------------------------------------------------------------+
