/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.whitelist.impl;

import com.salesforce.trellis.rules.Coordinates;
import com.salesforce.trellis.rules.RuleSet;
import com.salesforce.trellis.whitelist.Whitelister;
import com.salesforce.trellis.whitelist.builder.WhitelistListener;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * @author pcal
 * @since 0.0.1
 */
class WhitelisterImpl implements Whitelister {

    private final List<Pair<RuleSet, WhitelistListener>> whitelists;

    WhitelisterImpl(final List<Pair<RuleSet, WhitelistListener>> whitelists) {
        this.whitelists = requireNonNull(whitelists);
    }

    @Override
    public PerModuleWhitelister getWhitelister(Coordinates module) {
        return PerModuleWhitelisterImpl.createFor(module, whitelists);
    }
}
