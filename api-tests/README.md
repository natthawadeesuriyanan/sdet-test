# GoREST API automation - Java 17, REST Assured, JUnit 5

API test suite for the GoREST `/users` resource (`https://gorest.co.in/public/v2`).
It covers the CRUD lifecycle, validation errors, authentication, not-found cases, query and pagination behaviour, and response contracts. Every record the suite creates is cleaned up, including when a test fails.

## Quick start

```bash
# 1. Token - pick ONE method
cp .env.example .env              # in the repo root (D:\sdet-test); then edit .env (git-ignored)
export GOREST_API_TOKEN=xxxx       # or an environment variable
mvn test -DGOREST_API_TOKEN=xxxx   # or a JVM property

# 2. Run
mvn test                          # everything
mvn test -Dgroups=crud            # by tag: crud | validation | auth | edge | query | smoke
mvn test -DGOREST_LOG_ALL=true    # print every exchange, not only failures
```

Requirements: JDK 17+ and Maven 3.9+.

### Token handling

| Source | Used for | Lookup order |
|---|---|---|
| JVM property `-DGOREST_API_TOKEN` | ad-hoc runs | 1 |
| Environment variable `GOREST_API_TOKEN` | **CI** | 2 |
| `.env` file in the working directory or **any parent folder** (normally the repo root, next to `api-tests/` and `ui-tests/`) | **local runs** | 3 |

* `.env` is listed in `.gitignore`. Only `.env.example` is committed.
* `Config` looks for `.env` in the working directory and then walks up the parent folders. One `.env` in the repo root therefore works whether the run starts from the root, from `api-tests/`, from Maven or from the IDE test runner.
* The first log line of every run shows where the token came from, for example `token=present .env=D:\sdet-test\.env`. If it says `token=MISSING .env=<none found ...>`, the file is not in the working directory or any parent.
* In logs and failure dumps, the `Authorization` header is masked (`Bearer ****abcd`).
* **A missing token fails the run. It never skips tests.** Any class annotated with `@RequiresToken` fails in `beforeAll` with a `MissingTokenException` that explains how to supply the token. `PublicReadTests` is the only class without that annotation, because anonymous reads should work without credentials.
* In CI, the token is stored as the repository secret `GOREST_API_TOKEN` and injected only as an environment variable. Before Maven runs, a separate workflow step fails the job if the secret is missing (for example on a fork PR).

## Architecture

```
src/test/java/com/sdet/gorest
├── config/       Config (sysprop > env > .env), RunContext (unique run id), MissingTokenException
├── http/         HttpExchangeRecorder (REST Assured filter), HttpExchange
├── client/       ApiClient (base spec, Auth modes, 429/5xx retry), UsersClient (/users endpoints)
├── model/        User, Gender, Status, ValidationError, ApiError  (Jackson POJOs)
├── data/         UserFactory (dynamic data), TestDataRegistry (ids we own), UserCleaner
├── service/      UserSteps (business-level arrange/verify steps)
├── assertions/   ApiAssertions (status+dump, schema, 422 shape, soft field compare), Schemas
├── extensions/   GoRestExtension (lifecycle: token gate, cleanup, failure dump), @RequiresToken
└── tests/        *Tests - test intent only, no raw REST Assured calls
src/test/resources/schemas   JSON schemas: user, user-list, validation-errors, error
```

Layering: **tests → UserSteps → UsersClient → ApiClient → REST Assured**.
* `UsersClient` returns the raw `Response`, so tests can assert on failures as well as successes.
* `UserSteps` asserts the happy path for setup, so a broken precondition is reported as a setup failure and doesn't surface later as a confusing error.

### Response validation (beyond the status code)
* The status code is checked with `assertStatus`. If it fails, the failure message contains the full exchange.
* The `Content-Type` header must be `application/json`.
* The body is validated against a **JSON schema**. The schemas reject unknown properties, so contract drift is caught. They also check enums and that `id` is a positive integer.
* Payloads are compared field by field with **soft assertions**, so every mismatch is reported in one run.
* Each write is followed by a **re-read** (GET after POST/PUT/DELETE). This proves the change was persisted, not just echoed back.
* Each rejected request is checked for side effects: after a 401 or 422, the stored record is unchanged, or no record was created.
* 422 responses must have the documented `[{field, message}]` shape and name the expected field.
* Response time must stay under a configurable budget (`GOREST_MAX_RESPONSE_TIME_MS`, default 10 s, which is generous for a live shared service).

### Logging on failure
`HttpExchangeRecorder` records every request and response of the current test, including retries and setup calls. When a test fails, `GoRestExtension.testFailed` prints the complete transcript: method, URL, headers with the token masked, request body, status line, response headers and response body. Passing tests stay quiet.

### Test data lifecycle
1. **Unique data:** `UserFactory` builds names like `Ana Silva sdet-<runId>-<seq>` and emails like `sdet.<runId>.<seq>.<uuid8>@example.com`. There are no hardcoded ids or emails. When a test needs an id that doesn't exist, it creates a user, deletes it, and uses that id.
2. **Registered at creation:** `UsersClient` registers every `201` id in `TestDataRegistry` as soon as the response arrives, before any assertion runs. A failing assertion therefore can't leak the record.
3. **Cleanup in three layers:**
   * `afterEach` runs even when the test fails.
   * `afterAll` retries anything left over.
   * A JVM shutdown hook runs on abort (for example Ctrl+C).
   * A 404 during cleanup counts as success.
4. **Ownership guarantee:** cleanup deletes **only** ids from the registry, meaning ids returned by this run's own 201 responses. The suite never updates or deletes users it did not create. Mutation tests always create their own target first.

## Allure Report

The suite generates Allure results (`target/allure-results`) via `allure-jupiter`; results directory is set in `src/test/resources/allure.properties`. CI generates the full HTML report (via the official `allure-commandline` npm package) and uploads it as the `api-allure-report` artifact on every run — includes the one `@Disabled` known-defect test shown correctly as skipped, not failed. To view locally: `allure generate target/allure-results --clean -o target/allure-report`, then `allure open target/allure-report` (opening `index.html` directly from the filesystem will not load correctly — it needs to be served).

## Scenarios (56 test executions across 33 test methods; 1 disabled as a known defect)

| Area | Scenario |
|---|---|
| CRUD | POST → 201, id assigned, payload echoed, schema valid |
| CRUD | GET by id returns exactly the persisted record |
| CRUD | PUT replaces all mutable fields; persisted; id stable |
| CRUD | DELETE → 204 with empty body; then GET 404 and DELETE again 404 |
| CRUD / security | POST ignores a client-supplied `id` (mass assignment); the existing user is untouched |
| CRUD / security | PUT ignores `id` in the body; the path id wins; the other user is untouched |
| Edge | Thai, accented and apostrophe names round-trip intact over UTF-8 (×3) |
| Validation | Each required field missing → 422 `can't be blank`; nothing persisted (×4) |
| Validation | `{}` → 422 listing all 4 fields |
| Validation | Malformed emails → 422 `is invalid` (×5) |
| Validation | gender/status outside the enum or empty → 422 (×4) |
| Validation | Whitespace-only name → 422 |
| Validation | Duplicate email → 422 `has already been taken`; still exactly one user |
| Validation | Malformed JSON → 400 (never 5xx) |
| Validation | `text/plain` body → rejected and nothing persisted (docs say 415, live returns 422: finding F1) |
| Validation | PUT with invalid email → 422; record unchanged |
| Validation | PUT to another user's email → 422; both users unchanged |
| Auth | POST with no token / invalid token → 401; no user created (×2) |
| Auth | PUT with no token → 404, invalid token → 401; user unchanged (×2) (finding F2) |
| Auth | DELETE with no token → 404, invalid token → 401; user still exists (×2) (finding F2) |
| Auth / isolation | A user created with our token is not visible to anonymous GET (404) |
| Not found | GET / PUT / DELETE on a deleted id → 404 with error body (×3) |
| Edge | ids `0`, `-1`, `abc`, `1.5` and an overflowing number → 4xx, never 5xx (×5) |
| Query | `?email=` filter finds our user; every result matches the filter |
| Query | `?email=` with no matches → 200 and `[]` |
| Query | `?name=` filter finds our user; every result matches the filter |
| Pagination | `per_page=5` honoured; page is not empty; `X-Pagination-Limit` and `X-Pagination-Page` are **required** and match |
| Pagination | `per_page=500` capped at 100 |
| Pagination | A page past the last one → 200 with `[]` |
| Smoke | Anonymous `GET /users` → 200, contract valid |
| Validation | Email duplicate differing only by case → 422; no second user (**`@Disabled`: known defect D1**) |
| Edge / boundary | Names of 255, 256 and 10,000 characters → success or 4xx, never 5xx (×3) |
| Method | `DELETE /users` without an id → rejected as 4xx, never 5xx, and **no user is deleted** (docs say 405, live returns 404 HTML: finding F3) |

## Findings from running against the live API

Every red test was triaged first: *is my test wrong, or does the system differ from its contract?* Only after that did the test change. A change never weakened the important invariant (request rejected, no side effect).

### Defects

| # | Severity | Finding | Evidence | Handling |
|---|---|---|---|---|
| D1 | Medium | **Email uniqueness is case-sensitive** — `A@X.COM` can be registered while `a@x.com` exists (201 instead of 422), letting one address own two accounts. | `duplicate_email_ignores_case` | Treated as a genuine defect, not accepted behavior: per RFC 5321 the domain part is case-insensitive, and almost every real system normalises the whole address before checking uniqueness. The assertion is kept as the desired contract, and the test is marked `@Disabled` with the reason documented in code — not silently skipped. Re-enabling requires confirming the intended email-normalisation behavior first — whether the whole address should be normalised, or just the domain per RFC 5321. The accidental duplicate created during the test run was still cleaned up correctly, since ids are registered in `TestDataRegistry` on the 201 response before any assertion runs. |

### Documentation vs live behaviour

| # | Request | Documented | Live | Handling |
|---|---|---|---|---|
| F1 | `POST /users` with `Content-Type: text/plain` | 415 | 422 (the header is ignored; every field reported blank) | Asserts 4xx and nothing persisted; accepts 415 or 422. |
| F2 | `PUT` / `DELETE /users/{id}` on our own user **without** a token | 401 | 404 (an invalid token does return 401; POST returns 401 in both cases) | Expected code set per auth mode; asserts the record is unchanged. 404 is reasonable because it doesn't reveal that the id exists. |
| F3 | `DELETE /users` (collection, no id) | 405 JSON error | 404 with an **HTML** page | Asserts 4xx, never 5xx, and that no user was deleted; accepts 404 or 405. The HTML body breaks the API's JSON error contract, which matters for clients that parse every error as JSON. |

Exactly one test is `@Disabled`, and only for a confirmed defect. A suite that disables tests whenever they go red stops meaning anything.

## Service limitations that shaped the design

| Limitation | Design response |
|---|---|
| **Rate limit:** 90 requests per minute per token by default, with `X-RateLimit-*` headers | `ApiClient` retries 429 responses (safe for any method, because the request was not processed). It waits for `Retry-After` or `X-RateLimit-Reset`, otherwise uses exponential back-off with jitter. It also pauses before the budget hits 0. 502, 503 and 504 are retried **only for idempotent methods**, so a POST is never duplicated. |
| Shared live service; one token's budget is shared by every run | Tests run sequentially (`junit-platform.properties`). The CI `concurrency` group allows only one run at a time. |
| **Per-token isolation:** records you create are visible only to your token; others get 404 | Reads send the token by default. Isolation is tested explicitly. **403 (modifying another token's record) can't be tested with a single token.** This would need a second token, so it's listed as future work. |
| Data is wiped and reseeded every 24 hours; other users change data all the time | No assertions on ordering, totals, or specific seed records. List tests check only properties that must hold for whatever the page contains. Cleanup accepts 404. |
| Filters are partial matches (`LIKE`) | The suite filters by unique values, checks that our record is *contained* in the results, and narrows to exact matches on the client side where needed. |
| PUT is documented as a full replace; partial PUT behaviour is unspecified | Update tests always send the full set of mutable fields. |
| **Transient 502 on writes:** the live service sometimes returns `502 Bad Gateway` with `Retry-After: 60` for every POST, while GETs keep working | POST is deliberately **not** retried on 5xx, because the server may already have created the record and a retry could duplicate it. Tests fail loudly instead. A 502 on the no-token case, where 401 is expected, rules out authentication. Wait at least 60 s and rerun; if it persists, reproduce in the GoREST web console. Tests are never changed to accept a 502. |
| Exact 401 message text varies (`Invalid token`, `Authentication failed`) | The suite asserts status, schema and a non-blank message, not the wording. |

## Possible extensions
* Injection-style strings (`<script>`, `' OR 1=1 --`) must be stored literally and never cause a 5xx.
* An explicit `null` (`{"name": null}`) as distinct from a missing field. The POJO currently omits nulls, so this needs a raw-body test.
* Whitespace trimming, idempotent repeated PUT, PUT with an empty or partial body.
* Malformed paging parameters (`page=0`, `page=-1`, `per_page=abc`) → never 5xx.
* Malformed `Authorization` headers (`Bearer` with no token, `Basic ...`) and the `?access-token=` query transport.
* The `Location` header on 201, which the docs mention.
* A second token (`GOREST_API_TOKEN_SECONDARY`) to test the 403 response and cross-token isolation for writes.
* Attaching the full `HttpExchangeRecorder` transcript to each failed test's Allure result, not just printing it to the console — Allure reporting itself is already implemented (see "Allure Report" above), this would extend it.
* Contract tests generated from an OpenAPI spec, if GoREST publishes one.
* Using `?force_status=429/503` to unit-test the retry policy against the real service.
