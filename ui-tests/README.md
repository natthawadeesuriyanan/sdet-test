# SauceDemo Playwright Tests

Java 17+ UI automation for `https://www.saucedemo.com/`, using the official Playwright Java library and JUnit 5 (Jupiter). The suite uses Page Objects split into `locators` (element lookup) and `actions` (behavior) per page, a fresh browser context per test, finite Playwright timeouts, no fixed sleeps, and no blanket retries — the only retries are a narrow, documented exception (see "Retry Policy" below).

## Setup

Prerequisites: Java 17 or later and Maven 3.9 or later.

Install the Chromium browser used by Playwright:

```powershell
mvn "-Dexec.classpathScope=test" "-Dexec.mainClass=com.microsoft.playwright.CLI" "-Dexec.args=install chromium" exec:java
```

Run the suite:

```powershell
mvn test
```

Run a focused group:

```powershell
mvn "-Dtest=CoreFlowTest" test
mvn "-Dtest=ProblemUserTest" test
mvn "-Dtest=EdgeCoverageTest" test
```

Run only known-defect regression tests (tests tagged `@Tag("known-defect")`, currently in `ProblemUserTest`):

```powershell
mvn "-Dgroups=known-defect" test
```

Run everything except known-defect regression tests:

```powershell
mvn "-DexcludedGroups=known-defect" test
```

The Playwright browser install is a one-time step per Playwright version. Playwright may download Chromium on first use if it is not already installed.

## Coverage

- `CoreFlowTest`: TC-01–03, TC-05–11, TC-14–22, TC-25–26. Includes standard-user checkout, locked-out rejection, authentication validation, incremental cart badge, checkout field validation (all three mandatory fields), sorting, totals/tax, and performance measurement.
- `ProblemUserTest`: TC-04, TC-12, TC-13a–13b, TC-23–24. Known defects are tagged `@Tag("known-defect")` and asserted as observed so a site change makes the regression check fail for review — see "Observed Defects Summary" below for the full list.
- `EdgeCoverageTest`: TC-27–33, TC-36–38. Includes the receipt PDF download (TC-37) and the Reset App State stale-label behavior (TC-38), both discovered during exploration and outside the original Part 0 scope.
- TC-34 and TC-35 remain manual: case sensitivity and whitespace trimming are unresolved in the supplied test design and have no agreed expected result.

The test plan's 8% tax rate is an observed assumption, not a published contract. It is defined once as `TAX_RATE` and applied to prices read from the site using decimal arithmetic. The suite verifies totals against that configured assumption.

## Observed Defects Summary

All defects below were confirmed by hand on the live site before being automated. Each test asserts the defect's *actual* observed behavior (not the "correct" behavior), so a future fix shows up as a test failure — a signal to update the test and release notes, not a bug in the suite.

| Test ID | Defect | Account | Severity |
|---|---|---|---|
| TC-04 | Typing in the Last Name field at checkout shifts the text into First Name instead; Last Name stays empty. Since all three fields are mandatory, this makes checkout impossible to complete. | `problem_user` | **Critical** — release blocker |
| TC-12 | Only 3 of 6 "Add to cart" buttons actually add their product; the other 3 do nothing. Specific affected products are pinned in `OBSERVED_ADD_BUTTONS`. | `problem_user` | High |
| TC-13a | The Remove button on the **Inventory page** (Add/Remove toggle under each product) does not remove the item or update the cart badge. | `problem_user` | High — the Cart-page Remove button (TC-13b) is unaffected, so this is isolated to the Inventory-page toggle specifically |
| TC-23 | All product images on the inventory page share the same broken/mismatched image source instead of each product having its own. | `problem_user` | Medium — cosmetic |
| TC-24 | All 4 sort dropdown options are selectable, but product order never actually changes for any of them. | `problem_user` | Medium |
| TC-38 | Reset App State correctly clears the cart, but the product button label doesn't revert from "Remove" to "Add to cart" on its own; self-corrects on the next click. | `standard_user` | Low — cosmetic, not a blocker, and `standard_user` is otherwise the "should always work" baseline |

Two related notes:
- TC-13a/13b together show the Remove-button defect is **location-specific**: broken on the Inventory page, working correctly on the Cart page. Both are now covered so a fix (or regression) in either location is caught independently.
- TC-23 asserts on duplicate `data-test="inventory-item-img"` source values rather than performing pixel-level image comparison — cheap, deterministic, and avoids the flakiness of an image-diffing library while still catching the defect's actual signature.

### Unresolved Ambiguities (not confirmed defects)

These are findings where the actual behavior is documented and tested, but it's unclear whether it's a bug or intentional — they're kept separate from the table above rather than labeled as confirmed defects:

| Test ID | Observed Behavior | Open Question |
|---|---|---|
| TC-30 | Applying a sort, then navigating to a product detail page and back via browser Back, does **not** keep the applied sort — **confirmed by running**, the resulting order matches neither the applied sort nor any other single dropdown option (not alphabetical, not price ascending or descending). The test asserts only that the order changed (`assertNotEquals`), not what it changed to, since the fallback order isn't shown to be deterministic. | Cart state persists across the same navigation (TC-28), so the inconsistency is worth a product/dev decision: should sort state persist too? And if the reset order isn't any documented sort, is it even deterministic, or could it vary between runs? |

If product/dev confirms either answer, this test's name and assertion should be updated accordingly — and, if the team decides it's a defect, it should move into the confirmed-defects table above with a `@Tag("known-defect")`.

## Allure Report

The suite generates Allure results (`target/allure-results`) via `allure-jupiter`; results directory is set in `src/test/resources/allure.properties`. CI generates the full HTML report (via the official `allure-commandline` npm package) and uploads it as the `ui-allure-report` artifact on every run. To view locally: `allure generate target/allure-results --clean -o target/allure-report`, then `allure open target/allure-report` (or serve it with any static file server — opening `index.html` directly from the filesystem will not load correctly).

## Retry Policy

Only three tests retry, via `@RetryingTest(3)` from `junit-pioneer`: the `performance_glitch_user` login/checkout timing assertions and the `standard_user` login baseline (`TC09`/`TC25`, `TC26`, `TC10`). Rationale: these assert a timing SLA against saucedemo.com, a live shared service, where a single slow network blip is noise unrelated to any real regression — a *consistent* slowdown still fails all 3 attempts, so the retry doesn't mask an actual performance regression. No other test retries: a flaky functional or known-defect assertion is a real bug to investigate, not noise to filter out by trying again.

## Parallel Execution

Enabled via `src/test/resources/junit-platform.properties`: test classes run concurrently (`classes.default = concurrent`), while methods within a class stay sequential (`mode.default = same_thread`). This matches Playwright's own guidance that `Playwright`/`Browser` objects aren't safe to share across threads — `BaseTest` uses `@TestInstance(PER_CLASS)` with non-static `Playwright`/`Browser` fields so each test class gets its own independent instance, rather than all three subclasses racing on one shared static field (which is what a naive `static` field here would cause under parallel execution). Verified: total suite time dropped from 56.7s to 35.3s with all 38 tests still passing, and the new total sits close to the slowest individual class rather than the sum of all three — confirming classes genuinely ran concurrently, not just that the config flag was set.

## Performance Measurements

Each performance scenario logs its measured elapsed time and flags outliers without failing by default. This avoids turning shared-runner or network variance into flaky failures. Defaults are 2 seconds for standard-user login, 8 seconds for performance-glitch login, and 45 seconds for the performance-glitch checkout. Thresholds are configurable; enable hard assertions only on a stable, pinned runner:

```powershell
mvn "-Dsaucedemo.performanceAssertions=true" "-Dsaucedemo.standardLoginMaxMs=3000" "-Dsaucedemo.performanceGlitchLoginMaxMs=10000" "-Dsaucedemo.performanceGlitchCheckoutMaxMs=60000" test
```

Override the base URL or browser mode with `SAUCEDEMO_BASE_URL` and `SAUCEDEMO_HEADLESS`, or equivalent `saucedemo.baseUrl` and `saucedemo.headless` system properties. The default is headless Chromium.
