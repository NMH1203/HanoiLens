# Hanoi Lens System Flow

What the Android app and the Spring Boot backend do for each step of the [user flow](user-flow.md). All backend steps run inside the single Spring Modulith application ([ARCHITECTURE.md](../ARCHITECTURE.md)); endpoint names refer to the proposed contract in [API.md](../API.md).

Rules this flow encodes:

- Landing page only for **new** accounts; returning users go straight to the **Explore Hanoi (home)** page.
- Two explore pages: **Explore Hanoi (home)** is app-wide; the **Heritage explore page** belongs to the heritage the user is at. Unlock and resume open the Heritage explore page first, then the visit map. Pause returns home; "continue exploring" after the reward check stays on the Heritage explore page.
- Arrival check: coordinates are used once for the presence check, then discarded. The heritage is unlocked on arrival. Live position on the visit map stays **on the device** (BRIEF §6.2).
- Recognition below the confidence threshold returns `UNKNOWN`; nothing is unlocked or saved as a recognized landmark (ARCHITECTURE §9.2, ADR 008).
- Historical RAG is reachable from the heritage preview before travel; on site it is reached only from **landmark info** after a confident recognition, scoped to that landmark. It answers only with citations.
- Journey states follow API §7: `ACTIVE ⇄ PAUSED → ENDED`.
- Finishing a visit first loads the user's **stamp collection**, then compares marked vs unlocked heritages.

Rendered diagrams (diagram-design; source HTML next to each PNG, generator specs in `docs/Diagram/HTML/src/`):

| | |
|---|---|
| 1/5 Session and sign-in | ![](../Diagram/system-flow-1-session.png) |
| 2/5 Discovery, preview and event memories | ![](../Diagram/system-flow-2-discover.png) |
| 3/5 Mark, arrive and unlock | ![](../Diagram/system-flow-3-arrival.png) |
| 4/5 Visit map, photos and recognition | ![](../Diagram/system-flow-4-visit.png) |
| 5/5 Reward, journey end and travel journal | ![](../Diagram/system-flow-5-finish-journal.png) |

## 1. Session, explore and discovery

```mermaid
flowchart TD
    START([Start]) --> TOKEN[Check stored session token]
    TOKEN --> VALID{Session valid?}
    VALID -- Yes --> LOADEX
    VALID -- No --> FORM[Show log in or sign up form]
    FORM --> CHOICE{Log in or sign up?}
    CHOICE -- Log in --> VERIFY[Verify credentials]
    CHOICE -- Sign up --> CREATE[Create and save account]
    VERIFY --> ISSUE[Issue access token and rotating refresh token]
    CREATE --> ISSUE
    ISSUE --> NEWACC{New account?}
    NEWACC -- Yes --> LANDING[Show landing page]
    NEWACC -- No --> LOADEX
    LANDING --> LOADEX["Load explore data<br/>published heritages, events, paused trip"]
    LOADEX --> SHOWEX["Show Explore Hanoi (home)"]

    SHOWEX --> SCREEN{Requested screen}
    SCREEN -- Map --> MARKERS[Query heritages and events, render markers]
    SCREEN -- Heritages --> HQUERY[Query heritage list]
    SCREEN -- Events --> EQUERY[Query published events]
    SCREEN -- Journal --> TRIPS[[Load user's trips - section 3]]
    SCREEN -- Resume trip --> RESUME[[Resume paused journey - section 2]]

    MARKERS --> PTYPE{Selected point type?}
    PTYPE -- Heritage --> PREVIEW
    PTYPE -- Event --> EDETAIL[Load event detail]
    HQUERY --> PREVIEW
    EQUERY --> EDETAIL
    EDETAIL --> LINKED{Event linked to a heritage?}
    LINKED -- Yes --> PREVIEW
    LINKED -- No --> MEMORY[Render event memory page from event data]
    MEMORY --> CAPTURE1[Capture photo]
    CAPTURE1 --> UPLOAD1[Presigned upload, validate and process photo]
    UPLOAD1 --> SAVEMEM[Save memory photo to journal]
    SAVEMEM --> TRIPS

    PREVIEW["Load heritage preview<br/>content, RAG entry"]
    PREVIEW -. question .-> RAG1[Historical RAG answer with citations]
    RAG1 -.-> PREVIEW
    PREVIEW --> WANTGO{User marks the heritage?}
    WANTGO -- No --> SHOWEX
    WANTGO -- Yes --> MARK[[Save mark in active journey - section 2]]
```

## 2. Journey, arrival, visit and reward

```mermaid
flowchart TD
    MARK["Save mark in journey<br/>create ACTIVE journey if none"] --> DIR{Directions requested?}
    DIR -- Yes --> ROUTE[Show route on map]
    DIR -- No --> GPS
    ROUTE --> GPS[Read GPS location on device]
    GPS --> CHECK["Arrival check<br/>POST /destinations/id/arrival-checks"]
    CHECK --> INSIDE{Inside heritage zone?}
    INSIDE -- No --> WAIT[Show distance, wait for next position]
    WAIT --> GPS
    INSIDE -- Yes --> UNLOCK["Record arrival and unlock heritage<br/>discard raw coordinates"]
    UNLOCK --> DETAIL[Load heritage detail]
    RESUME[Resume paused journey, set ACTIVE] --> SITE
    DETAIL --> SITE["Render heritage explore page<br/>history, landmarks"]
    SITE -- Map --> VISIT["Render visit map<br/>live position on device only"]

    VISIT --> ACTION{User action?}
    ACTION -- Move --> VISIT
    ACTION -- Take photo --> CAPTURE[Capture heritage photo]
    CAPTURE --> UPLOAD[Presigned upload and validate photo]
    UPLOAD --> SAVEPH[Save photo to journey]
    SAVEPH --> IDREQ{Identification requested?}
    IDREQ -- No --> VISIT
    IDREQ -- Yes --> CLASSIFY[Landmark classifier]
    CLASSIFY --> CONF{Confidence above threshold?}
    CONF -- Yes --> INFO[Show landmark info, set RAG context]
    CONF -- No --> UNKNOWN[Show UNKNOWN, no landmark selected]
    UNKNOWN --> VISIT
    INFO -- Ask AI --> RAG["Historical RAG scoped to landmark<br/>answer with citations"]
    RAG --> WHERE{Where next?}
    WHERE -- Map --> VISIT
    WHERE -- Explore site --> SITE
    ACTION -- Pause --> PAUSE[Set journey PAUSED, save progress]
    PAUSE --> SHOWEX[[Show Explore Hanoi home - section 1]]
    ACTION -- Finish visit --> STAMPS[Load user's stamp collection, owner only]
    STAMPS --> COMPARE[Compare marked vs unlocked heritages]

    COMPARE --> ALL{All marked heritages unlocked?}
    ALL -- Yes --> REWARD[Grant badge and random gift, save reward]
    ALL -- No --> CONT{User continues exploring?}
    REWARD --> CONT
    CONT -- Yes --> SITE
    CONT -- No --> ENDJ[Set journey ENDED, save progress]
    ENDJ --> UPDJ[Update travel journal]
    UPDJ --> OPENJ{User opens travel journal?}
    OPENJ -- Yes --> TRIPS[[Load user's trips - section 3]]
    OPENJ -- No --> AGAIN[[Continue using app? - section 3]]
```

## 3. Travel journal

```mermaid
flowchart TD
    TRIPS[Load user's trips] --> READ["Read journeys, photos, rewards<br/>owner only"]
    READ --> SIGN[Create short-lived signed read URLs]
    SIGN --> RENDER[Render trip details and photos]
    RENDER --> DL{Download requested?}
    DL -- Yes --> DOWNLOAD[Download image]
    DL -- No --> AGAIN{User continues using app?}
    DOWNLOAD --> AGAIN
    AGAIN -- Yes --> SHOWEX[[Show Explore Hanoi home - section 1]]
    AGAIN -- No --> FINISH([End])
```

## Changes from the reviewed diagrams (2026-10-22)

| # | Review comment | Fix |
|---|---|---|
| 1 | "Load heritage preview" had no outgoing path | `Load heritage preview → User marks the heritage?` |
| 2 | `UNKNOWN` still continued to "Save unlocked landmark" | Confidence decision: below threshold shows `UNKNOWN` and returns to the visit map; unlocking happens only on arrival |
| 3 | Returning users were sent to the landing page | `New account? — Yes → landing page, No → load explore data` |
| 4 | No marking step before "marked vs unlocked"; no pause/resume | `Save mark in journey` precedes arrival; `Pause` sets `PAUSED`, `Resume trip` sets `ACTIVE` and reopens the visit map |
| — | Unlabelled exits on "User continue exploring?" and "User continue using app?" | Every decision exit labelled Yes/No |
| — | Typos "Dowload", "Unknow", "User interesting the heritage?", "Question ask?" | Corrected wording |
| — | RAG reachable only after recognition | RAG answer available from the heritage preview, and on site after landmark info |
| — | Team request (2026-10-22), mirrored from the user flow | Explore Hanoi (home) vs Heritage explore page; unlock/resume → Heritage explore page → visit map; no direct Ask AI on the visit map (RAG after landmark info, then Map / Explore site); Finish visit → stamp collection → compare; continue exploring → Heritage explore page; static tips removed from the preview |
