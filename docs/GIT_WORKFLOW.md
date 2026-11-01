# Git Workflow

How the seven team members branch, commit, and open pull requests on the Hanoi Lens repository. Every branch and commit is tied to a task ID `T01`–`T45` from the Google Sheet [**Task** → *Phân công*](https://docs.google.com/spreadsheets/d/11kdBSt4PNLAvb7pWNT9jZZVfpbT6CQF9x7ltqW3-bb4/edit?gid=221713974#gid=221713974).

Folder layout, folder owners, and naming rules for code are in [PROJECT_STRUCTURE_AND_NAMING.md](PROJECT_STRUCTURE_AND_NAMING.md).

---

## 1. The `main` branch

- `main` is the default, protected integration branch on GitHub.
- **Never push directly to `main` and never force-push** (`git push -f`).
- Every change goes through a branch and a pull request (PR) that a reviewer approves before it is merged.

## 2. Branch names

```text
feature/<short-name>-<task-id>
fix/<short-name>-<task-id>
```

Use `feature/` for planned work and `fix/` for bug fixes. Examples, by task owner on the Sheet:

| Member | Example branch |
|---|---|
| Nguyễn Minh Hoàng | `feature/vision-classifier-T32` |
| Lê Bá Ninh | `feature/visit-reward-check-T38` |
| Lê Hiền Anh | `feature/postgis-arrival-T18` |
| Nguyễn Minh Lương | `feature/rag-grounded-answer-T35` |
| Lương Quỳnh Anh | `feature/journey-stamp-export-T40` |
| Hà Thế Duy | `feature/idor-security-tests-T13` |
| Lê Bá Tiệp | `feature/jwt-refresh-rotation-T10` |

Branch names must not contain the word "claude" in any letter case.

## 3. Four steps for every task

**Step 1 — Start from the latest `main`**

```bash
git checkout main
git pull origin main
git checkout -b feature/<short-name>-<task-id>
```

On the Sheet, set the task's **Trạng thái** (status) to `Đang làm` (in progress).

**Step 2 — Commit only your own files**

```bash
git status
git add <path-to-your-file>
git commit -m "feat(<scope>): <short description> (<task-id>)"
```

Do not use `git add .`; it picks up local config and generated files.

**Step 3 — Bring in `main`, then push**

```bash
git fetch origin
git merge origin/main
git push -u origin feature/<short-name>-<task-id>
```

Resolve conflicts on your machine before pushing; never push a conflict into a PR.

**Step 4 — Open the PR and update the Sheet**

1. Open a PR from your branch into `main`. The PR title follows the commit format in section 4 and includes the task ID.
2. Reviewers are added automatically from [`.github/CODEOWNERS`](../.github/CODEOWNERS): `backend/` `@lebatiep`, `android/` `@qual05`, `ai-runtime/` `@NMH1203` and `@luongnm23ba14184-lgtm`, everything else `@NMH1203`.
3. On the Sheet: set the status to `Đang review` (in review), and `Xong` (done) after merge; on the *Github* tab, record the branch name and PR link.
4. After the merge, tell the team on Zalo/Discord to run `git pull origin main`.

## 4. Commit messages (Conventional Commits)

```text
<type>(<scope>): <short description, max 72 characters> (<task-id>)
```

**Types**

| Type | Use for |
|---|---|
| `feat` | A new feature |
| `fix` | A logic, security, or UI bug fix |
| `refactor` | Restructuring without behavior change |
| `test` | Unit, integration, or architecture tests |
| `db` | Flyway migrations |
| `docs` | Design docs or diagrams |
| `chore` | Gradle, Maven, Docker, CI configuration |

**Scopes**

- Backend modules: `identity`, `profile`, `destination`, `event`, `visit`, `media`, `recap`, `shared`
- Android: `android-explore`, `android-map`, `android-camera`, `android-events`, `android-journal`, `android-auth`
- AI and security: `ai-vision`, `ai-rag`, `security`, `ci`

**Examples**

```text
feat(destination): implement PostGIS geofence arrival check endpoint (T18)
feat(android-map): add on-device live location marker in VisitMapFragment (T21)
fix(security): strip GPS EXIF metadata from exported images (T43)
feat(ai-rag): enforce citation grounding in landmark QA response (T35)
docs(destination): document arrival-check error codes (T18)
```

Commit titles and bodies must not contain the word "claude" in any letter case and carry no AI attribution lines (`Co-Authored-By: …`, "Generated with …").

## 5. Rules that keep merges conflict-free

1. **Stay inside your area.** Edit only the folder, module, or files you own (see [PROJECT_STRUCTURE_AND_NAMING.md](PROJECT_STRUCTURE_AND_NAMING.md)). If you need another module's behavior, ask its owner or use its public interface or event; do not edit their files.
2. **Never edit a merged migration.** Add a new Flyway file (`V6__<owner>_<description>.sql`, and so on) instead.
3. **Contract first.** When the backend/AI and Android work on the same feature, both follow the JSON contract in [API.md](API.md) (later `openapi.yaml`, task T01), so each side can work against mocks.
4. **Merge `main` before you push** (step 3), so conflicts are solved locally.
5. **Never commit generated or sensitive files:** `build/`, `target/`, `.gradle/`, `.idea/`, `__pycache__/`, `.venv/`, model weights (`.pt`, `.pth`, `.onnx`, `.h5`), `.env` / `.env.*` (except `.env.example`), `local.properties`, keystores (`*.jks`, `*.keystore`), secrets, or personal photos.
6. **Personal agent config stays local:** `.claude/`, `.antigravity/`, `.agent-comms/`, `.agent/`, `AGENTS.md` are in `.gitignore` and are never committed.
