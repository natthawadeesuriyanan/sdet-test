package com.sdet.gorest.config;

/** Thrown when a test needs to authenticate but no token was supplied. Fails the run - never skips. */
public class MissingTokenException extends IllegalStateException {

    public MissingTokenException() {
        super("""
                The GoREST API tests require a personal access token to be supplied via the environment variable GOREST_TOKEN.
                """);
    }
}
