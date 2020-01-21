package com.salesforce.trellis.rules.impl;

import com.salesforce.trellis.rules.Coordinates;

import java.util.Arrays;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Matches against a list coordinates other matchers.  We consider ourselves a match
 * if at least one of the composed matchers is a match.
 *
 * @author pcal
 * @since 0.0.1
 */
final class OrMatcher extends CompositeMatcher {

    static Matcher get(final Matcher... matchers) {
        return get(Arrays.asList(matchers));
    }

    static Matcher get(List<Matcher> matchers) {
        requireNonNull(matchers);
        matchers = flatten(matchers, OrMatcher.class);
        if (matchers.size() == 0) {
            throw new IllegalArgumentException("empty matchers");
        } else if (matchers.size() == 1) {
            return matchers.get(0);
        } else {
            return new OrMatcher(matchers);
        }
    }

    private OrMatcher(final List<Matcher> matchers) {
        super(matchers);
    }

    @Override
    public boolean matches(Coordinates coordinates) {
        for (final Matcher m : getMatchers()) {
            if (m.matches(coordinates)) return true;
        }
        return false;
    }

}
