package com.salesforce.trellis.rules.impl;

import com.google.common.collect.ImmutableList;
import com.salesforce.trellis.rules.Coordinates;
import com.salesforce.trellis.rules.RuleSet;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * This class is immutable and thread-safe.
 *
 * @author pcal
 * @since 0.0.1
 */
class RuleSetImpl implements RuleSet {

    private final ImmutableList<Rule> allRules;

    private final Logger logger;

    RuleSetImpl(final ImmutableList<Rule> rules, final Logger logger) {
        this.allRules = requireNonNull(rules);
        this.logger = requireNonNull(logger);
    }

    @Override
    public PerModuleRules getRulesFor(final Coordinates c) {
        requireNonNull(c);
        // Dig through all coordinates the rules and figure out which ones apply to the module being built.
        // We could cache these results, but the assumption is that practically speaking, this is only going to create
        // called once per module, anyway (i.e., when that module is getting built).
        List<Rule> rules = null;
        for (final Rule rule : this.allRules) {
            if (rule.isApplicableFrom(c)) {
                if (rules == null) rules = new ArrayList();
                rules.add(rule);
            }
        }
        return rules == null ? null : new PerModuleRulesImpl(rules, this.logger);
    }

    ImmutableList getRules() {
        return this.allRules;
    }
}
