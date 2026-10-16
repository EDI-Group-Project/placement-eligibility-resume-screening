package com.placement.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the EXISTING {@code applications} table (see sql/01_schema.sql):
 *   application_id BIGINT PK, job_id, student_id, status, screening_score
 *   DECIMAL(5,2), applied_at, updated_at, UNIQUE(job_id, student_id).
 *
 * No schema change needed - screening_score was already provisioned on this
 * table; this class is simply the first DAO to read/write it. Follows the
 * same plain-JDBC style as JobDAO/StudentDAO in this package.
 *
 * An application row means "this student applied for this job". Only apply()
 * creates one. saveScreeningScore() is UPDATE-ONLY: screening or ranking a
 * student must never turn them into an applicant, otherwise the applicant list
 * for a job would contain people who never applied. apply() relies on the
 * table's own DEFAULT for status, exactly as before.
 */
public class ApplicationDAO {

    public record ApplicationRecord(long applicationId, String jobId, int studentId, String status,
                                     Double screeningScore, Timestamp appliedAt, Timestamp updatedAt) {}

    private static final String SELECT =
            "SELECT application_id, job_id, student_id, status, screening_score, applied_at, updated_at " +
            "FROM applications ";

    /**
     * Records a student's application for a job (idempotent: applying twice keeps the single existing row).
     *
     * @return true if a new application was created, false if the student had already applied
     */
    public boolean apply(String jobId, int studentId) throws SQLException {
        if (find(jobId, studentId) != null) return false;
        // The no-op UPDATE makes a concurrent double-click harmless (UNIQUE(job_id, student_id) holds).
        String sql = "INSERT INTO applications (job_id, student_id) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE application_id = application_id";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, jobId);
            ps.setInt(2, studentId);
            ps.executeUpdate();
        }
        return true;
    }

    /**
     * Stores the screening score on an EXISTING application. Creates nothing.
     *
     * @return true if the student has applied for this job and the score was stored,
     *         false if there is no application (no row is created)
     */
    public boolean saveScreeningScore(String jobId, int studentId, double screeningScore) throws SQLException {
        String sql = "UPDATE applications SET screening_score = ? WHERE job_id = ? AND student_id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDouble(1, screeningScore);
            ps.setString(2, jobId);
            ps.setInt(3, studentId);
            return ps.executeUpdate() > 0;
        }
    }

    public Double findScreeningScore(String jobId, int studentId) throws SQLException {
        String sql = "SELECT screening_score FROM applications WHERE job_id = ? AND student_id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, jobId);
            ps.setInt(2, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                double value = rs.getDouble(1);
                return rs.wasNull() ? null : value;
            }
        }
    }

    public ApplicationRecord find(String jobId, int studentId) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + "WHERE job_id = ? AND student_id = ?")) {
            ps.setString(1, jobId);
            ps.setInt(2, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** All application rows for a job, e.g. to combine with screening results for ranking. */
    public List<ApplicationRecord> findByJob(String jobId) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + "WHERE job_id = ? ORDER BY screening_score DESC")) {
            ps.setString(1, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                List<ApplicationRecord> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    private ApplicationRecord map(ResultSet rs) throws SQLException {
        double score = rs.getDouble("screening_score");
        Double scoreOrNull = rs.wasNull() ? null : score;
        return new ApplicationRecord(
                rs.getLong("application_id"),
                rs.getString("job_id"),
                rs.getInt("student_id"),
                rs.getString("status"),
                scoreOrNull,
                rs.getTimestamp("applied_at"),
                rs.getTimestamp("updated_at"));
    }
}
