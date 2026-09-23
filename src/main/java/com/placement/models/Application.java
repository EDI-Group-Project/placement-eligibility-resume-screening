package com.placement.models;
public class Application {
    private long id; private String jobId,company,role,status,appliedAt; private double screeningScore;
    public long getId(){return id;} public void setId(long v){id=v;}
    public String getJobId(){return jobId;} public void setJobId(String v){jobId=v;}
    public String getCompany(){return company;} public void setCompany(String v){company=v;}
    public String getRole(){return role;} public void setRole(String v){role=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public double getScreeningScore(){return screeningScore;} public void setScreeningScore(double v){screeningScore=v;}
    public String getAppliedAt(){return appliedAt;} public void setAppliedAt(String v){appliedAt=v;}
    public String toProtocolLine(){return String.join("|",safe(jobId),safe(company),safe(role),safe(status),String.valueOf(screeningScore),safe(appliedAt));}
    private static String safe(String s){return s==null?"":s.replace("|","/");}
}
