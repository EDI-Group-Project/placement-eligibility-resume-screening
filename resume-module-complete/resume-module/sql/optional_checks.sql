-- =====================================================================================================
-- NO DATABASE MIGRATION IS REQUIRED.
-- All three features read/write only the tables you already have:
--   applications  (job_id, student_id, status, screening_score, applied_at, ...)  UNIQUE(job_id, student_id)
--   resumes       (student_id UNIQUE, file_name, file_path, extracted_text, uploaded_at)
-- This file contains READ-ONLY checks you can run to confirm the assumptions the code relies on.
-- =====================================================================================================

-- 1. Confirm the structure (the DAOs use exactly these columns).
SHOW CREATE TABLE applications;
SHOW CREATE TABLE resumes;

-- 2. The applicant list reads "WHERE job_id = ?". The UNIQUE(job_id, student_id) index already serves it
--    (job_id is its first column), so no extra index is needed. Expect to see that unique key here:
SHOW INDEX FROM applications;

-- 3. Which status values exist in your data? (The applicant table shows applications.status exactly as stored.)
SELECT status, COUNT(*) AS applications FROM applications GROUP BY status;

-- 4. Applicant counts per job (what "Download applicants" will return for each job).
SELECT job_id, COUNT(*) AS applicants FROM applications GROUP BY job_id ORDER BY applicants DESC;

-- 5. Applications whose student no longer exists (the applicant list skips these). Adjust `students` /
--    `student_id` if your student table is named differently.
-- SELECT a.* FROM applications a LEFT JOIN students s ON s.student_id = a.student_id WHERE s.student_id IS NULL;

-- 6. OLD BEHAVIOUR CHECK. Before this change ApplicationDAO.saveScreeningScore() created an application row for
--    any screened student. The socket handler shipped earlier never called it, so normally there is nothing to
--    clean up. If YOUR code did call it, review rows that carry a score, and delete only the ones you know are not
--    real applications:
SELECT job_id, student_id, status, screening_score, applied_at
FROM applications
WHERE screening_score IS NOT NULL
ORDER BY applied_at;
