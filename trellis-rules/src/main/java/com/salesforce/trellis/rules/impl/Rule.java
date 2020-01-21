/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.rules.impl;

import com.salesforce.trellis.rules.Coordinates;
import com.salesforce.trellis.rules.DependencyScope;
import com.salesforce.trellis.rules.OutboundDependency;
import com.salesforce.trellis.rules.Permissibility;
import com.salesforce.trellis.rules.builder.RuleDistance;
import com.salesforce.trellis.rules.builder.RuleOptionality;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Arrays;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Encapsulates a single resolved rule.
 * <p>
 * This class is immutable and thread-safe.
 *
 * @author pcal
 * @since 0.0.1
 */
class Rule implements Comparable<Rule> {

    // ===================================================================
    // Fields

    private final Matcher fromMatcher;
    private final Matcher toMatcher;
    private final Permissibility perm;
    private final Set<? extends DependencyScope> applicableScopes;
    private final RuleDistance distance;
    private final RuleOptionality optionality;

    // ===================================================================
    // Constructor

    Rule(final Matcher from,
         final Matcher to,
         final Permissibility perm,
         final Set<? extends DependencyScope> applicableScopes,
         final RuleDistance distance,
         final RuleOptionality optionality) {
        this.fromMatcher = requireNonNull(from);
        this.toMatcher = requireNonNull(to);
        this.applicableScopes = requireNonNull(applicableScopes);
        this.perm = requireNonNull(perm);
        this.distance = requireNonNull(distance);
        this.optionality = requireNonNull(optionality);
    }

    // ===================================================================
    // Package methods

    /**
     * @return true if this rule is applicable for checking dependencies parse the given module.
     */
    boolean isApplicableFrom(final Coordinates c) {
        return fromMatcher.matches(requireNonNull(c));
    }

    /**
     * @return the permissibility coordinates dependencies to the given module.  Returns null if this rule doesn't have
     * anything to say about the permissibility.
     */
    Permissibility getPermissibility(final OutboundDependency dep) {
        if (applicableScopes.contains(dep.getScope()) && //
            matchesDirect(dep.isDirect(), this.distance) && //
            matchesOptional(dep.isOptional(), this.optionality) && //
            toMatcher.matches(dep.getTo())) {
            return perm;
        }
        return null;
    }


    // ===================================================================
    // Object impl

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof Rule)) return false;
        final Rule that = (Rule) o;
        return new EqualsBuilder().append(this.perm, that.perm).append(this.fromMatcher, that.fromMatcher)
            .append(this.toMatcher, that.toMatcher).append(this.distance, that.distance)
            .append(this.applicableScopes, that.applicableScopes).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.perm).append(this.fromMatcher).append(this.toMatcher)
            .append(this.distance).append(this.applicableScopes).toHashCode();
    }

    @Override
    public String toString() {
        return this.perm + " " + this.fromMatcher + "->" + this.toMatcher;
    }

    // ===================================================================
    // Comparable impl

    @Override
    public int compareTo(Rule that) {
        // dump the applicableScopes into arrays that we can compare
        final DependencyScope[] thisScopes = this.applicableScopes.toArray(new DependencyScope[0]);
        Arrays.sort(thisScopes);
        final DependencyScope[] thatScopes = that.applicableScopes.toArray(new DependencyScope[0]);
        Arrays.sort(thatScopes);
        return new CompareToBuilder().append(this.perm, that.perm).append(this.fromMatcher, that.fromMatcher)
            .append(this.toMatcher, that.toMatcher).append(this.distance, that.distance)
            .append(thisScopes, thatScopes).toComparison();
    }

    // ===================================================================
    // Utility methods

    /**
     * @return whether the given directness value matches the given RuleDistance.
     */
    private boolean matchesDirect(boolean isDirect, RuleDistance distance) {
        switch (distance) {
            case DIRECT_ONLY:
                return isDirect;
            case TRANSITIVE_ONLY:
                return !isDirect;
            default:
                return true;
        }
    }

    /**
     * @return whether the given optionality value matches the given RuleOptionality.
     */
    private boolean matchesOptional(boolean isOptional, RuleOptionality optionality) {
        switch (optionality) {
            case OPTIONAL_ONLY:
                return isOptional;
            case NON_OPTIONAL_ONLY:
                return !isOptional;
            default:
                return true;
        }
    }
}
