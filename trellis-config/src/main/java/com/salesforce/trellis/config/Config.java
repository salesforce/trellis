/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.config;

import com.salesforce.trellis.rules.builder.RuleSetBuilder;
import com.salesforce.trellis.whitelist.builder.WhitelisterBuilder;

/**
 * Parses one or more rules config into a single rule set.
 *
 * @author pcal
 * @since 0.0.1
 */
public interface Config {

    /**
     * Applies this configuration to the given rules builder.
     */
    void applyTo(RuleSetBuilder rulesBuilder, ParserListener listenerOrNull) throws ConfigException;

    /**
     * Applies this configuration to the given whitelister builder.
     */
    void applyTo(WhitelisterBuilder builder, ParserListener listenerOrNull) throws ConfigException;
}
