package com.salesforce.trellis.rules.impl;

import com.salesforce.trellis.rules.Coordinates;
import org.apache.commons.io.FilenameUtils;

import static java.util.Objects.requireNonNull;

/**
 * Matches a wildcard expression.
 *
 * @author pcal
 * @since 0.0.1
 */
final class WildcardMatcher implements Matcher {

    private final String expression;

    WildcardMatcher(String expression) {
        this.expression = requireNonNull(expression);
    }

    @Override
    public boolean matches(Coordinates coordinates) {
        // TODO figure out if this is better than regexes
        return FilenameUtils.wildcardMatch(coordinates.getCanonicalString(), this.expression);
    }

    @Override
    public int compareTo(Matcher o) {
        return this.toString().compareTo(o.toString());
    }

    @Override
    public String toString() {
        return this.expression;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof com.salesforce.trellis.rules.impl.WildcardMatcher)) return false;
        final com.salesforce.trellis.rules.impl.WildcardMatcher that =
            (com.salesforce.trellis.rules.impl.WildcardMatcher) o;
        return this.expression.equals(that.expression);
    }

    @Override
    public int hashCode() {
        return this.expression.hashCode();
    }
}
