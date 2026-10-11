package com.placement.models.resume;

import com.placement.models.Student;

/**
 * Pairs an existing {@link Student} with its {@link ScreeningResult} for one
 * job, so a list of these can be ranked by {@code CandidateMergeSort}.
 *
 * Does not duplicate any Student field - it only references the existing
 * model and adds the screening outcome alongside it.
 */
public class RankedCandidate {

    private final Student student;
    private final ScreeningResult screening;

    public RankedCandidate(Student student, ScreeningResult screening) {
        this.student = student;
        this.screening = screening;
    }

    public Student getStudent() { return student; }
    public ScreeningResult getScreening() { return screening; }

    /** Convenience accessor used by CandidateMergeSort's comparison rule. */
    public double getTotalScore() {
        return screening == null ? 0.0 : screening.getTotalScore();
    }

    /** Convenience accessor used by CandidateMergeSort's tie-break rule. */
    public String getName() {
        return student == null || student.getName() == null ? "" : student.getName();
    }
}
