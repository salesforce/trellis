/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.whitelist.builder;

import com.salesforce.trellis.rules.RuleSet;
import com.salesforce.trellis.rules.builder.RuleBuildingException;
import com.salesforce.trellis.whitelist.Whitelister;
import com.salesforce.trellis.whitelist.impl.WhitelisterBuilderImpl;

/**
 * Builds a Whitelister.
 *
 * @author pcal
 * @since 0.0.1
 */
public interface WhitelisterBuilder {

    /**
     * Get a new instance.
     */
    static WhitelisterBuilder create() {
        return new WhitelisterBuilderImpl();
    }

    /**
     */
    WhitelisterBuilder add(final RuleSet rules, final WhitelistListener listener);

    /**
     * Build it.
     */
    Whitelister build() throws RuleBuildingException;

}
