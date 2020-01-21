package com.salesforce.trellis.config.impl;

import com.salesforce.trellis.common.GoldFileValidator;
import com.salesforce.trellis.config.ConfigException;
import com.salesforce.trellis.config.FileAdapter;
import com.salesforce.trellis.config.MavenHelper;
import com.salesforce.trellis.config.impl.YamlModel.RuleModel;
import com.salesforce.trellis.rules.Coordinates;
import com.salesforce.trellis.rules.DependencyScope;
import com.salesforce.trellis.rules.OutboundDependency;
import com.salesforce.trellis.rules.builder.RuleAction;
import com.salesforce.trellis.whitelist.Whitelister;
import com.salesforce.trellis.whitelist.Whitelister.PerModuleWhitelister;
import com.salesforce.trellis.whitelist.builder.WhitelistedDependency;
import com.salesforce.trellis.whitelist.builder.WhitelisterBuilder;
import com.salesforce.trellis.whitelist.impl.MockWhitelistedDependencyFactory;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static com.salesforce.trellis.config.impl.ConfigTestUtils.createTestFile;
import static com.salesforce.trellis.config.impl.ConfigTestUtils.dep;
import static com.salesforce.trellis.config.impl.ConfigTestUtils.string;
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
public class YamlWhitelistTest {

    private static final Path RESOURCES = Paths.get("src/test/resources/com/salesforce/trellis/config/impl");
    private static final DependencyScope COMPILE_SCOPE = DependencyScope.parse("compile");
    private static final MavenHelper MAVEN_HELPER = new MockMavenHelper();

    /**
     * Directly unit test a couple of key methods in WhitelistListener.
     */
    @Test
    public void testListener() throws Exception {
        final AutoWhitelister listener;
        {
            final FileAdapter whitelistFile = FileAdapter.forPath(Paths.get("bogusFile"));
            listener = new AutoWhitelister(whitelistFile, RuleAction.WARN, null, LoggerFactory.getLogger(getClass()));
        }
        final YamlModel whitelistModel = new YamlModel();
        {
            final List<YamlModel.RuleModel> rules = new ArrayList<>();
            rules.add(new RuleModel("WARN", "sfdc.core:platform-encryption", "sfdc.ui:ui-platform-encryption",
                COMPILE_SCOPE.toString()));
            rules.add(new RuleModel("WARN", "sfdc.core:activities", "sfdc.ui:ui-activities", COMPILE_SCOPE.toString()));
            rules.add(new RuleModel("WARN", "sfdc.core:activities", "junit:junit", COMPILE_SCOPE.toString()));
            whitelistModel.setRules(rules);
        }
        listener.stripRulesFrom(Coordinates.of("sfdc.core", "activities"), whitelistModel);
        assertEquals(1, whitelistModel.getRules().size());
        assertEquals(1, whitelistModel.getRules().get(0).getFrom().size());
        assertEquals(string("sfdc.core:platform-encryption"), whitelistModel.getRules().get(0).getFrom().get(0));

        final Set<WhitelistedDependency> whitelistedDeps = new HashSet<>();
        // add a new one
        whitelistedDeps.add(MockWhitelistedDependencyFactory
            .create(Coordinates.parse("sfdc.core:platform-encryption"), Coordinates.parse("junit:junit"), COMPILE_SCOPE,
                null));
        // add one that's already there
        whitelistedDeps.add(MockWhitelistedDependencyFactory.create(Coordinates.parse("sfdc.core:platform-encryption"),
            Coordinates.parse("sfdc.ui:ui-platform-encryption"), COMPILE_SCOPE, null));
        // add a new one from a different module
        whitelistedDeps.add(MockWhitelistedDependencyFactory
            .create(Coordinates.parse("sfdc.core:activities"), Coordinates.parse("junit:junit"), COMPILE_SCOPE, null));
        listener.appendRules(whitelistedDeps, whitelistModel);
        assertEquals(4, whitelistModel.getRules().size());
        new YamlModelTransformer(whitelistModel).canonicalize();
        assertEquals(3, whitelistModel.getRules().size());
    }

    /**
     * Sanity test coordinates parsing a single file.
     */
    @Test
    public void testBasicWhitelist() throws Exception {
        final Path TEST_RESOURCES = RESOURCES.resolve("YamlWhitelistTest-testBasicWhitelist");
        final PathFileAdapter rulesFile = createTestFile(TEST_RESOURCES.resolve("testBasicWhitelist-rules.yaml"));
        // ensure the existing whitelist also gets copied over
        createTestFile(TEST_RESOURCES.resolve("testBasicWhitelist-whitelist.yaml"));

        final Whitelister w;
        {
            final AutoWhitelisterFactory wlf =
                (file, rule, headerTextOrNull) -> new AutoWhitelister(file, rule, headerTextOrNull,
                    LoggerFactory.getLogger(getClass()));
            final MockParserListener listener = new MockParserListener();
            // also do a quick integration test with a live rules builder.
            final YamlConfigBuilderImpl yb = new YamlConfigBuilderImpl();
            Properties props = new Properties();
            props.put("trellis.whitelist.headerComment", "THIS IS THE DEFAULT HEADER");
            props.put("${trellis.whitelist.headerComment}", "IF YOU SEE THIS MESSAGE, YOU HAVE A BUG");
            yb.listenerFactory(wlf).addFile(rulesFile).mavenHelper(new MockMavenHelper(props));
            final WhitelisterBuilder wb = WhitelisterBuilder.create();
            yb.build().applyTo(wb, listener);
            w = wb.build();
            assertTrue(listener.getEvents().isEmpty());
        }
        assertNull(w.getWhitelister(Coordinates.parse("sfdc.ui:no-rules")));
        {
            final PerModuleWhitelister wl = w.getWhitelister(Coordinates.parse("sfdc.core:platform-encryption"));
            assertNotNull(wl);
            wl.notifyDependency(OutboundDependency
                .create(Coordinates.parse("sfdc.core:platform-encryption-api"), COMPILE_SCOPE, true, false));
            //wll.dependencies.isEmpty();
            wl.notifyDependency(
                OutboundDependency.create(Coordinates.parse("junit:junit"), COMPILE_SCOPE, true, false));
            wl.notifyDone();
            //assertEquals(1, wll.dependencies.size());
        }
        {
            {
                // no-longer-violating-module is no longer violating the rule against junit dependencies
                final PerModuleWhitelister wl =
                    w.getWhitelister(Coordinates.parse("sfdc.core:no-longer-violating-module"));
                wl.notifyDone();
            }
            {
                final PerModuleWhitelister wl =
                    w.getWhitelister(Coordinates.parse("sfdc.core:newly-whitelisted-model"));
                assertNotNull(wl);
                wl.notifyDependency(
                    OutboundDependency.create(Coordinates.parse("junit:junit"), COMPILE_SCOPE, true, false));
                //assertEquals(2, wll.dependencies.size());
                wl.notifyDone();
            }
            {
                final PerModuleWhitelister wl =
                    w.getWhitelister(Coordinates.parse("sfdc.core:another-whitelisted-model"));
                assertNotNull(wl);
                wl.notifyDependency(
                    OutboundDependency.create(Coordinates.parse("junit:junit"), COMPILE_SCOPE, true, false));
                //assertEquals(2, wll.dependencies.size());
                wl.notifyDone();
            }
        }
        final Path updatedFile = rulesFile.getPath().resolveSibling("testBasicWhitelist-whitelist.yaml");
        final Path goldFile = TEST_RESOURCES.resolve("testBasicWhitelist-whitelist.yaml.expected");
        new GoldFileValidator(goldFile).validate(updatedFile);
    }


    /**
     * Test boundary condition for when all of the rules have burned off.
     */
    @Test
    public void testEvaporation() throws Exception {
        final Path TEST_RESOURCES = RESOURCES.resolve("YamlWhitelistTest-testEvaporation");
        final PathFileAdapter rulesFiles = createTestFile(TEST_RESOURCES.resolve("testEvaporation-rules.yaml"));
        // also ensure the existing whitelist gets copied into the target dir.
        createTestFile(TEST_RESOURCES.resolve("testEvaporation-whitelist.yaml"));
        final Whitelister w;
        {
            final AutoWhitelisterFactory wlf =
                (file, rule, headerTextOrNull) -> new AutoWhitelister(file, rule, headerTextOrNull,
                    LoggerFactory.getLogger(getClass()));
            final MockParserListener listener = new MockParserListener();
            // also do a quick integration test with a live rules builder.
            final YamlConfigBuilderImpl yb = new YamlConfigBuilderImpl();
            yb.listenerFactory(wlf).addFile(rulesFiles).mavenHelper(new MockMavenHelper());
            final WhitelisterBuilder wb = WhitelisterBuilder.create();
            yb.build().applyTo(wb, listener);
            w = wb.build();
            assertTrue(listener.getEvents().isEmpty());
        }
        {
            w.getWhitelister(Coordinates.parse("myapp:foo")).notifyDone();
            w.getWhitelister(Coordinates.parse("myapp:bar")).notifyDone();
            w.getWhitelister(Coordinates.parse("myapp:baz")).notifyDone();
        }
        final Path updatedFile = rulesFiles.getPath().resolveSibling("testEvaporation-whitelist.yaml");
        final Path goldFile = TEST_RESOURCES.resolve("testEvaporation-whitelist.yaml.expected");
        new GoldFileValidator(goldFile).validate(updatedFile);
    }

    /**
     * Test the whitelist files will get created as needed.
     */
    @Test
    public void testMissingWhitelist() throws Exception {
        final Path TEST_RESOURCES = RESOURCES.resolve("YamlWhitelistTest-testMissingWhitelist");
        final PathFileAdapter rulesFile = createTestFile(TEST_RESOURCES.resolve("testMissingWhitelist-rules.yaml"));
        final Whitelister w;
        final AutoWhitelisterFactory wlf =
            (file, rule, headerTextOrNull) -> new AutoWhitelister(file, rule, headerTextOrNull,
                LoggerFactory.getLogger(getClass()));
        {
            final MockParserListener listener = new MockParserListener();
            // also do a quick integration test with a live rules builder.
            final YamlConfigBuilderImpl yb = new YamlConfigBuilderImpl();
            yb.listenerFactory(wlf).addFile(rulesFile).mavenHelper(MAVEN_HELPER);
            final WhitelisterBuilder wb = WhitelisterBuilder.create();
            yb.build().applyTo(wb, listener);
            w = wb.build();
            assertTrue(listener.getEvents().isEmpty());
        }
        assertNull(w.getWhitelister(Coordinates.parse("sfdc.ui:no-rules")));
        {
            final PerModuleWhitelister wl = w.getWhitelister(Coordinates.parse("sfdc.core:platform-encryption"));
            assertNotNull(wl);
            wl.notifyDependency(OutboundDependency
                .create(Coordinates.parse("sfdc.core:platform-encryption-api"), COMPILE_SCOPE, true, false));
            //wll.dependencies.isEmpty();
            wl.notifyDependency(
                OutboundDependency.create(Coordinates.parse("junit:junit"), COMPILE_SCOPE, true, false));
            wl.notifyDone();
            //assertEquals(1, wll.dependencies.size());
        }
        {
            final PerModuleWhitelister wl = w.getWhitelister(Coordinates.parse("sfdc.core:newly-whitelisted-model"));
            assertNotNull(wl);
            wl.notifyDependency(
                OutboundDependency.create(Coordinates.parse("junit:junit"), COMPILE_SCOPE, true, false));
            //assertEquals(2, wll.dependencies.size());
            wl.notifyDone();
        }
        final Path updatedFile = rulesFile.getPath().resolveSibling("testMissingWhitelist-whitelist.yaml");
        final Path goldFile = TEST_RESOURCES.resolve("testMissingWhitelist-whitelist.yaml.expected");
        new GoldFileValidator(goldFile).validate(updatedFile);
    }

    /**
     * Regression test for NPE when no comment is specified.
     */
    @Test
    public void testMinimal() throws Exception {
        final Path TEST_RESOURCES = RESOURCES.resolve("YamlWhitelistTest-testMinimal");
        final PathFileAdapter rulesFile = createTestFile(TEST_RESOURCES.resolve("testMinimal-rules.yaml"));
        final AutoWhitelisterFactory wlf =
            (file, rule, headerTextOrNull) -> new AutoWhitelister(file, rule, headerTextOrNull,
                LoggerFactory.getLogger(getClass()));
        final Whitelister w;
        {
            final MockParserListener listener = new MockParserListener();
            // also do a quick integration test with a live rules builder.
            final YamlConfigBuilderImpl yb = new YamlConfigBuilderImpl();
            yb.listenerFactory(wlf).addFile(rulesFile).mavenHelper(MAVEN_HELPER);
            final WhitelisterBuilder wb = WhitelisterBuilder.create();
            yb.build().applyTo(wb, listener);
            w = wb.build();
            assertTrue(listener.getEvents().isEmpty());
        }
        {
            final PerModuleWhitelister wl = w.getWhitelister(Coordinates.parse("foo:bar"));
            assertNotNull(wl);
            wl.notifyDependency(OutboundDependency.create(Coordinates.parse("baz:bop"), COMPILE_SCOPE, true, false));
            wl.notifyDone();
        }

        final Path updatedFile = rulesFile.getPath().resolveSibling("testMinimal-whitelist.yaml");
        final Path goldFile = TEST_RESOURCES.resolve("testMinimal-whitelist.yaml.expected");
        new GoldFileValidator(goldFile).validate(updatedFile);
    }

    /**
     * Regression test for multiple rules pointing to same whitelist.
     */
    @Test
    public void testSameFile() throws Exception {
        final Path TEST_RESOURCES = RESOURCES.resolve("YamlWhitelistTest-testSameFile");
        final PathFileAdapter rulesFile = createTestFile(TEST_RESOURCES.resolve("testSameFile-rules.yaml"));
        final AutoWhitelisterFactory wlf =
            (file, rule, headerTextOrNull) -> new AutoWhitelister(file, rule, headerTextOrNull,
                LoggerFactory.getLogger(getClass()));
        final Whitelister w;
        {
            final MockParserListener listener = new MockParserListener();
            // also do a quick integration test with a live rules builder.
            final YamlConfigBuilderImpl yb = new YamlConfigBuilderImpl();
            yb.listenerFactory(wlf).addFile(rulesFile).mavenHelper(MAVEN_HELPER);
            final WhitelisterBuilder wb = WhitelisterBuilder.create();
            yb.build().applyTo(wb, listener);
            w = wb.build();
            assertTrue(listener.getEvents().isEmpty());
        }
        {
            final PerModuleWhitelister wl = w.getWhitelister(Coordinates.parse("my:module"));
            assertNotNull(wl);
            wl.notifyDependency(dep(Coordinates.parse("foo:foo"), COMPILE_SCOPE, true, false));
            wl.notifyDependency(dep(Coordinates.parse("bar:bar"), COMPILE_SCOPE, true, false));
            wl.notifyDependency(dep(Coordinates.parse("ok:ok"), COMPILE_SCOPE, true, false));
            wl.notifyDone();
        }
        final Path updatedFile = rulesFile.getPath().resolveSibling("testSameFile-whitelist.yaml");
        final Path goldFile = TEST_RESOURCES.resolve("testSameFile-whitelist.yaml.expected");
        new GoldFileValidator(goldFile).validate(updatedFile);
    }


    /**
     * Regression test for multiple rules pointing to same whitelist with incompatible whitelist configs.
     */
    @Test
    public void testSameFileMismatch() throws Exception {
        final Path TEST_RESOURCES = RESOURCES.resolve("YamlWhitelistTest-testSameFileMismatch");
        final PathFileAdapter rulesFile = createTestFile(TEST_RESOURCES.resolve("testSameFileMismatch-rules.yaml"));
        final AutoWhitelisterFactory wlf =
            (file, rule, headerTextOrNull) -> new AutoWhitelister(file, rule, headerTextOrNull,
                LoggerFactory.getLogger(getClass()));
        final Whitelister w;
        {
            final MockParserListener listener = new MockParserListener(false);
            // also do a quick integration test with a live rules builder.
            final YamlConfigBuilderImpl yb = new YamlConfigBuilderImpl();
            yb.listenerFactory(wlf).addFile(rulesFile).mavenHelper(MAVEN_HELPER);
            final WhitelisterBuilder wb = WhitelisterBuilder.create();
            try {
                yb.build().applyTo(wb, listener);
                fail("did not get expected exception");
            } catch (ConfigException expected) {}

            assertFalse(listener.getEvents().isEmpty());
            assertEquals(1, listener.getEvents().size());
            assertTrue(listener.getEvents().get(0).getMessage().contains("multiple whitelist configurations"));
        }
    }
}
