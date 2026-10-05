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
import com.saucedemo.helper.CheckoutTestData;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.nio.file.Files;
import java.nio.file.Path;

public abstract class BaseTest {
    protected static Playwright playwright;
    protected static Browser browser;

    protected BrowserContext context;
    protected Page page;
    protected LoginLocators loginLocators;
    protected LoginActions loginActions;
    protected InventoryLocators inventoryLocators;
    protected InventoryActions inventoryActions;
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
    static void startBrowser() {
        playwright = Playwright.create();
        playwright.selectors().setTestIdAttribute("data-test"); 
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(TestConfig.headless()));
    }

    @AfterAll
    static void stopBrowser() {
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
        inventoryLocators = new InventoryLocators(page);
        inventoryActions = new InventoryActions(inventoryLocators);
        cartLocators = new CartLocators(page);
        cartActions = new CartActions(cartLocators);
        checkoutLocators = new CheckoutLocators(page);
        checkoutActions = new CheckoutActions(checkoutLocators);
        loginActions.open(TestConfig.baseUrl());
    }

    @AfterEach
    void closeIsolatedContext() {
        if (context != null) {
            context.close();
        }
    }

    protected InventoryActions loginAs(String username) {
        loginActions.login(username, TestConfig.PASSWORD);
        inventoryActions.waitUntilLoaded();
        return inventoryActions;
    }

    protected CartActions addAndOpenCart(InventoryActions inventory, String... productNames) {
        for (String productName : productNames) {
            inventory.addProduct(productName);
        }
        inventory.openCart();
        return cartActions;
    }

    protected CheckoutActions beginCheckout(CartActions cart, String first, String last, String postal) {
        cart.checkout();
        checkoutActions.enterInformation(first, last, postal);
        checkoutActions.continueToOverview();
        return checkoutActions;
    }

    protected CheckoutActions beginCheckout(CartActions cart) {
        CheckoutTestData data = CheckoutTestData.unique();
        return beginCheckout(cart, data.firstName(), data.lastName(), data.postalCode());
    }
}