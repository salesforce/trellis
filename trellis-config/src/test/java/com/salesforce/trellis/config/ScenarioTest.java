package com.salesforce.trellis.config;

import com.salesforce.trellis.common.GoldFileSerializer;
import com.salesforce.trellis.common.GoldFileValidator;
import com.salesforce.trellis.config.impl.ConfigGoldFileConfig;
import com.salesforce.trellis.config.impl.MockMavenHelper;
import com.salesforce.trellis.config.impl.MockParserListener;
import com.salesforce.trellis.rules.Coordinates;
import com.salesforce.trellis.rules.DependencyScope;
import com.salesforce.trellis.rules.OutboundDependency;
import com.salesforce.trellis.rules.Permissibility;
import com.salesforce.trellis.rules.RuleSet;
import com.salesforce.trellis.rules.RuleSet.PerModuleRules;
import com.salesforce.trellis.rules.builder.RuleSetBuilder;
import com.salesforce.trellis.rules.impl.RulesGoldFileConfig;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Test some end-to-end scenarios of trellis-config + trellis-rules.
 *
 * @author pcal
 * @since 0.0.3
 */

public class ScenarioTest {

    private static final Path RESOURCES = Paths.get("src/test/resources/com/salesforce/trellis/config");

    private GoldLog goldLog = new GoldLog();

    /**
     * Directly unit test a couple of key methods in WhitelistListener.
     */
    @Test
    public void testBasicRules() throws Exception {
        final Path TEST_RESOURCES = RESOURCES.resolve("ScenarioTest-testBasicRules");
        final FileAdapter whitelistFile = FileAdapter.forPath(TEST_RESOURCES.resolve("dependency-rules.yaml"));

        final Config config = YamlConfigBuilder.create().addFile(whitelistFile).mavenHelper(new MockMavenHelper())
            .logger(LoggerFactory.getLogger(ScenarioTest.class)).build();
        final RuleSetBuilder rb = RuleSetBuilder.create();
        config.applyTo(rb, new MockParserListener());
        final RuleSet rules = rb.build();
        final List<Permissibility> results = new ArrayList<Permissibility>();

        {
            final Coordinates from = Coordinates.parse("myapp:foo");
            final PerModuleRules pm = rules.getRulesFor(from);
            checkDep(pm, from, "myapp:foo", results);
            checkDep(pm, from, "myapp:bad-module", results);
            checkDep(pm, from, "myapp:really-bad-module", results);
        }
        {
            final Coordinates from = Coordinates.parse("myapp:special-module");
            final PerModuleRules pm = rules.getRulesFor(from);
            checkDep(pm, from, "myapp:foo", results);
            checkDep(pm, from, "myapp:bad-module", results);
            checkDep(pm, from, "myapp:really-bad-module", results);
        }

        new GoldFileValidator(TEST_RESOURCES.resolve("results.goldfile")).validate(this.goldLog.toString());
    }

    private void checkDep(final PerModuleRules pm,
                          final Coordinates from,
                          final String coordinates,
                          final List<Permissibility> results) throws Exception {
        final OutboundDependency dep =
            OutboundDependency.create(Coordinates.parse(coordinates), DependencyScope.parse("compile"), true, false);
        final Permissibility p = pm.checkDependency(dep);
        goldLog.append(p, from + " -> " + coordinates);
    }

    private static GoldFileSerializer serializer() {
        return GoldFileSerializer.create(ConfigGoldFileConfig.get(), RulesGoldFileConfig.get());
    }

    private static class GoldLog {

        private final StringWriter sw = new StringWriter();
        private final PrintWriter pw = new PrintWriter(sw);

        void append(Object result, String comment) throws Exception {
            pw.println("# " + comment);
            pw.println(serializer().toString(result));
            pw.println();
        }

        @Override
        public String toString() {
            return this.sw.toString();
        }
    }

}
