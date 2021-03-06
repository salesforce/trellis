/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.rules.builder;

/**
 * A valid value for a Rule's 'distance' property.  This can be used to limit the dependencies to which a Rule applies
 * based on the distance between the 'from' and the 'to' nodes in the graph of declared dependencies.
 *
 * @author pcal
 * @since 0.0.1
 */
public enum RuleDistance {

    /**
     * The rule applies to all dependencies.  This is the default.
     */
    ANY,

    /**
     * The rule applies only to direct dependencies (i.e., distance == 1).
     */
    DIRECT_ONLY,

    /**
     * The rule applies only to transitive dependencies (i.e., distance > 1).
     */
    TRANSITIVE_ONLY;

}
