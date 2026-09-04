package com.placement.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.placement.models.Company;

public class CompanyDAO {

    private String storedHash;

    public Company findByHrEmail(String hrEmail) {

        String query = "SELECT company_id, company_name, hr_name, hr_email, hr_password "
                + "FROM company WHERE hr_email = ? AND is_active = TRUE";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, hrEmail);

            try (ResultSet rs = statement.executeQuery()) {

                if (!rs.next()) return null;

                Company company = new Company();
                company.setCompanyId(rs.getInt("company_id"));
                company.setCompanyName(rs.getString("company_name"));
                company.setHrName(rs.getString("hr_name"));
                company.setHrEmail(rs.getString("hr_email"));
                storedHash = rs.getString("hr_password");
                return company;
            }

        } catch (SQLException e) {
            System.out.println("DB error (company lookup): " + e.getMessage());
            return null;
        }
    }

    public String getLastStoredHash() {
        return storedHash;
    }
}