package com.salesforce.trellis.config.impl;

import com.google.common.collect.ImmutableSet;
import com.salesforce.trellis.config.MavenHelper;
import com.salesforce.trellis.rules.Coordinates;
import org.codehaus.plexus.interpolation.fixed.FixedStringSearchInterpolator;
import org.codehaus.plexus.interpolation.fixed.PropertiesBasedValueSource;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;

/**
 * Crude hack, basically a copy-paste from the real impl in the plugin module.
 *
 * @author pcal
 * @since 0.0.1
 */
public class MockMavenHelper implements MavenHelper {

    private static final String USER_HOME_KEY = "user.home";
    private static final String USER_HOME_VALUE = "/home/pcalahan/";
    private static final Path POM_PATH = Paths.get("/home/pcalahan/my/pom.xml");
    private final FixedStringSearchInterpolator interpolator;

    public MockMavenHelper() {
        final Properties properties = new Properties();
        properties.setProperty(USER_HOME_KEY, USER_HOME_VALUE);
        PropertiesBasedValueSource cliProps = new PropertiesBasedValueSource(properties);
        this.interpolator = FixedStringSearchInterpolator.create(cliProps);

    }

    public MockMavenHelper(Properties properties) {
        PropertiesBasedValueSource cliProps = new PropertiesBasedValueSource(properties);
        this.interpolator = FixedStringSearchInterpolator.create(cliProps);

    }

    @Override
    public Set<Coordinates> getDependenciesFromPom(Path pathToPom) {
        if (pathToPom.equals(POM_PATH)) {
            return ImmutableSet.of(Coordinates.of("sfdc.core", "importedA"), Coordinates.of("sfdc.core", "importedB"));
        } else {
            return null;
        }
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
