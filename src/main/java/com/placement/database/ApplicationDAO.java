package com.placement.database;
import com.placement.models.Application;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
public class ApplicationDAO {
    public boolean exists(String jobId,int studentId)throws SQLException{try(Connection c=DatabaseConnection.getConnection();PreparedStatement ps=c.prepareStatement("SELECT 1 FROM applications WHERE job_id=? AND student_id=?")){ps.setString(1,jobId);ps.setInt(2,studentId);try(ResultSet rs=ps.executeQuery()){return rs.next();}}}
    public long insert(String jobId,int studentId)throws SQLException{try(Connection c=DatabaseConnection.getConnection();PreparedStatement ps=c.prepareStatement("INSERT INTO applications(job_id,student_id,status) VALUES(?,?,?)",Statement.RETURN_GENERATED_KEYS)){ps.setString(1,jobId);ps.setInt(2,studentId);ps.setString(3,"APPLIED");ps.executeUpdate();try(ResultSet rs=ps.getGeneratedKeys()){return rs.next()?rs.getLong(1):0;}}}
    public List<Application> findByStudent(int studentId)throws SQLException{
        String sql="SELECT a.application_id,a.job_id,c.company_name,j.job_profile,a.status,COALESCE(a.screening_score,0),a.applied_at FROM applications a JOIN job_postings j ON j.job_id=a.job_id JOIN company c ON c.company_id=j.company_id WHERE a.student_id=? ORDER BY a.applied_at DESC";
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement ps=c.prepareStatement(sql)){ps.setInt(1,studentId);try(ResultSet rs=ps.executeQuery()){List<Application> out=new ArrayList<>();while(rs.next()){Application a=new Application();a.setId(rs.getLong(1));a.setJobId(rs.getString(2));a.setCompany(rs.getString(3));a.setRole(rs.getString(4));a.setStatus(rs.getString(5));a.setScreeningScore(rs.getDouble(6));a.setAppliedAt(rs.getTimestamp(7).toString());out.add(a);}return out;}}}
}
