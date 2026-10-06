# SDET Assignment — SauceDemo UI & GoREST API Automation

This repository covers Parts 0–4 of the assignment: scenario design, Playwright UI automation, REST Assured API automation, CI, and notes on the AI-assisted workflow used throughout.

```
test-coverage/     Part 0 — scenario design (login/cart/checkout, saucedemo.com)
ui-tests/          Part 1 — Playwright for Java (Maven project, own Dockerfile)
api-tests/         Part 2 — REST Assured for Java (Maven project, own Dockerfile)
.github/workflows/ Part 3 — CI pipeline
docker-compose.yml Optional — run either suite without a local Java/Maven install
README.md          This file
```

Each of `ui-tests/` and `api-tests/` has its own `README.md` with full setup detail, architecture, and findings specific to that suite. This file summarizes both and covers everything that spans the whole repo.

---

## Setup & Commands

Prerequisites: JDK 17+, Maven 3.9+.

```bash
git clone <your-repo>
cd <your-project>

export GOREST_API_TOKEN=your-token

(cd ui-tests && mvn clean test)
(cd api-tests && mvn clean test)
```

Each suite also supports running a subset by tag — see `ui-tests/README.md` (`-Dgroups=known-defect`) and `api-tests/README.md` (`-Dgroups=crud|validation|auth|edge|query|smoke`) for the full list.

### Optional: Dockerized execution (stretch goal)

Runs either suite with no local Java or Maven install — only Docker is required:

```bash
export GOREST_API_TOKEN=your-token   # or rely on api-tests/.env — see below
docker compose run --rm ui-tests
docker compose run --rm api-tests
```

Verified: `ui-tests` → 38 ran, 0 failed; `api-tests` → 56 ran, 0 failed, 1 skipped (known defect D1) — matching the local, non-Docker results exactly.

Notes:
- The assignment's example command is `docker compose run tests` (one generic service). This repo defines two services instead, `ui-tests` and `api-tests`, mirroring the two independent Maven projects and the two-job CI structure — a deliberate deviation, not an oversight.
- `ui-tests` uses the official `mcr.microsoft.com/playwright/java` image, version-pinned to match `pom.xml`'s Playwright version exactly (a mismatch here causes Playwright to fail to locate a browser binary). This image already bundles JDK 17, Maven, and the browsers, so no separate install step is needed inside the container.
- `api-tests` uses a plain `maven:3.9-eclipse-temurin-17` image, since no browser is needed.
- Both Dockerfiles have a `.dockerignore` that excludes `.env` — without it, `COPY . .` would bake the live GoREST token into the image layer, a secret leak independent of `.gitignore` (which only protects git history, not image layers).
- `docker-compose.yml` sits at the repo root, alongside `api-tests/.env`, so Docker Compose's automatic `.env` loading picks up `GOREST_API_TOKEN` without needing it exported manually — confirmed working in practice, not just assumed from Compose's documented behavior.
- Each service bind-mounts its own `target/` to the host, so `--rm` deleting the container doesn't also delete Surefire reports or (for `ui-tests`) failure screenshots — those would otherwise be unrecoverable after a local Docker run, unlike on CI where `actions/upload-artifact` already covers this. A shared named volume caches Maven dependencies across runs so they aren't re-downloaded every time.

### Optional: Allure report (stretch goal)

Both suites generate Allure results (`target/allure-results`) via `allure-jupiter`, and CI generates the full HTML report (using the official `allure-commandline` npm package, not a third-party composite Action) and uploads it as a build artifact — `ui-allure-report` and `api-allure-report`, downloadable from any Actions run. Verified by downloading and opening both locally: full suite breakdown, 100% pass rate, the one skipped (known-defect) API test shown correctly.

### Optional: Retry on flake (stretch goal)

Only the three `performance_glitch_user`/timing-SLA tests in `ui-tests` (`@RetryingTest(3)`, via `junit-pioneer`) retry — nothing else. Rationale: these assert against a live, shared service's response time, where a single slow network blip is noise, not a regression; a consistent slowdown still fails all 3 attempts. Functional and known-defect assertions are never retried — a flaky functional test is a real bug to fix, not noise to filter out. `api-tests` has no retries at all, for the same reason it stays sequential (see below): masking a rate-limit-induced failure would be worse than seeing it.

### Optional: Parallel execution (stretch goal)

Enabled for `ui-tests` only: `junit-platform.properties` runs test classes concurrently (`classes.default = concurrent`) while methods within a class stay sequential (`mode.default = same_thread`), per Playwright's own guidance that Playwright/Browser objects aren't safe to share across threads. `BaseTest` uses `@TestInstance(PER_CLASS)` with non-static `Playwright`/`Browser` fields, so each test class gets its own independent instance rather than all three subclasses racing on one shared static field. Verified: total suite time dropped from 56.7s to 35.3s (all 38 tests still passing), with the new total close to the slowest individual class rather than the sum of all three — the expected signature of working parallelism, not just a config flag that silently did nothing.

`api-tests` deliberately stays sequential — see `api-tests/README.md`: the live GoREST service has a shared 90 req/min rate limit per token, and parallelizing would make tests compete for that budget, turning rate-limit noise into false failures. This is an intentional scope boundary, not an oversight.

---

## Part 0 — Scenario Design

See [`test-coverage/`](./test-coverage) for the full scenario table (38 scenarios, priority-ordered TC-01–TC-38, ambiguities flagged inline). Summary of the approach:

- Scenarios are ordered **Critical → High → Medium → Low** by business impact and likelihood, not by discovery order.
- `problem_user` and `performance_glitch_user` each get dedicated scenarios, because their whole purpose is to diverge from `standard_user` — the design explicitly calls out *how* each diverges (specific broken buttons, a stuck button label, a broken checkout field, sort that silently no-ops, a login/checkout timing SLA) rather than testing them identically to `standard_user`.
- Several scenarios were added after initial exploration and are clearly marked as such (PDF receipt download, Reset App State's stale button label) — these weren't anticipated going in and were folded in once found, rather than being left out because they weren't in the original scope.
- A few findings are deliberately left as **unresolved ambiguities** rather than confirmed defects (e.g. sort order not persisting across navigation) because it isn't clear whether that's a bug or an intentional design choice — see `test-coverage.md`'s "Automation Value" section for the reasoning on what's worth automating versus left as a documented, unautomated finding.

## Part 1 — UI Automation (Playwright + Java)

Full detail: [`ui-tests/README.md`](./ui-tests/README.md)

Highlights:
- Page Object Model split into `locators` (element lookup, e.g. `LoginLocators`) and `actions` (behavior, e.g. `LoginActions`) per page, resilient locators (`getByTestId` wired to the site's `data-test` attribute, `getByRole` elsewhere), fresh `BrowserContext` per test for isolation.
- Confirmed defects are automated as **known-defect regression tests** (`@Tag("known-defect")`): they assert the observed (broken) behavior rather than the "correct" behavior, so a future fix shows up as a test failure instead of silently passing either way. Full defect list with severity in `ui-tests/README.md`'s "Observed Defects Summary."
- Performance scenarios (`performance_glitch_user` login/checkout, `standard_user` baseline) log elapsed time every run and only hard-fail when explicitly enabled, to avoid turning network/CI variance into flaky failures.
- `@TestInstance(PER_CLASS)` with non-static `Playwright`/`Browser` fields in `BaseTest`, enabling safe class-level parallel execution — see "Optional: Parallel execution" below.

## Part 2 — API Automation (REST Assured + Java)

Full detail: [`api-tests/README.md`](./api-tests/README.md)

Highlights:
- Layered architecture: `tests → UserSteps → UsersClient → ApiClient → REST Assured`, so tests read as business intent and raw HTTP detail stays out of test bodies.
- Every created record is tracked in a `TestDataRegistry` and cleaned up in three layers (`afterEach`, `afterAll`, a JVM shutdown hook), so a failing or interrupted run never leaks data into the shared live service.
- Response validation goes beyond status codes: JSON Schema validation (rejecting unknown properties to catch contract drift), field-by-field soft assertions, and a re-read after every write to confirm persistence rather than trusting an echoed response.
- One confirmed defect (case-sensitive email uniqueness) is `@Disabled` with the reason documented in code — not silently skipped — and several documentation-vs-live-behavior mismatches are captured as accepted-status-set assertions rather than failures, since the live API's actual contract differs from its published docs in minor ways.

## Part 3 — CI

Workflow: [`.github/workflows/cicd.yml`](./.github/workflows/cicd.yml). Assumption: the default branch is `main`, as already set in the starter file's trigger config (`push`/`pull_request` on `branches: [main]`), which was correct and left unchanged.

### Bugs found in `starter-kit/ci-broken.yml` and fixed

| # | Bug | Fix |
|---|---|---|
| 1 | No `actions/checkout` step — the runner had no project files to test. | Added `actions/checkout@v4` as the first step of each job. |
| 2 | `java-version: '8'` — this project targets Java 17 (`pom.xml` sets `maven.compiler.release=17`). | Set to `java-version: '17'`. |
| 3 | `mvn clean test \|\| true` — masks real test failures; a passing step meant nothing. | Removed `\|\| true` so a genuine failure fails the job. |
| 4 | No `working-directory` — the repo is multi-module (`ui-tests/`, `api-tests/`, each with its own `pom.xml`); `mvn` at repo root finds no `pom.xml`. | Added `working-directory: ui-tests` / `working-directory: api-tests` to each suite's steps. |
| 5 | `GOREST_API_TOKEN` was never injected, so every `@RequiresToken` test would fail with a confusing wall of errors instead of one clear signal. | Added a step that fails fast with an explicit message if the `GOREST_API_TOKEN` secret is missing (e.g. on a fork PR), then passes it via `env:` to the test step. |
| 6 | No step installed Playwright's browser binaries — the suite would fail on first browser launch with "Executable doesn't exist." | Added an `Install Playwright browsers` step running the Playwright CLI's `install --with-deps chromium`. |
| 7 | Both suites ran as sequential steps in a single job. Combined with bug #3's `\|\| true`, a real UI failure would still show the job as "passed"; simply removing `\|\| true` without restructuring would instead let a failing UI step block the API step from ever running, hiding its result. | Split into two independent jobs, `ui-tests` and `api-tests`, so either suite's pass/fail is accurate and visible on its own, and one suite's failure never hides or blocks the other. |
| 8 | No `concurrency` control. The API suite hits a shared, rate-limited live service (90 req/min per token); overlapping runs against the same token risk spurious 429s unrelated to real failures. | Added a workflow-level `concurrency` group keyed on the branch ref, cancelling any in-progress run when a new one starts. |

### Bugs found while implementing the fix (not seeded, found by running the pipeline for real)

| # | Bug | Fix |
|---|---|---|
| 9 | `Install Playwright browsers` step failed with `ClassNotFoundException: com.microsoft.playwright.CLI`. The Playwright dependency is `scope: test` in `pom.xml`, but `exec:java`'s default classpath doesn't include test-scope dependencies. | Added `-Dexec.classpathScope=test` to the install command so `exec-maven-plugin` can see the test-scoped Playwright dependency. |
| 10 | After the pipeline went green, GitHub's Annotations panel still showed 2 errors: `mikepenz/action-junit-report` failed to create a Check Run ("Resource not accessible by integration"). `GITHUB_TOKEN` defaults to insufficient permissions for this unless explicitly granted. | Added a workflow-level `permissions: { contents: read, checks: write }` block. |

### Making it trustworthy, not just green

- Each suite is a separate job, so a reviewer sees both results independently without one masking the other.
- `mikepenz/action-junit-report` publishes a pass/fail/skip count directly on the Actions run summary for each suite — no need to download anything to see what happened.
- Surefire XML reports and (for the UI suite) failure screenshots are uploaded as artifacts on every run, pass or fail, so a failure is diagnosable from the Actions tab alone.
- The API job fails fast with an explicit message if `GOREST_API_TOKEN` is missing, rather than letting every token-dependent test fail individually with a less obvious cause.

### Completed run

[CI run on `main`](https://github.com/natthawadeesuriyanan/sdet-test/actions/runs/37316976494) — triggered by merging PR #4 (parallel execution) into `main`. All 4 jobs passed: UI Tests (Playwright), API Tests (REST Assured), UI Test Results (38 ran, 38 passed, 0 failed), API Test Results (56 ran, 55 passed, 1 skipped, 0 failed).

### On-demand test runs via PR comments

A second workflow, [`.github/workflows/pr-command.yml`](./.github/workflows/pr-command.yml), lets a trusted collaborator trigger a specific test run from a PR comment, instead of waiting for the full suite or re-running everything locally:

```
/ui-test                              → all UI tests, Chromium
/ui-test core-flow-test               → CoreFlowTest only, Chromium
/ui-test core-flow-test browser chrome    → CoreFlowTest on Google Chrome
/ui-test all browser chrome,msedge    → all UI tests on Chrome and Edge, in parallel
/api-test                             → all API tests
/api-test user-validation-tests       → UserValidationTests only
/ui-test help, /api-test help         → usage and the list of available test classes
```

The bot reacts with 🚀, posts a "running" status on the PR's head commit, and comments back a pass/fail table per browser (or per API suite) with a link to the full run — so a reviewer sees the result without leaving the PR.

**Security model** (this runs with the base repo's permissions, so it's deliberately restrictive):
- only comments from `OWNER`/`MEMBER`/`COLLABORATOR` are honoured; anyone else gets a polite refusal, not silent ignoring;
- the comment body is validated against an allowlist and never interpolated directly into a shell command;
- `/ui-test` runs get a read-only token and no secrets at all;
- `/api-test` is refused outright on PRs from forks, because it needs `GOREST_API_TOKEN` — a fork could otherwise read a secret it shouldn't have access to just by commenting.

**Multi-browser support** (`browser chrome,msedge`, `firefox`, `webkit`) required adding `TestConfig.browser()` and a `launchBrowser()` switch in `BaseTest` — the workflow passes the selection as `-Dsaucedemo.browser`, which only has an effect because that code exists; without it, the flag would silently do nothing and every run would still launch Chromium.

## Part 4 — AI-Assisted Workflow Notes

**Tools and scope:** Claude, as an assistant across Parts 0–3 — translating established TypeScript/Playwright testing patterns into idiomatic Java/JUnit equivalents while ramping up on Java specifically, drafting Page Object / REST Assured boilerplate, and structuring this README. Test strategy, scenario priority, every defect's exact behavior, and whether a given fix actually worked were decided and verified by hand against the live site and pipeline — not generated or taken on faith.

**A documented fix that was never actually applied:** after debugging a CI failure (`ClassNotFoundException` in the Playwright-install step), the fix and its rationale were written into this README's bug table as resolved — but the real `cicd.yml` was never updated to match, and the exact same failure reappeared on the next pipeline run. I caught the gap by comparing a fresh Actions log against what the README claimed, not by trusting that account. Lesson: an AI's own running summary of "what was fixed" is a claim to verify against the actual file, not a fact, especially deep into a long multi-file session.

**Not delegated:** confirming that a described fix was genuinely present in the file it was supposed to change, for both CI config and test code. Several times a plausible, well-reasoned fix turned out to exist only in conversation, not in the file — caught only by re-running the real pipeline or suite and reading the actual result, never by re-reading the explanation of what should have happened.

---

## Assumptions Made Where the Assignment Was Unclear

- **UI:** the 8% checkout tax rate is not documented anywhere on saucedemo.com; it was reverse-derived from one observed example and stored as a single named constant so it's a one-line fix if ever wrong. Several login/checkout edge cases (max field length, whitespace trimming, case sensitivity) have no documented expected behavior and are flagged as open ambiguities rather than guessed at silently — see `test-coverage.md`.
- **API:** GoREST's live behavior differs from its published docs in three places (`text/plain` POST body, unauthenticated PUT/DELETE status code, and the error-body shape for `DELETE /users` with no id). Rather than picking one side and failing on the other, the suite asserts the invariant that actually matters (request rejected, no side effect) and accepts either documented or observed status code — see `api-tests/README.md`'s "Documentation vs live behaviour" table.
- Both suites treat `standard_user` / a fresh GoREST token as the "should always work" baseline, and deliberately test `problem_user` and rate-limit/isolation edge cases as the sources of divergence, per the assignment's emphasis on going beyond the happy path.

## What I'd Do Differently With More Time

- **UI:** dig deeper into the two remaining open ambiguities (TC-34/35 case sensitivity and whitespace trimming; TC-30 sort-persistence) rather than stopping at "confirmed, cause unclear" — e.g. repeat TC-30 from different starting sort options to see whether the fallback order is actually deterministic, which would narrow down whether it's a real bug or an intentional reset. In a real team setting this is also the kind of finding worth a quick confirmation from whoever owns the feature, rather than guessing either way.
- **API:** add a second GoREST token to test cross-token isolation and the 403 response on writes, which isn't testable with a single token; add the "possible extensions" already listed in `api-tests/README.md` (injection-string storage, explicit-null handling, malformed pagination params); revisit whether a bounded, rate-limit-aware form of parallelism is worth the added complexity, now that `ui-tests` has a working parallel-execution pattern to borrow from.
- **Both:** an `environment.properties` file for Allure so the report records the OS/Java version a run used, and extending parallel execution's CI config to account for GitHub-hosted runners' smaller CPU core count, which may not show the same speedup seen locally.
