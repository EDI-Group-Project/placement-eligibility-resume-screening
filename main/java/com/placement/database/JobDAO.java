package com.placement.database;

import com.placement.models.JobPosting;
import java.sql.*;
import java.util.*;

public class JobDAO {
    private static final String SELECT =
            "SELECT j.job_id,j.company_id,c.company_name,j.job_profile,j.min_package,j.deadline," +
            "e.min_cgpa,e.max_backlogs,e.department,e.passing_year,e.required_skills " +
            "FROM job_postings j JOIN company c ON c.company_id=j.company_id " +
            "JOIN eligibility e ON e.eligibility_id=j.eligibility_id ";

    public List<JobPosting> findAll() throws SQLException {
        try (Connection c=DatabaseConnection.getConnection();
             PreparedStatement ps=c.prepareStatement(SELECT+"WHERE j.is_active=TRUE ORDER BY j.deadline,j.job_id");
             ResultSet rs=ps.executeQuery()) {
            List<JobPosting> list=new ArrayList<>();
            while(rs.next()) list.add(map(rs));
            return list;
        }
    }

    public JobPosting findById(String id) throws SQLException {
        try (Connection c=DatabaseConnection.getConnection();
             PreparedStatement ps=c.prepareStatement(SELECT+"WHERE j.job_id=? AND j.is_active=TRUE")) {
            ps.setString(1,id);
            try(ResultSet rs=ps.executeQuery()){ return rs.next()?map(rs):null; }
        }
    }

    public JobPosting insert(JobPosting j, int createdByFacultyId) throws SQLException {
        try(Connection c=DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                long eligibilityId;
                String eSql="INSERT INTO eligibility(company_id,min_cgpa,max_backlogs,department,passing_year,required_skills,created_by) VALUES(?,?,?,?,?,?,?)";
                try(PreparedStatement ps=c.prepareStatement(eSql,Statement.RETURN_GENERATED_KEYS)){
                    ps.setInt(1,j.getCompanyId()); ps.setDouble(2,j.getMinCgpa()); ps.setInt(3,j.getMaxBacklogs());
                    ps.setString(4,String.join(",",j.getBranches())); ps.setInt(5,j.getPassingYear());
                    ps.setString(6,String.join(",",j.getRequiredSkills()==null?List.of():j.getRequiredSkills()));
                    ps.setInt(7,createdByFacultyId); ps.executeUpdate();
                    try(ResultSet rs=ps.getGeneratedKeys()){ if(!rs.next()) throw new SQLException("Eligibility ID not generated"); eligibilityId=rs.getLong(1); }
                }
                String sql="INSERT INTO job_postings(job_id,company_id,eligibility_id,job_profile,min_package,deadline) VALUES(?,?,?,?,?,?)";
                try(PreparedStatement ps=c.prepareStatement(sql)){
                    ps.setString(1,j.getId()); ps.setInt(2,j.getCompanyId()); ps.setLong(3,eligibilityId);
                    ps.setString(4,j.getRole()); ps.setString(5,j.getMinPackage()); ps.setString(6,j.getDeadline()); ps.executeUpdate();
                }
                c.commit(); return j;
            } catch(SQLException e){c.rollback();throw e;} finally{c.setAutoCommit(true);}
        }
    }

    private JobPosting map(ResultSet rs)throws SQLException{
        JobPosting j=new JobPosting();
        j.setId(rs.getString("job_id")); j.setCompanyId(rs.getInt("company_id"));
        j.setCompany(rs.getString("company_name")); j.setRole(rs.getString("job_profile"));
        j.setMinPackage(rs.getString("min_package")); j.setMinCgpa(rs.getDouble("min_cgpa"));
        String b=rs.getString("department"); j.setBranches(csv(b));
        j.setMaxBacklogs(rs.getInt("max_backlogs")); j.setPassingYear(rs.getInt("passing_year"));
        j.setRequiredSkills(csv(rs.getString("required_skills"))); j.setDeadline(rs.getString("deadline")); return j;
    }
    private List<String> csv(String raw){return raw==null||raw.isBlank()?List.of():Arrays.stream(raw.split(",")).map(String::trim).filter(s->!s.isBlank()).toList();}
}
