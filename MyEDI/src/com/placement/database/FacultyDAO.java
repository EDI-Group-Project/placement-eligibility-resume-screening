package com.placement.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.placement.models.Faculty;

public class FacultyDAO {

    // returns the faculty's stored password hash, or null if no match on email+role
    public Faculty findByEmailAndRole(String email, String role) {

        String query = "SELECT faculty_id, name, email, password, role, department, phone "
                + "FROM faculty WHERE email = ? AND role = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, email);
            statement.setString(2, role);

            try (ResultSet rs = statement.executeQuery()) {

                if (!rs.next()) return null;

                Faculty faculty = new Faculty();
                faculty.setFacultyId(rs.getInt("faculty_id"));
                faculty.setName(rs.getString("name"));
                faculty.setEmail(rs.getString("email"));
                faculty.setRole(rs.getString("role"));
                faculty.setDepartment(rs.getString("department"));
                faculty.setPhone(rs.getString("phone"));
                // stash password hash temporarily for verification by caller
                storedHash = rs.getString("password");
                return faculty;
            }

        } catch (SQLException e) {
            System.out.println("DB error (faculty lookup): " + e.getMessage());
            return null;
        }
    }

    private String storedHash;

    public String getLastStoredHash() {
        return storedHash;
    }
}