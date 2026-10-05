package com.saucedemo.helper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Money {
    private static final Pattern AMOUNT = Pattern.compile("\\$([\\d,]+\\.\\d{2})");

    private Money() {
    }

    public static BigDecimal parse(String label) {
        Matcher matcher = AMOUNT.matcher(label);
        if (!matcher.find()) {
            throw new IllegalArgumentException("No USD amount found in: " + label);
        }
        return new BigDecimal(matcher.group(1).replace(",", ""));
    }

    public static BigDecimal tax(BigDecimal subtotal, BigDecimal rate) {
        return subtotal.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
}