package com.salesforce.trellis.maven.plugins;

import com.salesforce.trellis.config.ConfigException;
import com.salesforce.trellis.maven.components.ReactorRulesWhitelisterComponent;
import com.salesforce.trellis.rules.builder.RuleBuildingException;
import com.salesforce.trellis.whitelist.Whitelister;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;

import static java.util.Objects.requireNonNull;

/**
 * Maven dependency enforcer plug-in implementation.
 *
 * @author s.srinivasan
 * @since 0.0.1
 */
@Mojo(name = "update-whitelists", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true,
                requiresDependencyResolution = ResolutionScope.TEST)
public class MavenDependencyWhitelistUpdaterPlugin extends AbstractMavenDependencyPlugin {

    private Logger logger = LoggerFactory.getLogger(MavenDependencyWhitelistUpdaterPlugin.class);

    // Maven Reactor scoped rules provider.
    private ReactorRulesWhitelisterComponent reactorRulesWhitelistComponent;

    @Inject
    public MavenDependencyWhitelistUpdaterPlugin(MavenProject mavenProject,
                                                 ReactorRulesWhitelisterComponent reactorRulesWhitelistComponent) {
        super(mavenProject);
        this.reactorRulesWhitelistComponent = requireNonNull(reactorRulesWhitelistComponent);
    }

    @Override
    public void doExecute() throws MojoExecutionException, MojoFailureException {
        try {
            // Get the reactor scoped dependency enforcement rules.
            Whitelister reactorRulesWhiteLister = reactorRulesWhitelistComponent
                .getReactorRulesWhitelister(getDependencyRulesConfigFiles(), getProperties());
            if (reactorRulesWhiteLister == null) {
                logger.warn("Skipping maven dependency enforcement rules update."
                                + " Unable to read reactor scoped maven dependency enforcer configuration.");
                return;
            }

            // Get the enforcement rules applicable for the current maven project.
            Whitelister.PerModuleWhitelister moduleRulesWhiteLister =
                            reactorRulesWhiteLister.getWhitelister(getMavenProjectCoordinates());
            if (moduleRulesWhiteLister == null) {
                logger.info("Nothing to update. There are no enforcement rules for this project.");
                return;
            }

            // Validate the module dependencies against the rules.
            updateModuleDependencyRulesWhitelist(moduleRulesWhiteLister);

            logger.info("Successfully validated and updated maven dependency enforcement rules.");
        } catch (IOException | ConfigException | RuleBuildingException exception) {
            throw new MojoExecutionException("Unable to update maven dependency enforcement rules ", exception);
        }
    }

    private void updateModuleDependencyRulesWhitelist(Whitelister.PerModuleWhitelister moduleRulesWhiteLister) {
        for (Artifact dependencyArtifact : getMavenProject().getArtifacts()) {
            moduleRulesWhiteLister.notifyDependency(toTrellisDependency(dependencyArtifact));
        }

        moduleRulesWhiteLister.notifyDone();
    }
}
