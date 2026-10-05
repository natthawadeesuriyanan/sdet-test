package com.sdet.gorest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Generic error body: {"message": "Resource not found"}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiError {
    private String message;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    @Override
    public String toString() { return "ApiError{message='" + message + "'}"; }
}
