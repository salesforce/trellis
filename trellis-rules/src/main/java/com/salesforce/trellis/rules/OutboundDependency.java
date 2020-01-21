package com.salesforce.trellis.rules;

import com.salesforce.trellis.rules.impl.OutboundDependencyImpl;

import static java.util.Objects.requireNonNull;

/**
 * Encapsulates information about a depedency from some given module to another.
 *
 * @author pcal
 * @since 0.0.5
 */
public interface OutboundDependency {

    /**
     * @param toModule The module to which dependencies are being checked.  Must be coordinates the
     * form "groupId:artifactId." May not be null.
     * @param scope The scope of the dependency being checked, e.g. "compile" or "test." May not
     * be null.
     * @param isDirect true if the dependency is declared in the pom (as opposed to being a transitive
     * dependency)
     * @param isOptional true if the dependency is optional
     */
    static OutboundDependency create(final Coordinates toModule,
                                     final DependencyScope scope,
                                     final boolean isDirect,
                                     final boolean isOptional) {
        return new OutboundDependencyImpl(requireNonNull(toModule), requireNonNull(scope), isDirect, isOptional);
    }

    Coordinates getTo();

    boolean isOptional();

    boolean isDirect();

    DependencyScope getScope();
}
