package com.placement.services.resume;

import com.placement.models.JobPosting;
import com.placement.models.Student;
import com.placement.models.resume.ResumeData;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Resume skill/keyword extraction and job-specific skill matching.
 *
 * Two separate jobs, both deterministic/rule-based (no ML):
 *   1. {@link #extractSkills(String, String)} - scan resume text for keywords
 *      from a built-in common-skills dictionary. Job-agnostic; used by
 *      ResumeParserService to populate ResumeData.extractedSkills.
 *   2. {@link #match(ResumeData, Student, JobPosting)} - compare a candidate's
 *      known skills (resume-extracted + the Student.skills profile field)
 *      against one JobPosting.getRequiredSkills() list, producing matched /
 *      missing sets and a 0-40 skill score.
 *
 * Precision rules (so the score stays explainable and not inflated):
 *   - Whole-token matching: a skill must not be glued to letters/digits, so
 *     "java" does not match inside "javascript" and ".net" does not match the
 *     domain in "name@mail.net".
 *   - AMBIGUOUS skills (c, r, go, express, spring, swift, ruby, excel) are also
 *     ordinary words/letters ("Spring 2023", "go-getter", "express interest").
 *     They are only counted when found inside the resume's SKILLS section; a
 *     student's profile skills and a job's required list are matched as-is.
 *   - Aliases (nodejs -> node.js, js -> javascript, postgres -> postgresql ...)
 *     are normalised on both sides, so "NodeJS" in a job matches "node.js" on a resume.
 */
public class SkillMatcherService {

    /** Max points this component contributes to ScreeningResult.totalScore. */
    public static final double MAX_SKILL_SCORE = 40.0;

    private static final Set<String> COMMON_SKILLS = Set.of(
            // languages
            "java", "python", "c", "c++", "c#", "javascript", "typescript", "sql", "r", "go", "kotlin", "swift", "php", "ruby",
            // web / frontend
            "html", "css", "react", "angular", "vue", "node.js", "nodejs", "express", "next.js", "redux", "bootstrap", "tailwind",
            // backend / frameworks
            "spring", "spring boot", "django", "flask", "hibernate", "rest api", "graphql", "microservices", ".net", "asp.net",
            // data / ml
            "machine learning", "deep learning", "data science", "pandas", "numpy", "tensorflow", "pytorch", "scikit-learn",
            "data structures", "algorithms", "dsa", "data analysis", "tableau", "power bi", "excel",
            // databases
            "mysql", "postgresql", "mongodb", "oracle", "sqlite", "redis", "firebase", "dbms",
            // cloud / devops
            "aws", "azure", "gcp", "docker", "kubernetes", "jenkins", "ci/cd", "linux", "git", "github", "gitlab",
            // mobile
            "android", "ios", "flutter", "react native",
            // networking / systems
            "operating systems", "computer networks", "socket programming", "tcp/ip", "distributed systems",
            // soft / general
            "communication", "leadership", "teamwork", "problem solving", "agile", "scrum", "project management", "time management"
    );

    /** Dictionary words that are also everyday words/letters - only trusted inside the Skills section. */
    private static final Set<String> AMBIGUOUS_SKILLS = Set.of(
            "c", "r", "go", "express", "spring", "swift", "ruby", "excel");

    /** Alternate spellings -> canonical dictionary form. Applied on BOTH sides of every comparison. */
    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("nodejs", "node.js"), Map.entry("node", "node.js"),
            Map.entry("js", "javascript"), Map.entry("ts", "typescript"),
            Map.entry("reactjs", "react"), Map.entry("react.js", "react"),
            Map.entry("vuejs", "vue"), Map.entry("vue.js", "vue"),
            Map.entry("expressjs", "express"), Map.entry("express.js", "express"),
            Map.entry("postgres", "postgresql"), Map.entry("mongo", "mongodb"),
            Map.entry("golang", "go"), Map.entry("k8s", "kubernetes"),
            Map.entry("springboot", "spring boot"), Map.entry("cpp", "c++"), Map.entry("csharp", "c#"),
            Map.entry("ml", "machine learning"), Map.entry("dl", "deep learning"),
            Map.entry("html5", "html"), Map.entry("css3", "css"),
            Map.entry("restful api", "rest api"), Map.entry("restful apis", "rest api"), Map.entry("rest apis", "rest api"),
            Map.entry("amazon web services", "aws"), Map.entry("ms excel", "excel"), Map.entry("microsoft excel", "excel"),
            Map.entry("powerbi", "power bi"), Map.entry("sklearn", "scikit-learn"), Map.entry("scikit learn", "scikit-learn")
    );

    /** One precompiled whole-token pattern per dictionary skill (built once, not per resume). */
    private static final Map<String, Pattern> SKILL_PATTERNS = buildPatterns();

    private static Map<String, Pattern> buildPatterns() {
        Map<String, Pattern> patterns = new HashMap<>();
        for (String skill : COMMON_SKILLS) {
            // Single letters must not match the start of "c++" / "c#".
            String after = skill.length() == 1 ? "(?![a-z0-9+#])" : "(?![a-z0-9])";
            patterns.put(skill, Pattern.compile("(?<![a-z0-9])" + Pattern.quote(skill) + after));
        }
        return patterns;
    }

    /** Result of matching a candidate's known skills against one job's required skills. */
    public record MatchResult(Set<String> matchedSkills, Set<String> missingSkills, double skillScore) {}

    /** Extracts unambiguous skills only (ambiguous words need a Skills section - see the 2-argument overload). */
    public Set<String> extractSkillsFromText(String rawText) {
        return extractSkills(rawText, "");
    }

    /**
     * @param rawText           the whole resume text
     * @param skillsSectionText the text under the resume's Skills header ("" if none was detected);
     *                          the only place ambiguous skills such as "c", "go" or "spring" are accepted
     */
    public Set<String> extractSkills(String rawText, String skillsSectionText) {
        Set<String> found = new TreeSet<>();
        if (rawText == null || rawText.isBlank()) return found;

        String all = rawText.toLowerCase(Locale.ROOT);
        String skillsOnly = skillsSectionText == null ? "" : skillsSectionText.toLowerCase(Locale.ROOT);

        for (Map.Entry<String, Pattern> e : SKILL_PATTERNS.entrySet()) {
            String skill = e.getKey();
            String haystack = AMBIGUOUS_SKILLS.contains(skill) ? skillsOnly : all;
            if (!haystack.isEmpty() && e.getValue().matcher(haystack).find()) {
                found.add(normalize(skill));
            }
        }
        return found;
    }

    public MatchResult match(ResumeData resumeData, Student student, JobPosting job) {
        Set<String> candidateSkills = new LinkedHashSet<>();
        if (resumeData != null) {
            candidateSkills.addAll(normalizeAll(resumeData.getExtractedSkills()));
        }
        if (student != null && student.getSkills() != null) {
            candidateSkills.addAll(normalizeAll(List.of(student.getSkills().split("[,;|\\n]"))));
        }

        // Distinct, non-blank, normalised required skills. Blank/duplicate entries must not
        // count toward the denominator or a fully-matched candidate would score below 40.
        Set<String> required = new LinkedHashSet<>();
        if (job != null && job.getRequiredSkills() != null) {
            for (String entry : job.getRequiredSkills()) {
                if (entry == null) continue;
                for (String part : entry.split("[,;|]")) {
                    String n = normalize(part);
                    if (!n.isEmpty()) required.add(n);
                }
            }
        }

        Set<String> matched = new TreeSet<>();
        Set<String> missing = new TreeSet<>();
        for (String req : required) {
            if (candidateSkills.contains(req)) matched.add(req);
            else missing.add(req);
        }

        // A job with no required skills has nothing to penalise against.
        double score = required.isEmpty() ? MAX_SKILL_SCORE : MAX_SKILL_SCORE * matched.size() / (double) required.size();
        return new MatchResult(matched, missing, round2(score));
    }

    private Set<String> normalizeAll(Iterable<String> skills) {
        Set<String> out = new LinkedHashSet<>();
        if (skills == null) return out;
        for (String s : skills) {
            String n = normalize(s);
            if (!n.isEmpty()) out.add(n);
        }
        return out;
    }

    /** Trim, lower-case, collapse inner whitespace, then map aliases to their canonical form. */
    private String normalize(String s) {
        if (s == null) return "";
        String n = s.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        return ALIASES.getOrDefault(n, n);
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
