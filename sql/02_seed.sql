USE placement_portal;

-- PBKDF2WithHmacSHA256 hashes for the demo password "pass123".
-- The application verifies these hashes with PasswordHasher.java.
SET @PASS123 = '120000$cGxhY2VtZW50LWRlbW8tc2FsdDE=$AdSY796m6lPG0XcvLy/hVlBAJSpR/jqfRwIUajp1hog=';

INSERT INTO students
(prn,name,email,password,department,cgpa,passing_year,backlogs,semester,phone,skills)
VALUES
('PRN001','Aarav Sharma','aarav@gmail.com',@PASS123,'CSE',8.50,2027,0,5,'9876543210','Java,SQL,Python'),
('PRN002','Priya Patil','priya@gmail.com',@PASS123,'IT',7.80,2027,0,5,'9876543211','Java,React,SQL'),
('PRN003','Rohan Mehta','rohan@gmail.com',@PASS123,'ECE',7.20,2027,1,7,'9876543212','Python,C,Embedded'),
('PRN004','Sneha Joshi','sneha@gmail.com',@PASS123,'CSE',9.10,2027,0,5,'9876543213','Java,Python,Machine Learning'),
('PRN005','Vivek Shah','vivek@gmail.com',@PASS123,'IT',6.90,2027,2,5,'9876543214','HTML,CSS,JavaScript')
ON DUPLICATE KEY UPDATE name=VALUES(name), department=VALUES(department), cgpa=VALUES(cgpa),
passing_year=VALUES(passing_year), backlogs=VALUES(backlogs), semester=VALUES(semester),
phone=VALUES(phone), skills=VALUES(skills);

INSERT INTO faculty
(name,email,password,role,department,phone)
VALUES
('Amit Kulkarni','amit@college.com',@PASS123,'TPO','CSE','9876500001'),
('Neha Deshmukh','neha@college.com',@PASS123,'TPC','IT','9876500002'),
('Rajesh Patil','rajesh@college.com',@PASS123,'DIRECTOR',NULL,'9876500003'),
('Pooja Joshi','pooja@college.com',@PASS123,'DEAN','ECE','9876500004'),
('Sanjay More','sanjay@college.com',@PASS123,'FACULTY','IT','9876500005')
ON DUPLICATE KEY UPDATE name=VALUES(name), password=VALUES(password), role=VALUES(role),
department=VALUES(department), phone=VALUES(phone), is_active=TRUE;

INSERT INTO company
(company_name,industry_type,hr_name,hr_email,hr_password,hr_phone)
VALUES
('TCS','IT','Anita Rao','anita@tcs.com',@PASS123,'9000000001'),
('Infosys','IT','Rahul Verma','rahul@infosys.com',@PASS123,'9000000002'),
('Accenture','Consulting','Kavita Shah','kavita@accenture.com',@PASS123,'9000000003'),
('Deloitte','Consulting','Vikas Mehta','vikas@deloitte.com',@PASS123,'9000000004'),
('Wipro','IT','Sneha Kapoor','sneha@wipro.com',@PASS123,'9000000005')
ON DUPLICATE KEY UPDATE company_name=VALUES(company_name), is_active=TRUE;

INSERT INTO eligibility
(company_id,min_cgpa,max_backlogs,department,passing_year,required_skills,created_by)
SELECT c.company_id,7.50,0,'CSE,IT',2027,'Java,SQL',f.faculty_id
FROM company c JOIN faculty f ON f.email='amit@college.com'
WHERE c.hr_email='anita@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM eligibility e JOIN company c2 ON c2.company_id=e.company_id
                  WHERE c2.hr_email='anita@tcs.com' AND e.passing_year=2027);

INSERT INTO eligibility
(company_id,min_cgpa,max_backlogs,department,passing_year,required_skills,created_by)
SELECT c.company_id,7.00,1,'CSE,IT,ECE',2027,'Python,SQL',f.faculty_id
FROM company c JOIN faculty f ON f.email='amit@college.com'
WHERE c.hr_email='rahul@infosys.com'
  AND NOT EXISTS (SELECT 1 FROM eligibility e JOIN company c2 ON c2.company_id=e.company_id
                  WHERE c2.hr_email='rahul@infosys.com' AND e.passing_year=2027);

INSERT INTO eligibility
(company_id,min_cgpa,max_backlogs,department,passing_year,required_skills,created_by)
SELECT c.company_id,7.50,0,'CSE,IT',2027,'Java,Python',f.faculty_id
FROM company c JOIN faculty f ON f.email='amit@college.com'
WHERE c.hr_email='kavita@accenture.com'
  AND NOT EXISTS (SELECT 1 FROM eligibility e JOIN company c2 ON c2.company_id=e.company_id
                  WHERE c2.hr_email='kavita@accenture.com' AND e.passing_year=2027);

INSERT INTO eligibility
(company_id,min_cgpa,max_backlogs,department,passing_year,required_skills,created_by)
SELECT c.company_id,8.00,0,'CSE,IT',2027,'Python,SQL,Excel',f.faculty_id
FROM company c JOIN faculty f ON f.email='sanjay@college.com'
WHERE c.hr_email='vikas@deloitte.com'
  AND NOT EXISTS (SELECT 1 FROM eligibility e JOIN company c2 ON c2.company_id=e.company_id
                  WHERE c2.hr_email='vikas@deloitte.com' AND e.passing_year=2027);

INSERT INTO eligibility
(company_id,min_cgpa,max_backlogs,department,passing_year,required_skills,created_by)
SELECT c.company_id,6.50,2,'CSE,IT,ECE',2027,'Java,HTML,CSS',f.faculty_id
FROM company c JOIN faculty f ON f.email='neha@college.com'
WHERE c.hr_email='sneha@wipro.com'
  AND NOT EXISTS (SELECT 1 FROM eligibility e JOIN company c2 ON c2.company_id=e.company_id
                  WHERE c2.hr_email='sneha@wipro.com' AND e.passing_year=2027);

INSERT INTO job_postings(job_id,company_id,eligibility_id,job_profile,min_package,deadline)
SELECT 'JOB001',c.company_id,e.eligibility_id,'Software Engineer','6 LPA','2026-09-15'
FROM company c JOIN eligibility e ON e.company_id=c.company_id AND e.passing_year=2027
WHERE c.hr_email='anita@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM job_postings WHERE job_id='JOB001');

INSERT INTO job_postings(job_id,company_id,eligibility_id,job_profile,min_package,deadline)
SELECT 'JOB002',c.company_id,e.eligibility_id,'Systems Engineer','5.5 LPA','2026-09-20'
FROM company c JOIN eligibility e ON e.company_id=c.company_id AND e.passing_year=2027
WHERE c.hr_email='rahul@infosys.com'
  AND NOT EXISTS (SELECT 1 FROM job_postings WHERE job_id='JOB002');

INSERT INTO job_postings(job_id,company_id,eligibility_id,job_profile,min_package,deadline)
SELECT 'JOB003',c.company_id,e.eligibility_id,'Associate Software Engineer','6.5 LPA','2026-09-25'
FROM company c JOIN eligibility e ON e.company_id=c.company_id AND e.passing_year=2027
WHERE c.hr_email='kavita@accenture.com'
  AND NOT EXISTS (SELECT 1 FROM job_postings WHERE job_id='JOB003');

INSERT INTO job_postings(job_id,company_id,eligibility_id,job_profile,min_package,deadline)
SELECT 'JOB004',c.company_id,e.eligibility_id,'Analyst','7 LPA','2026-10-05'
FROM company c JOIN eligibility e ON e.company_id=c.company_id AND e.passing_year=2027
WHERE c.hr_email='vikas@deloitte.com'
  AND NOT EXISTS (SELECT 1 FROM job_postings WHERE job_id='JOB004');

INSERT INTO job_postings(job_id,company_id,eligibility_id,job_profile,min_package,deadline)
SELECT 'JOB005',c.company_id,e.eligibility_id,'Project Engineer','5 LPA','2026-10-10'
FROM company c JOIN eligibility e ON e.company_id=c.company_id AND e.passing_year=2027
WHERE c.hr_email='sneha@wipro.com'
  AND NOT EXISTS (SELECT 1 FROM job_postings WHERE job_id='JOB005');

SELECT 'Seed data loaded successfully.' AS status;
