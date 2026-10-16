package com.placement.services.resume;

import com.placement.models.JobPosting;
import com.placement.models.Student;
import com.placement.models.resume.ResumeData;
import com.placement.models.resume.ScreeningResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Produces an explainable resume SCREENING score for one (student, job,
 * resume) combination - completely separate from job ELIGIBILITY.
 *
 * Eligibility (CGPA/backlogs/branch/passing-year/required-skills gate) is
 * decided entirely by the existing {@code EligibilityDAO}/{@code
 * StudentService.getEligibleUnnotifiedStudents}; this class is never
 * consulted for that decision and never duplicates it. A caller combining
 * both (e.g. for the PDF report) runs them independently and presents both
 * results side by side.
 *
 * Score breakdown (sums to 0-100), every component traceable to a concrete
 * rule - nothing here is a learned/statistical weight:
 *   - skill match:      0-40  (SkillMatcherService: required skills found in resume/profile)
 *   - projects:         0-20  (entries under a "Projects" section: one per title line;
 *                               bullet/description lines are NOT counted as extra projects)
 *   - experience:       0-20  (entries under "Experience"/"Internship", counted the same way)
 *   - certifications:   0-10  (entries under "Certifications", counted the same way)
 *   - relevant keywords: 0-10 (action-verb / impact-keyword density in the whole resume)
 */
public class ScreeningService {

    private static final double MAX_PROJECT_SCORE = 20.0;
    private static final double MAX_EXPERIENCE_SCORE = 20.0;
    private static final double MAX_CERTIFICATION_SCORE = 10.0;
    private static final double MAX_KEYWORD_SCORE = 10.0;

    private static final double POINTS_PER_PROJECT = 5.0;
    private static final double POINTS_PER_EXPERIENCE_ENTRY = 7.0;
    private static final double POINTS_PER_CERTIFICATION = 3.0;

    /** Generic, placement-relevant action/impact keywords - deliberately small and reviewable. */
    private static final List<String> RELEVANT_KEYWORDS = List.of(
            "developed", "designed", "implemented", "built", "led", "managed", "optimized", "improved",
            "automated", "achieved", "collaborated", "deployed", "created", "analyzed", "reduced", "increased"
    );

    /** Compiled once (not per resume): whole-word patterns for RELEVANT_KEYWORDS. */
    private static final Pattern[] KEYWORD_PATTERNS = RELEVANT_KEYWORDS.stream()
            .map(k -> Pattern.compile("\\b" + k + "\\b"))
            .toArray(Pattern[]::new);

    private final SkillMatcherService skillMatcherService;

    public ScreeningService() {
        this(new SkillMatcherService());
    }

    public ScreeningService(SkillMatcherService skillMatcherService) {
        this.skillMatcherService = skillMatcherService;
    }

    public ScreeningResult screen(Student student, JobPosting job, ResumeData resumeData) {
        ScreeningResult result = new ScreeningResult();
        result.setStudentId(student != null ? student.getStudentId() : 0);
        result.setJobId(job != null ? job.getId() : null);

        if (resumeData == null || resumeData.isEmpty()) {
            // No usable resume text: eligibility can still proceed independently,
            // but screening has nothing to score - return a safe, explained zero result.
            result.getImprovementAreas().add("No resume text could be read. Upload a text-based PDF or DOCX resume (not a scanned image) to get a screening score.");
            return result;
        }

        SkillMatcherService.MatchResult skillMatch = skillMatcherService.match(resumeData, student, job);
        result.setMatchedSkills(new ArrayList<>(skillMatch.matchedSkills()));
        result.setMissingSkills(new ArrayList<>(skillMatch.missingSkills()));
        result.setSkillScore(skillMatch.skillScore());

        int projectCount = countEntries(resumeData.getProjectsSectionText());
        double projectScore = round2(Math.min(projectCount * POINTS_PER_PROJECT, MAX_PROJECT_SCORE));
        result.setProjectScore(projectScore);

        int experienceCount = countEntries(resumeData.getExperienceSectionText());
        double experienceScore = round2(Math.min(experienceCount * POINTS_PER_EXPERIENCE_ENTRY, MAX_EXPERIENCE_SCORE));
        result.setExperienceScore(experienceScore);

        int certificationCount = countEntries(resumeData.getCertificationsSectionText());
        double certificationScore = round2(Math.min(certificationCount * POINTS_PER_CERTIFICATION, MAX_CERTIFICATION_SCORE));
        result.setCertificationScore(certificationScore);

        double keywordScore = round2(Math.min(countKeywordHits(resumeData.getRawText()), MAX_KEYWORD_SCORE));
        result.setKeywordScore(keywordScore);

        double total = round2(result.getSkillScore() + projectScore + experienceScore + certificationScore + keywordScore);
        result.setTotalScore(total);

        result.setStrengths(buildStrengths(result, projectCount, experienceCount, certificationCount));
        result.setImprovementAreas(buildImprovementAreas(result, projectCount, experienceCount, certificationCount));

        return result;
    }

    /**
     * Counts entries in a section: one per non-bullet line (the project / role / certificate title).
     * Lines starting with a bullet marker are descriptions of the entry above them, so a single
     * project described in four bullets is ONE project, not four. If a section is written as bullets
     * only, each bullet counts as an entry.
     */
    private int countEntries(String sectionText) {
        if (sectionText == null || sectionText.isBlank()) return 0;
        int titles = 0;
        int bullets = 0;
        for (String raw : sectionText.split("\n")) {
            String line = raw.strip();
            if (line.isEmpty()) continue;
            if (isBulletLine(line)) bullets++;
            else titles++;
        }
        return titles > 0 ? titles : bullets;
    }

    private boolean isBulletLine(String line) {
        char c = line.charAt(0);
        return c == '-' || c == '*' || c == '\u2022' || c == '\u00B7' || c == '\u25AA' || c == '\u25CF'
                || c == '\u2013' || (c >= '\uE000' && c <= '\uF8FF');   // last range: Symbol-font bullets from PDFs
    }

    private int countKeywordHits(String rawText) {
        if (rawText == null || rawText.isBlank()) return 0;
        String lower = rawText.toLowerCase(Locale.ROOT);
        int hits = 0;
        for (Pattern keyword : KEYWORD_PATTERNS) {
            if (keyword.matcher(lower).find()) hits++;
        }
        return hits;
    }

    private List<String> buildStrengths(ScreeningResult r, int projectCount, int experienceCount, int certificationCount) {
        List<String> strengths = new ArrayList<>();
        if (r.getSkillScore() >= SkillMatcherService.MAX_SKILL_SCORE * 0.75) {
            strengths.add("Strong match with the job's required skills (" + r.getMatchedSkills().size() + " matched).");
        }
        if (projectCount >= 2) {
            strengths.add("Demonstrates multiple relevant projects (" + projectCount + " found).");
        }
        if (experienceCount >= 1) {
            strengths.add("Includes internship/work experience entries (" + experienceCount + " found).");
        }
        if (certificationCount >= 1) {
            strengths.add("Lists relevant certifications (" + certificationCount + " found).");
        }
        if (r.getKeywordScore() >= MAX_KEYWORD_SCORE * 0.6) {
            strengths.add("Resume uses strong action/impact language (e.g. developed, led, optimized).");
        }
        if (strengths.isEmpty()) {
            strengths.add("No standout strengths detected yet based on the current resume content.");
        }
        return strengths;
    }

    private List<String> buildImprovementAreas(ScreeningResult r, int projectCount, int experienceCount, int certificationCount) {
        List<String> improvements = new ArrayList<>();
        if (!r.getMissingSkills().isEmpty()) {
            improvements.add("Missing required skills for this job: " + String.join(", ", r.getMissingSkills()) + ".");
        }
        if (projectCount == 0) {
            improvements.add("Add a \"Projects\" section describing relevant academic or personal projects.");
        }
        if (experienceCount == 0) {
            improvements.add("Add any internship, part-time, or practical work experience, if available.");
        }
        if (certificationCount == 0) {
            improvements.add("Consider adding relevant certifications or online courses completed.");
        }
        if (r.getKeywordScore() < MAX_KEYWORD_SCORE * 0.3) {
            improvements.add("Use more specific action verbs (developed, built, led, optimized) to describe achievements.");
        }
        return improvements;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
