/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
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
