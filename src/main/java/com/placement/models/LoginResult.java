package com.placement.models;

public class LoginResult {
    private final boolean success;
    private final String message;
    private final String displayName;
    private final int accountId;
    private final String accountType;

    public LoginResult(boolean success, String message, String displayName) {
        this(success, message, displayName, 0, null);
    }

    public LoginResult(boolean success, String message, String displayName, int accountId, String accountType) {
        this.success = success;
        this.message = message;
        this.displayName = displayName;
        this.accountId = accountId;
        this.accountType = accountType;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getDisplayName() { return displayName; }
    public int getAccountId() { return accountId; }
    public String getAccountType() { return accountType; }
}
