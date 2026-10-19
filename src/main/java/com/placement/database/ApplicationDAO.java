package com.placement.database;
import com.placement.models.Application;
import com.placement.models.Student;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
public class ApplicationDAO {
    public boolean exists(String jobId,int studentId)throws SQLException{try(Connection c=DatabaseConnection.getConnection();PreparedStatement ps=c.prepareStatement("SELECT 1 FROM applications WHERE job_id=? AND student_id=?")){ps.setString(1,jobId);ps.setInt(2,studentId);try(ResultSet rs=ps.executeQuery()){return rs.next();}}}
    public long insert(String jobId,int studentId)throws SQLException{try(Connection c=DatabaseConnection.getConnection();PreparedStatement ps=c.prepareStatement("INSERT INTO applications(job_id,student_id,status) VALUES(?,?,?)",Statement.RETURN_GENERATED_KEYS)){ps.setString(1,jobId);ps.setInt(2,studentId);ps.setString(3,"APPLIED");ps.executeUpdate();try(ResultSet rs=ps.getGeneratedKeys()){return rs.next()?rs.getLong(1):0;}}}
    public List<Application> findByStudent(int studentId)throws SQLException{
        String sql="SELECT a.application_id,a.job_id,c.company_name,j.job_profile,a.status,COALESCE(a.screening_score,0),a.applied_at FROM applications a JOIN job_postings j ON j.job_id=a.job_id JOIN company c ON c.company_id=j.company_id WHERE a.student_id=? ORDER BY a.applied_at DESC";
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement ps=c.prepareStatement(sql)){ps.setInt(1,studentId);try(ResultSet rs=ps.executeQuery()){List<Application> out=new ArrayList<>();while(rs.next()){Application a=new Application();a.setId(rs.getLong(1));a.setJobId(rs.getString(2));a.setCompany(rs.getString(3));a.setRole(rs.getString(4));a.setStatus(rs.getString(5));a.setScreeningScore(rs.getDouble(6));a.setAppliedAt(rs.getTimestamp(7).toString());out.add(a);}return out;}}}

    /**
     * Stores the screening score on the student's EXISTING application for this job; does nothing when the
     * student has not applied. It used to insert a 'SCREENED' row in that case, but a row in {@code applications}
     * must mean "the student applied": otherwise screening a non-applicant made them show up in the job's
     * applicant list and blocked their own later "APPLY_JOB" ("You have already applied").
     */
    public void saveScreeningScore(String jobId, int studentId, double score) throws SQLException {
        String update = "UPDATE applications SET screening_score=?, status=CASE WHEN status='APPLIED' THEN 'SCREENED' ELSE status END WHERE job_id=? AND student_id=?";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(update)) {
            ps.setDouble(1, score); ps.setString(2, jobId); ps.setInt(3, studentId);
            ps.executeUpdate();
        }
    }

    /** One student's application for a job, with the student's details (never the password hash). */
    public record Applicant(Student student, long applicationId, String status, Double screeningScore, String appliedAt) {}

    private static final DateTimeFormatter APPLIED_AT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Everyone with an application for this job, earliest first. screeningScore is null until the application has been screened. */
    public List<Applicant> findApplicantsByJob(String jobId) throws SQLException {
        String sql = "SELECT s.student_id, s.prn, s.name, s.email, s.department, s.cgpa, s.passing_year, s.backlogs, s.semester, s.phone, s.skills, " +
                "a.application_id, a.status, a.screening_score, a.applied_at " +
                "FROM applications a JOIN students s ON s.student_id = a.student_id " +
                "WHERE a.job_id = ? ORDER BY a.applied_at, a.application_id";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Applicant> out = new ArrayList<>();
                while (rs.next()) {
                    Student s = new Student();
                    s.setStudentId(rs.getInt("student_id")); s.setPrn(rs.getString("prn")); s.setName(rs.getString("name"));
                    s.setEmail(rs.getString("email")); s.setDepartment(rs.getString("department")); s.setCgpa(rs.getDouble("cgpa"));
                    s.setPassingYear(rs.getInt("passing_year")); s.setBacklogs(rs.getInt("backlogs")); s.setSemester(rs.getInt("semester"));
                    s.setPhone(rs.getString("phone")); s.setSkills(rs.getString("skills"));
                    double score = rs.getDouble("screening_score");
                    Double screening = rs.wasNull() ? null : score;
                    Timestamp at = rs.getTimestamp("applied_at");
                    out.add(new Applicant(s, rs.getLong("application_id"), rs.getString("status"), screening,
                            at == null ? "" : at.toLocalDateTime().format(APPLIED_AT)));
                }
                return out;
            }
        }
    }
}

