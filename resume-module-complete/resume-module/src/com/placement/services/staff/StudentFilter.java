package com.placement.services.staff;

import java.util.Set;

/**
 * Filters for the staff student export. All filters are optional and combined with AND.
 *
 * Eligibility and resume score are defined PER JOB (a student can be eligible for one company and not
 * another, and a resume scores differently against each job's required skills), so those two filters need
 * {@code jobId}; without a job they cannot be answered and the request is rejected with a clear message.
 *
 * @param jobId          job to evaluate eligibility / resume score against (null or blank = none)
 * @param branches       accepted branches, lower-case (empty = any); matched against Student.getDepartment()
 * @param minCgpa        inclusive lower bound (null = none)
 * @param maxCgpa        inclusive upper bound (null = none)
 * @param eligibility    ANY, ELIGIBLE or NOT_ELIGIBLE (needs a job unless ANY)
 * @param minResumeScore inclusive lower bound on the 0-100 screening score (needs a job; students without a
 *                       readable resume are excluded when this is set)
 */
public record StudentFilter(String jobId, Set<String> branches, Double minCgpa, Double maxCgpa,
                            Eligibility eligibility, Double minResumeScore) {

    public enum Eligibility { ANY, ELIGIBLE, NOT_ELIGIBLE }

    public static StudentFilter none() {
        return new StudentFilter(null, Set.of(), null, null, Eligibility.ANY, null);
    }

    public boolean hasJob() {
        return jobId != null && !jobId.isBlank();
    }

    /** @throws IllegalArgumentException with a message that is safe to show to the user */
    public void validate() {
        if (minCgpa != null && (minCgpa < 0 || minCgpa > 10)) throw new IllegalArgumentException("Minimum CGPA must be between 0 and 10.");
        if (maxCgpa != null && (maxCgpa < 0 || maxCgpa > 10)) throw new IllegalArgumentException("Maximum CGPA must be between 0 and 10.");
        if (minCgpa != null && maxCgpa != null && minCgpa > maxCgpa) throw new IllegalArgumentException("Minimum CGPA cannot be greater than maximum CGPA.");
        if (minResumeScore != null && (minResumeScore < 0 || minResumeScore > 100)) throw new IllegalArgumentException("Minimum resume score must be between 0 and 100.");
        boolean needsJob = (eligibility != null && eligibility != Eligibility.ANY) || minResumeScore != null;
        if (needsJob && !hasJob()) {
            throw new IllegalArgumentException("Select a job to filter by eligibility or resume score.");
        }
    }
}
