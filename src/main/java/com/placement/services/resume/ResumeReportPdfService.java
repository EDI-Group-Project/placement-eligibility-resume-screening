package com.placement.services.resume;

import com.placement.models.JobPosting;
import com.placement.models.Student;
import com.placement.models.resume.AtsFeedbackResult;
import com.placement.models.resume.ResumeData;
import com.placement.models.resume.ScreeningResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static com.placement.services.resume.MinimalPdfWriter.Line;

/**
 * Builds the candidate screening/feedback PDF report:
 *   candidate details -> extracted resume info -> eligibility result/reasons
 *   -> matched/missing skills -> skill score -> screening score breakdown
 *   -> strengths -> missing areas -> improvement suggestions -> ATS feedback.
 *
 * Rendering itself is delegated to {@link MinimalPdfWriter} (zero-dependency,
 * hand-written PDF object structure - see its javadoc). This class is only
 * responsible for laying the report content out as a list of lines.
 */
public class ResumeReportPdfService {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    public byte[] generateReport(Student student, JobPosting job, ResumeData resumeData,
                                  ScreeningResult screening, AtsFeedbackResult ats,
                                  boolean eligible, List<String> eligibilityReasons) throws IOException {
        List<Line> lines = buildLines(student, job, resumeData, screening, ats, eligible, eligibilityReasons);
        return MinimalPdfWriter.write(lines);
    }

    public void generateReportToFile(Student student, JobPosting job, ResumeData resumeData,
                                      ScreeningResult screening, AtsFeedbackResult ats,
                                      boolean eligible, List<String> eligibilityReasons,
                                      Path outputPath) throws IOException {
        byte[] pdfBytes = generateReport(student, job, resumeData, screening, ats, eligible, eligibilityReasons);
        Path parent = outputPath.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);   // Files.write fails if the folder does not exist yet
        Files.write(outputPath, pdfBytes);
    }

    private List<Line> buildLines(Student student, JobPosting job, ResumeData resumeData,
                                   ScreeningResult screening, AtsFeedbackResult ats,
                                   boolean eligible, List<String> eligibilityReasons) {
        List<Line> lines = new ArrayList<>();

        lines.add(Line.bold("Resume Screening Report"));
        lines.add(Line.of("Generated: " + LocalDateTime.now().format(TIMESTAMP_FORMAT)));

        // ---- Candidate details ----
        lines.add(Line.boldSpaced("Candidate Details"));
        lines.add(Line.of("Name: " + safe(student != null ? student.getName() : null)));
        lines.add(Line.of("PRN: " + safe(student != null ? student.getPrn() : null)));
        lines.add(Line.of("Email: " + safe(student != null ? student.getEmail() : null)));
        lines.add(Line.of("Department: " + safe(student != null ? student.getDepartment() : null)));
        lines.add(Line.of("CGPA: " + (student != null ? String.valueOf(student.getCgpa()) : "N/A")));
        lines.add(Line.of("Backlogs: " + (student != null ? String.valueOf(student.getBacklogs()) : "N/A")));
        lines.add(Line.of("Job: " + (job != null ? safe(job.getCompany()) + " - " + safe(job.getRole()) : "N/A")));

        // ---- Extracted resume info ----
        lines.add(Line.boldSpaced("Extracted Resume Information"));
        if (resumeData == null || resumeData.isEmpty()) {
            lines.add(Line.of("No readable resume text was found."));
        } else {
            lines.add(Line.of("File: " + safe(resumeData.getFileName()) + " (" + resumeData.getFileType() + ")"));
            lines.add(Line.of("Word count: " + resumeData.getWordCount()));
            lines.add(Line.of("Email detected: " + (resumeData.isEmailDetected() ? "Yes" : "No")
                    + "   Phone detected: " + (resumeData.isPhoneDetected() ? "Yes" : "No")));
            addWrappedField(lines, "Skills found in resume: ", String.join(", ", resumeData.getExtractedSkills()));
        }

        // ---- Eligibility result ----
        lines.add(Line.boldSpaced("Eligibility Result"));
        lines.add(Line.of(eligible ? "ELIGIBLE for this job posting." : "NOT ELIGIBLE for this job posting."));
        if (eligibilityReasons != null) {
            for (String reason : eligibilityReasons) {
                addWrappedField(lines, "- ", reason);
            }
        }

        // ---- Skill match ----
        lines.add(Line.boldSpaced("Matched / Missing Skills"));
        if (screening != null) {
            addWrappedField(lines, "Matched: ", screening.getMatchedSkills().isEmpty() ? "None" : String.join(", ", screening.getMatchedSkills()));
            addWrappedField(lines, "Missing: ", screening.getMissingSkills().isEmpty() ? "None" : String.join(", ", screening.getMissingSkills()));
            lines.add(Line.of("Skill score: " + screening.getSkillScore() + " / 40"));
        } else {
            lines.add(Line.of("Not available."));
        }

        // ---- Screening score breakdown ----
        lines.add(Line.boldSpaced("Screening Score Breakdown"));
        if (screening != null) {
            lines.add(Line.of("Skill match:      " + screening.getSkillScore() + " / 40"));
            lines.add(Line.of("Projects:         " + screening.getProjectScore() + " / 20"));
            lines.add(Line.of("Experience:       " + screening.getExperienceScore() + " / 20"));
            lines.add(Line.of("Certifications:   " + screening.getCertificationScore() + " / 10"));
            lines.add(Line.of("Relevant keywords: " + screening.getKeywordScore() + " / 10"));
            lines.add(Line.bold("TOTAL SCREENING SCORE: " + screening.getTotalScore() + " / 100"));
        } else {
            lines.add(Line.of("Not available."));
        }

        // ---- Strengths ----
        lines.add(Line.boldSpaced("Strengths"));
        if (screening != null && !screening.getStrengths().isEmpty()) {
            for (String s : screening.getStrengths()) addWrappedField(lines, "- ", s);
        } else {
            lines.add(Line.of("None identified."));
        }

        // ---- Missing areas / improvement suggestions ----
        lines.add(Line.boldSpaced("Missing Areas & Improvement Suggestions"));
        if (screening != null && !screening.getImprovementAreas().isEmpty()) {
            for (String s : screening.getImprovementAreas()) addWrappedField(lines, "- ", s);
        } else {
            lines.add(Line.of("None identified."));
        }

        // ---- ATS feedback ----
        lines.add(Line.boldSpaced("ATS-Friendly Resume Feedback"));
        if (ats != null) {
            lines.add(Line.of("ATS score: " + ats.getAtsScore() + " / 100"));
            for (String s : ats.getWarnings()) addWrappedField(lines, "Warning: ", s);
            for (String s : ats.getSuggestions()) addWrappedField(lines, "Suggestion: ", s);
        } else {
            lines.add(Line.of("Not available."));
        }

        return lines;
    }

    /** Wraps a long "label: value" field across multiple lines at ~95 chars so it doesn't run off the page. */
    private void addWrappedField(List<Line> lines, String prefix, String text) {
        String full = prefix + (text == null ? "" : text);
        int maxChars = 95;
        if (full.length() <= maxChars) {
            lines.add(Line.of(full));
            return;
        }
        int start = 0;
        boolean first = true;
        while (start < full.length()) {
            int end = Math.min(start + maxChars, full.length());
            if (end < full.length()) {
                int lastSpace = full.lastIndexOf(' ', end);
                if (lastSpace > start) end = lastSpace;
            }
            String chunk = full.substring(start, end).strip();
            lines.add(Line.of((first ? "" : "  ") + chunk));
            first = false;
            start = end;
        }
    }

    private String safe(String value) {
        return (value == null || value.isBlank()) ? "N/A" : value;
    }
}
