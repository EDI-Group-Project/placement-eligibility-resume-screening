package com.placement.models;

public class Company {
    private int companyId;
    private String companyName;
    private String hrName;
    private String hrEmail;
    private String hrPassword;
    private boolean active;

    public int getCompanyId() { return companyId; }
    public void setCompanyId(int companyId) { this.companyId = companyId; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getHrName() { return hrName; }
    public void setHrName(String hrName) { this.hrName = hrName; }
    public String getHrEmail() { return hrEmail; }
    public void setHrEmail(String hrEmail) { this.hrEmail = hrEmail; }
    public String getHrPassword() { return hrPassword; }
    public void setHrPassword(String hrPassword) { this.hrPassword = hrPassword; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
