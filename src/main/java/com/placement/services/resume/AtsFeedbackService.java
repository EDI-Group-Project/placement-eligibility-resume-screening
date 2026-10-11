package com.placement.services.resume;

import com.placement.models.resume.AtsFeedbackResult;
import com.placement.models.resume.ResumeData;

import java.util.Locale;

/**
 * Deterministic, rule-based ATS (Applicant Tracking System) friendliness
 * checks - purely about resume FORMAT/STRUCTURE readability, independent of
 * {@link ScreeningService}'s job-specific content score.
 *
 * Every check here is a fixed heuristic (section headers present, contact
 * info present, resume length in a sane range, bullet usage, a couple of
 * always-applicable formatting tips). No ML/statistical inference.
 */
public class AtsFeedbackService {

    private static final int MIN_RECOMMENDED_WORDS = 150;
    private static final int MAX_RECOMMENDED_WORDS = 1200;
    private static final int PENALTY_PER_WARNING = 12;

    public AtsFeedbackResult evaluate(ResumeData resumeData) {
        AtsFeedbackResult result = new AtsFeedbackResult();

        if (resumeData == null || resumeData.isEmpty()) {
            result.addWarning("No readable text was found in the uploaded resume.");
            result.addSuggestion("Upload a text-based PDF or a .docx file (not a scanned photo/image) so content can be read by ATS software.");
            result.setAtsScore(0);
            return result;
        }

        checkSection(result, resumeData.getSkillsSectionText(), "Skills");
        checkSection(result, resumeData.getProjectsSectionText(), "Projects");
        checkSection(result, resumeData.getExperienceSectionText(), "Experience");
        checkSection(result, resumeData.getCertificationsSectionText(), "Certifications");

        if (resumeData.isEmailDetected()) {
            result.addPositive("A valid-looking email address was found.");
        } else {
            result.addWarning("No email address was detected.");
            result.addSuggestion("Add a professional email address near the top of the resume.");
        }

        if (resumeData.isPhoneDetected()) {
            result.addPositive("A phone number was found.");
        } else {
            result.addWarning("No phone number was detected.");
            result.addSuggestion("Add a reachable phone number near the top of the resume.");
        }

        int wordCount = resumeData.getWordCount();
        if (wordCount < MIN_RECOMMENDED_WORDS) {
            result.addWarning("Resume content looks very short (" + wordCount + " words).");
            result.addSuggestion("Expand on projects, skills, and experience - aim for at least half a page of real content.");
        } else if (wordCount > MAX_RECOMMENDED_WORDS) {
            result.addWarning("Resume content looks long (" + wordCount + " words).");
            result.addSuggestion("Trim to the most relevant 1-2 pages; ATS systems and recruiters favor concise resumes.");
        } else {
            result.addPositive("Resume length looks reasonable (" + wordCount + " words).");
        }

        if (hasBulletCharacters(resumeData.getRawText())) {
            result.addPositive("Uses bullet points, which ATS parsers handle well.");
        } else {
            result.addSuggestion("Use bullet points (e.g. '- ' or '* ') to list achievements and responsibilities for easier ATS and recruiter scanning.");
        }

        // Always-applicable, general ATS best-practice tips (not detectable from extracted text alone).
        result.addSuggestion("Avoid tables, text boxes, images, and multi-column layouts - many ATS parsers fail to read them correctly.");
        result.addSuggestion("Use standard section headings (e.g. \"Skills\", \"Projects\", \"Experience\", \"Certifications\") so parsers can find them.");

        int score = 100 - (result.getWarnings().size() * PENALTY_PER_WARNING);
        result.setAtsScore(Math.max(0, Math.min(100, score)));

        return result;
    }

    private void checkSection(AtsFeedbackResult result, String sectionText, String label) {
        if (sectionText != null && !sectionText.isBlank()) {
            result.addPositive("A \"" + label + "\" section was found.");
        } else {
            result.addWarning("No \"" + label + "\" section was detected.");
            result.addSuggestion("Add a clearly labeled \"" + label + "\" section so ATS software and recruiters can find it.");
        }
    }

    private boolean hasBulletCharacters(String rawText) {
        if (rawText == null) return false;
        for (String line : rawText.split("\n")) {
            String trimmed = line.strip();
            if (trimmed.startsWith("-") || trimmed.startsWith("*") || trimmed.startsWith("•")) {
                return true;
            }
        }
        return rawText.toLowerCase(Locale.ROOT).contains("•");
    }
}
