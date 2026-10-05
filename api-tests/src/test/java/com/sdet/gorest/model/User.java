package com.sdet.gorest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

/**
 * Request and response model for /users. Fields are Strings (not enums) on purpose so negative
 * tests can send invalid values; use {@link Gender} / {@link Status} for valid ones.
 * Null fields are omitted from JSON, which lets tests express "field missing" precisely.
 * "with" methods return copies, so a payload can be reused safely as the expected value.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {

    private Long id;
    private String name;
    private String email;
    private String gender;
    private String status;

    public User() {
    }

    public User(Long id, String name, String email, String gender, String status) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.gender = gender;
        this.status = status;
    }

    public User copy() {
        return new User(id, name, email, gender, status);
    }

    public User withId(Long v) { User u = copy(); u.id = v; return u; }
    public User withName(String v) { User u = copy(); u.name = v; return u; }
    public User withEmail(String v) { User u = copy(); u.email = v; return u; }
    public User withGender(String v) { User u = copy(); u.gender = v; return u; }
    public User withGender(Gender v) { return withGender(v.value()); }
    public User withStatus(String v) { User u = copy(); u.status = v; return u; }
    public User withStatus(Status v) { return withStatus(v.value()); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User u)) return false;
        return Objects.equals(id, u.id) && Objects.equals(name, u.name) && Objects.equals(email, u.email)
                && Objects.equals(gender, u.gender) && Objects.equals(status, u.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, email, gender, status);
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + name + "', email='" + email + "', gender='" + gender + "', status='" + status + "'}";
    }
}
