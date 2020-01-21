/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.config;

import com.salesforce.trellis.rules.Coordinates;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;

/**
 * Provides access to maven-specific functionality that we need when building up the configuration.
 *
 * @author pcal
 * @since 0.0.1
 */
public interface MavenHelper {

    /**
     * Interpolate maven property values of the form ${my.property} in the given string and return the result.
     */
    Set<Coordinates> getDependenciesFromPom(final Path pathToPom) throws IOException;

    /**
     * @return a function that will interpolate an input string using maven property values.  i.e., markers of the form
     * ${my.property} in the function input string will be substituted with property values.
     */
    Function<String, String> getInterpolator();

    /**
     * @return a function that will interpolate an input string using the given properties plus maven property values.
     * i.e., markers of the form ${my.property} in the function input string will be substituted with property
     * values.  The given Properties take precedence in the case of conflicts.
     */
    Function<String, String> createInterpolator(final Properties additionalProperties);
}
