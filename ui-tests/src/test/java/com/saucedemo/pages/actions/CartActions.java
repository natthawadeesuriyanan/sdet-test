package com.saucedemo.pages.actions;

import com.saucedemo.pages.locators.CartLocators;
import com.saucedemo.helper.Money;

import java.math.BigDecimal;
import java.util.List;

public final class CartActions {
    private final CartLocators locators;

    public CartActions(CartLocators locators) {
        this.locators = locators;
    }

    public List<String> itemNames() {
        return locators.itemNames().allTextContents();
    }

    public List<BigDecimal> itemPrices() {
        return locators.itemPrices().allTextContents().stream().map(Money::parse).toList();
    }

    public void remove(String productName) {
        locators.removeButton(productName).click();
    }

    public void checkout() {
        locators.checkoutButton().click();
    }
}