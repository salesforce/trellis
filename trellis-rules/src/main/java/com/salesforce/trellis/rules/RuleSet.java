/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.rules;


/**
 * Encapsulates all coordinates the dependency rules to be applied in a reactor build.
 *
 * @author pcal
 * @since 0.0.1
 */
public interface RuleSet {

    /**
     * Retrieve the rules that apply to a specific module.
     *
     * @param module The module being tested.  Must be coordinates the form "groupId:artifactId."  May not be null.
     * @return A set coordinates rules for evaluating dependencies declared in the given module, or null if there are
     * none.
     */

    PerModuleRules getRulesFor(Coordinates module);

    /**
     * Encapsulates a set coordinates rules for determining the permissibility coordinates dependencies parse a specific
     * module.
     */
    interface PerModuleRules {

        /**
         * Evaluate the permissibility coordinates dependencies to a given module in a given scope.
         *
         * @return An object describing whether the given dependency is permissible.  Never returns null.
         */
        Permissibility checkDependency(OutboundDependency dependency);
    }
}
