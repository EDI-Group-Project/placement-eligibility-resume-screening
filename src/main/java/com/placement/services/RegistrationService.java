package com.placement.services;

import com.placement.database.StudentDAO;
import com.placement.models.Student;
import com.placement.security.PasswordHasher;
import com.placement.validation.StudentValidator;

public class RegistrationService {
    private final StudentDAO studentDAO=new StudentDAO();
    public String registerStudent(Student s,String plainPassword){
        try{
            if(!StudentValidator.validateName(s.getName()))return"Invalid name.";
            if(!StudentValidator.validateEmail(s.getEmail()))return"Invalid email.";
            if(!StudentValidator.validatePassword(plainPassword))return"Password must be at least 8 characters.";
            if(!StudentValidator.validateDepartment(s.getDepartment()))return"Invalid department.";
            if(!StudentValidator.validateCgpa(s.getCgpa()))return"CGPA must be between 0 and 10.";
            if(!StudentValidator.validatePassingYear(s.getPassingYear()))return"Invalid passing year.";
            if(!StudentValidator.validateBacklogs(s.getBacklogs()))return"Backlogs cannot be negative.";
            if(!StudentValidator.validateSemester(s.getSemester()))return"Semester must be between 1 and 8.";
            if(!StudentValidator.validatePhone(s.getPhone()))return"Phone number must be 10 digits.";
            if(!StudentValidator.validateSkills(s.getSkills()))return"Skills field cannot be empty.";
            if(studentDAO.emailExists(s.getEmail()))return"Email already registered.";
            if(s.getPrn()==null||s.getPrn().isBlank()) s.setPrn("PRN-"+System.currentTimeMillis());
            s.setPassword(PasswordHasher.hashPassword(plainPassword));
            return studentDAO.insert(s)>0?"SUCCESS":"Database insertion failed.";
        }catch(Exception e){return"Database error: "+e.getMessage();}
    }
}
