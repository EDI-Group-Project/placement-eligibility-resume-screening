package com.placement.services;

import com.placement.database.CompanyDAO;
import com.placement.database.JobDAO;
import com.placement.models.Company;
import com.placement.models.JobPosting;
import java.util.List;
import java.util.UUID;

public class JobService {
    private final JobDAO jobDAO=new JobDAO();
    private final CompanyDAO companyDAO=new CompanyDAO();

    public List<JobPosting> getAllJobs() throws Exception {return jobDAO.findAll();}
    public JobPosting getJob(String id) throws Exception {return jobDAO.findById(id);}
    public JobPosting addJob(String company,String role,String minPackage,double minCgpa,List<String> branches,int maxBacklogs,String deadline,int createdByFacultyId) throws Exception {
        Company c=companyDAO.findByNameOrEmail(company);
        if(c==null) throw new IllegalArgumentException("Company must be an existing company name or recruiter email.");
        JobPosting j=new JobPosting();
        j.setId("JOB-"+UUID.randomUUID().toString().substring(0,8).toUpperCase());
        j.setCompanyId(c.getCompanyId()); j.setCompany(c.getCompanyName()); j.setRole(role); j.setMinPackage(minPackage);
        j.setMinCgpa(minCgpa); j.setBranches(branches); j.setMaxBacklogs(maxBacklogs);
        j.setPassingYear(java.time.LocalDate.now().getYear()+1);
        j.setRequiredSkills(List.of()); j.setDeadline(deadline);
        return jobDAO.insert(j,createdByFacultyId);
    }
}
