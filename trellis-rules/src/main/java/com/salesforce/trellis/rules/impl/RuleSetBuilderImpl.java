package com.salesforce.trellis.rules.impl;

import com.google.common.collect.ImmutableList;
import com.salesforce.trellis.rules.RuleSet;
import com.salesforce.trellis.rules.builder.GroupSet;
import com.salesforce.trellis.rules.builder.RuleSetBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * This class is immutable and thread-safe.
 *
 * @author pcal
 * @since 0.0.1
 */
public class RuleSetBuilderImpl implements RuleSetBuilder, Consumer<Rule> {

    // ===================================================================
    // Fields

    private ExpressionResolver context;
    private Logger logger;
    private final ImmutableList.Builder<Rule> rules;
    private boolean isBuilt;

    // ===================================================================
    // Constructors

    public RuleSetBuilderImpl() {
        this.context = new ExpressionResolver.DefaultExpressionResolver();
        this.rules = ImmutableList.builder();
    }

    // ===================================================================
    // RuleSetBuilder impl

    @Override
    public RuleSetBuilder logger(final Logger logger) {
        assertNotBuilt();
        this.logger = requireNonNull(logger);
        return this;
    }

    @Override
    public RuleSetBuilder groups(final GroupSet groups) {
        this.context = new ExpressionResolver.WithGroupsExpressionResolver(((GroupSetImpl) groups).getGroupMatchers());
        return this;
    }

    @Override
    public RuleSetBuilder addRules(RuleSet otherRules) {
        this.rules.addAll(((RuleSetImpl) otherRules).getRules());
        return this;
    }

    @Override
    public RuleBuilder rule() {
        assertNotBuilt();
        return new RuleBuilderImpl(this.context, this);
    }

    @Override
    public RuleSet build() {
        assertNotBuilt();
        isBuilt = true;
        final ImmutableList<Rule> builtRules = this.rules.build();
        return new RuleSetImpl(builtRules, logger == null ? LoggerFactory.getLogger(this.getClass()) : logger);
    }

    // ===================================================================
    // Consumer implementation

    @Override
    public void accept(Rule rule) {
        this.rules.add(requireNonNull(rule));
    }

    // ===================================================================
    // Private methods

    private void assertNotBuilt() {
        if (isBuilt) throw new IllegalStateException("has already been built");
    }

    static Matcher mergeMatcherWithException(List<Matcher> matchers, List<Matcher> exceptions) {
        if (matchers.isEmpty() && exceptions.isEmpty()) {
            throw new IllegalStateException("they can't both be empty");
        }
        if (exceptions.isEmpty()) {
            return OrMatcher.get(matchers);
        } else if (matchers.isEmpty()) {
            return NotMatcher.get(OrMatcher.get(exceptions));
        } else {
            return AndMatcher.get(OrMatcher.get(matchers), NotMatcher.get(OrMatcher.get(exceptions)));
        }
    }
}
