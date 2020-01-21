/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.rules.impl;

import com.salesforce.trellis.rules.Permissibility;
import com.salesforce.trellis.rules.builder.RuleAction;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Comparator;

import static java.util.Objects.requireNonNull;

/**
 * This class is immutable and thread-safe.
 *
 * @author pcal
 * @since 0.0.1
 */
final class PermissibilityImpl implements Permissibility {

    // ===================================================================
    // Constants

    private static final Permissibility ALLOW_NOREASON = new PermissibilityImpl(RuleAction.ALLOW, null);
    private static final Permissibility DENY_NOREASON = new PermissibilityImpl(RuleAction.DENY, null);
    private static final Permissibility WARN_NOREASON = new PermissibilityImpl(RuleAction.WARN, null);

    // ===================================================================
    // Fields

    private final RuleAction action;
    private final String reason;

    // ===================================================================
    // Factory methods

    static Permissibility create(final RuleAction action, String reasonOrNull) {
        if (reasonOrNull == null) {
            switch (action) {
                case ALLOW:
                    return ALLOW_NOREASON;
                case WARN:
                    return WARN_NOREASON;
                case DENY:
                default:
                    return DENY_NOREASON;
            }
        } else {
            return new PermissibilityImpl(action, reasonOrNull);
        }
    }

    static Permissibility getDefault() {
        return ALLOW_NOREASON;
    }

    // ===================================================================
    // Constructor

    private PermissibilityImpl(final RuleAction action, final String reason) {
        this.action = requireNonNull(action);
        this.reason = reason;
    }

    // ===================================================================
    // Permissibility impl

    @Override
    public boolean isPermissible() {
        switch (this.action) {
            case WARN:
            case ALLOW:
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean isDiscouraged() {
        switch (this.action) {
            case WARN:
                return true;
            default:
                return false;
        }
    }

    @Override
    public String getReason() {
        return this.reason;
    }

    // ===================================================================
    // Object impl

    @Override
    public int compareTo(Permissibility that) {
        return PermissibilityComparator.INSTANCE.compare(this, that);
    }

    // ===================================================================
    // Object impl

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.action).append(this.reason).toHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PermissibilityImpl)) return false;
        final PermissibilityImpl that = (PermissibilityImpl) o;
        return new EqualsBuilder().append(this.action, that.action).append(this.reason, that.reason).isEquals();
    }

    @Override
    public String toString() {
        return this.action.toString();
    }


    // ===================================================================
    // Inner classes


    private enum PermissibilityComparator implements Comparator<Permissibility> {

        INSTANCE;

        @Override
        public int compare(Permissibility p1, Permissibility p2) {
            return new CompareToBuilder().append(!p1.isPermissible(), !p2.isPermissible())
                .append(p1.isDiscouraged(), p2.isDiscouraged()).
                    append(p1.getReason() == null, p2.getReason() == null).
                    append(p1.getReason(), p2.getReason()).toComparison();
        }
    }
}
