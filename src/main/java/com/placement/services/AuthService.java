package com.placement.services;

import com.placement.database.CompanyDAO;
import com.placement.database.FacultyDAO;
import com.placement.database.StudentDAO;
import com.placement.models.Company;
import com.placement.models.Faculty;
import com.placement.models.LoginResult;
import com.placement.models.Student;
import com.placement.security.PasswordHasher;

public class AuthService {
    private final FacultyDAO facultyDAO = new FacultyDAO();
    private final CompanyDAO companyDAO = new CompanyDAO();
    private final StudentDAO studentDAO = new StudentDAO();

    public LoginResult login(String role, String email, String password) {
        try {
            if (role == null || email == null || password == null) return new LoginResult(false,"Missing login fields.",null);
            String normalized=role.trim().toUpperCase();
            if ("RECRUITER".equals(normalized)) {
                Company c=companyDAO.findByHrEmail(email);
                if(c==null) return new LoginResult(false,"No recruiter account found for this email.",null);
                if(!PasswordHasher.verifyPassword(password,c.getHrPassword())) return new LoginResult(false,"Incorrect password.",null);
                return new LoginResult(true,"Login successful.",c.getHrName()+" ("+c.getCompanyName()+")",c.getCompanyId(),"RECRUITER");
            }
            if ("STUDENT".equals(normalized)) {
                Student s=studentDAO.findByEmail(email);
                if(s==null) return new LoginResult(false,"No student account found for this email.",null);
                if(!PasswordHasher.verifyPassword(password,s.getPassword())) return new LoginResult(false,"Incorrect password.",null);
                return new LoginResult(true,"Login successful.",s.getName(),s.getStudentId(),"STUDENT");
            }
            Faculty f=facultyDAO.findByEmailAndRole(email, normalized);
            if(f==null) return new LoginResult(false,"No "+normalized+" account found for this email.",null);
            if(!PasswordHasher.verifyPassword(password,f.getPassword())) return new LoginResult(false,"Incorrect password.",null);
            return new LoginResult(true,"Login successful.",f.getName(),f.getFacultyId(),normalized);
        } catch (Exception e) {
            return new LoginResult(false,"Database authentication error: "+e.getMessage(),null);
        }
    }
}
