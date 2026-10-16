package com.placement.sockets;

import com.placement.database.ApplicationDAO;
import com.placement.models.JobPosting;
import com.placement.services.JobService;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.sql.SQLException;

/**
 * APPLY_JOB - a STUDENT applies for a job:  token, jobId  ->  SUCCESS|APPLIED  or  SUCCESS|ALREADY_APPLIED.
 *
 * This is what creates the rows that "Download applicants for a job" lists. The student is always the one in
 * the session (never an id sent by the client), staff cannot apply on a student's behalf, and applying twice
 * is harmless. If your project already has an apply flow that inserts into the applications table, you do not
 * need this class - delete it and its one line in ClientHandler.
 *
 * Eligibility is deliberately NOT enforced here; the applicant list shows each applicant's eligibility instead.
 */
public class ApplicationCommandHandler {

    private final JobService jobService;
    private final ApplicationDAO applicationDAO;

    public ApplicationCommandHandler() {
        this(new JobService(), new ApplicationDAO());
    }

    public ApplicationCommandHandler(JobService jobService, ApplicationDAO applicationDAO) {
        this.jobService = jobService;
        this.applicationDAO = applicationDAO;
    }

    public boolean handles(String command) {
        return "APPLY_JOB".equals(command);
    }

    public void handle(String command, BufferedReader reader, PrintWriter writer) {
        try {
            String token = ProtocolIO.readBoundedLine(reader, ProtocolIO.MAX_SHORT_LINE);
            if (!SessionManager.isValid(token)) {
                writer.println("FAILED|Not authenticated. Please log in again.");
                return;
            }
            if (!RolePolicy.isStudent(token)) {
                writer.println("FAILED|Only students can apply for a job.");
                return;
            }
            String jobId = ProtocolIO.readOptional(reader);
            if (jobId.isEmpty()) {
                writer.println("FAILED|Job ID is required.");
                return;
            }
            JobPosting job = jobService.getJob(jobId);
            if (job == null) {
                writer.println("FAILED|Job not found: " + jobId);
                return;
            }
            boolean created = applicationDAO.apply(job.getId() != null ? job.getId() : jobId,
                    SessionManager.getAccountId(token));
            writer.println(created ? "SUCCESS|APPLIED" : "SUCCESS|ALREADY_APPLIED");
        } catch (ProtocolIO.LineTooLongException e) {
            writer.println("FAILED|The request is too large.");
        } catch (SQLException e) {
            System.out.println("Database error in APPLY_JOB: " + e.getMessage());
            writer.println("FAILED|Could not complete the request. Please try again.");
        } catch (Exception e) {
            System.out.println("Unexpected error in APPLY_JOB: " + e);
            writer.println("FAILED|Could not complete the request. Please try again.");
        }
    }
}
