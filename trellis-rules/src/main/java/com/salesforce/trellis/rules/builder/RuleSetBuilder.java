/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.rules.builder;

import com.salesforce.trellis.rules.DependencyScope;
import com.salesforce.trellis.rules.RuleSet;
import com.salesforce.trellis.rules.impl.RuleSetBuilderImpl;
import org.slf4j.Logger;

/**
 * Called by the parser to populate RuleSet instances.
 *
 * @author pcal
 * @since 0.0.1
 */
public interface RuleSetBuilder {

    /**
     * @return a new instance coordinates RuleSetBuilder.
     */
    static RuleSetBuilder create() {
        return new RuleSetBuilderImpl();
    }

    /**
     * Set the logger to be used by the built RuleSet.
     */
    RuleSetBuilder logger(Logger logger);

    /**
     * Add a set of groups that will be usable for resolving expressions in subsequently-built rules.
     */
    RuleSetBuilder groups(GroupSet groups);

    /**
     */
    RuleSetBuilder addRules(RuleSet otherRules);

    /**
     * Create a rule with the given action, which must be "DENY", "ALLOW" or "WARN."
     *
     * @return a nested builder for specifying what goes in the rule.
     */
    RuleBuilder rule();


    /**
     * Builds a RuleSet instance that reflects the directions given to the builder.
     *
     * @return an instance coordinates RuleSet based on the provided directives.
     */
    RuleSet build() throws RuleBuildingException;


    interface RuleBuilder {

        RuleBuilder action(RuleAction action);

        RuleBuilder from(String expression) throws RuleBuildingException;

        RuleBuilder exceptFrom(String expression) throws RuleBuildingException;

        RuleBuilder to(String expression) throws RuleBuildingException;

        RuleBuilder exceptTo(String expression) throws RuleBuildingException;

        RuleBuilder scope(DependencyScope scope);

        RuleBuilder distance(RuleDistance distance);

        RuleBuilder optionality(RuleOptionality optionality);

        RuleBuilder reason(String message);


        /**
         * Should be called once when you're done building the group.
         *
         * @throws RuleBuildingException if your forgot a required value or something similar.
         */
        void build() throws RuleBuildingException;
    }

}
