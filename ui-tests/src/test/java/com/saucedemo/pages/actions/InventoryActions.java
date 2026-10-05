package com.saucedemo.pages.actions;

import com.saucedemo.pages.locators.InventoryLocators;
import com.saucedemo.helper.Money;

import java.math.BigDecimal;
import java.util.List;

public final class InventoryActions {
    private final InventoryLocators locators;

    public InventoryActions(InventoryLocators locators) {
        this.locators = locators;
    }

    public void waitUntilLoaded() {
        locators.title().waitFor();
        locators.products().first().waitFor();
    }

    public int productCount() {
        return locators.products().count();
    }

    public List<String> productNames() {
        return locators.products().getByTestId("inventory-item-name").allTextContents();
    }

    public List<BigDecimal> productPrices() {
        return locators.products().getByTestId("inventory-item-price")
                .allTextContents().stream().map(Money::parse).toList();
    }

    public BigDecimal priceFor(String productName) {
        return Money.parse(locators.productPrice(productName).innerText());
    }

    public void addProduct(String productName) {
        locators.addButton(productName).click();
    }

    public void removeProduct(String productName) {
        locators.removeButton(productName).click();
    }

    public void sortBy(String value) {
        locators.sortControl().selectOption(value);
    }

    public void openProduct(String productName) {
        locators.productName(productName).click();
    }

    public List<String> imageSources() {
        return locators.productImages().all().stream()
            .map(image -> image.getAttribute("src"))
            .toList();
    }

    public void openCart() {
        locators.cartLink().click();
    }

    public void resetAppState() {
        locators.menuButton().click();
        locators.resetSidebarLink().click();
    }
}