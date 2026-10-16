package com.placement.testsupport;

import com.placement.database.ApplicationDAO;
import com.placement.database.ResumeDAO;
import com.placement.models.JobPosting;
import com.placement.models.Student;
import com.placement.services.JobService;
import com.placement.services.StudentService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Shared test helpers: in-memory fakes for the DAOs and services (so no MySQL is needed) plus a tiny
 * independent .xlsx reader used to check exported files.
 */
public final class TestSupport {

    private TestSupport() {}

    // ------------------------------------------------------------------ models

    /** Student whose PRN and phone are set without needing setters (the real Student's setters are not assumed). */
    public static class TestStudent extends Student {
        private final String prn;
        private final String phone;

        public TestStudent(String prn, String phone) {
            this.prn = prn;
            this.phone = phone;
        }

        @Override public String getPrn() { return prn; }
        @Override public String getPhone() { return phone; }
    }

    public static Student student(int id, String name, String prn, String email, String dept, double cgpa, int backlogs, String phone) {
        TestStudent s = new TestStudent(prn, phone);
        s.setStudentId(id);
        s.setName(name);
        s.setEmail(email);
        s.setDepartment(dept);
        s.setCgpa(cgpa);
        s.setBacklogs(backlogs);
        return s;
    }

    public static Student student(int id, String name) {
        return student(id, name, "PRN" + id, name.toLowerCase().replace(' ', '.') + "@college.edu", "Computer", 8.0, 0, "9000000000");
    }

    public static JobPosting job(String id, String role, String... requiredSkills) {
        JobPosting j = new JobPosting();
        j.setId(id);
        j.setCompany("Acme");
        j.setRole(role);
        j.setRequiredSkills(List.of(requiredSkills));
        return j;
    }

    // ------------------------------------------------------------------ fakes

    public static class FakeResumeDAO extends ResumeDAO {
        public final Map<Integer, ResumeRecord> rows = new HashMap<>();
        public final Set<Integer> failSaveFor = new HashSet<>();
        public int findAllCalls = 0;
        private long nextId = 1;

        @Override
        public void saveOrUpdate(int studentId, String fileName, String filePath, String text) throws SQLException {
            if (failSaveFor.contains(studentId)) throw new SQLException("simulated database failure");
            rows.put(studentId, new ResumeRecord(nextId++, studentId, fileName, filePath, text, new Timestamp(0)));
        }

        @Override public ResumeRecord findByStudentId(int studentId) throws SQLException { return rows.get(studentId); }
        @Override public boolean existsForStudent(int studentId) throws SQLException { return rows.containsKey(studentId); }
        @Override public String findExtractedTextByStudentId(int studentId) throws SQLException {
            ResumeRecord r = rows.get(studentId);
            return r == null ? null : r.extractedText();
        }
        @Override public Map<Integer, ResumeRecord> findAll() throws SQLException {
            findAllCalls++;
            return new HashMap<>(rows);
        }
        @Override public Set<Integer> findStudentIdsWithResume() throws SQLException { return new HashSet<>(rows.keySet()); }

        /** Puts resume text straight into the "database" (as if it had been uploaded earlier). */
        public void put(int studentId, String text) {
            rows.put(studentId, new ResumeRecord(nextId++, studentId, "r" + studentId + ".txt", "x", text, new Timestamp(0)));
        }
    }

    public static class FakeApplicationDAO extends ApplicationDAO {
        public final List<ApplicationRecord> records = new ArrayList<>();
        private long nextId = 1;

        /** Seeds an application as if the student had applied. */
        public void addApplication(String jobId, int studentId, String status, Double score, Timestamp appliedAt) {
            records.add(new ApplicationRecord(nextId++, jobId, studentId, status, score, appliedAt, appliedAt));
        }

        @Override public ApplicationRecord find(String jobId, int studentId) throws SQLException {
            for (ApplicationRecord r : records) if (r.jobId().equals(jobId) && r.studentId() == studentId) return r;
            return null;
        }

        @Override public boolean apply(String jobId, int studentId) throws SQLException {
            if (find(jobId, studentId) != null) return false;
            addApplication(jobId, studentId, "APPLIED", null, new Timestamp(0));
            return true;
        }

        /** Update-only, exactly like the real DAO: never creates a row. */
        @Override public boolean saveScreeningScore(String jobId, int studentId, double score) throws SQLException {
            for (int i = 0; i < records.size(); i++) {
                ApplicationRecord r = records.get(i);
                if (r.jobId().equals(jobId) && r.studentId() == studentId) {
                    records.set(i, new ApplicationRecord(r.applicationId(), jobId, studentId, r.status(), score, r.appliedAt(), r.updatedAt()));
                    return true;
                }
            }
            return false;
        }

        @Override public List<ApplicationRecord> findByJob(String jobId) throws SQLException {
            List<ApplicationRecord> out = new ArrayList<>();
            for (ApplicationRecord r : records) if (r.jobId().equals(jobId)) out.add(r);
            return out;
        }
    }

    public static class FakeStudentService extends StudentService {
        public final List<Student> students = new ArrayList<>();
        /** Ids that the "existing eligibility logic" reports as eligible and not yet notified. */
        public final Set<Integer> eligibleIds = new HashSet<>();
        public volatile int lastLookup = -1;

        @Override public List<Student> getAllStudents() { return new ArrayList<>(students); }

        @Override public Student findStudentById(int id) {
            lastLookup = id;
            for (Student s : students) if (s.getStudentId() == id) return s;
            return null;
        }

        @Override public List<Student> getEligibleUnnotifiedStudents(JobPosting job) {
            List<Student> out = new ArrayList<>();
            for (Student s : students) if (eligibleIds.contains(s.getStudentId())) out.add(s);
            return out;
        }
    }

    public static class FakeJobService extends JobService {
        public final Map<String, JobPosting> jobs = new LinkedHashMap<>();

        public FakeJobService with(JobPosting job) {
            jobs.put(job.getId(), job);
            return this;
        }

        @Override public JobPosting getJob(String id) { return jobs.get(id); }
    }

    // ------------------------------------------------------------------ independent .xlsx reader

    /** Cell reference (A1, B2 ...) -> text, read straight from the zip/XML so it checks the real file format. */
    public static Map<String, String> readXlsx(byte[] xlsx) throws Exception {
        Document sheet = parse(entry(xlsx, "xl/worksheets/sheet1.xml"));
        Map<String, String> cells = new LinkedHashMap<>();
        NodeList nodes = sheet.getElementsByTagNameNS("*", "c");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element c = (Element) nodes.item(i);
            String text;
            if ("inlineStr".equals(c.getAttribute("t"))) {
                text = c.getElementsByTagNameNS("*", "t").item(0).getTextContent();
            } else {
                text = c.getElementsByTagNameNS("*", "v").item(0).getTextContent();
            }
            cells.put(c.getAttribute("r"), text);
        }
        return cells;
    }

    public static String sheetName(byte[] xlsx) throws Exception {
        Document wb = parse(entry(xlsx, "xl/workbook.xml"));
        return ((Element) wb.getElementsByTagNameNS("*", "sheet").item(0)).getAttribute("name");
    }

    public static byte[] entry(byte[] zip, String name) throws Exception {
        try (ZipInputStream z = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry e;
            while ((e = z.getNextEntry()) != null) {
                if (e.getName().equals(name)) return z.readAllBytes();
            }
        }
        throw new IllegalStateException("missing zip entry " + name);
    }

    private static Document parse(byte[] xml) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        return f.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
    }
}
