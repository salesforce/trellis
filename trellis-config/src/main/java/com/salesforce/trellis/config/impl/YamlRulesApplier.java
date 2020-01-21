/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.config.impl;

import com.salesforce.trellis.config.impl.YamlModel.RuleModel;
import com.salesforce.trellis.rules.DependencyScope;
import com.salesforce.trellis.rules.builder.RuleAction;
import com.salesforce.trellis.rules.builder.RuleBuildingException;
import com.salesforce.trellis.rules.builder.RuleDistance;
import com.salesforce.trellis.rules.builder.RuleOptionality;
import com.salesforce.trellis.rules.builder.RuleSetBuilder;

import java.util.Collection;

import static com.salesforce.trellis.rules.builder.RuleSetBuilder.RuleBuilder;
import static java.util.Objects.requireNonNull;

/**
 * Walks a YamlModel and applies it to a given RuleSetBuilder.
 * <p/>
 * Note that this doesn't do anything with imports or whitelists.
 *
 * @author pcal
 * @since 0.0.1
 */
class YamlRulesApplier {

    // ===================================================================
    // Fields

    private final ConfigErrorReporter errorLog;
    private final RuleSetBuilder rulesBuilder;

    // ===================================================================
    // Constructor

    YamlRulesApplier(final RuleSetBuilder rulesBuilder, final ConfigErrorReporter errorLog) {
        this.rulesBuilder = requireNonNull(rulesBuilder);
        this.errorLog = requireNonNull(errorLog);
    }

    // ===================================================================
    // Package methods

    /**
     * Applies everything in the given model to our RuleSetBuilder.
     */
    void apply(final Collection<RuleModel> rules) {
        if (rules != null) {
            for (final RuleModel rule : rules) {
                apply(rule);
            }
        }
    }

    /**
     * Applies only the given rule to the RuleSetBuilder.
     */
    void apply(final RuleModel rule) {
        requireNonNull(rule, "null rule");
        boolean errorsEncountered = false;
        final RuleBuilder rb = this.rulesBuilder.rule();
        if (rule.getAction() != null) {
            RuleAction action = null;
            try {
                action = RuleAction.valueOf(rule.getAction().toString());
            } catch (IllegalArgumentException e) {
                this.errorLog.error(rule.getAction().getLocation(), e);
                errorsEncountered = true;
            }
            if (action != null) rb.action(action);
        }
        if (rule.getFrom() != null) {
            for (SourceLocatableString f : rule.getFrom()) {
                try {
                    rb.from(f.toString());
                } catch (RuleBuildingException e) {
                    this.errorLog.error(f.getLocation(), e);
                    errorsEncountered = true;
                }
            }
        }
        if (rule.getExceptFrom() != null) {
            for (SourceLocatableString f : rule.getExceptFrom()) {
                try {
                    rb.exceptFrom(f.toString());
                } catch (RuleBuildingException e) {
                    this.errorLog.error(f.getLocation(), e);
                    errorsEncountered = true;
                }
            }
        }
        if (rule.getTo() != null) {
            for (SourceLocatableString t : rule.getTo()) {
                try {
                    rb.to(t.toString());
                } catch (RuleBuildingException e) {
                    this.errorLog.error(t.getLocation(), e);
                    errorsEncountered = true;
                }
            }
        }
        if (rule.getExceptTo() != null) {
            for (SourceLocatableString t : rule.getExceptTo()) {
                try {
                    rb.exceptTo(t.toString());
                } catch (RuleBuildingException e) {
                    this.errorLog.error(t.getLocation(), e);
                    errorsEncountered = true;
                }
            }
        }
        if (rule.getScope() != null) {
            DependencyScope scope = null;
            try {
                scope = DependencyScope.parse(rule.getScope().toString());
            } catch (IllegalArgumentException e) {
                this.errorLog.error(rule.getScope().getLocation(), e);
                errorsEncountered = true;
            }
            if (scope != null) rb.scope(scope);
        }
        if (rule.getDistance() != null) {
            RuleDistance distance = null;
            try {
                distance = RuleDistance.valueOf(rule.getDistance().toString());
            } catch (IllegalArgumentException e) {
                this.errorLog.error(rule.getDistance().getLocation(), e);
                errorsEncountered = true;
            }
            if (distance != null) rb.distance(distance);
        }
        if (rule.getOptionality() != null) {
            RuleOptionality optionality = null;
            try {
                optionality = RuleOptionality.valueOf(rule.getOptionality().toString());
            } catch (IllegalArgumentException e) {
                this.errorLog.error(rule.getDistance().getLocation(), e);
                errorsEncountered = true;
            }
            if (optionality != null) rb.optionality(optionality);
        }
        if (rule.getReason() != null) {
            rb.reason(rule.getReason().toString());
        }
        if (!errorsEncountered) {
            try {
                rb.build();
            } catch (RuleBuildingException e) {
                this.errorLog.error(rule.getLocation(), e);
            }
        }
    }

}
