/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.rules.impl;

import com.salesforce.trellis.rules.DependencyScope;
import com.salesforce.trellis.rules.Permissibility;
import com.salesforce.trellis.rules.builder.RuleAction;
import com.salesforce.trellis.rules.builder.RuleDistance;
import com.salesforce.trellis.rules.builder.RuleOptionality;
import com.salesforce.trellis.rules.builder.RuleSetBuilder.RuleBuilder;
import com.salesforce.trellis.rules.builder.RuleBuildingException;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * This class is immutable and thread-safe.
 *
 * @author pcal
 * @since 0.0.1
 */
class RuleBuilderImpl implements RuleBuilder {

    // ===================================================================
    // Fields

    private final ExpressionResolver resolver;
    private final Consumer<Rule> consumer;
    private RuleAction action;
    private final List<Matcher> from = new ArrayList<>();
    private final List<Matcher> to = new ArrayList<>();
    private final List<Matcher> exceptFrom = new ArrayList<>();
    private final List<Matcher> exceptTo = new ArrayList<>();
    private EnumSet<DependencyScopeFactory.MavenDependencyScope> applicableScopes;
    private String reason;
    private boolean isBuilt = false;
    private RuleDistance distance;
    private RuleOptionality optionality;

    // ===================================================================
    // Constructor

    RuleBuilderImpl(final ExpressionResolver context, final Consumer<Rule> consumer) {
        this.resolver = context;
        this.consumer = requireNonNull(consumer);
    }

    // ===================================================================
    // RuleBuilder implementation

    @Override
    public RuleBuilder action(final RuleAction action) {
        assertNotBuilt();
        this.action = requireNonNull(action, "null action");
        return this;
    }

    @Override
    public RuleBuilder from(final String expression) throws RuleBuildingException {
        assertNotBuilt();
        from.add(this.resolver.resolve(expression));
        return this;
    }

    @Override
    public RuleBuilder exceptFrom(final String expression) throws RuleBuildingException {
        assertNotBuilt();
        exceptFrom.add(this.resolver.resolve(expression));
        return this;
    }

    @Override
    public RuleBuilder to(final String expression) throws RuleBuildingException {
        assertNotBuilt();
        to.add(this.resolver.resolve(expression));
        return this;
    }

    @Override
    public RuleBuilder exceptTo(final String expression) throws RuleBuildingException {
        assertNotBuilt();
        exceptTo.add(this.resolver.resolve(expression));
        return this;
    }

    @Override
    public RuleBuilder scope(final DependencyScope scope) {
        assertNotBuilt();
        requireNonNull(scope, "scope must not be null");
        if (!(scope instanceof DependencyScopeFactory.MavenDependencyScope)) {
            throw new IllegalArgumentException("bad scope " + scope);
        }
        final DependencyScopeFactory.MavenDependencyScope mavenScope =
            (DependencyScopeFactory.MavenDependencyScope) scope;
        if (this.applicableScopes == null) {
            this.applicableScopes = EnumSet.of(mavenScope);
        } else {
            this.applicableScopes.add(mavenScope);
        }
        return this;
    }

    @Override
    public RuleBuilder distance(final RuleDistance distance) {
        this.distance = requireNonNull(distance);
        return this;
    }

    @Override
    public RuleBuilder optionality(RuleOptionality optionality) {
        this.optionality = requireNonNull(optionality);
        return this;
    }

    @Override
    public RuleBuilder reason(final String message) {
        assertNotBuilt();
        this.reason = requireNonNull(message);
        return this;
    }

    @Override
    public void build() throws RuleBuildingException {
        assertNotBuilt();
        isBuilt = true;
        this.consumer.accept(buildRule());
    }

    // ===================================================================
    // Package methods fo run

    Rule buildRule() throws RuleBuildingException {
        if (action == null) {
            throw new RuleBuildingException("'action' was not set");
        }
        if (this.from.isEmpty() && this.exceptFrom.isEmpty()) {
            throw new RuleBuildingException("must provide 'from' or 'exceptFrom' expressions");
        }
        if (this.to.isEmpty() && this.exceptTo.isEmpty()) {
            throw new RuleBuildingException("must provide 'to' or 'exceptTo' expressions");
        }
        if (this.applicableScopes == null) {
            // default is that the rule applies to all dependency scopes
            applicableScopes = DependencyScopeFactory.MavenDependencyScope.ANY;
        }
        if (this.distance == null) {
            this.distance = RuleDistance.ANY;
        }
        if (this.optionality == null) {
            this.optionality = RuleOptionality.ANY;
        }
        final Matcher fromMatcher = RuleSetBuilderImpl.mergeMatcherWithException(this.from, this.exceptFrom);
        final Matcher toMatcher = RuleSetBuilderImpl.mergeMatcherWithException(this.to, this.exceptTo);
        final Permissibility perm = PermissibilityImpl.create(action, this.reason);
        return new Rule(fromMatcher, toMatcher, perm, this.applicableScopes, this.distance, this.optionality);
    }

    private void assertNotBuilt() {
        if (isBuilt) throw new IllegalStateException("has already been built");
    }
}
