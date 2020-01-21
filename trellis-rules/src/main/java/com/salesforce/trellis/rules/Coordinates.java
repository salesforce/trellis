package com.salesforce.trellis.rules;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import static java.util.Objects.requireNonNull;

/**
 * Coordinates of a maven artifact.
 * <p/>
 * This class is immutable and thread-safe.
 *
 * @author pcal
 * @since 0.0.1
 */
public class Coordinates implements Comparable<Coordinates> {

    private final String groupId;
    private final String artifactId;

    public static Coordinates parse(String coordinates) {
        final String[] parts = StringUtils.split(coordinates, ':');
        if (parts.length == 2) {
            return new Coordinates(parts[0].trim(), parts[1].trim());
        } else {
            throw new IllegalArgumentException("illegal coordinates " + coordinates);
        }
    }

    public static Coordinates of(String groupId, String artifactId) {
        return new Coordinates(groupId, artifactId);
    }

    public static Coordinates of(String groupId, String artifactId, String version) {
        return new Coordinates(groupId, artifactId); // FIXME
    }

    protected Coordinates(final String groupId, final String artifactId) {
        this.groupId = requireNonNull(groupId).trim();
        this.artifactId = requireNonNull(artifactId).trim();
    }

    public String getCanonicalString() {
        return this.groupId + ":" + this.artifactId;
    }

    @Override
    public String toString() {
        return getCanonicalString();
    }

    @Override
    public int compareTo(Coordinates o) {
        return this.getCanonicalString().compareTo(o.getCanonicalString());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Coordinates)) return false;
        final Coordinates that = (Coordinates) o;
        return new EqualsBuilder().
            append(this.groupId, that.groupId).
            append(this.artifactId, that.artifactId).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.groupId).append(this.artifactId).toHashCode();
    }
}
