/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.config;

import com.salesforce.trellis.config.impl.YamlConfigBuilderImpl;
import org.slf4j.Logger;

/**
 * Use this to set up the config for a set coordinates config to be parsed into a single set coordinates rules.
 *
 * @author pcal
 * @since 0.0.1
 */
public interface YamlConfigBuilder {

    /**
     * Create a new instance coordinates YamlConfigBuilder.  This is your entry point.
     */
    static YamlConfigBuilder create() {
        return new YamlConfigBuilderImpl();
    }

    /**
     * Add a single file to be parsed.
     */
    YamlConfigBuilder mavenHelper(final MavenHelper helper);

    /**
     * Add a single file to be parsed.
     */
    YamlConfigBuilder addFile(final FileAdapter file);

    /**
     * Specify a logger to use.
     */
    YamlConfigBuilder logger(final Logger logger);

    /**
     * Build the parser.
     */
    Config build() throws ConfigException;
}
