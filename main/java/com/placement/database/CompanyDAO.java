package com.placement.database;

import com.placement.models.Company;
import java.sql.*;

public class CompanyDAO {
    public Company findByHrEmail(String email) throws SQLException {
        String sql = "SELECT company_id,company_name,hr_name,hr_email,hr_password,is_active FROM company " +
                     "WHERE LOWER(hr_email)=LOWER(?) AND is_active=TRUE";
        try(Connection c=DatabaseConnection.getConnection(); PreparedStatement ps=c.prepareStatement(sql)){
            ps.setString(1,email);
            try(ResultSet rs=ps.executeQuery()){return rs.next()?map(rs):null;}
        }
    }

    public Company findByNameOrEmail(String value) throws SQLException {
        String sql = "SELECT company_id,company_name,hr_name,hr_email,hr_password,is_active FROM company " +
                     "WHERE is_active=TRUE AND (LOWER(company_name)=LOWER(?) OR LOWER(hr_email)=LOWER(?)) LIMIT 1";
        try(Connection c=DatabaseConnection.getConnection(); PreparedStatement ps=c.prepareStatement(sql)){
            ps.setString(1,value); ps.setString(2,value);
            try(ResultSet rs=ps.executeQuery()){return rs.next()?map(rs):null;}
        }
    }
    private Company map(ResultSet rs)throws SQLException{
        Company company=new Company(); company.setCompanyId(rs.getInt("company_id"));
        company.setCompanyName(rs.getString("company_name")); company.setHrName(rs.getString("hr_name"));
        company.setHrEmail(rs.getString("hr_email")); company.setHrPassword(rs.getString("hr_password"));
        company.setActive(rs.getBoolean("is_active")); return company;
    }
}
