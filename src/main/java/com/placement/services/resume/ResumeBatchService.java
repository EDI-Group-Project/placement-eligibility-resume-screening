package com.placement.services.resume;

import com.placement.models.JobPosting;
import com.placement.models.Student;
import com.placement.models.resume.AtsFeedbackResult;
import com.placement.models.resume.ResumeData;
import com.placement.models.resume.ScreeningResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Batch resume upload for staff (TPO / TPC / Admin): up to {@value #MAX_FILES} resumes per batch, each one
 * belonging to a student named by the caller (student id, PRN or email - the {@code resumes} table holds exactly
 * one resume per student).
 *
 * Every file goes through the SAME path as a single upload ({@link ResumeScreeningService#uploadResume}: type /
 * size / content checks, parse once, store file + cached text), and is processed on its own: a bad file, an unknown
 * student or a database error is reported on that file's row and never stops the others. No transaction spans the
 * batch - the per-file status says exactly what was stored.
 *
 * The analysis shown per resume is the existing one: ATS feedback, and - when a job is given - the job screening
 * score. Screening here is read-only ({@link ResumeScreeningService#screen}); nothing is written to applications.
 */
public class ResumeBatchService {

    public static final int MAX_FILES = 20;

    public static final String PROCESSED = "PROCESSED";     // stored, text extracted
    public static final String UNREADABLE = "UNREADABLE";   // stored, but no text could be extracted (scanned / image-only)
    public static final String FAILED = "FAILED";           // rejected, nothing stored

    private static final int MAX_STORED_BATCHES = 50;

    private final ResumeScreeningService service;

    public ResumeBatchService(ResumeScreeningService service) {
        this.service = service;
    }

    /** @param roster all students (loaded once per batch, used to resolve the references); @param job optional, adds screening */
    public Batch start(List<Student> roster, JobPosting job) {
        return new Batch(roster, job);
    }

    // ------------------------------------------------------------------
    // result of one resume
    // ------------------------------------------------------------------

    /** Outcome of one file. {@code student}, {@code resume}, {@code ats} and {@code screening} are null when not applicable. */
    public record Item(int index, String studentRef, Student student, String fileName, String status, String message,
                       ResumeData resume, AtsFeedbackResult ats, ScreeningResult screening) {

        /**
         * RESULT|index|studentRef|studentId|prn|name|fileName|status|words|atsScore|atsWarnings|skills
         *        |screeningTotal|skillScore|matchedSkills|missingSkills|message
         * (the screening columns are empty unless the batch was started with a job).
         */
        public String toProtocolLine() {
            return String.join("|", "RESULT", String.valueOf(index), field(studentRef),
                    student == null ? "" : String.valueOf(student.getStudentId()),
                    student == null ? "" : field(student.getPrn()),
                    student == null ? "" : field(student.getName()),
                    field(fileName), status,
                    resume == null ? "" : String.valueOf(resume.getWordCount()),
                    ats == null ? "" : String.valueOf(ats.getAtsScore()),
                    ats == null ? "" : field(String.join("; ", ats.getWarnings())),
                    resume == null ? "" : field(String.join(",", resume.getExtractedSkills())),
                    screening == null ? "" : String.valueOf(screening.getTotalScore()),
                    screening == null ? "" : String.valueOf(screening.getSkillScore()),
                    screening == null ? "" : field(String.join(",", screening.getMatchedSkills())),
                    screening == null ? "" : field(String.join(",", screening.getMissingSkills())),
                    field(message));
        }
    }

    // ------------------------------------------------------------------
    // one batch in progress
    // ------------------------------------------------------------------

    public final class Batch {
        private final List<Student> roster;
        private final JobPosting job;
        private final Set<Integer> stored = new HashSet<>();      // students that already got a resume in THIS batch
        private final List<Item> items = new ArrayList<>();

        private Batch(List<Student> roster, JobPosting job) {
            this.roster = roster;
            this.job = job;
        }

        /**
         * Processes one file and records its outcome. Never throws.
         *
         * @param problem non-null when the file could not even be read from the request (too large, truncated, not
         *                Base64); it is reported as that file's failure and {@code bytes} is ignored
         */
        public Item add(String studentRef, String fileName, byte[] bytes, String problem) {
            int index = items.size() + 1;
            String name = fileName == null ? "" : fileName;
            Student student = resolve(roster, studentRef);
            Item item;
            try {
                if (problem != null) item = failed(index, studentRef, student, name, problem);
                else if (student == null) item = failed(index, studentRef, null, name, studentRef == null || studentRef.isBlank()
                        ? "Student ID, PRN or email is required." : "Student not found: " + shorten(studentRef));
                else if (stored.contains(student.getStudentId()))
                    item = failed(index, studentRef, student, name, "This student already has a resume in this batch. Only one resume per student is kept.");
                else item = store(index, studentRef, student, name, bytes);
            } catch (IllegalArgumentException e) {      // the upload checks write their messages for end users
                item = failed(index, studentRef, student, name, e.getMessage());
            } catch (Exception e) {                     // one bad file must not sink the batch; details stay in the server log
                System.out.println("Batch resume " + index + " failed: " + e);
                item = failed(index, studentRef, student, name, "Could not store this resume. Please try again.");
            }
            items.add(item);
            return item;
        }

        public List<Item> items() {
            return List.copyOf(items);
        }

        public int count(String status) {
            return (int) items.stream().filter(i -> i.status().equals(status)).count();
        }

        /** BATCH|batchId|total|processed|unreadable|failed|jobId  followed by one RESULT line per file. */
        public List<String> lines(String batchId) {
            List<String> out = new ArrayList<>();
            out.add("BATCH|" + batchId + "|" + items.size() + "|" + count(PROCESSED) + "|" + count(UNREADABLE)
                    + "|" + count(FAILED) + "|" + (job == null ? "" : field(job.getId())));
            for (Item i : items) out.add(i.toProtocolLine());
            return out;
        }

        private Item store(int index, String ref, Student student, String fileName, byte[] bytes) throws Exception {
            int id = student.getStudentId();
            boolean replaced = service.hasResume(id);
            ResumeData data = service.uploadResume(id, fileName, bytes);       // from here on the resume IS stored
            stored.add(id);
            String note = replaced ? "Replaced the previous resume." : "";
            String status = data.isEmpty() ? UNREADABLE : PROCESSED;
            if (data.isEmpty()) note = ("No readable text found (scanned, image-only or unsupported PDF). A .docx file is the most reliable. " + note).strip();
            try {
                if (job != null && !data.isEmpty()) {
                    ResumeScreeningService.Outcome o = service.screen(student, job);
                    return new Item(index, ref, student, fileName, status, note, data, o.ats(), o.screening());
                }
                return new Item(index, ref, student, fileName, status, note, data, service.atsFeedback(id), null);
            } catch (Exception e) {                // stored fine, only the analysis failed - do not call that FAILED
                System.out.println("Batch resume " + index + " analysis failed: " + e);
                return new Item(index, ref, student, fileName, status, (note + " The analysis could not be completed.").strip(), data, null, null);
            }
        }

        private Item failed(int index, String ref, Student student, String fileName, String message) {
            return new Item(index, ref, student, fileName, FAILED, message, null, null, null);
        }
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    /** Finds a student by numeric id first, then by PRN or email (case-insensitive), in an already loaded roster. */
    public static Student resolve(List<Student> roster, String ref) {
        if (roster == null || ref == null || ref.isBlank()) return null;
        String value = ref.trim();
        try {
            int id = Integer.parseInt(value);
            for (Student s : roster) if (s.getStudentId() == id) return s;
        } catch (NumberFormatException ignored) {
            // not a number - fall through to PRN / email
        }
        for (Student s : roster) {
            if (value.equalsIgnoreCase(s.getPrn()) || value.equalsIgnoreCase(s.getEmail())) return s;
        }
        return null;
    }

    private static String shorten(String s) {
        String v = s == null ? "" : s.strip();
        return v.length() > 40 ? v.substring(0, 40) + "..." : v;
    }

    /** A protocol field must not contain the separator or a line break. */
    private static String field(String s) {
        return s == null ? "" : s.replace('|', '/').replace('\r', ' ').replace('\n', ' ');
    }

    // ------------------------------------------------------------------
    // recent batches, so a result can be looked up again by its id
    // ------------------------------------------------------------------

    // shortcut: kept in memory only (last MAX_STORED_BATCHES batches, lost on server restart), because the schema has
    // no batch table; upgrade to a table if staff need history that survives a restart.
    private static final Map<String, List<String>> RECENT = new LinkedHashMap<String, List<String>>() {
        @Override protected boolean removeEldestEntry(Map.Entry<String, List<String>> eldest) {
            return size() > MAX_STORED_BATCHES;
        }
    };

    public static String newBatchId() {
        return UUID.randomUUID().toString();
    }

    public static void remember(String batchId, List<String> lines) {
        synchronized (RECENT) {
            RECENT.put(batchId, List.copyOf(lines));
        }
    }

    /** The lines of an earlier batch, or null when the id is unknown or has been pushed out by newer batches / a restart. */
    public static List<String> recall(String batchId) {
        synchronized (RECENT) {
            return RECENT.get(batchId);
        }
    }
}
