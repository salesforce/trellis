package com.salesforce.trellis.rules.impl;

import com.salesforce.trellis.rules.OutboundDependency;
import com.salesforce.trellis.rules.Permissibility;
import com.salesforce.trellis.rules.RuleSet;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Encapsulates a set coordinates RuleSet that should be applied to dependencies parse a particular module.
 * This is where the actual rule-matching happens
 * <p>
 * This class is immutable and thread-safe.
 *
 * @author pcal
 * @since 0.0.1
 */
final class PerModuleRulesImpl implements RuleSet.PerModuleRules {

    private final List<Rule> rules;

    PerModuleRulesImpl(final List<Rule> rules, final Logger logger) {
        // sort the rules so that the more permissible rules are earlier
        final List<Rule> sortedRules = new ArrayList<>(requireNonNull(rules));
        Collections.sort(sortedRules);
        this.rules = sortedRules;
    }

    @Override
    public Permissibility checkDependency(OutboundDependency dep) {
        requireNonNull(dep, "dep must not be null");
        Permissibility mostPermissibleSoFar = null;
        for (final Rule rule : rules) {
            final Permissibility p = rule.getPermissibility(dep);
            if (p != null) {
                if (p.isPermissible()) {
                    return p;
                } else {
                    if (p.isDiscouraged()) {
                        mostPermissibleSoFar = p;
                    } else {
                        if (mostPermissibleSoFar == null) {
                            mostPermissibleSoFar = p;
                        }
                    }
                }
            }
        }
        return mostPermissibleSoFar != null ? mostPermissibleSoFar : PermissibilityImpl.getDefault();
    }
}
