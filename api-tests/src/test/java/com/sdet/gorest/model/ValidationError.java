package com.sdet.gorest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** One entry of a 422 response: {"field": "email", "message": "is invalid"}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ValidationError {
    private String field;
    private String message;

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    @Override
    public String toString() { return field + ": " + message; }
}
