package com.salesforce.trellis.config.impl;

import com.salesforce.trellis.common.GoldFileValidator;
import com.salesforce.trellis.config.FileAdapter;
import com.salesforce.trellis.config.MavenHelper;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static com.salesforce.trellis.config.impl.ConfigTestUtils.model2string;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test parsing coordinates yaml rules config.
 *
 * @author pcal
 * @since 0.0.1
 */
public class YamlInterpolatorTest {

    private static final MavenHelper MAVEN_HELPER = new MockMavenHelper();
    private static final Path RESOURCES = Paths.get("src/test/resources/com/salesforce/trellis/config/impl");

    /**
     * Sanity test coordinates parsing a single file.
     */
    @Test
    public void testUnchanged() throws Exception {
        final Path TEST_RESOURCES = RESOURCES.resolve("YamlInterpolatorTest-testUnchanged");
        final FileAdapter yamlFile = FileAdapter.forPath(TEST_RESOURCES.resolve("testUnchanged.yaml"));
        final YamlParser parser = new YamlParser(yamlFile);
        final YamlModel original = parser.readValue(yamlFile.getReader(), YamlModel.class);
        final YamlModel interpolated =
            new YamlModelInterpolator(MAVEN_HELPER, original.getProperties()).interpolate(original);
        new GoldFileValidator(TEST_RESOURCES.resolve("testUnchanged.yaml.expected"))
            .validate(model2string(interpolated));
    }

    /**
     * Sanity test coordinates parsing a single file.
     */
    @Test
    public void testBasicProps() throws Exception {
        final Path TEST_RESOURCES = RESOURCES.resolve("YamlInterpolatorTest-testBasicProps");
        final FileAdapter yamlFile = FileAdapter.forPath(TEST_RESOURCES.resolve("testBasicProps.yaml"));
        final YamlParser parser = new YamlParser(yamlFile);
        final YamlModel original = parser.readValue(yamlFile.getReader(), YamlModel.class);
        final YamlModel interpolated =
            new YamlModelInterpolator(MAVEN_HELPER, original.getProperties()).interpolate(original);
        new GoldFileValidator(TEST_RESOURCES.resolve("testBasicProps.yaml.expected"))
            .validate(model2string(interpolated));
    }

    @Test
    public void testMultilineProperties() throws Exception {
        final Path TEST_RESOURCES = RESOURCES.resolve("YamlInterpolatorTest-testMultiline");
        final FileAdapter yamlFile = FileAdapter.forPath(TEST_RESOURCES.resolve("testMultiline.yaml"));
        final YamlParser parser = new YamlParser(yamlFile);
        final YamlModel original = parser.readValue(yamlFile.getReader(), YamlModel.class);
        final YamlModelInterpolator interpolator =
            new YamlModelInterpolator(MAVEN_HELPER, original.getProperties());
        assertNotNull(original.getProperties());
        String rawHc = original.getProperties().getProperty("trellis.whitelist.headerComment");
        assertNotNull(rawHc);
        final SourceLocatableString headerComment =
            interpolator.interpolate(SourceLocatableString.of(rawHc));
        assertEquals("foo bar baz\nbing boopy boo\n", headerComment.toString());

    }
}
