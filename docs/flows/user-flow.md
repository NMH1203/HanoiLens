# Hanoi Lens User Flow

What the user sees and decides. The system-side counterpart is [system-flow.md](system-flow.md). Product rules come from [the product brief](../HANOI_LENS_PRODUCT_BRIEF.md) §5–§8; the StarUML source is `docs/Diagram/HanoiLens.mdj` (UserFlow) and the rendered images are in `docs/Diagram/`.

Rules this flow encodes:

- GPS arrival **unlocks** the marked heritage immediately (BRIEF §6.2). Landmark recognition is optional and never gates unlocking (BRIEF §6.4).
- The Historical RAG entry point is visible on the heritage preview **before** travel; on site, AI chat opens from landmark info after a photo is identified (BRIEF §6.1, §6.3).
- An event linked to a heritage continues as that heritage; an event without one opens the **event memory page** (BRIEF §6.7).
- A trip can be **paused** and resumed later from the Explore Hanoi home page (BRIEF §6.1, §6.8).
- Two different explore pages: **Explore Hanoi (home)** is the app-wide page shown after sign-in (all heritages, events, map, journal, resume trip); the **Heritage explore page** belongs to the heritage the user has arrived at (its history and landmarks) and is reached from the visit.

Rendered diagrams (diagram-design; source HTML next to each PNG, regenerate with `node docs/Diagram/HTML/src/gen.js docs/Diagram/HTML/src/u1.js <out.html>`):

| | |
|---|---|
| 1/3 Sign in and discover | ![](../Diagram/user-flow-1-discover.png) |
| 2/3 Trip: arrive, unlock, visit, reward | ![](../Diagram/user-flow-2-trip.png) |
| 3/3 Travel journal | ![](../Diagram/user-flow-3-journal.png) |

## 1. Sign in and discover

```mermaid
flowchart TD
    START([Start]) --> OPEN[Open app]
    OPEN --> SIGNED{Signed in?}
    SIGNED -- Yes --> EXPLORE["Explore Hanoi (home)"]
    SIGNED -- No --> HASACC{Have an account?}
    HASACC -- Yes --> LOGIN[Log in]
    HASACC -- No --> SIGNUP[Sign up]
    LOGIN --> EXPLORE
    SIGNUP --> LANDING[Landing page]
    LANDING --> EXPLORE

    EXPLORE --> WANT{Want what?}
    WANT -- Map --> MAP[Choose a point on the map]
    WANT -- Heritages --> HLIST[Choose a heritage from the list]
    WANT -- Events --> ELIST[Choose an event from the list]
    WANT -- Journal --> JOURNAL[[Travel journal - section 3]]
    WANT -- Resume trip --> SITE[[Heritage explore page - section 2]]

    MAP --> PTYPE{Point type?}
    PTYPE -- Heritage --> PREVIEW
    PTYPE -- Event --> EDETAIL[Show event detail]
    HLIST --> PREVIEW
    ELIST --> EDETAIL

    EDETAIL --> LINKED{Linked to a heritage?}
    LINKED -- Yes --> PREVIEW
    LINKED -- No --> MEMORY[Event memory page]
    MEMORY --> MEMPHOTO[Take photos and save them as memories]
    MEMPHOTO --> JOURNAL

    PREVIEW["Heritage preview<br/>history, Ask AI entry"]
    PREVIEW -. optional .-> ASK1[Chat with Historical AI]
    ASK1 -.-> PREVIEW
    PREVIEW --> GO{Want to go there?}
    GO -- No --> EXPLORE
    GO -- Yes --> MARK[[Mark heritage in trip - section 2]]
```

## 2. Trip: mark, arrive, unlock, visit, reward

```mermaid
flowchart TD
    MARK[Mark heritage in the trip] --> DIR{Need directions?}
    DIR -- Yes --> ROUTE[Show route on map]
    DIR -- No --> TRAVEL[Go to the heritage]
    ROUTE --> TRAVEL
    TRAVEL --> ARRIVE[GPS confirms arrival]
    ARRIVE --> UNLOCK[Heritage unlocked automatically]
    UNLOCK --> SITE["Heritage explore page<br/>history, landmarks of this site"]
    SITE -- Map --> VISITMAP["Visit map<br/>live position, on device only"]

    VISITMAP --> NEXT{What next?}
    NEXT -- Move or read history --> VISITMAP
    NEXT -- Take photo --> PHOTO[Take a photo]
    PHOTO --> IDENT{Identified?}
    IDENT -- No or UNKNOWN --> VISITMAP
    IDENT -- Yes --> RESULT[Show landmark info]
    RESULT -- Ask AI --> ASK["Chat with Historical AI<br/>about this landmark"]
    ASK --> WHERE{Where next?}
    WHERE -- Map --> VISITMAP
    WHERE -- Explore site --> SITE
    NEXT -- Pause --> PAUSE[Trip saved as paused]
    PAUSE --> EXPLORE[[Explore Hanoi home - section 1]]
    NEXT -- Finish here --> STAMP[View stamp collection]
    STAMP --> ALL{All marked heritages unlocked?}

    ALL -- Yes --> GIFT[Collect Journey Stamp badge and random gift]
    ALL -- No --> CONT{Continue exploring?}
    GIFT --> CONT
    CONT -- Yes --> SITE
    CONT -- No --> SAVE[Save progress and end trip]
    SAVE --> UPDATE[Travel journal updated]
    UPDATE --> JOURNAL[[Travel journal - section 3]]
```

## 3. Travel journal

```mermaid
flowchart TD
    JOURNAL{See travel journal?}
    JOURNAL -- Yes --> TRIP[Choose a past trip]
    JOURNAL -- No --> AGAIN
    TRIP --> DETAILS[View details and photos]
    DETAILS --> DL{Download photo?}
    DL -- Yes --> DOWNLOAD[Download photo]
    DL -- No --> AGAIN{Continue using the app?}
    DOWNLOAD --> AGAIN
    AGAIN -- Yes --> EXPLORE[[Explore Hanoi home - section 1]]
    AGAIN -- No --> FINISH([End])
```

## Changes from the reviewed diagrams (2026-10-22)

| # | Review comment | Fix |
|---|---|---|
| 1 | Typo "Continue exxploring" | "Continue exploring" (already fixed in `.mdj`, not in `UserFlow.jpg`) |
| 2 | Point type only had Event | Added the Heritage branch (already in `.mdj`, missing in the JPG) |
| 3 | GPS arrival did not unlock; unlocking depended on recognition | `GPS confirms arrival → Heritage unlocked automatically`; recognition is an optional step after "Take a photo" and loops back to the visit map |
| 3b | Typo "hisstotical" | "history" (already fixed in `.mdj`) |
| 4 | Event without heritage went to the heritage trip flow | `Linked to a heritage? — No → Event memory page → save memories to the journal` |
| 5 | No AI entry before arrival | Heritage preview carries history and the Ask AI entry; optional chat loop before deciding to go |
| 9 | No marking step; no pause/resume | `Mark heritage in the trip` precedes travel; `Pause` saves the trip and `Resume trip` on the explore page returns to the visit map |
| — | Recognition failure looped forever on "Take a photo" | Removed: `UNKNOWN` is shown and the user continues |
| — | Team request (2026-10-22): order of on-site pages | `Heritage unlocked → Heritage explore page → Visit map`; resuming a paused trip also opens the Heritage explore page; `Finish here → Stamp collection → All marked heritages unlocked?` |
| — | Team request (2026-10-22): "Explore page" meant two different pages | Renamed to **Explore Hanoi (home)** (after sign-in, after pause, journal) and **Heritage explore page** (on site: after unlock, "Where next? → Explore site", "Continue exploring? → Yes") |
| — | Team request (2026-10-22): on the visit map, AI chat is only reachable after a photo identifies a landmark | `Take photo → Identified? → Landmark info → Chat with AI → Where next? (Map / Explore site)`; the visit map no longer has a direct "Ask AI" branch. The heritage preview keeps its Ask AI entry |
