package com.saucedemo.tests;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.saucedemo.config.TestConfig;
import com.saucedemo.pages.actions.CartActions;
import com.saucedemo.pages.actions.CheckoutActions;
import com.saucedemo.pages.actions.InventoryActions;
import com.saucedemo.pages.actions.LoginActions;
import com.saucedemo.pages.locators.CartLocators;
import com.saucedemo.pages.locators.CheckoutLocators;
import com.saucedemo.pages.locators.InventoryLocators;
import com.saucedemo.pages.locators.LoginLocators;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Parallel execution note: Playwright objects are NOT safe to share across
 * threads (see playwright.dev/java/docs/test-runners). Per Playwright's own
 * guidance, this class uses @TestInstance(PER_CLASS) with non-static
 * playwright/browser fields, so each test CLASS gets its own independent
 * instance rather than all subclasses sharing one static field (the bug a
 * static field here would cause: two classes running concurrently on
 * different threads would race on the same memory location). Combined with
 * junit-platform.properties (same_thread within a class, concurrent across
 * classes), this launches one browser per class — same performance as
 * before — while remaining correct under parallel execution.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseTest {
    protected Playwright playwright;
    protected Browser browser;

    protected BrowserContext context;
    protected Page page;
    protected LoginActions loginActions;
    protected LoginLocators loginLocators;
    protected InventoryActions inventoryActions;
    protected InventoryLocators inventoryLocators;
    protected CartLocators cartLocators;
    protected CartActions cartActions;
    protected CheckoutLocators checkoutLocators;
    protected CheckoutActions checkoutActions;

    @RegisterExtension
    protected final AfterTestExecutionCallback screenshotOnFailure = extensionContext -> {
        if (extensionContext.getExecutionException().isEmpty() || page == null) {
            return;
        }

        Path screenshotPath = Path.of(
                "target",
                "screenshots",
                extensionContext.getRequiredTestClass().getSimpleName() + "_"
                        + extensionContext.getRequiredTestMethod().getName() + "_"
                        + System.currentTimeMillis() + ".png");
        try {
            Files.createDirectories(screenshotPath.getParent());
            page.screenshot(new Page.ScreenshotOptions().setPath(screenshotPath).setFullPage(true));
            System.out.printf("Failure screenshot saved to: %s%n", screenshotPath.toAbsolutePath());
        } catch (Exception screenshotFailure) {
            System.err.printf("Could not save failure screenshot: %s%n", screenshotFailure.getMessage());
        }
    };

    @BeforeAll
    void startBrowser() {
        playwright = Playwright.create();
        playwright.selectors().setTestIdAttribute("data-test"); 
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(TestConfig.headless()));
    }

    @AfterAll
    void stopBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @BeforeEach
    void createIsolatedContext() {
        context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1440, 900));
        page = context.newPage();
        page.setDefaultTimeout(TestConfig.timeoutMs());
        page.setDefaultNavigationTimeout(Math.max(20_000, TestConfig.timeoutMs()));

        loginLocators = new LoginLocators(page);
        loginActions = new LoginActions(page, loginLocators);
        loginActions.open(TestConfig.baseUrl());   
        inventoryLocators = new InventoryLocators(page);
        cartLocators = new CartLocators(page);
        checkoutLocators = new CheckoutLocators(page);
        checkoutActions = new CheckoutActions(checkoutLocators);

        inventoryActions = new InventoryActions(inventoryLocators);
        cartActions = new CartActions(cartLocators);
    }

    @AfterEach
    void closeIsolatedContext() {
        if (context != null) {
            context.close();
        }
    }

    protected InventoryActions loginAs(String username) {
        loginActions.login(username, TestConfig.PASSWORD);
        InventoryActions inventory = new InventoryActions(inventoryLocators);
        inventory.waitUntilLoaded();
        return inventory;
    }

    protected CartActions addAndOpenCart(InventoryActions inventory, String... productNames) {
        for (String productName : productNames) {
            inventory.addProduct(productName);
        }
        inventory.openCart();
        return new CartActions(cartLocators);
    }

    protected CheckoutActions beginCheckout(CartActions cart) {
        cart.checkout();
        checkoutActions = new CheckoutActions(checkoutLocators);
        checkoutActions.enterInformation("Ada", "Lovelace", "94105");
        checkoutActions.continueToOverview();
        return checkoutActions;
    }

    protected CheckoutActions beginCheckout(CartActions cart, String first, String last, String postal) {
        cart.checkout();
        checkoutActions = new CheckoutActions(checkoutLocators);
        checkoutActions.enterInformation(first, last, postal);
        checkoutActions.continueToOverview();
        return checkoutActions;
    }
}