package com.saucedemo.helper;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public record CheckoutTestData(String firstName, String lastName, String postalCode) {
    public static CheckoutTestData unique() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String postalCode = Integer.toString(ThreadLocalRandom.current().nextInt(10_000, 100_000));
        return new CheckoutTestData("Name" + suffix, "Last" + suffix, postalCode);
    }
}