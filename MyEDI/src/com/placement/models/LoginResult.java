package com.placement.models;

public class LoginResult {

    private final boolean success;
    private final String message;
    private final String displayName;

    public LoginResult(boolean success, String message, String displayName) {
        this.success = success;
        this.message = message;
        this.displayName = displayName;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getDisplayName() { return displayName; }
}