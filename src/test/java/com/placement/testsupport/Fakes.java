package com.placement.testsupport;

import com.placement.database.ApplicationDAO;
import com.placement.database.ResumeDAO;
import com.placement.models.JobPosting;
import com.placement.models.Student;
import com.placement.services.JobService;
import com.placement.services.StudentService;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** In-memory stand-ins for the database-backed classes, so the resume tests need no MySQL (same approach as ResumePipelineTest). */
public final class Fakes {

    private Fakes() { }

    /** The resumes table. */
    public static class ResumeDao extends ResumeDAO {
        public final Map<Integer, ResumeRecord> rows = new HashMap<>();
        /** Students whose save fails, to simulate a database error on one file of a batch. */
        public final Set<Integer> failFor = new HashSet<>();
        public int saves;

        @Override public synchronized void saveOrUpdate(int studentId, String fileName, String filePath, String text) throws SQLException {
            if (failFor.contains(studentId)) throw new SQLException("db down");
            saves++;
            rows.put(studentId, new ResumeRecord(saves, studentId, fileName, filePath, text, new Timestamp(0)));
        }

        @Override public synchronized ResumeRecord findByStudentId(int studentId) { return rows.get(studentId); }

        @Override public synchronized boolean existsForStudent(int studentId) { return rows.containsKey(studentId); }
    }

    /** The applications table: what findApplicantsByJob returns per job, and every screening score that would have been written. */
    public static class ApplicationDao extends ApplicationDAO {
        public final Map<String, List<Applicant>> byJob = new HashMap<>();
        public final Map<String, Double> scores = new HashMap<>();

        @Override public List<Applicant> findApplicantsByJob(String jobId) throws SQLException { return byJob.getOrDefault(jobId, List.of()); }

        @Override public void saveScreeningScore(String jobId, int studentId, double score) { scores.put(jobId + "/" + studentId, score); }
    }

    /** The students table. {@code query} records its arguments and answers with {@code queryResult}. */
    public static class Students extends StudentService {
        public final List<Student> all = new ArrayList<>();
        public List<Student> queryResult = new ArrayList<>();
        public Object[] lastQuery;

        @Override public List<Student> getAllStudents() { return new ArrayList<>(all); }

        @Override public Student findStudentById(int id) {
            return all.stream().filter(s -> s.getStudentId() == id).findFirst().orElse(null);
        }

        @Override public List<Student> query(String search, String dept, int year, double minCgpa, int maxBacklogs,
                                             int semester, String sortField, boolean asc) {
            lastQuery = new Object[]{search, dept, year, minCgpa, maxBacklogs, semester, sortField, asc};
            return queryResult;
        }
    }

    /** The job_postings table. */
    public static class Jobs extends JobService {
        public final Map<String, JobPosting> jobs = new HashMap<>();

        @Override public JobPosting getJob(String id) { return jobs.get(id); }
    }

    // ------------------------------------------------------------------ builders

    public static Student student(int id, String prn, String name, String email, String dept, double cgpa,
                                  int year, int backlogs, int semester, String phone, String skills) {
        Student s = new Student();
        s.setStudentId(id); s.setPrn(prn); s.setName(name); s.setEmail(email); s.setDepartment(dept); s.setCgpa(cgpa);
        s.setPassingYear(year); s.setBacklogs(backlogs); s.setSemester(semester); s.setPhone(phone); s.setSkills(skills);
        s.setPassword("SECRET-PASSWORD-HASH");     // must never show up in any export or response
        return s;
    }

    public static JobPosting job(String id, String company, String role, String... requiredSkills) {
        JobPosting j = new JobPosting();
        j.setId(id); j.setCompany(company); j.setRole(role); j.setRequiredSkills(List.of(requiredSkills));
        return j;
    }
}
