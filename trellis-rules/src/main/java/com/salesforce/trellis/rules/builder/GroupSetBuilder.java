/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.rules.builder;

import com.salesforce.trellis.rules.impl.GroupSetBuilderImpl;

/**
 * @author pcal
 * @since 0.0.1
 */
public interface GroupSetBuilder {

    /**
     * @return a new instance coordinates RuleSetBuilder.
     */
    static GroupSetBuilder create() {
        return new GroupSetBuilderImpl();
    }

    /**
     * Create a group with the given name.
     *
     * @return a nested builder for specifying what goes in the group.
     */
    GroupBuilder group();

    GroupSet build();
}
