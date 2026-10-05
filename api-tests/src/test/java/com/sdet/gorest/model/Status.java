package com.sdet.gorest.model;

public enum Status {
    ACTIVE("active"), INACTIVE("inactive");

    private final String value;

    Status(String value) { this.value = value; }

    public String value() { return value; }

    public static Status of(String value) {
        for (Status s : values()) if (s.value.equals(value)) return s;
        throw new IllegalArgumentException("Unknown status " + value);
    }

    public Status opposite() { return this == ACTIVE ? INACTIVE : ACTIVE; }
}
