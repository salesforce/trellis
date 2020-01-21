package com.salesforce.trellis.maven.components;

import com.salesforce.trellis.config.ConfigException;
import com.salesforce.trellis.rules.builder.RuleBuildingException;
import com.salesforce.trellis.whitelist.Whitelister;
import com.salesforce.trellis.whitelist.builder.WhitelisterBuilder;
import org.apache.maven.execution.MavenSession;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 * Maintains the reactor scoped maven dependency based enforcer whitelist configuration updater.
 *
 * @author s.srinivasan
 * @since 0.0.1
 */
@Named
@Singleton
public class ReactorRulesWhitelisterComponent {

    private final ReactorDependencyRulesConfig rulesConfig;

    // Maven reactor scoped dependency enforcer rules whitelist configuration updater.
    private Whitelister reactorRulesWhitelister;

    // Attempt to parse reactor rules configuration only once.
    private boolean firstAttemptToGetReactorRulesWhitelister = true;

    @Inject
    ReactorRulesWhitelisterComponent(MavenSession mavenSession) {
        this.rulesConfig = new ReactorDependencyRulesConfig(mavenSession);
    }

    /**
     * Get reactor scoped rules whitelist configuration updater. Very first time, parses the maven dependency
     * enforcer configuration file(s) at the configuration root and retains the parsed instance for reuse.
     *
     * @return the reactor scoped rules whitelist updater, ready to use or null if the previous attempt failed
     *         with an exception.
     * @throws FileNotFoundException
     *             If the configuration root does not exist.
     * @throws ConfigException
     *             If the configuration files cannot be parsed by yaml parser.
     * @throws RuleBuildingException
     *             If the whitelist builder runs into an error.
     */
    public Whitelister getReactorRulesWhitelister(final String[] dependencyRulesConfigFiles,
                                                  final Properties pluginPropertiesOrNull)
        throws IOException, ConfigException, RuleBuildingException {
        synchronized (this) {
            if (reactorRulesWhitelister == null && firstAttemptToGetReactorRulesWhitelister) {
                firstAttemptToGetReactorRulesWhitelister = false;

                final WhitelisterBuilder reactorRulesWhiteListBuilder = WhitelisterBuilder.create();
                this.rulesConfig.setDependencyRulesConfigFiles(dependencyRulesConfigFiles);
                if (pluginPropertiesOrNull != null) this.rulesConfig.setPluginProperties(pluginPropertiesOrNull);
                this.rulesConfig.applyTo(reactorRulesWhiteListBuilder);
                reactorRulesWhitelister = reactorRulesWhiteListBuilder.build();
            }
        }

        return reactorRulesWhitelister;
    }
}
