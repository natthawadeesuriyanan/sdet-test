# SDET Assignment — SauceDemo UI & GoREST API Automation

## Reviewer snapshot

This repository covers all required parts of the assignment:

| Part | Implementation | Result |
|---|---|---|
| 0. Test design | 38 risk-prioritized scenarios | Critical → Low |
| 1. UI | Playwright + Java + JUnit 5 | **38 passed** |
| 2. API | REST Assured + Java + JUnit 5 | **55 passed, 1 known-defect skipped** |
| 3. CI/CD | Fixed GitHub Actions pipeline | **All 4 jobs passed** |
| 4. AI workflow | Usage, verification, and concrete miss documented | ✓ |

The main focus is not test count. It is **risk-based coverage, maintainable automation, diagnosable failures, trustworthy CI, and accountable use of AI**.

---

## Quick start

### Prerequisites

JDK 17+, Maven 3.9+, and a GoREST API token.

```bash
git clone <your-repo>
cd <your-project>

export GOREST_API_TOKEN=your-token

(cd ui-tests && mvn clean test)
(cd api-tests && mvn clean test)
```

The API project also supports a local `.env`. Secrets are excluded from git and Docker build context.

For targeted runs, see the suite READMEs:
- UI: `-Dgroups=known-defect`
- API: `-Dgroups=crud|validation|auth|edge|query|smoke`

## Repository structure

```text
test-coverage/          Part 0 — scenario design
ui-tests/               Part 1 — Playwright for Java
api-tests/              Part 2 — REST Assured for Java
.github/workflows/      Part 3 — CI
docker-compose.yml      Optional Docker execution
README.md               This overview
```

The suite-specific READMEs contain implementation-level setup, architecture, and findings. This file stays focused on reviewer-level decisions and evidence.

---

# Part 0 — Test design

Full scenario set: [`test-coverage/`](./test-coverage)

I designed **38 scenarios**, ordered by business impact and likelihood.

Coverage deliberately includes:

- Happy path, invalid input, boundaries, and edge cases.
- Dedicated `locked_out_user`, `problem_user`, and `performance_glitch_user` scenarios based on their observed differences from `standard_user`.
- Newly discovered behaviors folded into the scenario set rather than ignored.
- `AMBIGUITY` markers where the application/specification does not define expected behavior.

This reflects the assignment's emphasis on equivalence classes, boundaries, risk, and explicit ambiguity rather than a generic happy-path checklist.

---

# Part 1 — UI automation

Full detail: [`ui-tests/README.md`](./ui-tests/README.md)

### Architecture

```text
JUnit tests
    ↓
Page Actions → Page Locators
    ↓
Playwright
```

Key decisions:

- Page Object Model, separating actions from locators.
- `getByTestId` for `data-test` attributes and `getByRole` elsewhere.
- Fresh `BrowserContext` per test for isolation.
- No blanket retries, `Thread.sleep`, or weak assertions.
- Class-level parallel execution with independent Playwright/Browser instances.

Negative and access-control cases are covered too — e.g. `locked_out_user` is asserted to be rejected with the exact "Epic sadface: Sorry, this user has been locked out." message, not just a generic error. Confirmed defects are tagged `@known-defect` and assert the **observed broken behavior**. If the product is fixed later, the regression test fails and forces the expected behavior to be reviewed.

Performance scenarios log elapsed time on every run; hard SLA assertions are opt-in so normal live-service/network variance does not create flaky failures.

**Result: 38 tests, 0 failures.**

---

# Part 2 — API automation

Full detail: [`api-tests/README.md`](./api-tests/README.md)

### Architecture

```text
Tests → UserSteps → UsersClient → ApiClient → REST Assured
```

The API suite uses:

- Reusable client/service layers and request/response POJOs.
- Shared assertion helpers and dynamic test data.
- Request/response logging for diagnosis.
- `TestDataRegistry` plus `afterEach`, `afterAll`, and shutdown cleanup.
- Re-reads after writes to verify persistence.
- JSON Schema validation with unknown-property rejection.
- Field-level soft assertions.

GoREST is a shared, rate-limited live service, so API tests remain sequential and do not assume fixed list ordering.

One confirmed case-sensitive email-uniqueness defect is explicitly skipped with its reason documented in code. Documentation/live-behavior mismatches are handled by testing the meaningful invariant where the exact status/body differs.

**Result: 56 tests discovered, 55 passed, 1 known defect skipped.**

---

# Part 3 — CI/CD

Workflow: [`.github/workflows/cicd.yml`](./.github/workflows/cicd.yml)

**Assumption:** the default branch is `main`, matching the starter workflow.

### Seeded CI issues found and fixed

| # | Issue | Fix |
|---|---|---|
| 1 | No checkout | Added `actions/checkout@v4` |
| 2 | Java 8 vs project Java 17 | Set Java 17 |
| 3 | `mvn ... \|\| true` masked failures | Removed failure masking |
| 4 | Wrong Maven working directory | Run each suite in its own project |
| 5 | API token not injected | Validate secret, then pass via `env` |
| 6 | Playwright browsers not installed | Added Playwright browser install |
| 7 | One suite could hide the other | Split UI/API into independent jobs |
| 8 | Concurrent runs risked GoREST rate limits | Added workflow concurrency |

### Bugs found during real implementation

Two useful examples:

- Playwright installation initially failed because its Maven dependency was test-scoped. Adding `-Dexec.classpathScope=test` fixed the classpath.
- GitHub's JUnit annotation action needed `checks: write`; adding the workflow permission removed the post-test annotation error.

### Making CI trustworthy

- UI and API results are independently visible.
- JUnit/Surefire reports are uploaded as artifacts.
- UI failure screenshots are preserved.
- Missing `GOREST_API_TOKEN` fails clearly before token-dependent tests run.

**Completed run:** [GitHub Actions run on `main`](https://github.com/natthawadeesuriyanan/sdet-test/actions/runs/37316976494)

All four jobs passed: UI 38/38; API 55 passed + 1 skipped; both result-report jobs passed.

---

# Part 4 — AI-assisted workflow

I used AI as an implementation and reasoning accelerator, not as the source of truth.

- **Part 0:** scenario prioritization and ambiguity analysis; verified behavior manually.
- **Part 1:** Page Object boilerplate and Java/JUnit translation.
- **Part 2:** REST Assured client/service-layer structure and boilerplate; verified against the live API.
- **Part 3:** CI debugging and candidate fixes.
- **Concrete miss:** AI documented a Playwright CLI fix, but the actual workflow file had not been updated. The next CI run exposed the same `ClassNotFoundException`.
- I caught this by comparing the fresh CI log with repository state.
- **Not delegated:** final code review, live behavior verification, test results, and CI execution.
- Rule: **AI output is a draft; repository state and real execution are evidence.**

---

# Assumptions and ambiguities

### UI

- The 8% checkout tax is not documented on SauceDemo; it was reverse-derived from an observed example and stored as one named constant.
- Maximum field length, whitespace trimming, and case sensitivity are unspecified and therefore documented as ambiguities rather than guessed.
- Sort persistence across navigation remains an open ambiguity until confirmed as intentional or defective.

### API

GoREST's live behavior differs from its published documentation in a few cases, including request content type and some unauthenticated/error responses. Where the invariant is clear, tests assert that invariant rather than creating failures from documentation-only differences.

Tests create and clean up only their own records and use a fresh authenticated token as the baseline.

---

# Stretch goals

These are intentionally separated from the required work.

### Docker

```bash
docker compose run --rm ui-tests
docker compose run --rm api-tests
```

UI uses a Playwright image pinned to the project version; API uses Maven + Temurin 17. `.dockerignore` excludes `.env`, and `target/` is mounted so reports/screenshots survive `--rm`.

### Allure

Both suites generate Allure results. CI builds and uploads HTML reports as artifacts.

### Targeted retry

Only three live-service timing/SLA tests retry up to three times. Functional and known-defect tests never use blanket retries.

### UI parallel execution

Test classes run concurrently while methods within a class remain sequential.

Local result: **56.7s → 35.3s**, with all 38 tests still passing.

API remains sequential because GoREST's shared **90 requests/minute per-token limit** makes uncontrolled parallelism more likely to create rate-limit noise.

---

# Beyond the brief — on-demand PR testing

I also built an optional [`pr-command.yml`](./.github/workflows/pr-command.yml) workflow for targeted test execution from PR comments:

```text
/ui-test
/ui-test core-flow-test browser chrome
/ui-test all browser chrome,msedge
/api-test
/api-test user-validation-tests
```

It reports status/results back to the PR and uses an allowlist, collaborator check, no secrets for UI runs, and fork protection for API runs.

While building it I also fixed issues involving action versioning, test discovery, abstract test classes reporting zero tests, artifact download behavior, and GitHub permissions.

---

# What I would improve with more time

- **UI:** investigate remaining case/whitespace and sort-persistence ambiguities with controlled starting states before classifying them as defects.
- **API:** add a second GoREST token for cross-token isolation and authenticated write behavior; add the remaining identified validation edge cases.
- **CI/reporting:** add environment metadata to Allure and evaluate CI parallelism against GitHub runner CPU limits.
