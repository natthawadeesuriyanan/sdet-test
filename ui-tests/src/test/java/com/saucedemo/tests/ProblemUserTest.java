package com.saucedemo.tests;

import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.saucedemo.pages.actions.CartActions;
import com.saucedemo.pages.actions.CheckoutActions;
import com.saucedemo.pages.actions.InventoryActions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProblemUserTest extends BaseTest {
    private static final String PROBLEM_USER = "problem_user";
    private static final Map<String, String> OBSERVED_ADD_BUTTONS = Map.of(
            "Sauce Labs Backpack", "Remove",
            "Sauce Labs Bike Light", "Remove",
            "Sauce Labs Bolt T-Shirt", "Add to cart",
            "Sauce Labs Fleece Jacket", "Add to cart",
            "Sauce Labs Onesie", "Remove",
            "Test.allTheThings() T-Shirt (Red)", "Add to cart");
    private InventoryActions inventory;
    
    @BeforeEach
    void loginAsProblemUser() {
        inventory = loginAs(PROBLEM_USER);
    }

    @Test
    @Tag("known-defect")
    void TC04_lastNameInputOverwritesFirstNameAndPreventsSubmission() {
        CartActions cart = addAndOpenCart(inventory, "Sauce Labs Backpack");
        cart.checkout();
        CheckoutActions checkout = checkoutActions;
        checkout.enterInformation("Ida", "Lover", "A1A 1A1");
        // Confirmed defect (highest severity in this suite): typing in the Last
        // Name field on the checkout form shifts the text into the First Name
        // field instead, leaving Last Name empty. If fixed, this
        // assertion will pass and should be uncomment out the following two lines to assert normal field behavior:
        // assertEquals("Ida", checkoutLocators.firstName().inputValue());
        // assertEquals("Lover", checkoutLocators.lastName().inputValue());
        checkout.continueToOverview();
        PlaywrightAssertions.assertThat(checkoutLocators.error()).containsText("Error: Last Name is required");
    }

    @Test
    @Tag("known-defect")
    void TC12_addToCartFailuresArePinnedToObservedProducts() {
        for (Map.Entry<String, String> expected : OBSERVED_ADD_BUTTONS.entrySet()) {
            inventory.addProduct(expected.getKey());
            PlaywrightAssertions.assertThat(inventoryLocators.productButton(expected.getKey())).hasText(expected.getValue());
        }
        // Confirmed defect: only 3 of the 6 "Add to cart" buttons actually add
        // their product to the cart. The other 3 buttons are broken and do not add their product.
        // TODO: once this defect is fixed, uncomment the line below to verify
        //PlaywrightAssertions.assertThat(inventoryLocators.cartBadge()).hasText("6");

    }

    @Test
    @Tag("known-defect")
    void TC13a_problemUserRemoveButtonOnInventoryPageDoesNotWork() {
        // Confirmed defect (verified manually on the live site): clicking "Remove"
        // on the Inventory page does NOT remove the item and does NOT update the
        // cart badge. The button stays stuck showing "Remove" instead of toggling
        inventory.addProduct("Sauce Labs Bike Light");
        inventory.addProduct("Sauce Labs Backpack");
        inventory.removeProduct("Sauce Labs Backpack");

        PlaywrightAssertions.assertThat(inventoryLocators.productButton("Sauce Labs Backpack")).hasText("Remove");
        // If the defect is fixed, change the expected text to 1 instead of 2.
        PlaywrightAssertions.assertThat(inventoryLocators.cartBadge()).hasText("2");
    }

    @Test
    void TC13b_problemUserRemoveCartItemsOnCartPage() {
        // Control case for TC13a: confirms the Cart-page Remove button works
        CartActions cart = addAndOpenCart(inventory, "Sauce Labs Backpack", "Sauce Labs Bike Light");
        cart.remove("Sauce Labs Backpack");

        assertEquals(List.of("Sauce Labs Bike Light"), cart.itemNames());
        PlaywrightAssertions.assertThat(cartLocators.cartBadge()).hasText("1");
    }

    @Test
    @Tag("known-defect")
    void TC23_problemUserProductImagesShareTheSameBrokenSource() {
    int expectedProductCount = inventory.productCount(); 
    List<String> sources = inventory.imageSources();
    Set<String> uniqueSources = sources.stream().collect(Collectors.toSet());

    assertEquals(expectedProductCount, sources.size());
    // Confirmed defect: all product images point to the same broken/mismatched
    // source instead of each product having its own image. 
    // If the defect is fixed, change the expected size to be equal to the product count.
    assertEquals(1, uniqueSources.size(), "Expected all product images to share the same broken source");
    }

    @Test
    @Tag("known-defect")
    void TC24_problemUserSortSelectionDoesNotChangeProductOrder() {
        // Confirmed defect: all 4 sort options are selectable (the dropdown itself
        // works), but the product order never actually changes for any of them.
        // The precise failure mode is "selection succeeds, re-render doesn't" —
        // asserting before == after (rather than "order is wrong") captures exactly
        // that, so a fix to any one option would be caught immediately.
        List<String> originalOrder = inventory.productNames();

        for (String option : List.of("az", "za", "lohi", "hilo")) {
            inventory.sortBy(option);
            assertEquals(originalOrder, inventory.productNames(), "Order changed after selecting sort option " + option);
        }
    }
}