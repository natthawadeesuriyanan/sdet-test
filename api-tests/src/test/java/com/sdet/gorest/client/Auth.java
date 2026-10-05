package com.sdet.gorest.client;

/** How a request authenticates. Negative auth scenarios are first-class, not ad-hoc header hacks. */
public enum Auth {
    /** Valid bearer token from configuration (fails loudly if missing). */
    TOKEN,
    /** No Authorization header at all. */
    NONE,
    /** Syntactically valid but unknown bearer token. */
    INVALID
}
