/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.rules.impl;

import com.salesforce.trellis.rules.Coordinates;

import static java.util.Objects.requireNonNull;

/**
 * Matches a wildcard expression.
 *
 * @author pcal
 * @since 0.0.3
 */
final class NotMatcher implements Matcher {

    private final Matcher negatedMatcher;

    static Matcher get(Matcher negatedMatcher) {
        return new NotMatcher(negatedMatcher);
    }

    private NotMatcher(final Matcher negatedMatcher) {
        this.negatedMatcher = requireNonNull(negatedMatcher);
    }

    @Override
    public boolean matches(final Coordinates coordinates) {
        return !this.negatedMatcher.matches(coordinates);
    }

    @Override
    public int compareTo(Matcher o) {
        if (!(NotMatcher.class.isAssignableFrom(o.getClass()))) {
            return this.getClass().getSimpleName().compareTo(o.getClass().getSimpleName());
        } else {
            return this.toString().compareTo(o.toString());
        }
    }

    @Override
    public String toString() {
        return "! " + negatedMatcher.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NotMatcher)) return false;
        final NotMatcher that =
            (NotMatcher) o;
        return this.negatedMatcher.equals(that.negatedMatcher);
    }

    @Override
    public int hashCode() {
        return this.negatedMatcher.hashCode();
    }
}
