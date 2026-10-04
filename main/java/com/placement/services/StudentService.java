package com.placement.services;

import com.placement.algorithms.StudentBinarySearch;
import com.placement.algorithms.StudentMergeSort;
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

    /**
     * Same eligible/unnotified student list as getEligibleUnnotifiedStudents(job)
     * (existing EligibilityDAO logic, untouched), but ranked by CGPA descending,
     * then name ascending, using the manual StudentMergeSort implementation.
     */
    public List<Student> getEligibleUnnotifiedStudentsSorted(JobPosting job) throws Exception {
        List<Student> eligible = eligibilityDAO.findEligible(job);
        return StudentMergeSort.sort(eligible);
    }

    /**
     * Looks up a single student by studentId using the manual
     * StudentBinarySearch implementation. Relies on StudentDAO.findAll()
     * already returning students ordered by student_id ascending (see
     * StudentDAO's "ORDER BY student_id" query) - no extra sort step needed.
     */
    public Student findStudentById(int studentId) throws Exception {
        List<Student> sortedByIdAscending = studentDAO.findAll();
        return StudentBinarySearch.search(sortedByIdAscending, studentId);
    }
}
