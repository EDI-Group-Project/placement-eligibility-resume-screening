package com.placement.database;

import com.placement.models.Notification;
import java.sql.*;
import java.util.*;

public class NotificationDAO {
    public Notification insert(String jobId, String subject, String message, String group, List<Integer> studentIds)
            throws SQLException {
        try (Connection c=DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                long id;
                String sql="INSERT INTO notifications(job_id,subject,message,recipient_group,recipient_count) VALUES(?,?,?,?,?)";
                try(PreparedStatement ps=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){
                    if(jobId==null||jobId.isBlank()) ps.setNull(1,Types.VARCHAR); else ps.setString(1,jobId);
                    ps.setString(2,subject); ps.setString(3,message); ps.setString(4,group); ps.setInt(5,studentIds.size());
                    ps.executeUpdate();
                    try(ResultSet rs=ps.getGeneratedKeys()){ if(!rs.next()) throw new SQLException("Notification ID not generated"); id=rs.getLong(1); }
                }
                if(!studentIds.isEmpty()){
                    try(PreparedStatement ps=c.prepareStatement(
                            "INSERT IGNORE INTO notification_recipients(notification_id,student_id) VALUES(?,?)")){
                        for(int studentId:studentIds){ ps.setLong(1,id); ps.setInt(2,studentId); ps.addBatch(); }
                        ps.executeBatch();
                    }
                }
                c.commit();
                return findById(id);
            } catch(SQLException e){ c.rollback(); throw e; } finally { c.setAutoCommit(true); }
        }
    }

    public List<Notification> findAll() throws SQLException{
        String sql="SELECT notification_id,job_id,subject,message,recipient_group,recipient_count,created_at FROM notifications ORDER BY created_at DESC";
        try(Connection c=DatabaseConnection.getConnection();
            PreparedStatement ps=c.prepareStatement(sql);
            ResultSet rs=ps.executeQuery()){
            List<Notification> list=new ArrayList<>(); while(rs.next()) list.add(map(rs)); return list;
        }
    }
    private Notification findById(long id)throws SQLException{
        String sql="SELECT notification_id,job_id,subject,message,recipient_group,recipient_count,created_at FROM notifications WHERE notification_id=?";
        try(Connection c=DatabaseConnection.getConnection(); PreparedStatement ps=c.prepareStatement(sql)){ps.setLong(1,id);try(ResultSet rs=ps.executeQuery()){return rs.next()?map(rs):null;}}
    }
    private Notification map(ResultSet rs)throws SQLException{
        Notification n=new Notification(); n.setId(rs.getLong("notification_id")); n.setJobId(rs.getString("job_id"));
        n.setSubject(rs.getString("subject")); n.setMessage(rs.getString("message")); n.setRecipientGroup(rs.getString("recipient_group"));
        n.setRecipientCount(rs.getInt("recipient_count")); n.setCreatedAt(rs.getTimestamp("created_at").toString()); return n;
    }
}
