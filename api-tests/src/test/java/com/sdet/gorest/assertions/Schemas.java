package com.sdet.gorest.assertions;

/** Classpath locations of JSON schemas (draft-04, as supported by REST Assured's validator). */
public final class Schemas {
    public static final String USER = "schemas/user.json";
    public static final String USER_LIST = "schemas/user-list.json";
    public static final String VALIDATION_ERRORS = "schemas/validation-errors.json";
    public static final String ERROR = "schemas/error.json";

    private Schemas() {
    }
}
