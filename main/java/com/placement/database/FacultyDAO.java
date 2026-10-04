package com.placement.database;

import com.placement.models.Faculty;
import java.sql.*;

public class FacultyDAO {
    public Faculty findByEmailAndRole(String email, String role) throws SQLException {
        String sql = "SELECT faculty_id,name,email,password,role,department,phone FROM faculty " +
                     "WHERE LOWER(email)=LOWER(?) AND UPPER(role)=UPPER(?) AND is_active=TRUE";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, role);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Faculty f = new Faculty();
                f.setFacultyId(rs.getInt("faculty_id"));
                f.setName(rs.getString("name"));
                f.setEmail(rs.getString("email"));
                f.setPassword(rs.getString("password"));
                f.setRole(rs.getString("role"));
                f.setDepartment(rs.getString("department"));
                f.setPhone(rs.getString("phone"));
                return f;
            }
        }
    }
}
