package com.placement.database;

import com.placement.models.Faculty;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FacultyDAO {
    public Faculty findByEmailAndRole(String email,String role)throws SQLException{
        String sql="SELECT faculty_id,name,email,password,role,department,phone FROM faculty WHERE LOWER(email)=LOWER(?) AND UPPER(role)=UPPER(?) AND is_active=TRUE";
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement ps=c.prepareStatement(sql)){
            ps.setString(1,email);ps.setString(2,role);try(ResultSet rs=ps.executeQuery()){return rs.next()?map(rs):null;}
        }
    }
    public List<Faculty> findAll()throws SQLException{
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement ps=c.prepareStatement("SELECT faculty_id,name,email,password,role,department,phone FROM faculty WHERE is_active=TRUE ORDER BY role,name");ResultSet rs=ps.executeQuery()){
            List<Faculty> list=new ArrayList<>();while(rs.next())list.add(map(rs));return list;
        }
    }
    public int insert(Faculty f)throws SQLException{
        String sql="INSERT INTO faculty(name,email,password,role,department,phone) VALUES(?,?,?,?,?,?)";
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement ps=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){
            ps.setString(1,f.getName());ps.setString(2,f.getEmail());ps.setString(3,f.getPassword());ps.setString(4,f.getRole());ps.setString(5,f.getDepartment());ps.setString(6,f.getPhone());ps.executeUpdate();
            try(ResultSet rs=ps.getGeneratedKeys()){return rs.next()?rs.getInt(1):0;}
        }
    }
    private Faculty map(ResultSet rs)throws SQLException{
        Faculty f=new Faculty();f.setFacultyId(rs.getInt("faculty_id"));f.setName(rs.getString("name"));f.setEmail(rs.getString("email"));f.setPassword(rs.getString("password"));f.setRole(rs.getString("role"));f.setDepartment(rs.getString("department"));f.setPhone(rs.getString("phone"));return f;
    }
}
