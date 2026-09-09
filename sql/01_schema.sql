CREATE DATABASE IF NOT EXISTS placement_portal
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE placement_portal;

CREATE TABLE IF NOT EXISTS students (
    student_id INT AUTO_INCREMENT PRIMARY KEY,
    prn VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    department VARCHAR(50) NOT NULL,
    cgpa DECIMAL(4,2) NOT NULL,
    passing_year YEAR NOT NULL,
    backlogs INT NOT NULL DEFAULT 0,
    semester INT,
    phone VARCHAR(15),
    skills VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_student_cgpa CHECK (cgpa BETWEEN 0 AND 10),
    CONSTRAINT chk_student_backlogs CHECK (backlogs >= 0),
    CONSTRAINT chk_student_semester CHECK (semester IS NULL OR semester BETWEEN 1 AND 8)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS faculty (
    faculty_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    department VARCHAR(50),
    phone VARCHAR(15),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_faculty_role CHECK (role IN ('DIRECTOR','DEAN','TPO','TPC','FACULTY'))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS company (
    company_id INT AUTO_INCREMENT PRIMARY KEY,
    company_name VARCHAR(100) NOT NULL,
    industry_type VARCHAR(50),
    hr_name VARCHAR(100) NOT NULL,
    hr_email VARCHAR(100) NOT NULL UNIQUE,
    hr_password VARCHAR(255) NOT NULL,
    hr_phone VARCHAR(15),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS eligibility (
    eligibility_id INT AUTO_INCREMENT PRIMARY KEY,
    company_id INT NOT NULL,
    min_cgpa DECIMAL(4,2) NOT NULL,
    max_backlogs INT NOT NULL DEFAULT 0,
    department VARCHAR(255) NOT NULL,
    passing_year YEAR NOT NULL,
    required_skills VARCHAR(255),
    created_by INT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_eligibility_company FOREIGN KEY (company_id) REFERENCES company(company_id),
    CONSTRAINT fk_eligibility_creator FOREIGN KEY (created_by) REFERENCES faculty(faculty_id),
    CONSTRAINT chk_eligibility_cgpa CHECK (min_cgpa BETWEEN 0 AND 10),
    CONSTRAINT chk_eligibility_backlogs CHECK (max_backlogs >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS job_postings (
    job_id VARCHAR(40) PRIMARY KEY,
    company_id INT NOT NULL,
    eligibility_id INT NOT NULL UNIQUE,
    job_profile VARCHAR(100) NOT NULL,
    min_package VARCHAR(50),
    deadline DATE NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_job_company FOREIGN KEY (company_id) REFERENCES company(company_id),
    CONSTRAINT fk_job_eligibility FOREIGN KEY (eligibility_id) REFERENCES eligibility(eligibility_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS notifications (
    notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id VARCHAR(40),
    subject VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    recipient_group VARCHAR(100) NOT NULL,
    recipient_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_job FOREIGN KEY (job_id) REFERENCES job_postings(job_id)
        ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS notification_recipients (
    notification_id BIGINT NOT NULL,
    student_id INT NOT NULL,
    delivered BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (notification_id, student_id),
    CONSTRAINT fk_recipient_notification FOREIGN KEY (notification_id)
        REFERENCES notifications(notification_id) ON DELETE CASCADE,
    CONSTRAINT fk_recipient_student FOREIGN KEY (student_id)
        REFERENCES students(student_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS resumes (
    resume_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500),
    extracted_text LONGTEXT,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_student_resume UNIQUE (student_id),
    CONSTRAINT fk_resume_student FOREIGN KEY (student_id)
        REFERENCES students(student_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS applications (
    application_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id VARCHAR(40) NOT NULL,
    student_id INT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'APPLIED',
    screening_score DECIMAL(5,2),
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_job_student UNIQUE (job_id, student_id),
    CONSTRAINT fk_application_job FOREIGN KEY (job_id)
        REFERENCES job_postings(job_id) ON DELETE CASCADE,
    CONSTRAINT fk_application_student FOREIGN KEY (student_id)
        REFERENCES students(student_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_students_department_year ON students(department, passing_year);
CREATE INDEX idx_students_cgpa ON students(cgpa);
CREATE INDEX idx_eligibility_company ON eligibility(company_id);
CREATE INDEX idx_notifications_job ON notifications(job_id);
CREATE INDEX idx_applications_status ON applications(status);

SELECT 'Schema created successfully.' AS status;
