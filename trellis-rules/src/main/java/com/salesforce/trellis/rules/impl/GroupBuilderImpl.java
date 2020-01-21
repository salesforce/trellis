package com.salesforce.trellis.rules.impl;

import com.salesforce.trellis.rules.builder.GroupBuilder;
import com.salesforce.trellis.rules.builder.RuleBuildingException;
import org.apache.commons.lang3.tuple.Pair;

import javax.lang.model.SourceVersion;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * @author pcal
 * @since 0.0.5
 */
class GroupBuilderImpl implements GroupBuilder {

    private final ExpressionResolver resolver;
    private Set<String> usedNames;
    private Consumer<Pair<String, Matcher>> consumer;
    private String name;
    private boolean isBuilt = false;
    private final List<Matcher> include = new ArrayList<>();
    private final List<Matcher> except = new ArrayList<>();

    GroupBuilderImpl(final ExpressionResolver resolver,
                     final Set<String> usedNames,
                     final Consumer<Pair<String, Matcher>> consumer) {
        this.resolver = requireNonNull(resolver);
        this.usedNames = requireNonNull(usedNames);
        this.consumer = requireNonNull(consumer);
    }

    @Override
    public GroupBuilder name(String name) throws RuleBuildingException {
        assertNotBuilt();
        requireNonNull(name, "name must not be null");
        if (!SourceVersion.isIdentifier(name)) {
            throw new RuleBuildingException(name + " is not a valid group name");
        }
        if (this.usedNames.contains(name)) {
            throw new RuleBuildingException("Duplicate group name " + name);
        }
        this.name = name;
        return this;
    }

    @Override
    public GroupBuilder include(String expression) throws RuleBuildingException {
        assertNotBuilt();
        this.include.add(this.resolver.resolve(expression));
        return this;
    }

    @Override
    public GroupBuilder except(String expression) throws RuleBuildingException {
        assertNotBuilt();
        this.except.add(this.resolver.resolve(expression));
        return this;
    }

    @Override
    public void build() throws RuleBuildingException {
        assertNotBuilt();
        if (this.name == null) {
            throw new RuleBuildingException("no group name provided");
        } else if (this.include.isEmpty()) {
            throw new RuleBuildingException("no group include provided");
        } else {
            consumer
                .accept(Pair.of(this.name, RuleSetBuilderImpl.mergeMatcherWithException(this.include, this.except)));
            this.isBuilt = true;
        }
    }

    private void assertNotBuilt() {
        if (isBuilt) throw new IllegalStateException("has already been built");
    }
}
