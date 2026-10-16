package com.placement.algorithms;

import com.placement.models.resume.RankedCandidate;

import java.util.ArrayList;
import java.util.List;

/**
 * Manual (from-scratch) Merge Sort ranking candidates by resume screening score.
 *
 * Sort order:
 *   1. totalScore   DESCENDING (highest screening score first)
 *   2. name         ASCENDING  (tie-breaker, alphabetical)
 *
 * Does NOT use Collections.sort / List.sort / Arrays.sort / Stream.sorted -
 * the divide-and-conquer algorithm is implemented directly below, the same
 * technique and structure used elsewhere in this project's DSA work.
 *
 * Complexity:
 *   Time:  O(n log n) best, average and worst case - unlike Bubble/Selection/
 *          Insertion Sort (all O(n^2) in the average/worst case), this holds
 *          regardless of how the candidate list is already ordered, which
 *          matters here because this list is re-sorted every time a new
 *          resume is screened or a ranking is re-requested for a job.
 *   Space: O(n) auxiliary (temporary buffer array used while merging).
 */
public final class CandidateMergeSort {

    private CandidateMergeSort() {
        // utility class - no instances
    }

    /**
     * Sorts a copy of the given list by totalScore descending, then name
     * ascending, and returns the sorted copy. The input list is left untouched.
     */
    public static List<RankedCandidate> sort(List<RankedCandidate> candidates) {
        if (candidates == null || candidates.size() <= 1) {
            return candidates == null ? new ArrayList<>() : new ArrayList<>(candidates);
        }

        RankedCandidate[] array = candidates.toArray(new RankedCandidate[0]);
        RankedCandidate[] buffer = new RankedCandidate[array.length];

        mergeSort(array, buffer, 0, array.length - 1);

        List<RankedCandidate> result = new ArrayList<>(array.length);
        for (RankedCandidate c : array) result.add(c);
        return result;
    }

    private static void mergeSort(RankedCandidate[] array, RankedCandidate[] buffer, int left, int right) {
        if (left >= right) {
            return; // 0 or 1 element - already "sorted"
        }

        int mid = left + (right - left) / 2;

        mergeSort(array, buffer, left, mid);
        mergeSort(array, buffer, mid + 1, right);
        merge(array, buffer, left, mid, right);
    }

    private static void merge(RankedCandidate[] array, RankedCandidate[] buffer, int left, int mid, int right) {
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
        while (leftIndex <= mid) {
            array[writeIndex++] = buffer[leftIndex++];
        }
        while (rightIndex <= right) {
            array[writeIndex++] = buffer[rightIndex++];
        }
    }

    /**
     * Comparison rule: true if `a` belongs at or before `b` in the target
     * order (totalScore descending, then name ascending). Kept as "<=" on
     * the tie-break so the merge is STABLE for equal (score, name) pairs.
     */
    private static boolean isInOrder(RankedCandidate a, RankedCandidate b) {
        int scoreCompare = Double.compare(b.getTotalScore(), a.getTotalScore()); // reversed -> descending
        if (scoreCompare != 0) {
            return scoreCompare < 0;
        }
        return a.getName().compareTo(b.getName()) <= 0;
    }
}
