# Test Coverage — Login & Cart/Checkout (saucedemo.com)

Scope: `standard_user`, `locked_out_user`, `problem_user`, `performance_glitch_user` (all with password `secret_sauce`). Other demo accounts on the site are out of scope.

IDs are numbered **sequentially from TC-01 to TC-38 in priority order** (Critical → High → Medium → Low), with two scenarios discovered during exploration (TC-37, TC-38) appended after Low rather than re-sorted into their own tier, to avoid renumbering everything else each time something new is found. TC-13 is split into TC-13a/TC-13b (same priority slot) because exploration showed it is actually two distinct behaviors, not one.

Items marked **AMBIGUITY** were not specified anywhere in the app or a spec, and were resolved by manual exploration where noted; unresolved ones carry an explicit assumption.

---

## Critical (TC-01 – TC-04)

| ID | Category | Account | Condition / Steps | Expected Result | Notes |
|----|----------|---------|---|---|---|
| TC-01 | Happy Path | standard_user | Login with valid username + `secret_sauce` | Redirect to `/inventory.html`, products visible | Core journey — everything else depends on this |
| TC-02 | Happy Path | standard_user | Full checkout: add item → cart → checkout → valid info → finish | Order confirmation ("Thank you for your order") | Core business flow this site exists to demo |
| TC-03 | Negative | locked_out_user | Login with valid credentials | Blocked with: "Epic sadface: Sorry, this user has been locked out." | Access-control enforcement — a broken lockout is security-class |
| TC-04 | **Blocking Bug (confirmed)** | problem_user | Fill Checkout Information form (First/Last Name/Zip) | **Confirmed: typing in Last Name field shifts focus/text into First Name — form cannot be completed, all fields mandatory** | Highest-value finding in this set: a confirmed release-blocker, not cosmetic |

## High (TC-05 – TC-22)

| ID | Category | Account | Condition / Steps | Expected Result | Notes |
|----|----------|---------|---|---|---|
| TC-05 | Negative | standard_user | Correct username, wrong password | Error: "Epic sadface: Username and password do not match any user in this service" | Core auth validation |
| TC-06 | Negative | standard_user | Non-existent username | **Confirmed (tested):** same generic error as TC-05 | AMBIGUITY resolved by running — app does not differentiate "not found" vs "wrong password" |
| TC-07 | Boundary/Negative | standard_user | Empty username, empty password, submit | Error: "Epic sadface: Username is required" | Validates field-check order |
| TC-08 | Boundary/Negative | standard_user | Valid username, empty password | Error: "Epic sadface: Password is required" | Core form validation |
| TC-09 | **Performance Assertion** | performance_glitch_user | Measure elapsed time: click Login → inventory grid visible | Within an agreed upper bound (e.g. ≤5–8s, TBD with team) | The actual regression-catching test for this account. Log measured time every run (pass or fail) to build a trend |
| TC-10 | **Performance Assertion** | standard_user | Same timing measurement as TC-09 | Within a tight threshold (e.g. ≤2s) | Baseline — TC-09's threshold is meaningless without this comparison |
| TC-11 | Happy Path | standard_user | Add 1 item, then remaining 5 incrementally | Badge updates correctly 1→2→3→4→5→6 | Merges former "add one" / "add all" into one incremental-assertion scenario |
| TC-12 | **Functional Bug (confirmed)** | problem_user | Attempt to add all 6 products to cart | **Confirmed: only 3 of 6 "Add to cart" buttons work** | Major defect on the primary action; automate which specific products fail, don't hardcode "any 3" |
| TC-13a | **Functional Bug (confirmed)** | problem_user | Click Remove on the **Inventory page** (Add/Remove toggle under the product) | **Confirmed: the item is not removed and the cart badge does not change** — the button stays stuck on "Remove" | Directly upstream of TC-04's severity. The defect is location-specific — see TC-13b |
| TC-13b | Functional (confirmed) | problem_user | Click Remove on the **Cart page** for a successfully-added item | **Confirmed: this Remove button works correctly** — item is removed and the badge updates | Same action, different page, different (correct) outcome — both locations now have explicit coverage |
| TC-14 | Negative | standard_user | Checkout with exactly one mandatory field empty at a time: First Name, then Last Name, then Postal Code (other two filled) | **Confirmed (tested):** empty First Name → "Error: First Name is required"; empty Last Name → "Error: Last Name is required"; empty Postal Code → "Error: Postal Code is required" | Core checkout validation, covering all three mandatory fields as separate equivalence classes. Bonus finding confirmed by testing: if all three fields are left empty at once, only "Error: First Name is required" is shown — the form validates fields in order (First Name → Last Name → Postal Code) and stops at the first one it finds empty, rather than listing every missing field |
| TC-15 | Functional | standard_user | Select "Name (A to Z)" sort option | Products reorder alphabetically ascending | Baseline — gives TC-24 (broken sort) something concrete to compare against |
| TC-16 | Functional | standard_user | Select "Name (Z to A)" | Products reorder alphabetically descending | — |
| TC-17 | Functional | standard_user | Select "Price (low to high)" | Products reorder by price ascending, **numeric** not lexicographic | Verifies numeric sort (e.g. $7.99 vs $15.99 sorted correctly as numbers, not strings) |
| TC-18 | Functional | standard_user | Select "Price (high to low)" | Products reorder by price descending | — |
| TC-19 | Functional | standard_user | Add 1 item → checkout overview page | "Item total" matches the exact price shown on the product page | Baseline single-item calculation check |
| TC-20 | Functional | standard_user | Add 3 items with different prices → checkout overview | "Item total" = sum of all 3 individual item prices | Verifies summation logic, not just pass-through of a single value |
| TC-21 | Functional | standard_user | From TC-20's cart, check the "Tax" line on checkout overview | Tax = Item total × 8%, rounded to 2 decimals | **AMBIGUITY: tax rate/formula not documented anywhere on the site.** Resolved by reverse-calculation from an observed example ($129.94 → $10.40 implies 8%). Automate by computing expected tax from a named constant (`TAX_RATE = 0.08`), not by hardcoding one example's numbers |
| TC-22 | Functional | standard_user | Same cart as TC-21 | Total = Item total + Tax (internal arithmetic consistency) | Independent of whatever the tax rate turns out to be — checks the three displayed numbers agree with each other |

## Medium (TC-23 – TC-32)

| ID | Category | Account | Condition / Steps | Expected Result | Notes |
|----|----------|---------|---|---|---|
| TC-23 | **Visual Bug (confirmed)** | problem_user | View product images on inventory page | **Confirmed: all product images are mismatched/incorrect** | Real defect, but cosmetic — doesn't block the flow |
| TC-24 | **Functional Bug (confirmed)** | problem_user | Select each of the 4 sort options in turn | **Confirmed: all 4 options are selectable, but product order never changes for any of them** (selection works, re-render doesn't) | Precise failure mode matters for diagnosis — this is a known-defect regression test, not a generic "sort broken" assertion |
| TC-25 | Functional / Resilience | performance_glitch_user | Login with valid credentials | Login eventually succeeds | Sanity prerequisite for TC-09; use explicit wait with a finite timeout, never a fixed `sleep()` |
| TC-26 | **Performance Assertion** | performance_glitch_user | Measure time for the full checkout flow | Within an agreed upper bound | Extends TC-09's measurement approach beyond just login |
| TC-27 | Functional (confirmed) | standard_user | Add item, then remove it | **Confirmed: cart icon shows with no number/badge** (not "0") | UI correctness, low business risk |
| TC-28 | Functional | standard_user | Add item → product detail page → browser Back → check cart | Cart retains the item | State-persistence edge case |
| TC-29 | Boundary (resolved) | standard_user | Checkout with a valid Canadian postal code (`A1A 1A1`) | Accepted — proceeds to overview page | AMBIGUITY resolved: no country field/format documented, so a real international format is a better test than an arbitrary guess |
| TC-30 | Edge Case | standard_user | Apply a sort, then navigate to product detail and back | **Confirmed (tested): sort selection does NOT persist** — and the resulting order matches neither the applied sort nor any other single dropdown option (not alphabetical, not price ascending or descending) | **AMBIGUITY: unclear whether this is intentional (no sort-state persistence by design) or a gap** — unlike cart state (TC-28), which does persist across the same navigation. Whether the reset order is even deterministic (vs. varying between runs) also hasn't been confirmed. Not tagged as a confirmed defect; flagged for further investigation. Two earlier assumptions about this were both wrong and corrected after actually running the test: first that sort would persist like cart state, then that it falls back to a plain "Name A-Z" default |
| TC-31 | Edge Case | standard_user | Add 2 items → cart → remove 1 item → proceed to checkout overview | Item total/Tax/Total reflect only the 1 remaining item, not the original 2 | Classic regression area: stale totals after cart mutation |
| TC-32 | Boundary | standard_user | Add the single cheapest item, then (separate run) the single most expensive item | Item total exactly matches that item's listed price in both cases | Checks rounding/precision at the low and high ends of the price range |

## Low (TC-33 – TC-36)

| ID | Category | Account | Condition / Steps | Expected Result | Notes |
|----|----------|---------|---|---|---|
| TC-33 | Edge Case | standard_user | Reach order confirmation, click Back twice to inventory | Cart resets to empty | Low likelihood in real usage, still cheap to check |
| TC-34 | Negative | standard_user | Case-swapped username (`Standard_User`) | To confirm by running | **AMBIGUITY unresolved** — case sensitivity not verified; low real-world likelihood |
| TC-35 | Boundary | standard_user | Username with leading/trailing whitespace | To confirm by running | **AMBIGUITY unresolved** — trimming behavior not verified; low real-world likelihood |
| TC-36 | Boundary | standard_user | Username/password ~300 chars of random text | Expect graceful failure, no crash | **AMBIGUITY: no documented max length.** Robustness sanity check, not a realistic scenario |

## Additional Scenarios (discovered during exploration — Medium priority)

| ID | Category | Account | Condition / Steps | Expected Result | Notes |
|----|----------|---------|---|---|---|
| TC-37 | Functional | standard_user | Complete an order, then click the receipt download control on the "Checkout: Complete!" page | A real PDF file downloads (filename ends in `.pdf`, file is non-empty) | **AMBIGUITY: not in the original Part 0 scope** — this receipt-download feature was found while exploring the confirmation page, not anticipated in advance |
| TC-38 | Functional (confirmed) | standard_user | Add an item, open the hamburger menu, click "Reset App State" | **Confirmed: the cart actually clears (badge disappears), but the product's button label stays stuck on "Remove" instead of reverting to "Add to cart"** | Cosmetic/stale-UI defect, not a blocker: clicking the stale "Remove" button removes nothing (cart is already empty) and self-corrects to "Add to cart"; normal add-to-cart works afterward |

---

## Automation Value — what's actually worth automating

Not everything above earns its keep as an automated test. The criteria I used: **will this run repeatedly, is the expected result deterministic, and does a human need to re-verify it by eye every time anyway?**

### Automate without hesitation (TC-01 – TC-22: all Critical and High items)
These are deterministic, core to the business flow or a confirmed defect, and cheap to assert programmatically (text match, element state, numeric comparison, elapsed time, arithmetic). This is where automation ROI is highest — they'll run on every commit and catch real regressions.

**Note on TC-19–22 (price/tax totals):** `AMBIGUITY: the 8% tax rate is not documented anywhere on the site or in any spec — it was derived by back-calculating from one observed example ($129.94 → $10.40).` The test code should store this as a single named constant (e.g. `TAX_RATE = 0.08`) and compute expected tax/total from the actual item prices scraped off the page, rather than hardcoding one example's dollar amounts. If Sauce Labs ever changes the rate, this becomes a one-line fix instead of a scattered find-and-replace across tests.

### Automate, but with caveats
- **TC-23 (image mismatch)** — full visual regression (pixel/image diffing) is expensive and flaky to maintain for a project this size. A cheaper, still-valid automated check: assert that the `src` attributes of the product images are not unique (duplicates present), which captures the defect's signature without an image-diff library.
- **TC-24 (sort broken)** and **TC-13a (Inventory-page Remove broken)** — automate both as *known-defect regression tests*: assert the observed (broken) behavior, not the "correct" behavior. If either defect is ever fixed, the corresponding test fails and flags that it needs updating, instead of silently passing either way.
- **TC-09 / TC-10 / TC-26 (performance)** — worth automating, but timing thresholds are inherently environment-sensitive (CI runner speed varies). Treat these as "log and flag outliers," not hard pass/fail gates, unless you can pin down a stable CI environment.

### Lower priority to automate — keep as a one-time manual/exploratory finding instead
- **TC-34, TC-35 (unresolved ambiguities)** — low real-world likelihood, and the "right" expected result isn't even settled yet. Better to spend 5 minutes resolving them manually and record the finding in the README, rather than spending engineering time automating a test whose expected outcome is still a guess. Automate afterward only if the resolved behavior turns out to matter.
- **TC-36 (robustness sanity)** — cheap to write, but low ongoing value: it's checking "the app doesn't crash on garbage input," unlikely to regress and not a core risk area for a demo e-commerce flow. Fine as a smoke-level check, not worth expanding further. (A SQL-injection-style variant of this was considered and dropped: the app is a static demo site with client-side credential checks, so there's no backend query surface for injection to meaningfully target — it would only duplicate TC-06's coverage.)
- **TC-33, TC-30 (rare edge cases)** — real but low-likelihood scenarios. Worth one automated test each for coverage completeness, but the first candidates to cut if time is tight.
- **TC-37 (PDF download), TC-38 (Reset App State stale label)** — both deterministic and worth automating despite being found late. TC-37 needs Playwright's download-event handling (`page.waitForDownload()`), not a simple click-and-assert. TC-38 is a cosmetic defect, not a blocker, so it stays untagged rather than `known-defect` — the test should also confirm the stale label self-corrects on the next click, not just that it starts out wrong.

### Bottom line
Under time pressure, automate everything in **Critical + High (TC-01–TC-22)** first — that alone covers the core login/checkout journey, the confirmed release-blocking defect, and the performance-regression mechanism, which is most of what a reviewer or a real team would actually care about. **Medium** is next if time allows. **Low** is where I'd stop and instead document the open ambiguities/findings in the README rather than force automation on scenarios with uncertain or low-value payoff — knowing what *not* to automate, and saying so explicitly, is itself part of the test design judgment this assignment is asking for.
