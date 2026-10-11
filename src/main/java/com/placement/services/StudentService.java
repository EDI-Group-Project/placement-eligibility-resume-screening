package com.placement.services;
import com.placement.algorithms.StudentBinarySearch;
import com.placement.algorithms.StudentMergeSort;
import com.placement.database.EligibilityDAO;
import com.placement.database.StudentDAO;
import com.placement.models.JobPosting;
import com.placement.models.Student;
import java.util.List;
public class StudentService {
 private final StudentDAO studentDAO=new StudentDAO();private final EligibilityDAO eligibilityDAO=new EligibilityDAO();private final StudentQueryService queryService=new StudentQueryService();
 public List<Student> getAllStudents()throws Exception{return studentDAO.findAll();}
 public boolean emailExists(String email)throws Exception{return studentDAO.emailExists(email);}
 public List<Student> getEligibleUnnotifiedStudents(JobPosting job)throws Exception{return eligibilityDAO.findEligible(job);}
 public List<Student> getEligibleUnnotifiedStudentsSorted(JobPosting job)throws Exception{return StudentMergeSort.sort(eligibilityDAO.findEligible(job));}
 public Student findStudentById(int id)throws Exception{return StudentBinarySearch.search(studentDAO.findAll(),id);}
 public List<Student> query(String search,String dept,int year,double minCgpa,int maxBacklogs,int semester,String sortField,boolean asc)throws Exception{return queryService.query(search,dept,year,minCgpa,maxBacklogs,semester,sortField,asc);}
}
