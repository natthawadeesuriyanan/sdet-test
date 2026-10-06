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
 * threads (see playwright.dev/java/docs/test-runners). This class uses
 * @TestInstance(PER_CLASS) with non-static playwright/browser fields, so
 * each test CLASS gets its own independent instance rather than all
 * subclasses sharing one static field.
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
        browser = launchBrowser(playwright, TestConfig.browser(), TestConfig.headless());
    }

    /**
     * Supports "chromium", "firefox", "webkit" (separate Playwright-bundled
     * engines) and "chrome", "msedge" (vendor browsers, launched via
     * Chromium's channel option rather than a separate engine — this is how
     * Playwright itself expects these two to be requested).
     */
    private static Browser launchBrowser(Playwright playwright, String browserName, boolean headless) {
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions().setHeadless(headless);
        switch (browserName) {
            case "firefox":
                return playwright.firefox().launch(options);
            case "webkit":
                return playwright.webkit().launch(options);
            case "chrome":
                return playwright.chromium().launch(options.setChannel("chrome"));
            case "msedge":
                return playwright.chromium().launch(options.setChannel("msedge"));
            case "chromium":
                return playwright.chromium().launch(options);
            default:
                throw new IllegalArgumentException(
                        "Unsupported browser '" + browserName
                                + "'. Expected one of: chromium, chrome, msedge, firefox, webkit.");
        }
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
