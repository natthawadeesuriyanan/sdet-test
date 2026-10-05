package com.saucedemo.pages.locators;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public final class CheckoutLocators {
    private final Page page;

    public CheckoutLocators(Page page) {
        this.page = page;
    }

    public Locator firstName() {
        return page.getByTestId("firstName");
    }

    public Locator lastName() {
        return page.getByTestId("lastName");
    }

    public Locator postalCode() {
        return page.getByTestId("postalCode");
    }

    public Locator error() {
        return page.getByTestId("error");
    }

    public Locator continueButton() {
        return page.getByTestId("continue");
    }

    public Locator subtotal() {
        return page.getByTestId("subtotal-label");
    }

    public Locator tax() {
        return page.getByTestId("tax-label");
    }

    public Locator total() {
        return page.getByTestId("total-label");
    }

    public Locator finishButton() {
        return page.getByTestId("finish");
    }

    public Locator checkoutTile() {
        return page.getByTestId("title");
    }

    public Locator confirmation() {
        return page.getByTestId("complete-header");
    }

    public Locator downloadReceiptButton() {
        return page.getByTestId("generate-pdf-order");
    }
}