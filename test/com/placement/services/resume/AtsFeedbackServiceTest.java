package com.placement.services.resume;

import com.placement.models.resume.AtsFeedbackResult;
import com.placement.models.resume.ResumeData;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AtsFeedbackServiceTest {

    private ResumeData wellFormedResume() {
        ResumeData data = new ResumeData();
        data.setStudentId(1);
        data.setFileName("resume.pdf");
        data.setFileType("PDF");
        data.setSkillsSectionText("Java, SQL, React");
        data.setProjectsSectionText("- Built an app\n- Built another app");
        data.setExperienceSectionText("- Internship at Acme");
        data.setCertificationsSectionText("- AWS Certified");
        data.setEmailDetected(true);
        data.setPhoneDetected(true);
        data.setExtractedSkills(Set.of("java", "sql", "react"));
        String raw = "- Built an app\n- Built another app\n- Internship at Acme\n- AWS Certified "
                + "contact me at test@example.com or 9876543210. ".repeat(20);
        data.setRawText(raw);
        data.setWordCount(raw.split("\\s+").length);
        return data;
    }

    @Test
    void evaluate_wellFormedResume_hasFewWarningsAndHighScore() {
        AtsFeedbackResult result = new AtsFeedbackService().evaluate(wellFormedResume());

        assertFalse(result.getPositives().isEmpty());
        assertTrue(result.getAtsScore() > 50);
        assertTrue(result.getPositives().stream().anyMatch(p -> p.contains("email")));
        assertTrue(result.getPositives().stream().anyMatch(p -> p.contains("phone")));
    }

    @Test
    void evaluate_missingSectionsAndContactInfo_flagsWarnings() {
        ResumeData data = new ResumeData();
        data.setStudentId(1);
        data.setFileName("bare.pdf");
        data.setFileType("PDF");
        data.setRawText("Just some plain text with no structure at all.");
        data.setWordCount(10);
        data.setEmailDetected(false);
        data.setPhoneDetected(false);

        AtsFeedbackResult result = new AtsFeedbackService().evaluate(data);

        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("Skills")));
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("Projects")));
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("email")));
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("phone")));
        assertTrue(result.getAtsScore() < 50);
    }

    @Test
    void evaluate_emptyResume_returnsZeroScoreWithGuidance() {
        ResumeData empty = ResumeData.empty(1, "empty.pdf");
        AtsFeedbackResult result = new AtsFeedbackService().evaluate(empty);

        assertEquals(0, result.getAtsScore());
        assertFalse(result.getWarnings().isEmpty());
        assertFalse(result.getSuggestions().isEmpty());
    }

    @Test
    void evaluate_bulletUsageDetected_addsPositive() {
        ResumeData data = wellFormedResume();
        AtsFeedbackResult result = new AtsFeedbackService().evaluate(data);
        assertTrue(result.getPositives().stream().anyMatch(p -> p.toLowerCase().contains("bullet")));
    }

    @Test
    void evaluate_scoreNeverNegative() {
        ResumeData data = new ResumeData();
        data.setStudentId(1);
        data.setFileName("bad.pdf");
        data.setFileType("PDF");
        data.setRawText("x");
        data.setWordCount(1);
        AtsFeedbackResult result = new AtsFeedbackService().evaluate(data);
        assertTrue(result.getAtsScore() >= 0);
    }
}
