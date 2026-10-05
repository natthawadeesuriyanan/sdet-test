package com.saucedemo.pages.actions;

import com.microsoft.playwright.Page;
import com.saucedemo.pages.locators.LoginLocators;

public final class LoginActions {
    private final Page page;
    private final LoginLocators locators;

    public LoginActions(Page page, LoginLocators locators) {
        this.page = page;
        this.locators = locators;
    }

    public void open(String baseUrl) {
        page.navigate(baseUrl);
    }

    public void enterUsername(String value) {
        locators.username().fill(value);
    }

    public void enterPassword(String value) {
        locators.password().fill(value);
    }

    public void clickLogin() {
        locators.loginButton().click();
    }

    public void login(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        clickLogin();
    }
}