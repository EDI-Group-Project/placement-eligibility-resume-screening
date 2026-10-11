USE placement_portal;

ALTER TABLE faculty DROP CHECK chk_faculty_role;
ALTER TABLE faculty
    ADD CONSTRAINT chk_faculty_role
    CHECK (role IN ('ADMIN','DIRECTOR','DEAN','TPO','TPC','FACULTY'));

SELECT 'Faculty role permissions migrated successfully.' AS status;
