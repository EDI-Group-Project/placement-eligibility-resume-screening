package com.placement.validation;

import java.util.regex.Pattern;

public final class StudentValidator {
    private static final Pattern EMAIL=Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE=Pattern.compile("^\\d{10}$");
    private StudentValidator(){}
    public static boolean validateName(String s){return s!=null&&s.matches("[A-Za-z .'-]{2,100}");}
    public static boolean validateEmail(String s){return s!=null&&EMAIL.matcher(s).matches();}
    public static boolean validatePassword(String s){return s!=null&&s.length()>=8;}
    public static boolean validateDepartment(String s){return s!=null&&!s.isBlank()&&s.length()<=50;}
    public static boolean validateCgpa(double v){return v>=0&&v<=10;}
    public static boolean validatePassingYear(int y){return y>=2000&&y<=2100;}
    public static boolean validateBacklogs(int v){return v>=0;}
    public static boolean validateSemester(int v){return v>=1&&v<=8;}
    public static boolean validatePhone(String s){return s==null||s.isBlank()||PHONE.matcher(s).matches();}
    public static boolean validateSkills(String s){return s!=null&&!s.isBlank()&&s.length()<=255;}
}
