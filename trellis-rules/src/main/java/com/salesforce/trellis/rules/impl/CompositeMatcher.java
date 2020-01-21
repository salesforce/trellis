/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.rules.impl;

import com.google.common.collect.ImmutableList;
import com.salesforce.trellis.common.CollectionComparator;

import java.util.Iterator;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Base class for matchers that are composed of a list of other matchers (And and Not).
 *
 * @author pcal
 * @since 0.0.1
 */
abstract class CompositeMatcher implements Matcher {

    private final List<Matcher> matchers;

    CompositeMatcher(final List<Matcher> matchers) {
        requireNonNull(matchers);
        if (matchers.size() < 1) {
            throw new IllegalArgumentException("what is the point?");
        }
        this.matchers = requireNonNull(matchers);
    }

    static <T extends CompositeMatcher> List<Matcher> flatten(final List<Matcher> matchers,
                                                              final Class<T> clazz) {
        final ImmutableList.Builder<Matcher> flattened = ImmutableList.builder();
        final Iterator<Matcher> i = matchers.iterator();
        while (i.hasNext()) {
            final Matcher m = i.next();
            if (m.getClass().equals(clazz)) {
                flattened.addAll(((CompositeMatcher) m).matchers);
            } else {
                flattened.add(m);
            }
        }
        return flattened.build();
    }

    List<Matcher> getMatchers() {
        return matchers;
    }

    @Override
    public int compareTo(Matcher o) {
        if (!(o.getClass().equals(this.getClass()))) {
            return this.getClass().getSimpleName().compareTo(o.getClass().getSimpleName());
        } else {
            final CompositeMatcher that = (CompositeMatcher) o;
            return new CollectionComparator().compare(this.matchers, that.matchers);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(this.getClass().equals(o.getClass()))) return false;
        final CompositeMatcher that =
            (CompositeMatcher) o;
        return this.matchers.equals(that.matchers);
    }

    @Override
    public int hashCode() {
        return this.matchers.hashCode();
    }

    @Override
    public String toString() {
        return this.matchers.toString();
    }
}
