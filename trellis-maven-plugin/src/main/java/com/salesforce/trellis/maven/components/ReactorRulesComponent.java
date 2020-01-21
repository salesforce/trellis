package com.salesforce.trellis.maven.components;

import com.salesforce.trellis.config.ConfigException;
import com.salesforce.trellis.rules.RuleSet;
import com.salesforce.trellis.rules.builder.RuleBuildingException;
import com.salesforce.trellis.rules.builder.RuleSetBuilder;
import org.apache.maven.execution.MavenSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 * Maintains the reactor scoped maven dependency based enforcer configuration.
 *
 * @author s.srinivasan
 * @since 0.0.1
 */
@Named
@Singleton
public class ReactorRulesComponent {

    private final Logger logger = LoggerFactory.getLogger(ReactorRulesComponent.class);

    private final ReactorDependencyRulesConfig rulesConfig;

    // Maven reactor scoped dependency enforcer rules.
    private RuleSet reactorRules;

    // Attempt to parse reactor rules configuration only once.
    private boolean firstAttemptToGetReactorRules = true;

    @Inject
    ReactorRulesComponent(MavenSession mavenSession) {
        this.rulesConfig = new ReactorDependencyRulesConfig(mavenSession);
    }

    /**
     * Get reactor scoped rules. Very first time, parses the maven dependency
     * enforcer configuration file(s) at the configuration root and retains the parsed instance for reuse.
     *
     * @return the reactor scoped rules, ready to use or null if the previous attempt failed
     *         with an exception.
     * @throws FileNotFoundException
     *             If the configuration root does not exist.
     * @throws ConfigException
     *             If the configuration files cannot be parsed by yaml parser.
     * @throws RuleBuildingException
     *             If the rules builder runs into an error.
     */
    public RuleSet getReactorRules(final String[] dependencyRulesConfigFiles, final Properties pluginPropertiesOrNull)
        throws IOException, ConfigException, RuleBuildingException {
        synchronized (this) {
            if (reactorRules == null && firstAttemptToGetReactorRules) {
                firstAttemptToGetReactorRules = false;

                final RuleSetBuilder reactorRulesBuilder = RuleSetBuilder.create();
                reactorRulesBuilder.logger(logger);
                this.rulesConfig.setDependencyRulesConfigFiles(dependencyRulesConfigFiles);
                if (pluginPropertiesOrNull != null) this.rulesConfig.setPluginProperties(pluginPropertiesOrNull);
                this.rulesConfig.applyTo(reactorRulesBuilder);
                reactorRules = reactorRulesBuilder.build();
            }
        }
        return reactorRules;
    }

}
