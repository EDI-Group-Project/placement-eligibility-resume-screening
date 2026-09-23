package com.placement.services;
import com.placement.database.ApplicationDAO;
import com.placement.models.Application;
import java.util.List;
public class ApplicationService {private final ApplicationDAO dao=new ApplicationDAO();public long apply(String jobId,int studentId)throws Exception{if(dao.exists(jobId,studentId))throw new IllegalArgumentException("You have already applied for this job.");return dao.insert(jobId,studentId);}public List<Application> getForStudent(int id)throws Exception{return dao.findByStudent(id);}}
