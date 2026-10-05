package com.saucedemo.pages.actions;

import com.saucedemo.pages.locators.CheckoutLocators;
import com.saucedemo.helper.Money;

import java.math.BigDecimal;

public final class CheckoutActions {
    private final CheckoutLocators locators;

    public CheckoutActions(CheckoutLocators locators) {
        this.locators = locators;
    }

    public void enterInformation(String firstName, String lastName, String postalCode) {
        locators.firstName().fill(firstName);
        locators.lastName().pressSequentially(lastName);
        locators.postalCode().fill(postalCode);
    }

    public void continueToOverview() {
        locators.continueButton().click();
    }

    public BigDecimal totalBeforeTax() {
        return Money.parse(locators.subtotal().innerText());
    }

    public BigDecimal tax() {
        return Money.parse(locators.tax().innerText());
    }

    public BigDecimal total() {
        return Money.parse(locators.total().innerText());
    }

    public void finish() {
        locators.finishButton().click();
    }
}