package com.placement.models;

import java.util.List;
import java.util.stream.Collectors;

public class JobPosting {
    private String id;
    private int companyId;
    private String company;
    private String role;
    private String minPackage;
    private double minCgpa;
    private List<String> branches;
    private int maxBacklogs;
    private int passingYear;
    private List<String> requiredSkills;
    private String deadline;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public int getCompanyId() { return companyId; }
    public void setCompanyId(int companyId) { this.companyId = companyId; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getMinPackage() { return minPackage; }
    public void setMinPackage(String minPackage) { this.minPackage = minPackage; }
    public double getMinCgpa() { return minCgpa; }
    public void setMinCgpa(double minCgpa) { this.minCgpa = minCgpa; }
    public List<String> getBranches() { return branches; }
    public void setBranches(List<String> branches) { this.branches = branches; }
    public int getMaxBacklogs() { return maxBacklogs; }
    public void setMaxBacklogs(int maxBacklogs) { this.maxBacklogs = maxBacklogs; }
    public int getPassingYear() { return passingYear; }
    public void setPassingYear(int passingYear) { this.passingYear = passingYear; }
    public List<String> getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(List<String> requiredSkills) { this.requiredSkills = requiredSkills; }
    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }

    public String toProtocolLine() {
        String branchCsv = branches == null ? "" : branches.stream().map(String::trim).collect(Collectors.joining(","));
        String skillCsv = requiredSkills == null ? "" : requiredSkills.stream().map(String::trim).collect(Collectors.joining(","));
        return String.join("|", id, company, role, minPackage, String.valueOf(minCgpa), branchCsv,
                String.valueOf(maxBacklogs), String.valueOf(passingYear), skillCsv, deadline == null ? "" : deadline);
    }
}
