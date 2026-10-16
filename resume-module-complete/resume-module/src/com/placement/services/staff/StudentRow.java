package com.placement.services.staff;

/**
 * One line of the filtered student table / Excel sheet.
 *
 * @param eligibility "" when no job was selected, otherwise {@link EligibilityProvider#ELIGIBLE_LABEL} or
 *                    {@link EligibilityProvider#NOT_ELIGIBLE_LABEL}
 * @param resumeScore 0-100 screening score for the selected job; null when no job was selected or the student
 *                    has no readable resume
 * @param hasResume   true when a resume is on file (even if no text could be read from it)
 */
public record StudentRow(int studentId, String prn, String name, String email, String branch,
                         double cgpa, int backlogs, String eligibility, Double resumeScore, boolean hasResume) {}
