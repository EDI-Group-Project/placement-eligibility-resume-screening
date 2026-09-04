package com.placement.services;

import com.placement.database.CompanyDAO;
import com.placement.database.FacultyDAO;
import com.placement.models.Company;
import com.placement.models.Faculty;
import com.placement.models.LoginResult;
import com.placement.security.PasswordHasher;

public class AuthService {

    private final FacultyDAO facultyDAO = new FacultyDAO();
    private final CompanyDAO companyDAO = new CompanyDAO();

    public LoginResult login(String role, String email, String password) {

        if (role == null || email == null || password == null) {
            return new LoginResult(false, "Missing login fields.", null);
        }

        String normalizedRole = role.trim().toUpperCase();

        if (normalizedRole.equals("RECRUITER")) {
            return loginAsRecruiter(email, password);
        } else if (normalizedRole.equals("DIRECTOR")
                || normalizedRole.equals("TPO")
                || normalizedRole.equals("TPC")) {
            return loginAsFaculty(email, password, normalizedRole);
        }

        return new LoginResult(false, "Unknown role.", null);
    }

    private LoginResult loginAsFaculty(String email, String password, String role) {

        Faculty faculty = facultyDAO.findByEmailAndRole(email, role);

        if (faculty == null) {
            return new LoginResult(false, "No " + role + " account found for this email.", null);
        }

        boolean matches = PasswordHasher.verifyPassword(password, facultyDAO.getLastStoredHash());

        if (!matches) {
            return new LoginResult(false, "Incorrect password.", null);
        }

        return new LoginResult(true, "Login successful.", faculty.getName());
    }

    private LoginResult loginAsRecruiter(String email, String password) {

        Company company = companyDAO.findByHrEmail(email);

        if (company == null) {
            return new LoginResult(false, "No recruiter account found for this email.", null);
        }

        boolean matches = PasswordHasher.verifyPassword(password, companyDAO.getLastStoredHash());

        if (!matches) {
            return new LoginResult(false, "Incorrect password.", null);
        }

        return new LoginResult(true, "Login successful.", company.getHrName() + " (" + company.getCompanyName() + ")");
    }
}