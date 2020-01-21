/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.maven.plugins;

import com.google.common.base.Stopwatch;
import com.salesforce.trellis.config.ConfigException;
import com.salesforce.trellis.maven.components.ReactorRulesComponent;
import com.salesforce.trellis.rules.Permissibility;
import com.salesforce.trellis.rules.RuleSet;
import com.salesforce.trellis.rules.builder.RuleBuildingException;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Maven dependency enforcer plug-in implementation.
 *
 * @author s.srinivasan
 * @since 0.0.1
 */
@Mojo(name = "enforce", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true,
                requiresDependencyResolution = ResolutionScope.TEST, requiresProject = true)
public class MavenDependencyEnforcerPlugin extends AbstractMavenDependencyPlugin {

    private Logger logger = LoggerFactory.getLogger(MavenDependencyEnforcerPlugin.class);

    // Maven Reactor scoped rules provider.
    private ReactorRulesComponent reactorRulesComponent;

    @Inject
    public MavenDependencyEnforcerPlugin(MavenProject mavenProject, ReactorRulesComponent reactorRulesComponent) {
        super(mavenProject);
        this.reactorRulesComponent = requireNonNull(reactorRulesComponent);
    }

    @Override
    public void doExecute() throws MojoExecutionException, MojoFailureException {
        try {
            // Get the reactor scoped dependency enforcement rules.
            RuleSet reactorRules =
                reactorRulesComponent.getReactorRules(getDependencyRulesConfigFiles(), getProperties());
            if (reactorRules == null) {
                logger.warn("Skipping enforcer validations."
                                + " Unable to read reactor scoped maven dependency enforcer configuration.");
                return;
            }

            final Stopwatch t = Stopwatch.createStarted();
            // Get the enforcement rules applicable for the current maven project.
            RuleSet.PerModuleRules moduleRules = reactorRules.getRulesFor(getMavenProjectCoordinates());
            if (moduleRules == null) {
                logger.info("Nothing to validate. There are no enforcement rules for this project.");
                return;
            }

            // Validate the module dependencies against the rules.
            validateModuleDependencyRules(moduleRules);

            logger.info("Successfully checked dependency constraints in " + t.toString());
        } catch (IOException | ConfigException | RuleBuildingException exception) {
            throw new MojoExecutionException("Unable to validate maven dependency enforcement rules ", exception);
        }
    }

    private void validateModuleDependencyRules(RuleSet.PerModuleRules moduleRules) throws MojoExecutionException {
        Map<String, String> disallowedDependencies = new HashMap<String,String>();

        for (Artifact dependencyArtifact : getMavenProject().getArtifacts()) {
            Permissibility permissibility = moduleRules.checkDependency(toTrellisDependency(dependencyArtifact));
            if (permissibility.isDiscouraged()) {
                logger.warn("The dependency " + dependencyArtifact + " is discouraged for this project! "
                                + (permissibility.getReason() == null ? "" : permissibility.getReason()));
            }
            if (!permissibility.isPermissible()) {
                if (permissibility.getReason() == null) {
                    logger.error(dependencyArtifact.toString() + " dependency is disallowed.");
                    disallowedDependencies.put(dependencyArtifact.toString(), "");
                }
                else {
                    logger.error(dependencyArtifact.toString() + " dependency is disallowed. "
                            + permissibility.getReason());
                    disallowedDependencies.put(dependencyArtifact.toString(), permissibility.getReason());
                }
            }
        }

        // Are there any disallowed dependencies?
        Set<String> disallowedKeys = disallowedDependencies.keySet();
        if (disallowedKeys.size() > 0) {
            String exceptionLog = "\n\n";
            for (String key : disallowedKeys) {
                if (disallowedDependencies.get(key).equals(""))
                    exceptionLog = exceptionLog + key;
                else
                    exceptionLog = exceptionLog + key + ":\n" + disallowedDependencies.get(key);

                exceptionLog = exceptionLog + "\n\n";
            }

            if (disallowedKeys.size() == 1)
                exceptionLog = "\nThe following dependency is not allowed for this project:" + exceptionLog;
            else
                exceptionLog = "\nThe following dependencies are not allowed for this project:" + exceptionLog;


            throw new MojoExecutionException(exceptionLog + getAdditionalInfoOnError());
        }
    }
}
