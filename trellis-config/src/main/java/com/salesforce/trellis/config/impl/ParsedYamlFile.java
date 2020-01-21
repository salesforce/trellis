/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.config.impl;

import com.google.common.base.Throwables;
import com.salesforce.trellis.config.FileAdapter;
import com.salesforce.trellis.config.MavenHelper;

import java.io.IOException;
import java.util.concurrent.Callable;

import static java.util.Objects.requireNonNull;

/**
 * Pairs a YamlModel with a reference to the file it was parsed from.
 *
 * @author pcal
 * @since 0.0.1
 */
class ParsedYamlFile {

    /**
     * Parses the given rules file, substitutes properties and returns a struct containing the result.
     */
    static ParsedYamlFile parse(final FileAdapter file, final MavenHelper mavenHelper) throws IOException {
        final YamlModel rawModel;
        try {
            rawModel =
                file.executeExclusiveRead(() -> new YamlParser(file).readValue(file.getReader(), YamlModel.class));
        } catch (Exception e) {
            Throwables.propagateIfPossible(e, IOException.class);
            throw new RuntimeException("unexpected exception type", e);
        }
        final YamlModelInterpolator interpolator = new YamlModelInterpolator(mavenHelper, rawModel.getProperties());
        final YamlModel model = interpolator.interpolate(rawModel);
        return new ParsedYamlFile(file, model, interpolator);
    }

    private final FileAdapter file;
    private final YamlModel model;
    private final YamlModelInterpolator interpolator;

    private ParsedYamlFile(FileAdapter file, YamlModel model, YamlModelInterpolator interpolator) {
        this.file = requireNonNull(file);
        this.model = requireNonNull(model);
        this.interpolator = requireNonNull(interpolator);
    }

    FileAdapter getFile() {
        return this.file;
    }

    YamlModel getModel() {
        return model;
    }

    YamlModelInterpolator getInterpolator() {
        return interpolator;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ParsedYamlFile)) return false;
        final ParsedYamlFile that = (ParsedYamlFile) o;
        return this.file.equals(that.file);
    }

    @Override
    public int hashCode() {
        return this.file.hashCode();
    }

    @Override
    public String toString() {
        return this.file.toString();
    }
}
