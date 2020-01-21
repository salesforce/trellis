/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.rules.impl;

import com.salesforce.trellis.rules.Coordinates;
import com.salesforce.trellis.rules.builder.RuleBuildingException;

import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * @author pcal
 * @since 0.0.5
 */
interface ExpressionResolver {

    Matcher resolve(String expression) throws RuleBuildingException;

    class DefaultExpressionResolver implements ExpressionResolver {

        DefaultExpressionResolver() {
        }

        @Override
        public Matcher resolve(String expression) throws RuleBuildingException {
            expression = requireNonNull(expression, "null expression").trim();
            if (expression.contains("*")) {
                return new WildcardMatcher(expression);
            } else if (expression.contains(":")) {
                final Coordinates c = Coordinates.parse(expression);
                // FIXME error handling
                return new SimpleMatcher(c);
            } else {
                throw new RuleBuildingException("Invalid group name '" + expression + "'");
            }
        }
    }


    class WithGroupsExpressionResolver extends DefaultExpressionResolver {

        private final Map<String, Matcher> groups;

        WithGroupsExpressionResolver(Map<String, Matcher> groups) {
            this.groups = requireNonNull(groups);
        }

        @Override
        public Matcher resolve(String expression) throws RuleBuildingException {
            expression = requireNonNull(expression, "null expression").trim();
            if (expression.contains("*") || expression.contains(":")) {
                return super.resolve(expression);
            } else {
                // if no colon or asterisk, it must be a group name, right?
                if (!groups.containsKey(expression)) {
                    throw new RuleBuildingException("Invalid group name '" + expression + "'");
                } else {
                    return groups.get(expression);
                }
            }
        }
    }


}
