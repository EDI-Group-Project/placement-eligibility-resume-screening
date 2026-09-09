package com.placement.services;

import com.placement.database.EligibilityDAO;
import com.placement.database.StudentDAO;
import com.placement.models.JobPosting;
import com.placement.models.Student;
import java.util.List;

public class StudentService {
    private final StudentDAO studentDAO=new StudentDAO();
    private final EligibilityDAO eligibilityDAO=new EligibilityDAO();

    public List<Student> getAllStudents() throws Exception { return studentDAO.findAll(); }
    public boolean emailExists(String email) throws Exception { return studentDAO.emailExists(email); }
    public List<Student> getEligibleUnnotifiedStudents(JobPosting job) throws Exception { return eligibilityDAO.findEligible(job); }
}
