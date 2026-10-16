package com.placement.services.staff;

import com.placement.models.JobPosting;
import com.placement.models.Student;
import com.placement.services.StudentService;

import java.util.HashSet;
import java.util.Set;

/**
 * Answers one question - "which students are eligible for this job?" - by asking the project's EXISTING
 * eligibility logic. This module never re-implements the CGPA / backlog / branch / passing-year rules, so
 * there is a single source of truth for them.
 *
 * DEFAULT implementation ({@link #fromStudentService}): the same list the TPC "View Eligible Students"
 * screen shows, i.e. {@code StudentService.getEligibleUnnotifiedStudents(job)} = students who match the job's
 * criteria AND have not been notified about it yet. Consequence: a student who is eligible but was already
 * notified is reported as "Not eligible / already notified" (see {@link #NOT_ELIGIBLE_LABEL}).
 *
 * EXACT answer (recommended once EligibilityDAO is extended): add a query that returns eligible students
 * regardless of notification state, then plug it in with ONE line where services are created:
 *
 *   EligibilityProvider exact = job -> ids(eligibilityDAO.findAllEligibleIncludingNotified(job));
 *
 * and pass it to the StudentExportService / ApplicantService / BatchResumeService constructors.
 */
@FunctionalInterface
public interface EligibilityProvider {

    String ELIGIBLE_LABEL = "Eligible";
    String NOT_ELIGIBLE_LABEL = "Not eligible / already notified";

    Set<Integer> eligibleStudentIds(JobPosting job) throws Exception;

    static EligibilityProvider fromStudentService(StudentService studentService) {
        return job -> {
            Set<Integer> ids = new HashSet<>();
            for (Student s : studentService.getEligibleUnnotifiedStudents(job)) {
                ids.add(s.getStudentId());
            }
            return ids;
        };
    }

    static String label(boolean eligible) {
        return eligible ? ELIGIBLE_LABEL : NOT_ELIGIBLE_LABEL;
    }
}
