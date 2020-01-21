package com.salesforce.trellis.whitelist.impl;

import com.salesforce.trellis.rules.Coordinates;
import com.salesforce.trellis.rules.DependencyScope;
import com.salesforce.trellis.whitelist.builder.WhitelistedDependency;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import static java.util.Objects.requireNonNull;

/**
 * Describes a dependency that needs to be whitelisted.
 *
 * @author pcal
 * @since 0.0.1
 */
final class WhitelistedDependencyImpl implements WhitelistedDependency {

    // ===================================================================
    // Fields

    private final Coordinates fromModule;
    private final Coordinates toModule;
    private final DependencyScope scope;
    private final String reasonOrNull;

    // ===================================================================
    // Constructors

    WhitelistedDependencyImpl(final Coordinates fromModule,
                              final Coordinates toModule,
                              final DependencyScope scope,
                              final String reasonOrNull) {
        this.fromModule = requireNonNull(fromModule);
        this.toModule = requireNonNull(toModule);
        this.scope = requireNonNull(scope);
        this.reasonOrNull = reasonOrNull;
    }

    // ===================================================================
    // Public methods

    @Override
    public Coordinates getFromModule() {
        return this.fromModule;
    }

    @Override
    public Coordinates getToModule() {
        return this.toModule;
    }

    @Override
    public DependencyScope getScope() {
        return this.scope;
    }

    @Override
    public String getReason() {
        return this.reasonOrNull;
    }

    // ===================================================================
    // Object overrides

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof WhitelistedDependencyImpl)) {
            return false;
        }
        final WhitelistedDependencyImpl that = (WhitelistedDependencyImpl) o;
        return new EqualsBuilder().append(this.fromModule, that.fromModule).append(this.toModule, that.toModule)
            .append(this.scope, that.scope).append(this.reasonOrNull, that.reasonOrNull).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.fromModule).append(this.toModule).append(this.scope)
            .append(this.reasonOrNull).toHashCode();
    }
}
