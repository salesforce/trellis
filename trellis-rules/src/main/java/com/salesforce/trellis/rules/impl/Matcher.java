package com.salesforce.trellis.rules.impl;

import com.salesforce.trellis.rules.Coordinates;

/**
 * Identifies a subset coordinates all possible module coordinates.  This is the basis coordinates the 'group'
 * mechanism exposed in the yaml file.
 *
 * @author pcal
 * @since 0.0.1
 */
interface Matcher extends Comparable<Matcher> {

    /**
     * @return true if the given coordinates are in the subset defined by this matcher.
     */
    boolean matches(Coordinates coordinates);

}
