package com.placement.services;

import com.placement.database.NotificationDAO;
import com.placement.database.StudentDAO;
import com.placement.models.JobPosting;
import com.placement.models.Notification;
import java.util.*;

public class NotificationService {
    private final NotificationDAO notificationDAO=new NotificationDAO();
    private final StudentDAO studentDAO=new StudentDAO();

    public Notification sendJobEligibilityNotification(JobPosting job,List<String> prns)throws Exception{
        Map<String,Integer> ids=new HashMap<>();
        for(var s:studentDAO.findAll()) if(s.getPrn()!=null) ids.put(s.getPrn().trim(),s.getStudentId());
        List<Integer> recipientIds=new ArrayList<>();
        for(String prn:prns){Integer id=ids.get(prn.trim()); if(id!=null)recipientIds.add(id);}
        return notificationDAO.insert(job.getId(),"Placement Opportunity: "+job.getCompany(),
                "You are eligible to apply for "+job.getRole()+". Deadline: "+job.getDeadline(),
                "Eligible Students",recipientIds);
    }

    public Notification sendGeneralNotification(String subject,String message,List<String> prns)throws Exception{
        Map<String,Integer> ids=new HashMap<>();
        for(var s:studentDAO.findAll()) if(s.getPrn()!=null) ids.put(s.getPrn().trim(),s.getStudentId());
        List<Integer> recipientIds=new ArrayList<>();
        for(String p:prns){Integer id=ids.get(p.trim());if(id!=null)recipientIds.add(id);}
        return notificationDAO.insert(null,subject,message,"All Students",recipientIds);
    }

    public List<Notification> getHistory() throws Exception{return notificationDAO.findAll();}
}
