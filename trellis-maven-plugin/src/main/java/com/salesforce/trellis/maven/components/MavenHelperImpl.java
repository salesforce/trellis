/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.maven.components;

import com.salesforce.trellis.config.MavenHelper;
import com.salesforce.trellis.rules.Coordinates;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.interpolation.fixed.FixedStringSearchInterpolator;
import org.codehaus.plexus.interpolation.fixed.PropertiesBasedValueSource;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of helper class to provide maven-specific functionality for the configuration building.
 *
 * @author pcal
 * @since 0.0.1
 */
class MavenHelperImpl implements MavenHelper {

    private final FixedStringSearchInterpolator interpolator;

    /**
     * @param properties Properties that will be available for interpolation in the rule files.  Properties later in
     * the list take precedence over those earlier.  Null values in the array are tolerated (and ignored).
     */
    MavenHelperImpl(final Properties... properties) {
        requireNonNull(properties);
        final Properties effectiveProperties = new Properties();
        for (Properties props : properties) {
            if (props != null) effectiveProperties.putAll(props);
        }
        PropertiesBasedValueSource cliProps = new PropertiesBasedValueSource(effectiveProperties);
        this.interpolator = FixedStringSearchInterpolator.create(cliProps);
    }

    @Override
    public Set<Coordinates> getDependenciesFromPom(Path pathToPom) throws IOException {
        requireNonNull(pathToPom, "path can't be null");
        final Model model;
        final FileReader reader;
        final File pomFile = pathToPom.toFile();
        reader = new FileReader(pomFile);
        try {
            model = new MavenXpp3Reader().read(reader);
        } catch (XmlPullParserException xpe) {
            throw new IOException(xpe);
        }
        model.setPomFile(pomFile);
        final MavenProject project = new MavenProject(model);
        final Set<Coordinates> out = new HashSet<Coordinates>();
        for (final Dependency dep : project.getDependencies()) {
            out.add(Coordinates.of(dep.getGroupId(), dep.getArtifactId()));
        }
        return out;
    }

    @Override
    public Function<String, String> getInterpolator() {
        return (s) -> this.interpolator.interpolate(s);
    }

    @Override
    public Function<String, String> createInterpolator(Properties additionalProperties) {
        final PropertiesBasedValueSource additionalValues = new PropertiesBasedValueSource(additionalProperties);
        final FixedStringSearchInterpolator fssi =
            FixedStringSearchInterpolator.create(additionalValues, this.interpolator);
        return (s) -> fssi.interpolate(s);
    }
}
