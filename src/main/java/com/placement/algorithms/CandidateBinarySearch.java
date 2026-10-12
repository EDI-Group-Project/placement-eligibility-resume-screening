package com.placement.algorithms;

import com.placement.models.Student;

import java.util.List;

/**
 * Manual (from-scratch) ITERATIVE Binary Search over the existing
 * {@link Student} model, keyed on studentId.
 *
 * Precondition: the input list MUST already be sorted by studentId
 * ASCENDING. The existing {@code StudentDAO.findAll()} already runs
 * "ORDER BY student_id", so its result can be passed straight in without
 * any extra sorting step - and in particular, this class never sorts data
 * itself before searching it.
 *
 * Does NOT use Collections.binarySearch or any other built-in search - the
 * loop below is the whole algorithm.
 *
 * Complexity:
 *   Time:  O(log n) - at ~100 candidates that's about 7 comparisons versus
 *          up to 100 for a linear scan; the gap widens as the candidate
 *          pool grows (e.g. ~9 comparisons at 500 candidates).
 *   Space: O(1) auxiliary (iterative - no recursion stack growth).
 */
public final class CandidateBinarySearch {

    private CandidateBinarySearch() {
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
