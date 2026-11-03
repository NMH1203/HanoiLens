# ai-runtime/

Optional Python FastAPI inference service. It is an **adapter only**: no business data, no database access, no API exposed to the Android app. Only the backend calls it.

**Folder owners:**

- Nguyễn Minh Hoàng (`@NMH1203`) — Model 1, landmark image classification (returns `landmarkId` + confidence, or `UNKNOWN`)
- Nguyễn Minh Lương (`@luongnm23ba14184-lgtm`) — Model 2, historical RAG (answers only from retrieved evidence, with citations)

Both review and merge changes under `ai-runtime/` (see `.github/CODEOWNERS`).

## Scope

- Exactly two AI capabilities: landmark classification and historical RAG. No event NLP, no real-time camera guidance, no photography tips.
- Never receives exact user coordinates; never logs raw image bytes or tokens.

## Read first

- [docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md) §9 — AI design
- [docs/API.md](../docs/API.md) — `/v1/ai/*` endpoints served by the backend
- [docs/PROJECT_STRUCTURE_AND_NAMING.md](../docs/PROJECT_STRUCTURE_AND_NAMING.md) — Python layout and naming
- [docs/GIT_WORKFLOW.md](../docs/GIT_WORKFLOW.md) — branches, commits, pull requests

The Python project is not created yet; the folder owners set it up.
