package com.placement.services;

import com.placement.database.StudentDAO;
import com.placement.models.Student;
import com.placement.security.PasswordHasher;
import com.placement.validation.StudentValidator;

public class RegistrationService {

    private final StudentDAO studentDAO = new StudentDAO();

    public String registerStudent(Student student, String plainPassword) {

        if (!StudentValidator.validateName(student.getName()))
            return "Invalid name.";

        if (!StudentValidator.validateEmail(student.getEmail()))
            return "Invalid email.";

        if (!StudentValidator.validatePassword(plainPassword))
            return "Password must be at least 8 characters.";

        if (!StudentValidator.validateDepartment(student.getDepartment()))
            return "Invalid department.";

        if (!StudentValidator.validateCgpa(student.getCgpa()))
            return "CGPA must be between 0 and 10.";

        if (!StudentValidator.validatePassingYear(student.getPassingYear()))
            return "Invalid passing year.";

        if (!StudentValidator.validateBacklogs(student.getBacklogs()))
            return "Backlogs cannot be negative.";

        if (!StudentValidator.validateSemester(student.getSemester()))
            return "Semester must be between 1 and 8.";

        if (!StudentValidator.validatePhone(student.getPhone()))
            return "Phone number must be 10 digits.";

        if (!StudentValidator.validateSkills(student.getSkills()))
            return "Skills field cannot be empty.";

        if (studentDAO.emailExists(student.getEmail()))
            return "Email already registered.";

        student.setPassword(PasswordHasher.hashPassword(plainPassword));

        boolean inserted = studentDAO.insertStudent(student);

        return inserted ? "SUCCESS" : "Database insertion failed.";
    }
}