package com.saucedemo.pages.locators;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public final class LoginLocators {
    private final Page page;

    public LoginLocators(Page page) {
        this.page = page;
    }

    public Locator username() {
        return page.getByTestId("username");
    }

    public Locator password() {
        return page.getByTestId("password");
    }

    public Locator loginButton() {
        return page.getByTestId("login-button");
    }

    public Locator error() {
        return page.getByTestId("error");
    }

    public Locator container() {
        return page.getByTestId("login-container");
    }
}