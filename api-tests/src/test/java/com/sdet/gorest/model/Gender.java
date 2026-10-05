package com.sdet.gorest.model;

public enum Gender {
    MALE("male"), FEMALE("female");

    private final String value;

    Gender(String value) { this.value = value; }

    public String value() { return value; }

    public static Gender of(String value) {
        for (Gender g : values()) if (g.value.equals(value)) return g;
        throw new IllegalArgumentException("Unknown gender " + value);
    }

    public Gender opposite() { return this == MALE ? FEMALE : MALE; }
}
