package com.placement.services.resume;

import com.placement.models.JobPosting;
import com.placement.models.Student;
import com.placement.models.resume.ResumeData;
import com.placement.models.resume.ScreeningResult;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests ScreeningService's explainable score breakdown using hand-built
 * ResumeData/Student/JobPosting objects - no file parsing, no database.
 */
class ScreeningServiceTest {

    private Student student() {
        Student s = new Student();
        s.setStudentId(1);
        s.setName("Test Student");
        s.setSkills("java,sql");
        return s;
    }

    private JobPosting job(List<String> requiredSkills) {
        JobPosting job = new JobPosting();
        job.setId("JOB-TEST");
        job.setCompany("Acme");
        job.setRole("Software Engineer");
        job.setRequiredSkills(requiredSkills);
        return job;
    }

    private ResumeData resumeWithSections(Set<String> skills, String projects, String experience, String certifications, String rawTextExtra) {
        ResumeData data = new ResumeData();
        data.setStudentId(1);
        data.setFileName("resume.pdf");
        data.setFileType("PDF");
        data.setExtractedSkills(skills);
        data.setProjectsSectionText(projects);
        data.setExperienceSectionText(experience);
        data.setCertificationsSectionText(certifications);
        String raw = String.join("\n", projects, experience, certifications, rawTextExtra);
        data.setRawText(raw.isBlank() ? "placeholder text" : raw);
        return data;
    }

    @Test
    void screen_fullMatchAllSections_scoresHigh() {
        Set<String> skills = new LinkedHashSet<>(List.of("java", "sql", "react"));
        ResumeData resume = resumeWithSections(
                skills,
                "Built an e-commerce site\nDeveloped a chat application",
                "Internship at Acme Corp",
                "AWS Certified Developer",
                "developed built led optimized"
        );

        ScreeningService service = new ScreeningService();
        ScreeningResult result = service.screen(student(), job(List.of("java", "sql", "react")), resume);

        assertEquals(40.0, result.getSkillScore(), 0.001);
        assertEquals(3, result.getMatchedSkills().size());
        assertTrue(result.getMissingSkills().isEmpty());
        assertEquals(10.0, result.getProjectScore(), 0.001); // 2 entries * 5
        assertEquals(7.0, result.getExperienceScore(), 0.001); // 1 entry * 7
        assertEquals(3.0, result.getCertificationScore(), 0.001); // 1 entry * 3
        assertTrue(result.getKeywordScore() > 0);
        assertEquals(result.getSkillScore() + result.getProjectScore() + result.getExperienceScore()
                + result.getCertificationScore() + result.getKeywordScore(), result.getTotalScore(), 0.001);
        assertFalse(result.getStrengths().isEmpty());
    }

    @Test
    void screen_missingSkillsAndEmptySections_scoresLowAndListsGaps() {
        ResumeData resume = resumeWithSections(Set.of("java"), "", "", "", "some unrelated text");

        ScreeningService service = new ScreeningService();
        ScreeningResult result = service.screen(student(), job(List.of("java", "python", "aws")), resume);

        assertEquals(1, result.getMatchedSkills().size());
        assertEquals(2, result.getMissingSkills().size());
        assertTrue(result.getMissingSkills().contains("python"));
        assertTrue(result.getMissingSkills().contains("aws"));
        assertEquals(0.0, result.getProjectScore(), 0.001);
        assertEquals(0.0, result.getExperienceScore(), 0.001);
        assertEquals(0.0, result.getCertificationScore(), 0.001);
        assertFalse(result.getImprovementAreas().isEmpty());
        // missing-skills message should be present among the improvement areas
        assertTrue(result.getImprovementAreas().stream().anyMatch(s -> s.contains("python")));
    }

    @Test
    void screen_noRequiredSkills_givesFullSkillScore() {
        ResumeData resume = resumeWithSections(Set.of(), "", "", "", "text");
        ScreeningService service = new ScreeningService();
        ScreeningResult result = service.screen(student(), job(List.of()), resume);
        assertEquals(40.0, result.getSkillScore(), 0.001);
    }

    @Test
    void screen_emptyResume_returnsZeroScoreSafely() {
        ResumeData empty = ResumeData.empty(1, "empty.pdf");
        ScreeningService service = new ScreeningService();
        ScreeningResult result = service.screen(student(), job(List.of("java")), empty);

        assertEquals(0.0, result.getTotalScore(), 0.001);
        assertFalse(result.getImprovementAreas().isEmpty());
    }

    @Test
    void screen_projectAndExperienceScoresAreCapped() {
        String manyProjects = "p1\np2\np3\np4\np5\np6\np7\np8";
        String manyExperience = "e1\ne2\ne3\ne4\ne5";
        ResumeData resume = resumeWithSections(Set.of(), manyProjects, manyExperience, "", "text");

        ScreeningService service = new ScreeningService();
        ScreeningResult result = service.screen(student(), job(List.of()), resume);

        assertEquals(20.0, result.getProjectScore(), 0.001);   // capped at MAX_PROJECT_SCORE
        assertEquals(20.0, result.getExperienceScore(), 0.001); // capped at MAX_EXPERIENCE_SCORE
    }
}
