package com.cpclub.backend.dto;

import jakarta.validation.constraints.Size;

public class UserProfileUpdateRequest {

    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    private String codeforcesHandle;

    public UserProfileUpdateRequest() {
    }

    public UserProfileUpdateRequest(String name, String codeforcesHandle) {
        this.name = name;
        this.codeforcesHandle = codeforcesHandle;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCodeforcesHandle() {
        return codeforcesHandle;
    }

    public void setCodeforcesHandle(String codeforcesHandle) {
        this.codeforcesHandle = codeforcesHandle;
    }
}
