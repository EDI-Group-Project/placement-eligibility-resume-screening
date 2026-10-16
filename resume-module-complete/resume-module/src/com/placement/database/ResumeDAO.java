package com.placement.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * DAO for the EXISTING {@code resumes} table (see sql/01_schema.sql):
 *   resume_id BIGINT PK, student_id INT UNIQUE FK, file_name, file_path,
 *   extracted_text LONGTEXT, uploaded_at TIMESTAMP.
 *
 * No schema change needed - every field this class reads/writes already
 * exists. Follows the same plain-JDBC / try-with-resources style as the
 * existing StudentDAO/JobDAO in this package, and reuses the existing
 * {@link DatabaseConnection#getConnection()} helper.
 *
 * Caching note (avoids repeated parsing per requirement in the task): a
 * caller should check {@link #findExtractedTextByStudentId(int)} first and
 * only invoke the resume parser when there is no cached text yet or the
 * student has uploaded a new file; the parsed text is then written back
 * with {@link #saveOrUpdate(int, String, String, String)} for next time.
 */
public class ResumeDAO {

    /** Minimal read-only view of one row of the resumes table. */
    public record ResumeRecord(long resumeId, int studentId, String fileName,
                                String filePath, String extractedText, Timestamp uploadedAt) {}

    private static final String SELECT =
            "SELECT resume_id, student_id, file_name, file_path, extracted_text, uploaded_at FROM resumes ";

    /** Inserts a resume row, or updates the existing one for this student (table has UNIQUE(student_id)). */
    public void saveOrUpdate(int studentId, String fileName, String filePath, String extractedText) throws SQLException {
        String sql = "INSERT INTO resumes (student_id, file_name, file_path, extracted_text) " +
                "VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE file_name = VALUES(file_name), file_path = VALUES(file_path), " +
                "extracted_text = VALUES(extracted_text), uploaded_at = CURRENT_TIMESTAMP";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setString(2, fileName);
            ps.setString(3, filePath);
            ps.setString(4, extractedText);
            ps.executeUpdate();
        }
    }

    public ResumeRecord findByStudentId(int studentId) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + "WHERE student_id = ?")) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Returns just the cached extracted_text (or null if no resume on file yet) - avoids mapping a whole record when only the text is needed. */
    public String findExtractedTextByStudentId(int studentId) throws SQLException {
        String sql = "SELECT extracted_text FROM resumes WHERE student_id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    /**
     * Every stored resume keyed by student id, in ONE query. Used by exports and batch screening so
     * scoring N students costs one database round trip instead of N.
     */
    public Map<Integer, ResumeRecord> findAll() throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT);
             ResultSet rs = ps.executeQuery()) {
            Map<Integer, ResumeRecord> all = new HashMap<>();
            while (rs.next()) {
                ResumeRecord record = map(rs);
                all.put(record.studentId(), record);
            }
            return all;
        }
    }

    /** Ids of students who have a resume on file - lightweight (no resume text is loaded). */
    public Set<Integer> findStudentIdsWithResume() throws SQLException {
        String sql = "SELECT student_id FROM resumes";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            Set<Integer> ids = new HashSet<>();
            while (rs.next()) ids.add(rs.getInt(1));
            return ids;
        }
    }

    public boolean existsForStudent(int studentId) throws SQLException {
        String sql = "SELECT 1 FROM resumes WHERE student_id = ? LIMIT 1";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private ResumeRecord map(ResultSet rs) throws SQLException {
        return new ResumeRecord(
                rs.getLong("resume_id"),
                rs.getInt("student_id"),
                rs.getString("file_name"),
                rs.getString("file_path"),
                rs.getString("extracted_text"),
                rs.getTimestamp("uploaded_at"));
    }
}
