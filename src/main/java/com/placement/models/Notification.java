package com.placement.models;

public class Notification {
    private long id;
    private String subject;
    private String message;
    private String recipientGroup;
    private String jobId;
    private int recipientCount;
    private String createdAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getRecipientGroup() { return recipientGroup; }
    public void setRecipientGroup(String recipientGroup) { this.recipientGroup = recipientGroup; }
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public int getRecipientCount() { return recipientCount; }
    public void setRecipientCount(int recipientCount) { this.recipientCount = recipientCount; }
    public String getCreatedAt() { return createdAt; }

    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String toProtocolLine() {
        return String.join("|",
                String.valueOf(id),
                jobId == null ? "" : jobId,
                subject == null ? "" : subject,
                message == null ? "" : message,
                recipientGroup == null ? "" : recipientGroup,
                String.valueOf(recipientCount),
                createdAt == null ? "" : createdAt);
    }
}
