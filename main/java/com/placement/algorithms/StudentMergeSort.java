package com.placement.algorithms;

import com.placement.models.Student;

import java.util.ArrayList;
import java.util.List;

/**
 * Manual (from-scratch) Merge Sort over the existing {@link Student} model.
 *
 * Sort order:
 *   1. cgpa       DESCENDING (higher CGPA first)
 *   2. name       ASCENDING  (tie-breaker, alphabetical)
 *
 * Does not use Collections.sort / List.sort / Arrays.sort / Stream.sorted -
 * the divide-and-conquer merge sort is implemented directly below.
 *
 * Complexity:
 *   Time:  O(n log n) best, average and worst case
 *   Space: O(n) auxiliary (temporary arrays used during the merge step)
 *
 * Typical caller: StudentService, feeding it the result of an existing
 * eligibility lookup (e.g. eligibilityDAO.findEligible(job)) before it is
 * sent back to a dashboard, so the students display ranked by CGPA.
 */
public final class StudentMergeSort {

    private StudentMergeSort() {
        // utility class - no instances
    }

    /**
     * Sorts a copy of the given list by CGPA descending, then name ascending,
     * and returns the sorted copy. The input list is left untouched.
     *
     * @param students list of students to sort (may be eligible-only or the
     *                  full roster - this class doesn't care about eligibility,
     *                  it only sorts what it's given)
     * @return a new, sorted List<Student>. Empty list in -> empty list out.
     */
    public static List<Student> sort(List<Student> students) {
        if (students == null || students.size() <= 1) {
            return students == null ? new ArrayList<>() : new ArrayList<>(students);
        }

        // Copy into a plain array to sort - keeps the recursion simple and
        // avoids mutating the caller's list.
        Student[] array = students.toArray(new Student[0]);
        Student[] buffer = new Student[array.length];

        mergeSort(array, buffer, 0, array.length - 1);

        List<Student> result = new ArrayList<>(array.length);
        for (Student s : array) result.add(s);
        return result;
    }

    /** Recursively splits [left, right] in half, sorts each half, then merges them. */
    private static void mergeSort(Student[] array, Student[] buffer, int left, int right) {
        if (left >= right) {
            return; // 0 or 1 element - already "sorted"
        }

        int mid = left + (right - left) / 2;

        mergeSort(array, buffer, left, mid);
        mergeSort(array, buffer, mid + 1, right);
        merge(array, buffer, left, mid, right);
    }

    /** Merges two adjacent sorted runs, array[left..mid] and array[mid+1..right], in order. */
    private static void merge(Student[] array, Student[] buffer, int left, int mid, int right) {
        // Copy the range being merged into the buffer so we can read from it
        // while overwriting `array` in sorted order.
        for (int i = left; i <= right; i++) {
            buffer[i] = array[i];
        }

        int leftIndex = left;
        int rightIndex = mid + 1;
        int writeIndex = left;

        while (leftIndex <= mid && rightIndex <= right) {
            if (isInOrder(buffer[leftIndex], buffer[rightIndex])) {
                array[writeIndex++] = buffer[leftIndex++];
            } else {
                array[writeIndex++] = buffer[rightIndex++];
            }
        }

        // Copy any remaining elements from whichever half still has some.
        while (leftIndex <= mid) {
            array[writeIndex++] = buffer[leftIndex++];
        }
        while (rightIndex <= right) {
            array[writeIndex++] = buffer[rightIndex++];
        }
    }

    /**
     * Comparison rule: true if `a` belongs at or before `b` in the target order
     * (CGPA descending, then name ascending). Using ">= "/comparisons this way
     * keeps the merge STABLE for equal CGPA+name pairs.
     */
    private static boolean isInOrder(Student a, Student b) {
        int cgpaCompare = Double.compare(b.getCgpa(), a.getCgpa()); // reversed -> descending
        if (cgpaCompare != 0) {
            return cgpaCompare < 0;
        }
        String nameA = a.getName() == null ? "" : a.getName();
        String nameB = b.getName() == null ? "" : b.getName();
        return nameA.compareTo(nameB) <= 0;
    }
}
