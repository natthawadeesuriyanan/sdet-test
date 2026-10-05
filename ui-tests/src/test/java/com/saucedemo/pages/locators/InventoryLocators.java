package com.saucedemo.pages.locators;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import java.util.regex.Pattern;

public final class InventoryLocators {
    private final Page page;
    private final Locator products;

    public InventoryLocators(Page page) {
        this.page = page;
        products = page.getByTestId("inventory-item");
    }

    public Locator title() {
        return page.getByText("Products", new Page.GetByTextOptions().setExact(true));
    }

    public Locator products() {
        return products;
    }

    public Locator inventoryList() {
        return page.getByTestId("inventory-list");
    }

    public Locator product(String name) {
        return products.filter(new Locator.FilterOptions().setHasText(name));
    }

    public Locator productName(String name) {
        return product(name).getByTestId("inventory-item-name");
    }

    public Locator productPrice(String name) {
        return product(name).getByTestId("inventory-item-price");
    }

    public Locator productButton(String name) {
        return product(name).getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName(Pattern.compile("^(Add to cart|Remove)$")));
    }

    public Locator addButton(String name) {
        return product(name).getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Add to cart"));
    }

    public Locator removeButton(String name) {
        return product(name).getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Remove"));
    }

    public Locator sortControl() {
        return page.getByRole(AriaRole.COMBOBOX,
                new Page.GetByRoleOptions().setName("Sort products"));
    }

    public Locator productImages() {
        return products.locator("img");
    }

    public Locator cartBadge() {
        return page.getByTestId("shopping-cart-badge");
    }

    public Locator cartLink() {
        return page.getByTestId("shopping-cart-link");
    }

    public Locator menuButton() {
        return page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Open Menu"));
    }

    public Locator resetSidebarLink() {
        return page.getByTestId("reset-sidebar-link");
    }
}