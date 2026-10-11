package com.placement.models.resume;

import java.util.ArrayList;
import java.util.List;

/**
 * Explainable resume SCREENING outcome for one (student, job) pair.
 *
 * This is intentionally separate from job ELIGIBILITY
 * ({@code com.placement.database.EligibilityDAO} / {@code StudentService}
 * .getEligibleUnnotifiedStudents), which only checks CGPA/backlogs/branch/
 * passing-year/required-skills rules. A student can be eligible with a weak
 * resume, or screen well on resume content while still being ineligible on
 * hard criteria - the two are computed independently and never merged.
 *
 * Every number here is produced by a fixed, documented rule in
 * {@code ScreeningService} (skill match %, section-presence/keyword counts) -
 * nothing is inferred by a statistical or ML model, so the breakdown below
 * is always fully explainable by re-reading the fields.
 */
public class ScreeningResult {

    private int studentId;
    private String jobId;

    // ---- skill match (max 40) ----
    private List<String> matchedSkills = new ArrayList<>();
    private List<String> missingSkills = new ArrayList<>();
    private double skillScore;

    // ---- resume content signals ----
    private double projectScore;        // max 20
    private double experienceScore;     // max 20
    private double certificationScore;  // max 10
    private double keywordScore;        // max 10

    private double totalScore;          // sum of the five scores above, 0-100

    private List<String> strengths = new ArrayList<>();
    private List<String> improvementAreas = new ArrayList<>();

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public List<String> getMatchedSkills() { return matchedSkills; }
    public void setMatchedSkills(List<String> matchedSkills) {
        this.matchedSkills = matchedSkills == null ? new ArrayList<>() : matchedSkills;
    }

    public List<String> getMissingSkills() { return missingSkills; }
    public void setMissingSkills(List<String> missingSkills) {
        this.missingSkills = missingSkills == null ? new ArrayList<>() : missingSkills;
    }

    public double getSkillScore() { return skillScore; }
    public void setSkillScore(double skillScore) { this.skillScore = skillScore; }

    public double getProjectScore() { return projectScore; }
    public void setProjectScore(double projectScore) { this.projectScore = projectScore; }

    public double getExperienceScore() { return experienceScore; }
    public void setExperienceScore(double experienceScore) { this.experienceScore = experienceScore; }

    public double getCertificationScore() { return certificationScore; }
    public void setCertificationScore(double certificationScore) { this.certificationScore = certificationScore; }

    public double getKeywordScore() { return keywordScore; }
    public void setKeywordScore(double keywordScore) { this.keywordScore = keywordScore; }

    public double getTotalScore() { return totalScore; }
    public void setTotalScore(double totalScore) { this.totalScore = totalScore; }

    public List<String> getStrengths() { return strengths; }
    public void setStrengths(List<String> strengths) {
        this.strengths = strengths == null ? new ArrayList<>() : strengths;
    }

    public List<String> getImprovementAreas() { return improvementAreas; }
    public void setImprovementAreas(List<String> improvementAreas) {
        this.improvementAreas = improvementAreas == null ? new ArrayList<>() : improvementAreas;
    }
}
