package com.placement.services.staff;

import com.placement.models.JobPosting;
import com.placement.models.Student;
import com.placement.models.resume.ResumeData;
import com.placement.services.JobService;
import com.placement.services.StudentService;
import com.placement.services.resume.ResumeScreeningService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Feature 2 - batch resume upload (1-20 resumes) for TPO / TPC / Admin (permission is enforced in the handler).
 *
 * How a batch works
 *   1. startBatch(job, n)   - staff pick the job and how many files they will send. The roster is loaded ONCE
 *                             and indexed by PRN and e-mail; eligibility for the job is computed ONCE.
 *   2. addFile(batch, file) - one call per file (each is its own small socket request, so a request never holds
 *                             more than one 2 MB resume in memory). The file is queued and processed on a small,
 *                             separate worker pool (JDK ThreadPoolExecutor) so a batch can never occupy the
 *                             server's client-handling threads.
 *   3. status(batch)        - staff poll to see every file's state, parse status, match score and eligibility.
 *
 * Per-file pipeline (all inside one try/catch, so ONE bad file never affects the others):
 *   file name -> student (PRN or e-mail in the name) -> existing upload workflow (validate, parse once, store file,
 *   cache text) -> screening score for the job -> eligibility for the job.
 *
 * File naming rule: the file name (without extension) must equal a student's PRN or e-mail, or contain it as a
 * token separated by '_' or spaces - e.g. "B24CE1001.pdf", "B24CE1001_Tushar.docx", "aarav@college.edu.pdf".
 *
 * NOTE (by design): the resume is stored through the existing upload workflow, so it REPLACES that student's
 * current resume (the resumes table keeps one resume per student). The item message says when this happened.
 *
 * Batches live in memory only (TTL 2 hours, at most 50 kept); the stored resumes themselves are in the database.
 */
public class BatchResumeService {

    public static final int MAX_FILES_PER_BATCH = 20;

    private static final int WORKER_THREADS = 3;
    private static final int QUEUE_CAPACITY = 60;
    private static final int MAX_STORED_BATCHES = 50;
    private static final long BATCH_TTL_MILLIS = 2L * 60 * 60 * 1000;

    public enum ItemState { QUEUED, PROCESSING, DONE, FAILED }

    /**
     * @param parseStatus PENDING, PARSED, UNREADABLE (stored, but no text could be read) or FAILED
     * @param matchScore  0-100 screening score for the batch's job; null unless PARSED
     */
    public record ItemView(int index, String fileName, ItemState state, String parseStatus, String studentPrn,
                           String studentName, Double matchScore, String eligibility, String message) {}

    public record BatchView(String batchId, String jobId, int expected, int added, int done, int failed,
                            int inProgress, boolean complete, List<ItemView> items) {}

    private final ResumeScreeningService resumeService;
    private final StudentService studentService;
    private final JobService jobService;
    private final EligibilityProvider eligibility;
    private final Map<String, Batch> batches = new ConcurrentHashMap<>();
    private final ThreadPoolExecutor executor = newExecutor();

    public BatchResumeService() {
        this(new ResumeScreeningService(), new StudentService(), new JobService());
    }

    public BatchResumeService(ResumeScreeningService resumeService, StudentService studentService, JobService jobService) {
        this(resumeService, studentService, jobService, EligibilityProvider.fromStudentService(studentService));
    }

    public BatchResumeService(ResumeScreeningService resumeService, StudentService studentService,
                              JobService jobService, EligibilityProvider eligibility) {
        this.resumeService = resumeService;
        this.studentService = studentService;
        this.jobService = jobService;
        this.eligibility = eligibility;
    }

    // ------------------------------------------------------------------
    // public API
    // ------------------------------------------------------------------

    /** @return the new batch id. @throws IllegalArgumentException with a user-safe message */
    public String startBatch(int ownerId, String jobId, int expectedFiles) throws Exception {
        if (expectedFiles < 1 || expectedFiles > MAX_FILES_PER_BATCH) {
            throw new IllegalArgumentException("A batch must contain between 1 and " + MAX_FILES_PER_BATCH + " resumes.");
        }
        if (jobId == null || jobId.isBlank()) throw new IllegalArgumentException("Select a job for the batch.");
        JobPosting job = jobService.getJob(jobId.trim());
        if (job == null) throw new IllegalArgumentException("Job not found: " + jobId.trim());

        purgeExpired();
        if (batches.size() >= MAX_STORED_BATCHES) {
            throw new IllegalArgumentException("Too many batches are being processed. Please try again in a few minutes.");
        }

        Batch batch = new Batch(UUID.randomUUID().toString(), ownerId, job, expectedFiles,
                eligibility.eligibleStudentIds(job));
        for (Student s : studentService.getAllStudents()) {
            batch.index(s.getPrn(), s);
            batch.index(s.getEmail(), s);
        }
        batches.put(batch.id, batch);
        return batch.id;
    }

    /**
     * Queues one file for processing. Returns its 1-based position in the batch. A problem with the FILE itself
     * (empty, wrong type, unknown student, unreadable ...) is reported in that file's status, not thrown.
     *
     * @throws IllegalArgumentException if the batch is unknown / expired / full / owned by someone else
     */
    public int addFile(String batchId, int ownerId, String fileName, byte[] bytes) {
        Batch batch = batches.get(batchId);
        if (batch == null) throw new IllegalArgumentException("Batch not found or expired.");
        if (batch.ownerId != ownerId) throw new IllegalArgumentException("This batch belongs to another user.");

        Item item;
        synchronized (batch) {
            if (batch.items.size() >= batch.expected) {
                throw new IllegalArgumentException("Batch is full (" + batch.expected + " files).");
            }
            item = new Item(batch.items.size() + 1, displayName(fileName));
            batch.items.add(item);
        }

        if (bytes == null || bytes.length == 0) {
            fail(item, "The uploaded file is empty.");
            return item.index;
        }
        final Item queued = item;
        try {
            executor.execute(() -> process(batch, queued, fileName, bytes));
        } catch (RejectedExecutionException e) {
            fail(item, "The server is busy. Please upload this file again shortly.");
        }
        return item.index;
    }

    /** Snapshot of a batch. @throws IllegalArgumentException if it does not exist (or expired) */
    public BatchView status(String batchId) {
        Batch batch = batches.get(batchId);
        if (batch == null) throw new IllegalArgumentException("Batch not found or expired.");

        List<ItemView> views = new ArrayList<>();
        int done = 0, failed = 0, inProgress = 0;
        for (Item item : batch.items) {
            ItemView v = item.view();
            views.add(v);
            switch (v.state()) {
                case DONE -> done++;
                case FAILED -> failed++;
                default -> inProgress++;
            }
        }
        boolean complete = views.size() == batch.expected && inProgress == 0;
        return new BatchView(batch.id, batch.job.getId(), batch.expected, views.size(), done, failed, inProgress, complete, views);
    }

    /** Stops the worker pool (used by tests / server shutdown). */
    public void shutdown() {
        executor.shutdownNow();
    }

    // ------------------------------------------------------------------
    // per-file processing - everything below runs on a worker thread
    // ------------------------------------------------------------------

    private void process(Batch batch, Item item, String originalFileName, byte[] bytes) {
        item.state = ItemState.PROCESSING;
        try {
            Student student = batch.match(originalFileName);
            item.studentPrn = nz(student.getPrn());
            item.studentName = nz(student.getName());
            item.eligibility = EligibilityProvider.label(batch.eligibleIds.contains(student.getStudentId()));

            boolean replaced = resumeService.hasResume(student.getStudentId());
            ResumeData parsed = resumeService.uploadResume(student.getStudentId(), originalFileName, bytes);

            if (parsed.isEmpty()) {
                item.parseStatus = "UNREADABLE";
                item.message = "Saved, but no readable text was found (scanned or unsupported PDF). Not scored.";
                item.state = ItemState.DONE;
                return;
            }

            ResumeScreeningService.Outcome outcome = resumeService.screenAndSave(student, batch.job);
            item.matchScore = Math.round(outcome.screening().getTotalScore() * 100.0) / 100.0;
            item.parseStatus = "PARSED";
            item.message = replaced ? "Parsed and scored. Replaced the student's previous resume." : "Parsed and scored.";
            item.state = ItemState.DONE;
        } catch (IllegalArgumentException e) {
            fail(item, e.getMessage());                    // validation messages are written for end users
        } catch (Throwable t) {
            System.out.println("Batch item '" + item.fileName + "' failed: " + t);
            fail(item, "Processing failed for this file. Please upload it again.");
        }
    }

    private static void fail(Item item, String message) {
        item.matchScore = null;
        item.parseStatus = "FAILED";
        item.message = message == null ? "Processing failed." : message;
        item.state = ItemState.FAILED;
    }

    private void purgeExpired() {
        long cutoff = System.currentTimeMillis() - BATCH_TTL_MILLIS;
        batches.values().removeIf(b -> b.createdAt < cutoff);
    }

    private static ThreadPoolExecutor newExecutor() {
        AtomicInteger counter = new AtomicInteger(1);
        return new ThreadPoolExecutor(WORKER_THREADS, WORKER_THREADS, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                r -> {
                    Thread t = new Thread(r, "resume-batch-" + counter.getAndIncrement());
                    t.setDaemon(true);                      // never blocks server shutdown
                    return t;
                },
                new ThreadPoolExecutor.AbortPolicy());
    }

    /** File name as shown to staff: no directory part, no protocol-breaking characters, bounded length. */
    private static String displayName(String fileName) {
        String base = baseName(fileName).replaceAll("[|\\p{Cntrl}]", "_");
        return base.length() > 120 ? base.substring(base.length() - 120) : base;
    }

    private static String baseName(String name) {
        if (name == null) return "";
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        return (slash >= 0 ? name.substring(slash + 1) : name).strip();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    // ------------------------------------------------------------------
    // state holders
    // ------------------------------------------------------------------

    private static final class Item {
        final int index;
        final String fileName;
        volatile ItemState state = ItemState.QUEUED;
        volatile String parseStatus = "PENDING";
        volatile String studentPrn = "";
        volatile String studentName = "";
        volatile String eligibility = "";
        volatile String message = "Waiting to be processed.";
        volatile Double matchScore;

        Item(int index, String fileName) {
            this.index = index;
            this.fileName = fileName;
        }

        ItemView view() {
            return new ItemView(index, fileName, state, parseStatus, studentPrn, studentName, matchScore, eligibility, message);
        }
    }

    private static final class Batch {
        final String id;
        final int ownerId;
        final JobPosting job;
        final int expected;
        final long createdAt = System.currentTimeMillis();
        final Set<Integer> eligibleIds;
        final List<Item> items = new CopyOnWriteArrayList<>();
        private final Map<String, Student> byKey = new HashMap<>();   // lower-case PRN / e-mail -> student
        private final Set<String> ambiguousKeys = new HashSet<>();

        Batch(String id, int ownerId, JobPosting job, int expected, Set<Integer> eligibleIds) {
            this.id = id;
            this.ownerId = ownerId;
            this.job = job;
            this.expected = expected;
            this.eligibleIds = eligibleIds;
        }

        void index(String key, Student student) {
            if (key == null || key.isBlank()) return;
            String k = key.trim().toLowerCase(Locale.ROOT);
            Student existing = byKey.putIfAbsent(k, student);
            if (existing != null && existing.getStudentId() != student.getStudentId()) ambiguousKeys.add(k);
        }

        /** The student a file belongs to, judged by its name. @throws IllegalArgumentException if none / several match */
        Student match(String fileName) {
            String base = baseName(fileName);
            int dot = base.lastIndexOf('.');
            String stem = (dot > 0 ? base.substring(0, dot) : base).trim();

            List<String> keys = new ArrayList<>();
            keys.add(stem);
            for (String token : stem.split("[_\\s]+")) {
                if (!token.isBlank()) keys.add(token);
            }

            Map<Integer, Student> found = new HashMap<>();
            for (String key : keys) {
                String k = key.toLowerCase(Locale.ROOT);
                if (ambiguousKeys.contains(k)) throw new IllegalArgumentException("The file name matches more than one student.");
                Student s = byKey.get(k);
                if (s != null) found.put(s.getStudentId(), s);
            }
            if (found.isEmpty()) {
                throw new IllegalArgumentException(
                        "The file name must contain the student's PRN or e-mail (for example B24CE1001.pdf).");
            }
            if (found.size() > 1) throw new IllegalArgumentException("The file name matches more than one student.");
            return found.values().iterator().next();
        }
    }
}
