USE placement_portal;

-- PBKDF2WithHmacSHA256 hashes for the demo password "pass123".
-- The application verifies these hashes with PasswordHasher.java.
SET @PASS123 = '120000$cGxhY2VtZW50LWRlbW8tc2FsdDE=$AdSY796m6lPG0XcvLy/hVlBAJSpR/jqfRwIUajp1hog=';


-- ============================================================
-- STUDENTS
-- 20 STUDENTS
-- ============================================================

INSERT INTO students
(prn,name,email,password,department,cgpa,passing_year,backlogs,semester,phone,skills)
VALUES
('PRN001','Aarav Sharma','aarav@gmail.com',@PASS123,'CSE',8.50,2027,0,5,'9876543210','Java,SQL,Python'),
('PRN002','Priya Patil','priya@gmail.com',@PASS123,'IT',7.80,2027,0,5,'9876543211','Java,React,SQL'),
('PRN003','Rohan Mehta','rohan@gmail.com',@PASS123,'ECE',7.20,2027,1,7,'9876543212','Python,C,Embedded'),
('PRN004','Sneha Joshi','sneha@gmail.com',@PASS123,'CSE',9.10,2027,0,5,'9876543213','Java,Python,Machine Learning'),
('PRN005','Vivek Shah','vivek@gmail.com',@PASS123,'IT',6.90,2027,2,5,'9876543214','HTML,CSS,JavaScript'),

('PRN006','Ananya Desai','ananya@gmail.com',@PASS123,'CSE',8.70,2027,0,5,'9876543220','Java,SQL,Python'),
('PRN007','Kunal Joshi','kunal@gmail.com',@PASS123,'IT',7.40,2027,1,5,'9876543221','Java,Spring,SQL'),
('PRN008','Isha Kulkarni','isha@gmail.com',@PASS123,'ECE',8.90,2027,0,7,'9876543222','Python,C,IoT'),
('PRN009','Aditya Pawar','aditya@gmail.com',@PASS123,'CSE',6.80,2027,2,5,'9876543223','C++,HTML,CSS'),
('PRN010','Meera Nair','meera@gmail.com',@PASS123,'IT',9.20,2027,0,5,'9876543224','Java,React,SQL'),
('PRN011','Siddharth Rao','siddharth@gmail.com',@PASS123,'CIVIL',7.10,2027,1,7,'9876543225','AutoCAD,Python,Excel'),
('PRN012','Riya Gupta','riya@gmail.com',@PASS123,'CSE',8.30,2028,0,3,'9876543226','Python,Machine Learning,SQL'),
('PRN013','Arjun Singh','arjun@gmail.com',@PASS123,'MECH',7.60,2027,0,7,'9876543227','Python,CAD,Excel'),
('PRN014','Kavya Shah','kavya@gmail.com',@PASS123,'IT',8.10,2027,0,5,'9876543228','JavaScript,React,Node'),
('PRN015','Rahul More','rahul@gmail.com',@PASS123,'ECE',6.70,2027,2,7,'9876543229','C,Embedded,Arduino'),
('PRN016','Nisha Patil','nisha@gmail.com',@PASS123,'CSE',9.40,2027,0,5,'9876543230','Java,Python,Spring,SQL'),
('PRN017','Vishal Thakur','vishal@gmail.com',@PASS123,'IT',7.90,2028,0,3,'9876543231','Python,Django,SQL'),
('PRN018','Tanvi Bhosale','tanvi@gmail.com',@PASS123,'CSE',7.00,2027,1,5,'9876543232','C++,Java,DBMS'),
('PRN019','Omkar Jadhav','omkar@gmail.com',@PASS123,'MECH',8.20,2027,0,7,'9876543233','Python,AutoCAD,Excel'),
('PRN020','Simran Khan','simran@gmail.com',@PASS123,'ECE',8.60,2027,0,7,'9876543234','Python,Embedded,IoT')
ON DUPLICATE KEY UPDATE
name=VALUES(name),
department=VALUES(department),
cgpa=VALUES(cgpa),
passing_year=VALUES(passing_year),
backlogs=VALUES(backlogs),
semester=VALUES(semester),
phone=VALUES(phone),
skills=VALUES(skills);


-- ============================================================
-- FACULTY + ADMIN
-- ============================================================

INSERT INTO faculty
(name,email,password,role,department,phone)
VALUES
('Amit Kulkarni','amit@college.com',@PASS123,'TPO','CSE','9876500001'),
('Neha Deshmukh','neha@college.com',@PASS123,'TPC','IT','9876500002'),
('Rajesh Patil','rajesh@college.com',@PASS123,'DIRECTOR',NULL,'9876500003'),
('Pooja Joshi','pooja@college.com',@PASS123,'DEAN','ECE','9876500004'),
('Sanjay More','sanjay@college.com',@PASS123,'FACULTY','IT','9876500005')
ON DUPLICATE KEY UPDATE
name=VALUES(name),
password=VALUES(password),
role=VALUES(role),
department=VALUES(department),
phone=VALUES(phone),
is_active=TRUE;


INSERT INTO faculty
(name,email,password,role,department,phone)
VALUES
('Aditi Deshpande','aditi@college.com',@PASS123,'TPO','CSE','9876500006'),
('Vikram Joshi','vikram@college.com',@PASS123,'TPC','IT','9876500007')
ON DUPLICATE KEY UPDATE
name=VALUES(name), password=VALUES(password), role=VALUES(role),
department=VALUES(department), phone=VALUES(phone), is_active=TRUE;


INSERT INTO faculty
(name,email,password,role,department,phone)
VALUES
('System Admin','admin@college.com',@PASS123,'ADMIN','Administration','9876500099')
ON DUPLICATE KEY UPDATE
name=VALUES(name),
password=VALUES(password),
role=VALUES(role),
department=VALUES(department),
phone=VALUES(phone),
is_active=TRUE;


-- ============================================================
-- COMPANIES
-- 20 COMPANIES
-- ============================================================

INSERT INTO company
(company_name,industry_type,hr_name,hr_email,hr_password,hr_phone)
VALUES
('TCS','IT','Anita Rao','anita@tcs.com',@PASS123,'9000000001'),
('Infosys','IT','Rahul Verma','rahul@infosys.com',@PASS123,'9000000002'),
('Accenture','Consulting','Kavita Shah','kavita@accenture.com',@PASS123,'9000000003'),
('Deloitte','Consulting','Vikas Mehta','vikas@deloitte.com',@PASS123,'9000000004'),
('Wipro','IT','Sneha Kapoor','sneha@wipro.com',@PASS123,'9000000005'),

('Capgemini','IT','Neeraj Shah','neeraj@capgemini.com',@PASS123,'9000000010'),
('Cognizant','IT','Asha Menon','asha@cognizant.com',@PASS123,'9000000011'),
('HCLTech','IT','Vivek Rao','vivek@hcl.com',@PASS123,'9000000012'),
('Tech Mahindra','IT','Rohit Kulkarni','rohit@techmahindra.com',@PASS123,'9000000013'),
('Persistent Systems','IT','Manish Joshi','manish@persistent.com',@PASS123,'9000000014'),
('LTI Mindtree','IT','Priyanka Shah','priyanka@ltimindtree.com',@PASS123,'9000000015'),
('IBM','IT','Arun Mehta','arun@ibm.com',@PASS123,'9000000016'),
('Oracle','IT','Snehal Patil','snehal@oracle.com',@PASS123,'9000000017'),
('Amazon','E-Commerce','Rakesh Verma','rakesh@amazon.com',@PASS123,'9000000018'),
('Microsoft','IT','Divya Nair','divya@microsoft.com',@PASS123,'9000000019'),
('Google','IT','Karan Shah','karan@google.com',@PASS123,'9000000020'),
('Mphasis','IT','Nitin Rao','nitin@mphasis.com',@PASS123,'9000000021'),
('Hexaware','IT','Pallavi Joshi','pallavi@hexaware.com',@PASS123,'9000000022'),
('Zensar','IT','Akshay More','akshay@zensar.com',@PASS123,'9000000023'),
('Cybage','IT','Monika Desai','monika@cybage.com',@PASS123,'9000000024')
ON DUPLICATE KEY UPDATE
company_name=VALUES(company_name),
is_active=TRUE;


-- ============================================================
-- ELIGIBILITY RULES
-- 20 ELIGIBILITY RULES
-- ============================================================

INSERT INTO eligibility
(company_id,min_cgpa,max_backlogs,department,passing_year,required_skills,created_by)
SELECT c.company_id,7.50,0,'CSE,IT',2027,'Java,SQL',f.faculty_id
FROM company c JOIN faculty f ON f.email='amit@college.com'
WHERE c.hr_email='anita@tcs.com'
AND NOT EXISTS (
    SELECT 1 FROM eligibility e
    JOIN company c2 ON c2.company_id=e.company_id
    WHERE c2.hr_email='anita@tcs.com' AND e.passing_year=2027
);


INSERT INTO eligibility
(company_id,min_cgpa,max_backlogs,department,passing_year,required_skills,created_by)
SELECT c.company_id,7.00,1,'CSE,IT,ECE',2027,'Python,SQL',f.faculty_id
FROM company c JOIN faculty f ON f.email='amit@college.com'
WHERE c.hr_email='rahul@infosys.com'
AND NOT EXISTS (
    SELECT 1 FROM eligibility e
    JOIN company c2 ON c2.company_id=e.company_id
    WHERE c2.hr_email='rahul@infosys.com' AND e.passing_year=2027
);


INSERT INTO eligibility
(company_id,min_cgpa,max_backlogs,department,passing_year,required_skills,created_by)
SELECT c.company_id,7.50,0,'CSE,IT',2027,'Java,Python',f.faculty_id
FROM company c JOIN faculty f ON f.email='amit@college.com'
WHERE c.hr_email='kavita@accenture.com'
AND NOT EXISTS (
    SELECT 1 FROM eligibility e
    JOIN company c2 ON c2.company_id=e.company_id
    WHERE c2.hr_email='kavita@accenture.com' AND e.passing_year=2027
);


INSERT INTO eligibility
(company_id,min_cgpa,max_backlogs,department,passing_year,required_skills,created_by)
SELECT c.company_id,8.00,0,'CSE,IT',2027,'Python,SQL,Excel',f.faculty_id
FROM company c JOIN faculty f ON f.email='sanjay@college.com'
WHERE c.hr_email='vikas@deloitte.com'
AND NOT EXISTS (
    SELECT 1 FROM eligibility e
    JOIN company c2 ON c2.company_id=e.company_id
    WHERE c2.hr_email='vikas@deloitte.com' AND e.passing_year=2027
);


INSERT INTO eligibility
(company_id,min_cgpa,max_backlogs,department,passing_year,required_skills,created_by)
SELECT c.company_id,6.50,2,'CSE,IT,ECE',2027,'Java,HTML,CSS',f.faculty_id
FROM company c JOIN faculty f ON f.email='neha@college.com'
WHERE c.hr_email='sneha@wipro.com'
AND NOT EXISTS (
    SELECT 1 FROM eligibility e
    JOIN company c2 ON c2.company_id=e.company_id
    WHERE c2.hr_email='sneha@wipro.com' AND e.passing_year=2027
);


-- Additional eligibility rules

INSERT INTO eligibility
(company_id,min_cgpa,max_backlogs,department,passing_year,required_skills,created_by)
SELECT c.company_id,7.00,1,'CSE,IT',2027,'Java,SQL',f.faculty_id
FROM company c JOIN faculty f ON f.email='amit@college.com'
WHERE c.hr_email='neeraj@capgemini.com'
AND NOT EXISTS (
    SELECT 1 FROM eligibility e
    WHERE e.company_id=c.company_id AND e.passing_year=2027
);


INSERT INTO eligibility
(company_id,min_cgpa,max_backlogs,department,passing_year,required_skills,created_by)
SELECT c.company_id,7.50,1,'CSE,IT,ECE',2027,'Python,SQL',f.faculty_id
FROM company c JOIN faculty f ON f.email='neha@college.com'
WHERE c.hr_email='asha@cognizant.com'
AND NOT EXISTS (
    SELECT 1 FROM eligibility e
    WHERE e.company_id=c.company_id AND e.passing_year=2027
);


INSERT INTO eligibility
(company_id,min_cgpa,max_backlogs,department,passing_year,required_skills,created_by)
SELECT c.company_id,6.50,2,'CSE,IT,ECE',2027,'Java,C++',f.faculty_id
FROM company c JOIN faculty f ON f.email='amit@college.com'
WHERE c.hr_email='vivek@hcl.com'
AND NOT EXISTS (
    SELECT 1 FROM eligibility e
    WHERE e.company_id=c.company_id AND e.passing_year=2027
);


INSERT INTO eligibility
(company_id,min_cgpa,max_backlogs,department,passing_year,required_skills,created_by)
SELECT c.company_id,7.00,1,'CSE,IT',2027,'JavaScript,React',f.faculty_id
FROM company c JOIN faculty f ON f.email='neha@college.com'
WHERE c.hr_email='rohit@techmahindra.com'
AND NOT EXISTS (
    SELECT 1 FROM eligibility e
    WHERE e.company_id=c.company_id AND e.passing_year=2027
);


INSERT INTO eligibility
(company_id,min_cgpa,max_backlogs,department,passing_year,required_skills,created_by)
SELECT c.company_id,7.50,0,'CSE,IT',2027,'Python,SQL',f.faculty_id
FROM company c JOIN faculty f ON f.email='amit@college.com'
WHERE c.hr_email='manish@persistent.com'
AND NOT EXISTS (
    SELECT 1 FROM eligibility e
    WHERE e.company_id=c.company_id AND e.passing_year=2027
);


INSERT INTO eligibility
(company_id,min_cgpa,max_backlogs,department,passing_year,required_skills,created_by)
SELECT c.company_id,7.20,1,'CSE,IT,ECE',2027,'Java,Python',f.faculty_id
FROM company c JOIN faculty f ON f.email='neha@college.com'
WHERE c.hr_email='priyanka@ltimindtree.com'
AND NOT EXISTS (
    SELECT 1 FROM eligibility e
    WHERE e.company_id=c.company_id AND e.passing_year=2027
);


INSERT INTO eligibility
(company_id,min_cgpa,max_backlogs,department,passing_year,required_skills,created_by)
SELECT c.company_id,8.00,0,'CSE,IT,ECE',2027,'Python,SQL',f.faculty_id
FROM company c JOIN faculty f ON f.email='amit@college.com'
WHERE c.hr_email='arun@ibm.com'
AND NOT EXISTS (
    SELECT 1 FROM eligibility e
    WHERE e.company_id=c.company_id AND e.passing_year=2027
);


INSERT INTO eligibility
(company_id,min_cgpa,max_backlogs,department,passing_year,required_skills,created_by)
SELECT c.company_id,8.00,0,'CSE,IT',2027,'Java,SQL',f.faculty_id
FROM company c JOIN faculty f ON f.email='amit@college.com'
WHERE c.hr_email='snehal@oracle.com'
AND NOT EXISTS (
    SELECT 1 FROM eligibility e
    WHERE e.company_id=c.company_id AND e.passing_year=2027
);


INSERT INTO eligibility
(company_id,min_cgpa,max_backlogs,department,passing_year,required_skills,created_by)
SELECT c.company_id,7.50,0,'CSE,IT',2027,'Java,Python,SQL',f.faculty_id
FROM company c JOIN faculty f ON f.email='amit@college.com'
WHERE c.hr_email='rakesh@amazon.com'
AND NOT EXISTS (
    SELECT 1 FROM eligibility e
    WHERE e.company_id=c.company_id AND e.passing_year=2027
);


INSERT INTO eligibility
(company_id,min_cgpa,max_backlogs,department,passing_year,required_skills,created_by)
SELECT c.company_id,8.50,0,'CSE,IT',2027,'Java,Python,SQL',f.faculty_id
FROM company c JOIN faculty f ON f.email='amit@college.com'
WHERE c.hr_email='divya@microsoft.com'
AND NOT EXISTS (
    SELECT 1 FROM eligibility e
    WHERE e.company_id=c.company_id AND e.passing_year=2027
);


INSERT INTO eligibility
(company_id,min_cgpa,max_backlogs,department,passing_year,required_skills,created_by)
SELECT c.company_id,8.00,0,'CSE,IT',2027,'Python,Machine Learning',f.faculty_id
FROM company c JOIN faculty f ON f.email='amit@college.com'
WHERE c.hr_email='karan@google.com'
AND NOT EXISTS (
    SELECT 1 FROM eligibility e
    WHERE e.company_id=c.company_id AND e.passing_year=2027
);


INSERT INTO eligibility
(company_id,min_cgpa,max_backlogs,department,passing_year,required_skills,created_by)
SELECT c.company_id,7.00,1,'CSE,IT',2027,'Java,SQL',f.faculty_id
FROM company c JOIN faculty f ON f.email='neha@college.com'
WHERE c.hr_email='nitin@mphasis.com'
AND NOT EXISTS (
    SELECT 1 FROM eligibility e
    WHERE e.company_id=c.company_id AND e.passing_year=2027
);


INSERT INTO eligibility
(company_id,min_cgpa,max_backlogs,department,passing_year,required_skills,created_by)
SELECT c.company_id,7.20,1,'CSE,IT,ECE',2027,'JavaScript,React',f.faculty_id
FROM company c JOIN faculty f ON f.email='neha@college.com'
WHERE c.hr_email='pallavi@hexaware.com'
AND NOT EXISTS (
    SELECT 1 FROM eligibility e
    WHERE e.company_id=c.company_id AND e.passing_year=2027
);


INSERT INTO eligibility
(company_id,min_cgpa,max_backlogs,department,passing_year,required_skills,created_by)
SELECT c.company_id,7.00,1,'CSE,IT',2027,'Java,Python',f.faculty_id
FROM company c JOIN faculty f ON f.email='amit@college.com'
WHERE c.hr_email='akshay@zensar.com'
AND NOT EXISTS (
    SELECT 1 FROM eligibility e
    WHERE e.company_id=c.company_id AND e.passing_year=2027
);


INSERT INTO eligibility
(company_id,min_cgpa,max_backlogs,department,passing_year,required_skills,created_by)
SELECT c.company_id,7.50,0,'CSE,IT',2027,'Java,Python,SQL',f.faculty_id
FROM company c JOIN faculty f ON f.email='neha@college.com'
WHERE c.hr_email='monika@cybage.com'
AND NOT EXISTS (
    SELECT 1 FROM eligibility e
    WHERE e.company_id=c.company_id AND e.passing_year=2027
);


-- ============================================================
-- JOB POSTINGS
-- 20 JOBS
-- ============================================================

INSERT INTO job_postings
(job_id,company_id,eligibility_id,job_profile,min_package,deadline)
SELECT 'JOB001',c.company_id,e.eligibility_id,'Software Engineer','6 LPA','2026-09-15'
FROM company c JOIN eligibility e ON e.company_id=c.company_id AND e.passing_year=2027
WHERE c.hr_email='anita@tcs.com'
AND NOT EXISTS (SELECT 1 FROM job_postings WHERE job_id='JOB001');


INSERT INTO job_postings
(job_id,company_id,eligibility_id,job_profile,min_package,deadline)
SELECT 'JOB002',c.company_id,e.eligibility_id,'Systems Engineer','5.5 LPA','2026-09-20'
FROM company c JOIN eligibility e ON e.company_id=c.company_id AND e.passing_year=2027
WHERE c.hr_email='rahul@infosys.com'
AND NOT EXISTS (SELECT 1 FROM job_postings WHERE job_id='JOB002');


INSERT INTO job_postings
(job_id,company_id,eligibility_id,job_profile,min_package,deadline)
SELECT 'JOB003',c.company_id,e.eligibility_id,'Associate Software Engineer','6.5 LPA','2026-09-25'
FROM company c JOIN eligibility e ON e.company_id=c.company_id AND e.passing_year=2027
WHERE c.hr_email='kavita@accenture.com'
AND NOT EXISTS (SELECT 1 FROM job_postings WHERE job_id='JOB003');


INSERT INTO job_postings
(job_id,company_id,eligibility_id,job_profile,min_package,deadline)
SELECT 'JOB004',c.company_id,e.eligibility_id,'Analyst','7 LPA','2026-10-05'
FROM company c JOIN eligibility e ON e.company_id=c.company_id AND e.passing_year=2027
WHERE c.hr_email='vikas@deloitte.com'
AND NOT EXISTS (SELECT 1 FROM job_postings WHERE job_id='JOB004');


INSERT INTO job_postings
(job_id,company_id,eligibility_id,job_profile,min_package,deadline)
SELECT 'JOB005',c.company_id,e.eligibility_id,'Project Engineer','5 LPA','2026-10-10'
FROM company c JOIN eligibility e ON e.company_id=c.company_id AND e.passing_year=2027
WHERE c.hr_email='sneha@wipro.com'
AND NOT EXISTS (SELECT 1 FROM job_postings WHERE job_id='JOB005');


-- Additional jobs

INSERT INTO job_postings
(job_id,company_id,eligibility_id,job_profile,min_package,deadline)
SELECT 'JOB006',c.company_id,e.eligibility_id,'Associate Software Engineer','5.8 LPA','2026-10-12'
FROM company c JOIN eligibility e ON e.company_id=c.company_id AND e.passing_year=2027
WHERE c.hr_email='neeraj@capgemini.com'
AND NOT EXISTS (SELECT 1 FROM job_postings WHERE job_id='JOB006');


INSERT INTO job_postings
(job_id,company_id,eligibility_id,job_profile,min_package,deadline)
SELECT 'JOB007',c.company_id,e.eligibility_id,'Programmer Analyst','6 LPA','2026-10-15'
FROM company c JOIN eligibility e ON e.company_id=c.company_id AND e.passing_year=2027
WHERE c.hr_email='asha@cognizant.com'
AND NOT EXISTS (SELECT 1 FROM job_postings WHERE job_id='JOB007');


INSERT INTO job_postings
(job_id,company_id,eligibility_id,job_profile,min_package,deadline)
SELECT 'JOB008',c.company_id,e.eligibility_id,'Graduate Engineer Trainee','5.2 LPA','2026-10-18'
FROM company c JOIN eligibility e ON e.company_id=c.company_id AND e.passing_year=2027
WHERE c.hr_email='vivek@hcl.com'
AND NOT EXISTS (SELECT 1 FROM job_postings WHERE job_id='JOB008');


INSERT INTO job_postings
(job_id,company_id,eligibility_id,job_profile,min_package,deadline)
SELECT 'JOB009',c.company_id,e.eligibility_id,'Software Developer','6.2 LPA','2026-10-20'
FROM company c JOIN eligibility e ON e.company_id=c.company_id AND e.passing_year=2027
WHERE c.hr_email='rohit@techmahindra.com'
AND NOT EXISTS (SELECT 1 FROM job_postings WHERE job_id='JOB009');


INSERT INTO job_postings
(job_id,company_id,eligibility_id,job_profile,min_package,deadline)
SELECT 'JOB010',c.company_id,e.eligibility_id,'Software Engineer','7 LPA','2026-10-22'
FROM company c JOIN eligibility e ON e.company_id=c.company_id AND e.passing_year=2027
WHERE c.hr_email='manish@persistent.com'
AND NOT EXISTS (SELECT 1 FROM job_postings WHERE job_id='JOB010');


INSERT INTO job_postings
(job_id,company_id,eligibility_id,job_profile,min_package,deadline)
SELECT 'JOB011',c.company_id,e.eligibility_id,'Software Engineer','6.5 LPA','2026-10-25'
FROM company c JOIN eligibility e ON e.company_id=c.company_id AND e.passing_year=2027
WHERE c.hr_email='priyanka@ltimindtree.com'
AND NOT EXISTS (SELECT 1 FROM job_postings WHERE job_id='JOB011');


INSERT INTO job_postings
(job_id,company_id,eligibility_id,job_profile,min_package,deadline)
SELECT 'JOB012',c.company_id,e.eligibility_id,'Associate Developer','8 LPA','2026-10-28'
FROM company c JOIN eligibility e ON e.company_id=c.company_id AND e.passing_year=2027
WHERE c.hr_email='arun@ibm.com'
AND NOT EXISTS (SELECT 1 FROM job_postings WHERE job_id='JOB012');


INSERT INTO job_postings
(job_id,company_id,eligibility_id,job_profile,min_package,deadline)
SELECT 'JOB013',c.company_id,e.eligibility_id,'Cloud Engineer','8.5 LPA','2026-10-30'
FROM company c JOIN eligibility e ON e.company_id=c.company_id AND e.passing_year=2027
WHERE c.hr_email='snehal@oracle.com'
AND NOT EXISTS (SELECT 1 FROM job_postings WHERE job_id='JOB013');


INSERT INTO job_postings
(job_id,company_id,eligibility_id,job_profile,min_package,deadline)
SELECT 'JOB014',c.company_id,e.eligibility_id,'Systems Development Engineer','10 LPA','2026-11-02'
FROM company c JOIN eligibility e ON e.company_id=c.company_id AND e.passing_year=2027
WHERE c.hr_email='rakesh@amazon.com'
AND NOT EXISTS (SELECT 1 FROM job_postings WHERE job_id='JOB014');


INSERT INTO job_postings
(job_id,company_id,eligibility_id,job_profile,min_package,deadline)
SELECT 'JOB015',c.company_id,e.eligibility_id,'Software Engineer','12 LPA','2026-11-05'
FROM company c JOIN eligibility e ON e.company_id=c.company_id AND e.passing_year=2027
WHERE c.hr_email='divya@microsoft.com'
AND NOT EXISTS (SELECT 1 FROM job_postings WHERE job_id='JOB015');


INSERT INTO job_postings
(job_id,company_id,eligibility_id,job_profile,min_package,deadline)
SELECT 'JOB016',c.company_id,e.eligibility_id,'Software Engineer','14 LPA','2026-11-08'
FROM company c JOIN eligibility e ON e.company_id=c.company_id AND e.passing_year=2027
WHERE c.hr_email='karan@google.com'
AND NOT EXISTS (SELECT 1 FROM job_postings WHERE job_id='JOB016');


INSERT INTO job_postings
(job_id,company_id,eligibility_id,job_profile,min_package,deadline)
SELECT 'JOB017',c.company_id,e.eligibility_id,'Associate Software Engineer','5.5 LPA','2026-11-10'
FROM company c JOIN eligibility e ON e.company_id=c.company_id AND e.passing_year=2027
WHERE c.hr_email='nitin@mphasis.com'
AND NOT EXISTS (SELECT 1 FROM job_postings WHERE job_id='JOB017');


INSERT INTO job_postings
(job_id,company_id,eligibility_id,job_profile,min_package,deadline)
SELECT 'JOB018',c.company_id,e.eligibility_id,'Full Stack Developer','6.8 LPA','2026-11-12'
FROM company c JOIN eligibility e ON e.company_id=c.company_id AND e.passing_year=2027
WHERE c.hr_email='pallavi@hexaware.com'
AND NOT EXISTS (SELECT 1 FROM job_postings WHERE job_id='JOB018');


INSERT INTO job_postings
(job_id,company_id,eligibility_id,job_profile,min_package,deadline)
SELECT 'JOB019',c.company_id,e.eligibility_id,'Java Developer','6.3 LPA','2026-11-15'
FROM company c JOIN eligibility e ON e.company_id=c.company_id AND e.passing_year=2027
WHERE c.hr_email='akshay@zensar.com'
AND NOT EXISTS (SELECT 1 FROM job_postings WHERE job_id='JOB019');


INSERT INTO job_postings
(job_id,company_id,eligibility_id,job_profile,min_package,deadline)
SELECT 'JOB020',c.company_id,e.eligibility_id,'Software Developer','7.2 LPA','2026-11-18'
FROM company c JOIN eligibility e ON e.company_id=c.company_id AND e.passing_year=2027
WHERE c.hr_email='monika@cybage.com'
AND NOT EXISTS (SELECT 1 FROM job_postings WHERE job_id='JOB020');


-- ============================================================
-- VERIFY
-- ============================================================

SELECT
    (SELECT COUNT(*) FROM students) AS total_students,
    (SELECT COUNT(*) FROM faculty) AS total_faculty,
    (SELECT COUNT(*) FROM company) AS total_companies,
    (SELECT COUNT(*) FROM eligibility) AS total_eligibility_rules,
    (SELECT COUNT(*) FROM job_postings) AS total_jobs,
    'Seed data loaded successfully.' AS status;