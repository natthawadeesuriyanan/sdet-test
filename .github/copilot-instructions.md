# Workspace Instructions

- This is a Java 17+ Maven test project using official Playwright for Java and JUnit 6.
- Keep UI automation in `src/test/java/com/saucedemo`; use the existing Page Object Model and isolated `BrowserContext` lifecycle.
- Prefer accessible role/name locators; use `[data-test=...]` only for app hooks without a suitable accessible name. Playwright Java `getByTestId` defaults to `data-testid`, while SauceDemo uses `data-test`.
- Use Playwright auto-waiting and web-first assertions. Do not add `Thread.sleep`, generic retries, or shared browser state between tests.
- Keep credentials, timeouts, URLs, performance limits, and tax assumptions centralized in `TestConfig`.
- Keep currency assertions in `BigDecimal`; do not hardcode checkout totals derived from the current catalog.
- Document known defects and changed live-site behavior in `README.md`; do not silently convert an observed defect into expected healthy behavior.
- Run `mvn test` after test changes. Install Chromium with the command documented in `README.md` when needed.
