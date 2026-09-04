package com.placement.validation;

import java.time.Year;

public class StudentValidator {

    public static boolean validateName(String name) {
        return name != null && !name.trim().isEmpty();
    }

    public static boolean validateEmail(String email) {
        return email != null && email.contains("@") && email.contains(".");
    }

    public static boolean validatePassword(String password) {
        return password != null && password.length() >= 8;
    }

    public static boolean validateDepartment(String department) {
        return department != null && !department.trim().isEmpty();
    }

    public static boolean validateCgpa(double cgpa) {
        return cgpa >= 0.0 && cgpa <= 10.0;
    }

    public static boolean validatePassingYear(int passingYear) {
        int currentYear = Year.now().getValue();
        return passingYear >= currentYear - 1 && passingYear <= currentYear + 6;
    }

    public static boolean validateBacklogs(int backlogs) {
        return backlogs >= 0;
    }

    public static boolean validateSemester(int semester) {
        return semester >= 1 && semester <= 8;
    }

    public static boolean validatePhone(String phone) {
        return phone != null && phone.matches("\\d{10}");
    }

    public static boolean validateSkills(String skills) {
        return skills != null && !skills.trim().isEmpty();
    }
}