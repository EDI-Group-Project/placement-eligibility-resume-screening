package com.placement.services.resume;

import com.placement.models.resume.ResumeData;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a resume file (PDF, DOCX, or plain TXT) into a job-agnostic
 * {@link ResumeData}: raw text, section-scoped text (skills/projects/
 * experience/certifications), detected skill keywords, and basic contact
 * info signals.
 *
 * Binary extraction is delegated to {@link PdfTextExtractor} (best-effort,
 * zero-dependency) and {@link DocxTextExtractor} (fully reliable,
 * zero-dependency - see each class's javadoc for how). Skill-keyword
 * detection is delegated to {@link SkillMatcherService}, which is also used
 * later for job-specific matching, so the dictionary is defined in one place.
 *
 * Recommended call pattern (to avoid re-parsing on every request, per the
 * "avoid unnecessary repeated resume parsing" requirement): a caller should
 * first check {@code ResumeDAO.findExtractedTextByStudentId(studentId)}; if
 * a cached {@code extracted_text} already exists, skip this class entirely
 * and re-run only the (cheap, deterministic) section/skill re-analysis on
 * that cached text via {@link #fromCachedText(String, int, String)}. Only
 * call {@link #parse} again when the student has uploaded a new file.
 */
public class ResumeParserService {

    /** Largest resume file accepted (2 MB). Larger inputs are treated as unreadable instead of being parsed. */
    public static final int MAX_FILE_BYTES = 2 * 1024 * 1024;

    private final SkillMatcherService skillMatcherService;

    public ResumeParserService() {
        this(new SkillMatcherService());
    }

    public ResumeParserService(SkillMatcherService skillMatcherService) {
        this.skillMatcherService = skillMatcherService;
    }

    /** Parses resume bytes straight from an upload (no temp file needed). Never throws for a bad/unsupported file - returns ResumeData.empty() instead. */
    public ResumeData parse(byte[] fileBytes, String fileName, int studentId) {
        if (fileBytes == null || fileBytes.length == 0 || fileBytes.length > MAX_FILE_BYTES) {
            return ResumeData.empty(studentId, fileName);
        }

        String fileType = detectFileType(fileName);
        String rawText;
        try {
            rawText = switch (fileType) {
                case "PDF" -> PdfTextExtractor.extract(fileBytes);
                case "DOCX" -> DocxTextExtractor.extract(fileBytes);
                case "TXT" -> new String(fileBytes, StandardCharsets.UTF_8).replace("\uFEFF", "");
                default -> "";
            };
        } catch (Exception e) {
            // Any unexpected parsing failure degrades safely to "no text found"
            // rather than propagating and breaking the caller's flow.
            rawText = "";
        }

        ResumeData data = buildFromText(rawText, studentId, fileName, fileType);
        return data;
    }

    /** Convenience overload for a file already on disk. */
    public ResumeData parse(Path filePath, int studentId) throws IOException {
        if (Files.size(filePath) > MAX_FILE_BYTES) {
            return ResumeData.empty(studentId, filePath.getFileName().toString());
        }
        byte[] bytes = Files.readAllBytes(filePath);
        return parse(bytes, filePath.getFileName().toString(), studentId);
    }

    /**
     * Re-runs section/skill analysis on already-extracted text (e.g. loaded
     * from {@code ResumeDAO.findExtractedTextByStudentId}) without touching
     * the original file again - the cheap path for repeat screenings.
     */
    public ResumeData fromCachedText(String cachedText, int studentId, String fileName) {
        return buildFromText(cachedText == null ? "" : cachedText, studentId, fileName, detectFileType(fileName));
    }

    private ResumeData buildFromText(String rawText, int studentId, String fileName, String fileType) {
        ResumeData data = new ResumeData();
        data.setStudentId(studentId);
        data.setFileName(fileName);
        data.setFileType(fileType);
        data.setRawText(rawText == null ? "" : rawText);

        if (data.isEmpty()) {
            return data;
        }

        populateSections(data);
        detectContactInfo(data);
        data.setWordCount(countWords(data.getRawText()));
        data.setExtractedSkills(skillMatcherService.extractSkills(data.getRawText(), data.getSkillsSectionText()));

        return data;
    }

    private String detectFileType(String fileName) {
        if (fileName == null) return "UNKNOWN";
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) return "PDF";
        if (lower.endsWith(".docx")) return "DOCX";
        if (lower.endsWith(".txt")) return "TXT";
        return "UNKNOWN";
    }

    // ------------------------------------------------------------------
    // Section detection: a deterministic, header-keyword based splitter -
    // no inference, just pattern matching on short header-like lines.
    // ------------------------------------------------------------------

    // Header lines must match ONE OF THESE EXACTLY (case-insensitive, trailing ':'
    // ignored) - not just "contain" the keyword. A body line like "Internship at
    // Acme Corp" must NOT be mistaken for the "Experience" section header itself,
    // which a substring/contains check would incorrectly do.
    private static final Set<String> SKILLS_HEADERS = Set.of(
            "skills", "technical skills", "key skills", "core skills", "skill set", "technologies",
            "soft skills", "technical expertise", "tools and technologies", "tools & technologies",
            "programming languages", "skills and abilities", "skills & abilities");
    private static final Set<String> PROJECT_HEADERS = Set.of(
            "projects", "academic projects", "personal projects", "key projects", "major projects",
            "project", "project experience", "project work");
    private static final Set<String> EXPERIENCE_HEADERS = Set.of(
            "experience", "work experience", "professional experience", "internship", "internships",
            "employment history", "work history", "internship experience", "industrial experience");
    private static final Set<String> CERTIFICATION_HEADERS = Set.of(
            "certifications", "certificates", "certification", "courses", "licenses & certifications",
            "licenses and certifications");
    private static final Set<String> OTHER_HEADERS = Set.of(
            "education", "objective", "summary", "career objective", "achievements", "extra-curricular activities",
            "extracurricular activities", "contact", "personal details", "declaration", "hobbies", "languages",
            "profile", "about me", "positions of responsibility", "awards", "awards and achievements",
            "awards & achievements", "publications", "interests", "references", "volunteer experience",
            "leadership", "activities", "co-curricular activities", "academic achievements",
            "scholastic achievements", "responsibilities", "strengths");

    private void populateSections(ResumeData data) {
        String[] lines = data.getRawText().split("\n");

        StringBuilder skills = new StringBuilder();
        StringBuilder projects = new StringBuilder();
        StringBuilder experience = new StringBuilder();
        StringBuilder certifications = new StringBuilder();

        int current = 0; // 0=none, 1=skills, 2=projects, 3=experience, 4=certifications

        for (String rawLine : lines) {
            String line = rawLine.strip();
            if (line.isEmpty()) continue;

            String headerMatch = matchesAnyHeader(line);
            if (headerMatch != null) {
                current = switch (headerMatch) {
                    case "SKILLS" -> 1;
                    case "PROJECTS" -> 2;
                    case "EXPERIENCE" -> 3;
                    case "CERTIFICATIONS" -> 4;
                    case "OTHER" -> 0;
                    default -> current;
                };
                continue; // don't include the header line itself in the section body
            }

            switch (current) {
                case 1 -> skills.append(line).append('\n');
                case 2 -> projects.append(line).append('\n');
                case 3 -> experience.append(line).append('\n');
                case 4 -> certifications.append(line).append('\n');
                default -> { /* before any recognised section header - ignored for section text */ }
            }
        }

        data.setSkillsSectionText(skills.toString().strip());
        data.setProjectsSectionText(projects.toString().strip());
        data.setExperienceSectionText(experience.toString().strip());
        data.setCertificationsSectionText(certifications.toString().strip());
    }

    /** Returns which section a header-like line IS (exact match, not "contains"), or null if it isn't a recognised header. */
    private String matchesAnyHeader(String line) {
        if (line.length() > 40) {
            return null; // real header lines are short
        }
        // Ignore decoration around the words: "SKILLS:", "-- Projects --", "Experience." ...
        String normalized = line.toLowerCase(Locale.ROOT).replaceAll("^[^a-z]+|[^a-z]+$", "").strip();
        if (normalized.isEmpty()) return null;

        if (CERTIFICATION_HEADERS.contains(normalized)) return "CERTIFICATIONS";
        if (EXPERIENCE_HEADERS.contains(normalized)) return "EXPERIENCE";
        if (PROJECT_HEADERS.contains(normalized)) return "PROJECTS";
        if (SKILLS_HEADERS.contains(normalized)) return "SKILLS";
        if (OTHER_HEADERS.contains(normalized)) return "OTHER";
        return null;
    }

    // ------------------------------------------------------------------
    // Contact info detection (used by AtsFeedbackService)
    // ------------------------------------------------------------------

    private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    // Not allowed to sit inside a longer digit run, so a 12-digit PRN / roll number is not mistaken for a phone.
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            // Indian mobile: 9876543210, 98765 43210, 98765-43210, +91 98765 43210, 0091-...
            "(?<!\\d)(?:(?:\\+|00)?91[\\s-]?)?[6-9]\\d{4}[\\s-]?\\d{5}(?!\\d)"
            // other international numbers: +1 415 555 2671
            + "|(?<![\\d+])\\+\\d{1,3}[\\s-]?\\(?\\d{2,5}\\)?[\\s-]?\\d{3,5}[\\s-]?\\d{3,5}(?!\\d)");

    private void detectContactInfo(ResumeData data) {
        Matcher emailMatcher = EMAIL_PATTERN.matcher(data.getRawText());
        data.setEmailDetected(emailMatcher.find());

        Matcher phoneMatcher = PHONE_PATTERN.matcher(data.getRawText());
        data.setPhoneDetected(phoneMatcher.find());
    }

    private int countWords(String text) {
        String trimmed = text.strip();
        if (trimmed.isEmpty()) return 0;
        return trimmed.split("\\s+").length;
    }
}
