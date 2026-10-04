package com.placement.database;

import com.placement.models.Student;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StudentDAO {
    private static final String BASE_SELECT =
            "SELECT student_id, prn, name, email, password, department, cgpa, passing_year, " +
            "backlogs, semester, phone, skills FROM students ";

    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT 1 FROM students WHERE LOWER(email)=LOWER(?) LIMIT 1";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public Student findByEmail(String email) throws SQLException {
        String sql = BASE_SELECT + "WHERE LOWER(email)=LOWER(?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public List<Student> findAll() throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(BASE_SELECT + "ORDER BY student_id");
             ResultSet rs = ps.executeQuery()) {
            List<Student> list = new ArrayList<>();
            while (rs.next()) list.add(map(rs));
            return list;
        }
    }

    public int insert(Student s) throws SQLException {
        String sql = "INSERT INTO students " +
                "(prn,name,email,password,department,cgpa,passing_year,backlogs,semester,phone,skills) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getPrn());
            ps.setString(2, s.getName());
            ps.setString(3, s.getEmail());
            ps.setString(4, s.getPassword());
            ps.setString(5, s.getDepartment());
            ps.setDouble(6, s.getCgpa());
            ps.setInt(7, s.getPassingYear());
            ps.setInt(8, s.getBacklogs());
            ps.setInt(9, s.getSemester());
            ps.setString(10, s.getPhone());
            ps.setString(11, s.getSkills());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private Student map(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setStudentId(rs.getInt("student_id"));
        s.setPrn(rs.getString("prn"));
        s.setName(rs.getString("name"));
        s.setEmail(rs.getString("email"));
        s.setPassword(rs.getString("password"));
        s.setDepartment(rs.getString("department"));
        s.setCgpa(rs.getDouble("cgpa"));
        s.setPassingYear(rs.getInt("passing_year"));
        s.setBacklogs(rs.getInt("backlogs"));
        s.setSemester(rs.getInt("semester"));
        s.setPhone(rs.getString("phone"));
        s.setSkills(rs.getString("skills"));
        return s;
    }
}
