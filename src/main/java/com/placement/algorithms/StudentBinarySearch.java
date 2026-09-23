package com.placement.algorithms;

import com.placement.models.Student;

import java.util.List;

/**
 * Manual (from-scratch) ITERATIVE Binary Search over the existing
 * {@link Student} model, keyed on studentId.
 *
 * Precondition: the input list MUST already be sorted by studentId ASCENDING.
 * StudentDAO.findAll() already runs "ORDER BY student_id", so its result can
 * be passed straight in without any extra sorting step.
 *
 * Does not use Collections.binarySearch or any other built-in search -
 * the loop below is the whole algorithm.
 *
 * Complexity:
 *   Time:  O(log n)
 *   Space: O(1) auxiliary (iterative - no recursion stack growth)
 */
public final class StudentBinarySearch {

    private StudentBinarySearch() {
        // utility class - no instances
    }

    /**
     * @param sortedByIdAscending list of students already sorted by studentId ascending
     * @param targetStudentId     the studentId to look for
     * @return the matching Student, or null if not found (or list is null/empty)
     */
    public static Student search(List<Student> sortedByIdAscending, int targetStudentId) {
        if (sortedByIdAscending == null || sortedByIdAscending.isEmpty()) {
            return null;
        }

        int low = 0;
        int high = sortedByIdAscending.size() - 1;

        while (low <= high) {
            int mid = low + (high - low) / 2; // avoids overflow vs (low+high)/2
            Student candidate = sortedByIdAscending.get(mid);
            int midId = candidate.getStudentId();

            if (midId == targetStudentId) {
                return candidate;
            } else if (midId < targetStudentId) {
                low = mid + 1;   // target is in the right half
            } else {
                high = mid - 1;  // target is in the left half
            }
        }

        return null; // not found
    }
}
