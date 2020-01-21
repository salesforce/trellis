package com.salesforce.trellis.common;

import com.google.common.collect.Collections2;
import com.google.common.math.BigIntegerMath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Helper class for ensuring that we are imposing a total ordering on instances of a given class.
 *
 * @author pcal
 * @since 0.0.1
 */
public class OrderingTester {

    /**
     * Creates all permutations of the given ordered lists, orders each permutation, and then asserts that the result is
     * equal to the original orderedList.  Note that the orderedList parameter is assumed to be ordered.
     */
    public <T> void testOrdering(List<? extends Comparable> orderedList) {
        if (orderedList.size() > 8) { // more than 8! permutations is just too much
            throw new IllegalArgumentException("too many elements");
        }
        {
            // check that the input list is sorted
            List<? extends Comparable> sanityCheck = new ArrayList<>(orderedList);
            Collections.sort(sanityCheck);
            assertEquals(sanityCheck, orderedList, "input list is not sorted");
        }
        final Set<List<? extends Comparable>> permutations;
        {
            // get all permutations of the list and sanity check that it's right
            permutations = new HashSet(Collections2.permutations(orderedList));
            assertEquals(BigIntegerMath.factorial(orderedList.size()).intValue(), permutations.size());
        }
        // sort every permutation and check that we get the same ordering
        int permutationNumber = 0;
        for (final List<? extends Comparable> permutation : permutations) {
            final List<? extends Comparable> orderedPermutation = new ArrayList<>(permutation);
            Collections.sort(orderedPermutation);
            assertEquals(orderedList, orderedPermutation,
                "permutation " + permutationNumber + " didn't sort correctly");
            permutationNumber++;
        }

    }
}
