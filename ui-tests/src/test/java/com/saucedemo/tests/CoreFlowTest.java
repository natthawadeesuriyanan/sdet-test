package com.saucedemo.tests;

import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.saucedemo.config.TestConfig;
import com.saucedemo.pages.actions.CartActions;
import com.saucedemo.pages.actions.CheckoutActions;
import com.saucedemo.pages.actions.InventoryActions;
import com.saucedemo.helper.Money;
import com.saucedemo.helper.TestMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.saucedemo.helper.ProductNames.SAUCE_LABS_BACKPACK;
import static com.saucedemo.helper.ProductNames.SAUCE_LABS_BIKE_LIGHT;

class CoreFlowTest extends BaseTest {
    private static final String STANDARD_USER = "standard_user";
    private static final String PERFORMANCE_GLITCH_USER = "performance_glitch_user";

    @Test
    void TC01_standardUserLogsInAndSeeSixProducts() {
        InventoryActions inventory = loginAs(STANDARD_USER);

        PlaywrightAssertions.assertThat(page).hasURL(Pattern.compile(".*/inventory\\.html"));
        assertEquals(6, inventory.productCount());
        PlaywrightAssertions.assertThat(inventoryLocators.inventoryList()).isVisible();
    }

    @Test
    void TC02_standardUserCompletesCheckout() {
        InventoryActions inventory = loginAs(STANDARD_USER);
        CartActions cart = addAndOpenCart(inventory, SAUCE_LABS_BACKPACK);
        CheckoutActions checkout = beginCheckout(cart);
        checkout.finish();

        PlaywrightAssertions.assertThat(checkoutLocators.confirmation()).isVisible();
        PlaywrightAssertions.assertThat(checkoutLocators.confirmation()).hasText("Thank you for your order!");
        PlaywrightAssertions.assertThat(checkoutLocators.checkoutTile()).hasText("Checkout: Complete!");
    }

    @Test
    void TC03_lockedOutUserIsRejectedWithSpecificMessage() {
        loginActions.login("locked_out_user", TestConfig.PASSWORD);

        PlaywrightAssertions.assertThat(loginLocators.error())
                .containsText("Epic sadface: Sorry, this user has been locked out.");
        PlaywrightAssertions.assertThat(loginLocators.container()).isVisible();
    }

    @ParameterizedTest(name = "TC05/TC06 invalid username/password={0}")
    @CsvSource({"standard_user,wrong_password", "not_a_real_user,secret_sauce"})
    void TC05_TC06_invalidCredentialsUseSameGenericError(String username, String password) {
        loginActions.login(username, password);

        PlaywrightAssertions.assertThat(loginLocators.error())
                .containsText("Epic sadface: Username and password do not match any user in this service");
    }

    @Test
    void TC07_emptyCredentialsRequireUsername() {
        loginActions.enterPassword(TestConfig.PASSWORD);
        loginActions.clickLogin();
        PlaywrightAssertions.assertThat(loginLocators.error()).containsText("Epic sadface: Username is required");
    }

    @Test
    void TC08_validUsernameWithoutPassword() {
        loginActions.enterUsername(STANDARD_USER);
        loginActions.clickLogin();
        PlaywrightAssertions.assertThat(loginLocators.error()).containsText("Epic sadface: Password is required");
    }

    @Test
    void TC09_TC25_performanceGlitchLoginCompletesWithinConfiguredLimit() {
        long start = System.nanoTime();
        InventoryActions inventory = loginAs(PERFORMANCE_GLITCH_USER);
        long elapsed = TestMetrics.elapsedMillis(start);
        long limit = TestConfig.performanceGlitchLoginMaxMs();
        TestMetrics.log("performance_glitch_user login", elapsed, limit);

        assertEquals(6, inventory.productCount());
        if (TestConfig.performanceAssertionsEnabled()) {
            assertTrue(elapsed <= limit, () -> "Login took " + elapsed + " ms; configured limit is " + limit + " ms");
        }
    }

    @Test
    void TC10_standardUserLoginMeetsPerformanceBaseline() {
        long start = System.nanoTime();
        InventoryActions inventory = loginAs(STANDARD_USER);
        long elapsed = TestMetrics.elapsedMillis(start);
        long limit = TestConfig.standardLoginMaxMs();
        TestMetrics.log("standard_user login", elapsed, limit);

        assertEquals(6, inventory.productCount());
        if (TestConfig.performanceAssertionsEnabled()) {
            assertTrue(elapsed <= limit, () -> "Login took " + elapsed + " ms; configured limit is " + limit + " ms");
        }
    }

    @Test
    void TC11_cartBadgeIncrementsForEachProductAdded() {
        InventoryActions inventory = loginAs(STANDARD_USER);
        List<String> productNames = inventory.productNames();

        for (int index = 0; index < productNames.size(); index++) {
            inventory.addProduct(productNames.get(index));
            PlaywrightAssertions.assertThat(inventoryLocators.cartBadge()).hasText(Integer.toString(index + 1));
        }
    }

    @ParameterizedTest(name = "TC14 checkout with first name={0}, last name={1}, postal code={2}")
    @CsvSource({
        "'','','', Error: First Name is required",
        "'', Lulu, 30120, Error: First Name is required",
        "Ada, '', 30120, Error: Last Name is required",
        "Ada, Lulu, '', Error: Postal Code is required"
    })
    void TC14_emptyFirstNameIsRejectedAtCheckout(String firstName, String lastName, String postalCode, String expectedError) {    
        InventoryActions inventory = loginAs(STANDARD_USER);
        CartActions cart = addAndOpenCart(inventory, SAUCE_LABS_BACKPACK);
        cart.checkout();
        CheckoutActions checkout = checkoutActions;
        checkout.enterInformation(firstName, lastName, postalCode);
        checkout.continueToOverview();
        PlaywrightAssertions.assertThat(checkoutLocators.error()).containsText(expectedError);
    }

    @ParameterizedTest(name = "TC sorting option {0}")
    @CsvSource({"az,NAME_ASC", "za,NAME_DESC", "lohi,PRICE_ASC", "hilo,PRICE_DESC"})
    void TC15_TC18_standardUserSortsProductsCorrectly(String option, String sortType) {
        InventoryActions inventory = loginAs(STANDARD_USER);

        if (sortType.startsWith("NAME")) {
            List<String> expected = new ArrayList<>(inventory.productNames());
            expected.sort(String.CASE_INSENSITIVE_ORDER);
            if (sortType.equals("NAME_DESC")) {
                Collections.reverse(expected);
            }
            inventory.sortBy(option);
            assertEquals(expected, inventory.productNames());
        } else {
            List<BigDecimal> expected = new ArrayList<>(inventory.productPrices());
            Collections.sort(expected);
            if (sortType.equals("PRICE_DESC")) {
                Collections.reverse(expected);
            }
            inventory.sortBy(option);
            assertEquals(expected, inventory.productPrices());
        }
    }

    @Test
    void TC19_singleItemSubtotalMatchesListedProductPrice() {
        InventoryActions inventory = loginAs(STANDARD_USER);
        BigDecimal listedPrice = inventory.priceFor(SAUCE_LABS_BIKE_LIGHT);
        CartActions cart = addAndOpenCart(inventory, SAUCE_LABS_BIKE_LIGHT);
        CheckoutActions checkout = beginCheckout(cart);

        assertEquals(listedPrice, checkout.totalBeforeTax());
    }

    @Test
    void TC20_TC21_TC22_verifyTotalAmount() {
        InventoryActions inventory = loginAs(STANDARD_USER);
        List<String> selected = inventory.productNames().subList(0, 3);
        BigDecimal expectedSubtotal = selected.stream().map(inventory::priceFor).reduce(BigDecimal.ZERO, BigDecimal::add);
        CartActions cart = addAndOpenCart(inventory, selected.toArray(String[]::new));
        CheckoutActions checkout = beginCheckout(cart);

        BigDecimal expectedTax = Money.tax(expectedSubtotal, TestConfig.TAX_RATE);
        assertEquals(expectedSubtotal, checkout.totalBeforeTax());
        assertEquals(expectedTax, checkout.tax());
        assertEquals(expectedSubtotal.add(expectedTax), checkout.total());
    }

    @Test
    void TC26_performanceGlitchUserCompletesCheckoutWithinConfiguredLimit() {
        long start = System.nanoTime();
        InventoryActions inventory = loginAs(PERFORMANCE_GLITCH_USER);
        CartActions cart = addAndOpenCart(inventory, SAUCE_LABS_BACKPACK);
        CheckoutActions checkout = beginCheckout(cart);
        checkout.finish();
        PlaywrightAssertions.assertThat(checkoutLocators.confirmation()).isVisible();
        PlaywrightAssertions.assertThat(checkoutLocators.confirmation()).hasText("Thank you for your order!");
        long elapsed = TestMetrics.elapsedMillis(start);
        long limit = TestConfig.performanceGlitchCheckoutMaxMs();
        TestMetrics.log("performance_glitch_user full checkout", elapsed, limit);

        if (TestConfig.performanceAssertionsEnabled()) {
            assertTrue(elapsed <= limit, () -> "Checkout took " + elapsed + " ms; configured limit is " + limit + " ms");
        }
    }
}