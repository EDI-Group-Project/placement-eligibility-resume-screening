package com.placement.models.resume;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Job-agnostic parsed content of a single resume.
 *
 * Produced by {@code com.placement.services.resume.ResumeParserService} from a raw
 * PDF/DOCX/TXT file. Deliberately does NOT reference a specific job posting - a
 * resume's own content doesn't change depending on what job is being screened
 * against, only how it's scored does (see {@link ScreeningResult}).
 *
 * All fields are plain, deterministic extraction results (regex/section-header
 * based) - nothing here is inferred by a model.
 */
public class ResumeData {

    private int studentId;
    private String fileName;
    private String fileType;       // "PDF", "DOCX", or "TXT"
    private String rawText;        // full extracted plain text

    // Section-scoped text, extracted by locating common section headers
    // (e.g. "Projects", "Experience", "Certifications", "Skills") in rawText.
    // Empty string when that section header wasn't found.
    private String skillsSectionText = "";
    private String projectsSectionText = "";
    private String experienceSectionText = "";
    private String certificationsSectionText = "";

    // Skill keywords found anywhere in rawText, matched against a built-in
    // common-skills dictionary (see SkillMatcherService). Job-agnostic.
    private Set<String> extractedSkills = new LinkedHashSet<>();

    // Simple structural signals used by AtsFeedbackService.
    private boolean emailDetected;
    private boolean phoneDetected;
    private int wordCount;

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }

    public String getSkillsSectionText() { return skillsSectionText; }
    public void setSkillsSectionText(String skillsSectionText) {
        this.skillsSectionText = skillsSectionText == null ? "" : skillsSectionText;
    }

    public String getProjectsSectionText() { return projectsSectionText; }
    public void setProjectsSectionText(String projectsSectionText) {
        this.projectsSectionText = projectsSectionText == null ? "" : projectsSectionText;
    }

    public String getExperienceSectionText() { return experienceSectionText; }
    public void setExperienceSectionText(String experienceSectionText) {
        this.experienceSectionText = experienceSectionText == null ? "" : experienceSectionText;
    }

    public String getCertificationsSectionText() { return certificationsSectionText; }
    public void setCertificationsSectionText(String certificationsSectionText) {
        this.certificationsSectionText = certificationsSectionText == null ? "" : certificationsSectionText;
    }

    public Set<String> getExtractedSkills() { return extractedSkills; }
    public void setExtractedSkills(Set<String> extractedSkills) {
        this.extractedSkills = extractedSkills == null ? new LinkedHashSet<>() : extractedSkills;
    }

    public boolean isEmailDetected() { return emailDetected; }
    public void setEmailDetected(boolean emailDetected) { this.emailDetected = emailDetected; }

    public boolean isPhoneDetected() { return phoneDetected; }
    public void setPhoneDetected(boolean phoneDetected) { this.phoneDetected = phoneDetected; }

    public int getWordCount() { return wordCount; }
    public void setWordCount(int wordCount) { this.wordCount = wordCount; }

    /** True when parsing produced no usable text at all (e.g. a scanned/image-only PDF). */
    public boolean isEmpty() {
        return rawText == null || rawText.isBlank();
    }

    /** An empty-but-safe placeholder used when a resume file is missing or unparseable. */
    public static ResumeData empty(int studentId, String fileName) {
        ResumeData data = new ResumeData();
        data.studentId = studentId;
        data.fileName = fileName;
        data.fileType = "UNKNOWN";
        data.rawText = "";
        data.extractedSkills = Collections.emptySet();
        return data;
    }
}
