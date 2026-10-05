package com.saucedemo.pages.locators;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public final class CartLocators {
    private final Page page;
    private final Locator items;

    public CartLocators(Page page) {
        this.page = page;
        items = page.getByTestId("inventory-item");
    }

    public Locator items() {
        return items;
    }

    public Locator item(String productName) {
        return items.filter(new Locator.FilterOptions().setHasText(productName));
    }

    public Locator itemNames() {
        return items.getByTestId("inventory-item-name");
    }

    public Locator itemPrices() {
        return items.getByTestId("inventory-item-price");
    }

    public Locator removeButton(String productName) {
        return item(productName).getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Remove"));
    }

    public Locator checkoutButton() {
        return page.getByTestId("checkout");
    }

    public Locator cartBadge() {
        return page.getByTestId("shopping-cart-badge");
    }
}