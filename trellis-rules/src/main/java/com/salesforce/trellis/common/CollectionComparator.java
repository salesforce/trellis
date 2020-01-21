package com.salesforce.trellis.common;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;


/**
 * Compares two collections.  Does parallel comparisons of corresponding elements.  Ties are resolve in favor of smaller
 * collections (because it just looks nicer in the file that way, IMHO).
 *
 * @author pcal
 * @since 0.0.1
 */
public class CollectionComparator<T extends Comparable> implements Comparator<Collection<T>> {

    public int compare(final Collection<T> c1, final Collection<T> c2) {
        final Iterator<T> i1 = c1.iterator(), i2 = c2.iterator();
        while (i1.hasNext()) {
            if (i2.hasNext()) {
                int componentsCompared = i1.next().compareTo(i2.next());
                if (componentsCompared != 0) {
                    return componentsCompared;
                }
            } else {
                // c2 has fewer components.  consider it to be lesser so it sorts earlier
                return -1;
            }
        }
        // they're equal so far.  if c2 still has elements, consider it greater.
        return i2.hasNext() ? 1 : 0;
    }
}
