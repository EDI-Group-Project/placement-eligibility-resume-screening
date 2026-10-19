package com.placement.services.resume;

import com.placement.database.ApplicationDAO;
import com.placement.models.Student;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Builds the two Excel files staff can download: a (filtered) student list and the applicant list of one job.
 * Pure functions - no database, no sockets - so the column layout is defined in exactly one place and can be
 * tested without either. The password hash is never part of an export.
 */
public class ExportService {

    public static final List<String> STUDENT_COLUMNS = List.of(
            "PRN", "Name", "Email", "Department", "CGPA", "Passing Year", "Backlogs", "Semester", "Phone", "Skills");

    public static final List<String> APPLICANT_COLUMNS = concat(STUDENT_COLUMNS,
            List.of("Application Status", "Screening Score", "Applied At"));

    public byte[] studentsWorkbook(List<Student> students) throws IOException {
        List<List<Object>> rows = new ArrayList<>();
        for (Student s : students) rows.add(studentCells(s));
        return XlsxWriter.write("Students", STUDENT_COLUMNS, rows);
    }

    public byte[] applicantsWorkbook(List<ApplicationDAO.Applicant> applicants) throws IOException {
        List<List<Object>> rows = new ArrayList<>();
        for (ApplicationDAO.Applicant a : applicants) rows.add(applicantCells(a));
        return XlsxWriter.write("Applicants", APPLICANT_COLUMNS, rows);
    }

    /** Cells in {@link #STUDENT_COLUMNS} order. Also used for the on-screen table so both always agree. */
    public static List<Object> studentCells(Student s) {
        return new ArrayList<>(Arrays.<Object>asList(
                s.getPrn() != null ? s.getPrn() : String.valueOf(s.getStudentId()),   // same fallback as Student.toProtocolLine()
                s.getName(), s.getEmail(), s.getDepartment(), s.getCgpa(), s.getPassingYear(), s.getBacklogs(),
                s.getSemester() > 0 ? s.getSemester() : null,       // students.semester is NULL or 1..8; the model reads NULL as 0
                s.getPhone(), s.getSkills()));
    }

    /** Cells in {@link #APPLICANT_COLUMNS} order. */
    public static List<Object> applicantCells(ApplicationDAO.Applicant a) {
        List<Object> cells = studentCells(a.student());
        cells.add(a.status());
        cells.add(a.screeningScore());      // null (empty cell) until the application has been screened
        cells.add(a.appliedAt());
        return cells;
    }

    private static List<String> concat(List<String> first, List<String> second) {
        List<String> all = new ArrayList<>(first);
        all.addAll(second);
        return List.copyOf(all);
    }
}
