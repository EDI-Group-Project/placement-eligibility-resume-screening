package com.placement.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.placement.models.Student;

public class StudentDAO {

    public boolean insertStudent(Student student) {

        String query = "INSERT INTO students "
                + "(name, email, password, department, cgpa, passing_year, "
                + "backlogs, semester, phone, skills) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, student.getName());
            statement.setString(2, student.getEmail());
            statement.setString(3, student.getPassword());
            statement.setString(4, student.getDepartment());
            statement.setDouble(5, student.getCgpa());
            statement.setInt(6, student.getPassingYear());
            statement.setInt(7, student.getBacklogs());
            statement.setInt(8, student.getSemester());
            statement.setString(9, student.getPhone());
            statement.setString(10, student.getSkills());

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("DB error on insert: " + e.getMessage());
            return false;
        }
    }

    public boolean emailExists(String email) {

        String query = "SELECT student_id FROM students WHERE email = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, email);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }

        } catch (SQLException e) {
            System.out.println("DB error on lookup: " + e.getMessage());
            return false;
        }
    }
}