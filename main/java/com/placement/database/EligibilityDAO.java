package com.placement.database;

import com.placement.models.JobPosting;
import com.placement.models.Student;
import java.sql.*;
import java.util.*;

public class EligibilityDAO {
    public List<Student> findEligible(JobPosting job) throws SQLException {
        String sql = """
            SELECT s.student_id,s.prn,s.name,s.email,s.password,s.department,s.cgpa,s.passing_year,
                   s.backlogs,s.semester,s.phone,s.skills
            FROM students s
            WHERE s.cgpa >= ?
              AND s.backlogs <= ?
              AND s.passing_year = ?
              AND FIND_IN_SET(UPPER(s.department), REPLACE(UPPER(?),' ',''))
              AND NOT EXISTS (
                    SELECT 1
                    FROM notification_recipients nr
                    JOIN notifications n ON n.notification_id=nr.notification_id
                    WHERE nr.student_id=s.student_id AND n.job_id=?
              )
            ORDER BY s.cgpa DESC, s.name
            """;
        try(Connection c=DatabaseConnection.getConnection(); PreparedStatement ps=c.prepareStatement(sql)){
            ps.setDouble(1,job.getMinCgpa()); ps.setInt(2,job.getMaxBacklogs()); ps.setInt(3,job.getPassingYear());
            ps.setString(4,String.join(",",job.getBranches())); ps.setString(5,job.getId());
            try(ResultSet rs=ps.executeQuery()){
                List<Student> list=new ArrayList<>(); while(rs.next()){
                    Student s=map(rs);
                    if(skillsMatch(s.getSkills(), job.getRequiredSkills())) list.add(s);
                }
                return list;
            }
        }
    }
    private boolean skillsMatch(String studentSkills, List<String> required){
        if(required==null||required.isEmpty()) return true;
        Set<String> student=new HashSet<>();
        if(studentSkills!=null) for(String x:studentSkills.split(",")) student.add(x.trim().toLowerCase());
        for(String r:required) if(!student.contains(r.trim().toLowerCase())) return false;
        return true;
    }
    private Student map(ResultSet rs)throws SQLException{
        Student s=new Student(); s.setStudentId(rs.getInt("student_id")); s.setPrn(rs.getString("prn"));
        s.setName(rs.getString("name")); s.setEmail(rs.getString("email")); s.setPassword(rs.getString("password"));
        s.setDepartment(rs.getString("department")); s.setCgpa(rs.getDouble("cgpa")); s.setPassingYear(rs.getInt("passing_year"));
        s.setBacklogs(rs.getInt("backlogs")); s.setSemester(rs.getInt("semester")); s.setPhone(rs.getString("phone")); s.setSkills(rs.getString("skills")); return s;
    }
}
