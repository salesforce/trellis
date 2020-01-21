package com.salesforce.trellis.config.impl;

import com.salesforce.trellis.common.GoldFileSerializer;
import com.salesforce.trellis.common.GoldFileValidator;
import com.salesforce.trellis.config.Config;
import com.salesforce.trellis.config.ConfigException;
import com.salesforce.trellis.config.FileAdapter;
import com.salesforce.trellis.config.MavenHelper;
import com.salesforce.trellis.config.YamlConfigBuilder;
import com.salesforce.trellis.rules.Coordinates;
import com.salesforce.trellis.rules.RuleSet;
import com.salesforce.trellis.rules.builder.MockRuleSetBuilder;
import com.salesforce.trellis.rules.builder.RuleSetBuilder;
import com.salesforce.trellis.rules.impl.RulesGoldFileConfig;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test parsing coordinates yaml rules config.
 *
 * @author pcal
 * @since 0.0.1
 */
public class YamlParserTest {

    private static final MavenHelper MAVEN_HELPER = new MockMavenHelper();
    private static final Path RESOURCES = Paths.get("src/test/resources/com/salesforce/trellis/config/impl");

    /**
     * Sanity test coordinates parsing a single file.
     */
    @Test
    public void testBasic() throws Exception {
        final Path TEST_RESOURCES = RESOURCES.resolve("YamlParserTest-testBasic");
        final FileAdapter yamlFile = FileAdapter.forPath(TEST_RESOURCES.resolve("testBasic.yaml"));
        final MockParserListener listener = new MockParserListener();
        {
            // make sure the parser calls all coordinates the right builder methods
            final StringWriter builderLog = new StringWriter();
            final RuleSetBuilder rb = RuleSetBuilder.create();
            final YamlConfigBuilder yb = YamlConfigBuilder.create().addFile(yamlFile).mavenHelper(MAVEN_HELPER);
            yb.build().applyTo(rb, listener);
            final RuleSet result = rb.build();
            new GoldFileValidator(TEST_RESOURCES.resolve("testBasic.goldfile")).validate(toString(result));
        }
        {
            // also do a quick integration test with a live rules builder.
            final YamlConfigBuilder yb = YamlConfigBuilder.create().addFile(yamlFile).mavenHelper(MAVEN_HELPER);
            final RuleSetBuilder rb = RuleSetBuilder.create();
            yb.build().applyTo(rb, listener);
            final RuleSet rules = rb.build();
            assertNotNull(rules.getRulesFor(Coordinates.parse("sfdc.core:platform-encryption-api")));
            assertNull(rules.getRulesFor(Coordinates.parse("sfdc.core:platform-encryption")));
            assertNull(rules.getRulesFor(Coordinates.parse("sfdc.core:no-such-module")));
        }
        assertTrue(listener.getEvents().isEmpty());
    }

    /**
     * Ensure we blow up when the import graph contains a cycle.
     */
    @Test
    public void testCyclicalImports() throws Exception {
        final Path TEST_RESOURCES = RESOURCES.resolve("YamlParserTest-testCyclicalImports");
        final FileAdapter yamlFile = FileAdapter.forPath(TEST_RESOURCES.resolve("a.yaml"));
        final MockParserListener listener = new MockParserListener(false);
        final MockRuleSetBuilder mrb = new MockRuleSetBuilder();
        final YamlConfigBuilder yb = YamlConfigBuilder.create().addFile(yamlFile).mavenHelper(MAVEN_HELPER);
        try {
            yb.build().applyTo(mrb, listener);
            fail("did not get expected exception");
        } catch (ConfigException expected) {
        }
        assertEquals(1, listener.getEvents().size());
        assertTrue(listener.getEvents().get(0).getMessage().toLowerCase().contains("cycl"),
            "Unexpected error message: " + listener.getEvents().get(0).getMessage());
    }

    /**
     * Test parsing a file that has issues.
     */
    @Test
    public void testParsingErrors() throws Exception {
        final Path TEST_RESOURCES = RESOURCES.resolve("YamlParserTest-testParsingErrors");
        final FileAdapter yamlFile = FileAdapter.forPath(TEST_RESOURCES.resolve("testParsingErrors.yaml"));
        final MockParserListener listener = new MockParserListener(false);
        final YamlConfigBuilder pb = YamlConfigBuilder.create().addFile(yamlFile).mavenHelper(MAVEN_HELPER);
        final Config config = pb.build();
        try {
            config.applyTo(RuleSetBuilder.create(), listener);
            fail("did not get expected exception");
        } catch (ConfigException expected) {
        }
        String errors = serializer().toString(listener.getEvents());
        new GoldFileValidator(TEST_RESOURCES.resolve("errors.goldfile")).validate(errors);
    }


    @Test
    public void testEquals() throws Exception {
        final Path TEST_RESOURCES = RESOURCES.resolve("YamlParserTest-testParsingErrors");
        final FileAdapter yamlFile = FileAdapter.forPath(TEST_RESOURCES.resolve("testParsingErrors.yaml"));
        final FileAdapter yamlFile2 = FileAdapter.forPath(TEST_RESOURCES.resolve("testParsingErrors2.yaml"));
        ParsedYamlFile pf = ParsedYamlFile.parse(yamlFile, MAVEN_HELPER);
        ParsedYamlFile pf2 = ParsedYamlFile.parse(yamlFile2, MAVEN_HELPER);

        // Asserts path, which is why it comes out false
        assertFalse(pf.equals(pf2));

        assertFalse(pf.equals(yamlFile));
        assertTrue(pf.equals(pf));
    }

    private String toString(Object o) throws Exception {
        return serializer().toString(o);
    }

    private static GoldFileSerializer serializer() {
        return GoldFileSerializer.create(ConfigGoldFileConfig.get(), RulesGoldFileConfig.get());
    }
}
