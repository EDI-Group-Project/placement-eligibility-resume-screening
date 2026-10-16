package com.placement.services.staff;

/**
 * One line of a job's applicant table / Excel sheet. Exists only for students who have an application row
 * for that job.
 *
 * @param applicationStatus value of applications.status exactly as stored
 * @param appliedAt         applications.applied_at formatted yyyy-MM-dd HH:mm ("" if missing)
 * @param resumeScore       0-100 score computed from the student's current resume; if they have no readable
 *                          resume, the score previously stored on the application; null if neither exists
 */
public record ApplicantRow(int studentId, String prn, String name, String email, String phone, String branch,
                           double cgpa, int backlogs, String eligibility, String applicationStatus,
                           String appliedAt, Double resumeScore) {}
