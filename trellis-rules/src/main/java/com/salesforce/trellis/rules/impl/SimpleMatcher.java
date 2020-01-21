package com.salesforce.trellis.rules.impl;

import com.salesforce.trellis.rules.Coordinates;

import static java.util.Objects.requireNonNull;

/**
 * Matches a specific coordinate.
 * <p/>
 * This class is immutable and thread-safe.
 *
 * @author pcal
 * @since 0.0.1
 */
final class SimpleMatcher implements Matcher {

    private final Coordinates coordinates;

    static SimpleMatcher get(String coordinates) {
        return new SimpleMatcher(Coordinates.parse(coordinates));
    }

    SimpleMatcher(Coordinates coordinates) {
        this.coordinates = requireNonNull(coordinates);
    }

    @Override
    public boolean matches(Coordinates thoseCoordinates) {
        requireNonNull(thoseCoordinates, "argument can't be null");
        return this.coordinates.equals(thoseCoordinates);
    }

    @Override
    public int compareTo(Matcher o) {
        if (!(SimpleMatcher.class.isAssignableFrom(o.getClass()))) {
            return this.getClass().getSimpleName().compareTo(o.getClass().getSimpleName());
        } else {
            return this.toString().compareTo(o.toString());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof com.salesforce.trellis.rules.impl.SimpleMatcher)) return false;
        final com.salesforce.trellis.rules.impl.SimpleMatcher that =
            (com.salesforce.trellis.rules.impl.SimpleMatcher) o;
        return this.coordinates.equals(that.coordinates);
    }

    @Override
    public int hashCode() {
        return this.coordinates.hashCode();
    }

    @Override
    public String toString() {
        return this.coordinates.toString();
    }
}
