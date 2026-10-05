# SauceDemo Playwright Tests

Java 17+ UI automation for `https://www.saucedemo.com/`, using the official Playwright Java library and JUnit 5 (Jupiter). The suite uses page objects, role-first locators, a fresh browser context per test, finite Playwright timeouts, and no fixed sleeps or retries.

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

- `CoreFlowTest`: TC-01–03, TC-05–11, TC-14–22, TC-25–26. Includes standard-user checkout, locked-out rejection, authentication validation, incremental cart badge, sorting, totals/tax, and performance measurement.
- `ProblemUserTest`: TC-04, TC-12–13, TC-23–24. Known defects are labeled and asserted as observed so a site change makes the regression check fail for review.
- `EdgeCoverageTest`: TC-27–33, TC-36–37.
- TC-34 and TC-35 remain manual: case sensitivity and whitespace trimming are unresolved in the supplied test design and have no agreed expected result.

The test plan's 8% tax rate is an observed assumption, not a published contract. It is defined once as `TAX_RATE` and applied to prices read from the site using decimal arithmetic. The suite verifies totals against that configured assumption.

## Observed Defects And Drift

On the live site explored for this project, `problem_user` showed all three documented product-add failures, duplicate broken image sources, unchanged product order for all four sort options, and the Last Name field overwriting First Name during checkout.

One supplied finding did not reproduce: the `problem_user` Remove button **on the cart page** removed an item successfully, so TC-13 asserts successful removal and badge recalculation there. The Inventory-page Remove button (the Add/Remove toggle under each product) was not separately automated; if it turns out to behave differently from the cart page, it would need its own scenario. Recheck both locations if the site changes.

The product image test looks for duplicate `404` asset sources rather than performing pixel comparisons. Sorting is intentionally a known-defect regression test: it currently asserts that the order remains unchanged for `problem_user`.

## Performance Measurements

Each performance scenario logs its measured elapsed time and flags outliers without failing by default. This avoids turning shared-runner or network variance into flaky failures. Defaults are 2 seconds for standard-user login, 8 seconds for performance-glitch login, and 45 seconds for the performance-glitch checkout. Thresholds are configurable; enable hard assertions only on a stable, pinned runner:

```powershell
mvn "-Dsaucedemo.performanceAssertions=true" "-Dsaucedemo.standardLoginMaxMs=3000" "-Dsaucedemo.performanceGlitchLoginMaxMs=10000" "-Dsaucedemo.performanceGlitchCheckoutMaxMs=60000" test
```

Override the base URL or browser mode with `SAUCEDEMO_BASE_URL` and `SAUCEDEMO_HEADLESS`, or equivalent `saucedemo.baseUrl` and `saucedemo.headless` system properties. The default is headless Chromium.
