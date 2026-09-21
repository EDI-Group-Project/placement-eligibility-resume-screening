package com.placement.algorithms;

import com.placement.models.Student;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure in-memory tests for StudentMergeSort / StudentBinarySearch.
 * No MySQL / DatabaseConnection involved - Student objects are built by hand.
 */
class StudentAlgorithmsTest {

    private static Student student(int id, String name, double cgpa) {
        Student s = new Student();
        s.setStudentId(id);
        s.setName(name);
        s.setCgpa(cgpa);
        return s;
    }

    // ---------------------------------------------------------------
    // StudentMergeSort
    // ---------------------------------------------------------------

    @Test
    void mergeSort_multipleStudents_sortsByCgpaDescThenNameAsc() {
        List<Student> input = new ArrayList<>(List.of(
                student(1, "Sneha", 8.90),
                student(2, "Rahul", 9.50),
                student(3, "Priya", 9.20),
                student(4, "Amit", 9.50)
        ));

        List<Student> sorted = StudentMergeSort.sort(input);

        assertEquals(4, sorted.size());
        // 9.50 tie -> Amit before Rahul (name ascending)
        assertEquals("Amit", sorted.get(0).getName());
        assertEquals(9.50, sorted.get(0).getCgpa());
        assertEquals("Rahul", sorted.get(1).getName());
        assertEquals(9.50, sorted.get(1).getCgpa());
        assertEquals("Priya", sorted.get(2).getName());
        assertEquals(9.20, sorted.get(2).getCgpa());
        assertEquals("Sneha", sorted.get(3).getName());
        assertEquals(8.90, sorted.get(3).getCgpa());

        // original list must be untouched
        assertEquals("Sneha", input.get(0).getName());
    }

    @Test
    void mergeSort_equalCgpa_ordersByNameAscending() {
        List<Student> input = List.of(
                student(1, "Zara", 7.5),
                student(2, "Anil", 7.5),
                student(3, "Meena", 7.5)
        );

        List<Student> sorted = StudentMergeSort.sort(input);

        assertEquals(List.of("Anil", "Meena", "Zara"),
                sorted.stream().map(Student::getName).toList());
    }

    @Test
    void mergeSort_emptyList_returnsEmptyList() {
        List<Student> sorted = StudentMergeSort.sort(new ArrayList<>());
        assertNotNull(sorted);
        assertTrue(sorted.isEmpty());
    }

    @Test
    void mergeSort_nullList_returnsEmptyList() {
        List<Student> sorted = StudentMergeSort.sort(null);
        assertNotNull(sorted);
        assertTrue(sorted.isEmpty());
    }

    @Test
    void mergeSort_oneStudent_returnsSameStudent() {
        List<Student> input = List.of(student(1, "Solo", 8.0));
        List<Student> sorted = StudentMergeSort.sort(input);
        assertEquals(1, sorted.size());
        assertEquals("Solo", sorted.get(0).getName());
    }

    // ---------------------------------------------------------------
    // StudentBinarySearch
    // ---------------------------------------------------------------

    private static List<Student> idSortedRoster() {
        // Must already be sorted by studentId ascending (as StudentDAO.findAll() returns it).
        return List.of(
                student(1, "Aarav", 8.4),
                student(2, "Isha", 7.1),
                student(3, "Rohan", 6.8),
                student(4, "Sneha", 9.0),
                student(5, "Vikram", 6.2)
        );
    }

    @Test
    void binarySearch_existingStudent_isFound() {
        Student found = StudentBinarySearch.search(idSortedRoster(), 3);
        assertNotNull(found);
        assertEquals("Rohan", found.getName());
    }

    @Test
    void binarySearch_nonExistingStudent_returnsNull() {
        Student found = StudentBinarySearch.search(idSortedRoster(), 999);
        assertNull(found);
    }

    @Test
    void binarySearch_firstStudent_isFound() {
        Student found = StudentBinarySearch.search(idSortedRoster(), 1);
        assertNotNull(found);
        assertEquals("Aarav", found.getName());
    }

    @Test
    void binarySearch_lastStudent_isFound() {
        Student found = StudentBinarySearch.search(idSortedRoster(), 5);
        assertNotNull(found);
        assertEquals("Vikram", found.getName());
    }

    @Test
    void binarySearch_emptyList_returnsNull() {
        Student found = StudentBinarySearch.search(new ArrayList<>(), 1);
        assertNull(found);
    }

    @Test
    void binarySearch_nullList_returnsNull() {
        Student found = StudentBinarySearch.search(null, 1);
        assertNull(found);
    }
}
