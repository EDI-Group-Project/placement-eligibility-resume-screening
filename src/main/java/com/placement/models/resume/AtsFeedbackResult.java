package com.placement.models.resume;

import java.util.ArrayList;
import java.util.List;

/**
 * ATS (Applicant Tracking System) friendliness feedback for a resume.
 *
 * Separate from {@link ScreeningResult}: this is about whether the resume's
 * FORMAT/STRUCTURE is parseable by typical ATS software and readable at a
 * glance (section headers present, contact info present, reasonable length,
 * bullet usage) - not about how well its content matches a specific job.
 *
 * Produced by deterministic, rule-based checks in
 * {@code com.placement.services.resume.AtsFeedbackService}. No ML involved.
 */
public class AtsFeedbackResult {

    private int atsScore; // 0-100, 100 = no issues found

    private final List<String> positives = new ArrayList<>();     // things already done right
    private final List<String> warnings = new ArrayList<>();      // issues found
    private final List<String> suggestions = new ArrayList<>();   // concrete fixes

    public int getAtsScore() { return atsScore; }
    public void setAtsScore(int atsScore) { this.atsScore = atsScore; }

    public List<String> getPositives() { return positives; }
    public List<String> getWarnings() { return warnings; }
    public List<String> getSuggestions() { return suggestions; }

    public void addPositive(String text) { positives.add(text); }
    public void addWarning(String text) { warnings.add(text); }
    public void addSuggestion(String text) { suggestions.add(text); }
}
