package com.placement.algorithms;

import com.placement.models.Student;
import com.placement.models.resume.RankedCandidate;
import com.placement.models.resume.ScreeningResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure in-memory tests for CandidateMergeSort / CandidateBinarySearch.
 * No MySQL / DatabaseConnection involved - objects are built by hand.
 */
class CandidateAlgorithmsTest {

    private static Student student(int id, String name) {
        Student s = new Student();
        s.setStudentId(id);
        s.setName(name);
        return s;
    }

    private static RankedCandidate candidate(int id, String name, double totalScore) {
        ScreeningResult result = new ScreeningResult();
        result.setStudentId(id);
        result.setTotalScore(totalScore);
        return new RankedCandidate(student(id, name), result);
    }

    // ---------------------------------------------------------------
    // CandidateMergeSort
    // ---------------------------------------------------------------

    @Test
    void mergeSort_multipleCandidates_sortsByScoreDescThenNameAsc() {
        List<RankedCandidate> input = new ArrayList<>(List.of(
                candidate(1, "Sneha", 55.0),
                candidate(2, "Rahul", 82.0),
                candidate(3, "Priya", 70.0),
                candidate(4, "Amit", 82.0)
        ));

        List<RankedCandidate> sorted = CandidateMergeSort.sort(input);

        assertEquals(4, sorted.size());
        // 82.0 tie -> Amit before Rahul (name ascending)
        assertEquals("Amit", sorted.get(0).getName());
        assertEquals(82.0, sorted.get(0).getTotalScore());
        assertEquals("Rahul", sorted.get(1).getName());
        assertEquals(82.0, sorted.get(1).getTotalScore());
        assertEquals("Priya", sorted.get(2).getName());
        assertEquals(70.0, sorted.get(2).getTotalScore());
        assertEquals("Sneha", sorted.get(3).getName());
        assertEquals(55.0, sorted.get(3).getTotalScore());

        // original list must be untouched
        assertEquals("Sneha", input.get(0).getName());
    }

    @Test
    void mergeSort_equalScores_ordersByNameAscending() {
        List<RankedCandidate> input = List.of(
                candidate(1, "Zara", 60.0),
                candidate(2, "Anil", 60.0),
                candidate(3, "Meena", 60.0)
        );

        List<RankedCandidate> sorted = CandidateMergeSort.sort(input);

        assertEquals(List.of("Anil", "Meena", "Zara"),
                sorted.stream().map(RankedCandidate::getName).toList());
    }

    @Test
    void mergeSort_emptyList_returnsEmptyList() {
        List<RankedCandidate> sorted = CandidateMergeSort.sort(new ArrayList<>());
        assertNotNull(sorted);
        assertTrue(sorted.isEmpty());
    }

    @Test
    void mergeSort_nullList_returnsEmptyList() {
        List<RankedCandidate> sorted = CandidateMergeSort.sort(null);
        assertNotNull(sorted);
        assertTrue(sorted.isEmpty());
    }

    @Test
    void mergeSort_oneCandidate_returnsSameCandidate() {
        List<RankedCandidate> input = List.of(candidate(1, "Solo", 40.0));
        List<RankedCandidate> sorted = CandidateMergeSort.sort(input);
        assertEquals(1, sorted.size());
        assertEquals("Solo", sorted.get(0).getName());
    }

    // ---------------------------------------------------------------
    // CandidateBinarySearch
    // ---------------------------------------------------------------

    private static List<Student> idSortedRoster() {
        // Must already be sorted by studentId ascending (as StudentDAO.findAll() returns it).
        return List.of(
                student(1, "Aarav"),
                student(2, "Isha"),
                student(3, "Rohan"),
                student(4, "Sneha"),
                student(5, "Vikram")
        );
    }

    @Test
    void binarySearch_existingStudent_isFound() {
        Student found = CandidateBinarySearch.search(idSortedRoster(), 3);
        assertNotNull(found);
        assertEquals("Rohan", found.getName());
    }

    @Test
    void binarySearch_nonExistingStudent_returnsNull() {
        Student found = CandidateBinarySearch.search(idSortedRoster(), 999);
        assertNull(found);
    }

    @Test
    void binarySearch_firstStudent_isFound() {
        Student found = CandidateBinarySearch.search(idSortedRoster(), 1);
        assertNotNull(found);
        assertEquals("Aarav", found.getName());
    }

    @Test
    void binarySearch_lastStudent_isFound() {
        Student found = CandidateBinarySearch.search(idSortedRoster(), 5);
        assertNotNull(found);
        assertEquals("Vikram", found.getName());
    }

    @Test
    void binarySearch_emptyList_returnsNull() {
        Student found = CandidateBinarySearch.search(new ArrayList<>(), 1);
        assertNull(found);
    }

    @Test
    void binarySearch_nullList_returnsNull() {
        Student found = CandidateBinarySearch.search(null, 1);
        assertNull(found);
    }
}
