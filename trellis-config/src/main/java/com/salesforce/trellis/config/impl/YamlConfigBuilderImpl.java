/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.config.impl;

import com.salesforce.trellis.config.Config;
import com.salesforce.trellis.config.FileAdapter;
import com.salesforce.trellis.config.MavenHelper;
import com.salesforce.trellis.config.YamlConfigBuilder;
import com.salesforce.trellis.rules.builder.GroupSetBuilder;
import com.salesforce.trellis.rules.builder.RuleSetBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Use this to set up the config for a set coordinates config to be parsed into a single set coordinates rules.
 *
 * @author pcal
 * @since 0.0.1
 */
public class YamlConfigBuilderImpl implements YamlConfigBuilder {

    // ===================================================================
    // Fields

    private final List<FileAdapter> files = new ArrayList<>();
    private AutoWhitelisterFactory listenerFactory;
    private MavenHelper helper;
    private Logger logger;
    private boolean isUsed = false;

    // ===================================================================
    // Constructors

    public YamlConfigBuilderImpl() {
    }

    // ===================================================================
    // YamlConfigBuilder impl

    @Override
    public YamlConfigBuilder mavenHelper(MavenHelper helper) {
        this.helper = requireNonNull(helper);
        return this;
    }

    @Override
    public YamlConfigBuilder addFile(FileAdapter path) {
        checkState();
        this.files.add(path);
        return this;
    }

    @Override
    public YamlConfigBuilder logger(Logger logger) {
        checkState();
        this.logger = requireNonNull(logger);
        return this;
    }

    @Override
    public Config build() {
        checkState();
        isUsed = true;
        if (helper == null) {
            throw new IllegalStateException("helper must be set");
        }
        if (logger == null) {
            logger = LoggerFactory.getLogger(getClass());
        }
        if (listenerFactory == null) {
            listenerFactory =
                (file, rule, headerTextOrNull) -> new AutoWhitelister(file, rule, headerTextOrNull, this.logger);
        }
        return new YamlConfigImpl(this.files, helper, listenerFactory, () -> RuleSetBuilder.create(),
            () -> GroupSetBuilder.create(), this.logger);
    }

    // ===================================================================
    // Package methods

    YamlConfigBuilderImpl listenerFactory(AutoWhitelisterFactory listenerFactory) {
        this.listenerFactory = requireNonNull(listenerFactory);
        return this;
    }

    // ===================================================================
    // Private

    void checkState() {
        if (isUsed) throw new IllegalStateException("builder as already been used");
    }

}
