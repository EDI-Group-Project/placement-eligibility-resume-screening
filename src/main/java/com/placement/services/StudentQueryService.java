package com.placement.services;
import com.placement.database.StudentDAO;
import com.placement.models.Student;
import java.util.*;
public class StudentQueryService {
 private final StudentDAO dao=new StudentDAO();
 public List<Student> query(String search,String dept,int year,double minCgpa,int maxBacklogs,int semester,String sortField,boolean asc)throws Exception{
  String q=search==null?"":search.trim().toLowerCase(Locale.ROOT);List<Student> out=new ArrayList<>();
  for(Student s:dao.findAll()){
   boolean a=q.isBlank()||has(s.getPrn(),q)||has(s.getName(),q)||has(s.getEmail(),q)||has(s.getDepartment(),q);
   boolean b=dept==null||dept.isBlank()||dept.equalsIgnoreCase("ALL")||dept.equalsIgnoreCase(s.getDepartment());
   boolean c=year<=0||s.getPassingYear()==year,d=minCgpa<0||s.getCgpa()>=minCgpa,e=maxBacklogs<0||s.getBacklogs()<=maxBacklogs,f=semester<=0||s.getSemester()==semester;
   if(a&&b&&c&&d&&e&&f)out.add(s);
  }
  Comparator<Student> cmp=switch(sortField==null?"CGPA":sortField.toUpperCase(Locale.ROOT)){case "NAME"->Comparator.comparing(x->x.getName()==null?"":x.getName(),String.CASE_INSENSITIVE_ORDER);case "DEPARTMENT"->Comparator.comparing(x->x.getDepartment()==null?"":x.getDepartment(),String.CASE_INSENSITIVE_ORDER);case "YEAR"->Comparator.comparingInt(Student::getPassingYear);case "BACKLOGS"->Comparator.comparingInt(Student::getBacklogs);case "SEMESTER"->Comparator.comparingInt(Student::getSemester);case "PRN"->Comparator.comparing(x->x.getPrn()==null?"":x.getPrn(),String.CASE_INSENSITIVE_ORDER);default->Comparator.comparingDouble(Student::getCgpa);};
  if(!asc)cmp=cmp.reversed();out.sort(cmp.thenComparing(x->x.getName()==null?"":x.getName(),String.CASE_INSENSITIVE_ORDER));return out;
 }
 private boolean has(String s,String q){return s!=null&&s.toLowerCase(Locale.ROOT).contains(q);}
}
