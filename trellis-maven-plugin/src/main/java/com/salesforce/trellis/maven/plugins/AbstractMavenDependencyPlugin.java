/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.maven.plugins;

import com.salesforce.trellis.rules.Coordinates;
import com.salesforce.trellis.rules.DependencyScope;
import com.salesforce.trellis.rules.OutboundDependency;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import static java.util.Objects.requireNonNull;

abstract class AbstractMavenDependencyPlugin extends AbstractMojo {

    private Logger logger = LoggerFactory.getLogger(MavenDependencyEnforcerPlugin.class);

    // Current maven project.
    private MavenProject mavenProject;

    @Parameter
    private boolean skip = false;

    @Parameter(defaultValue = "dependency-rules.yaml")
    private String[] configFiles;

    @Parameter
    private String additionalInfoOnError;

    @Parameter
    private Properties properties;

    // Lazily-built set describing all of the artifacts on which we have a direct/declared dependency. Dependency
    // doesn't implement equals/hashCode so we have to do it this way.
    private Set<Coordinates> directDependencies;

    public AbstractMavenDependencyPlugin(MavenProject mavenProject) {
        this.mavenProject = requireNonNull(mavenProject);
    }

    protected MavenProject getMavenProject() {
        return mavenProject;
    }

    protected String[] getDependencyRulesConfigFiles() {
        return configFiles;
    }

    protected Coordinates getMavenProjectCoordinates() {
        return Coordinates.of(mavenProject.getGroupId(), mavenProject.getArtifactId());
    }

    protected String getAdditionalInfoOnError() {
        return additionalInfoOnError == null ? "" : " " + additionalInfoOnError;
    }

    /**
     * @return optional properties that were configured in the plugin.  These will
     * be made available for ${substitution} in the rules files.
     */
    protected Properties getProperties() {
        return properties;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            logger.info("Skipping trellis goal!");
            return;
        }

        doExecute();
    }

    protected abstract void doExecute() throws MojoExecutionException, MojoFailureException;

    /**
     * Utility method for wrapping maven Artifacts in trellis' preferred abstraction.
     */
    OutboundDependency toTrellisDependency(Artifact dependencyArtifact) {
        if (directDependencies == null) {
            // lazily figure out which of our deps are direct
            this.directDependencies = new HashSet<>();
            getMavenProject().getDependencies()
                .forEach(d -> directDependencies.add(Coordinates.of(d.getGroupId(), d.getArtifactId())));
        }
        Coordinates to = Coordinates.of(dependencyArtifact.getGroupId(), dependencyArtifact.getArtifactId(),
            dependencyArtifact.getVersion());
        return OutboundDependency
            .create(to, DependencyScope.parse(dependencyArtifact.getScope()), directDependencies.contains(to),
                dependencyArtifact.isOptional());
    }
}
