/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.rules.impl;

import com.google.common.collect.ImmutableMap;
import com.salesforce.trellis.rules.builder.GroupSet;

import static java.util.Objects.requireNonNull;

/**
 *
 * @author pcal
 * @since 0.0.1
 */
class GroupSetImpl implements GroupSet {

    private final ImmutableMap<String, Matcher> groupMatchers;

    GroupSetImpl(final ImmutableMap<String, Matcher> groupMatchers) {
        this.groupMatchers = requireNonNull(groupMatchers);
    }

    ImmutableMap<String, Matcher> getGroupMatchers() {
        return this.groupMatchers;
    }
}
