/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.config.impl;

import com.salesforce.trellis.config.MavenHelper;
import com.salesforce.trellis.config.impl.YamlModel.GroupModel;
import com.salesforce.trellis.rules.Coordinates;
import com.salesforce.trellis.rules.builder.GroupBuilder;
import com.salesforce.trellis.rules.builder.GroupSetBuilder;
import com.salesforce.trellis.rules.builder.RuleBuildingException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Walks a YamlModel and applies it to a given RuleSetBuilder.
 * <p/>
 * Note that this doesn't do anything with imports or whitelists.
 *
 * @author pcal
 * @since 0.0.1
 */
class YamlGroupsApplier {

    // ===================================================================
    // Fields

    private final ConfigErrorReporter errorLog;
    private final GroupSetBuilder gsb;
    private final MavenHelper mavenHelper;

    // ===================================================================
    // Constructor

    YamlGroupsApplier(final GroupSetBuilder groupSetBuilder,
                      final MavenHelper mavenHelper,
                      final ConfigErrorReporter errorLog) {
        this.mavenHelper = requireNonNull(mavenHelper);
        this.errorLog = requireNonNull(errorLog);
        this.gsb = requireNonNull(groupSetBuilder);
    }

    // ===================================================================
    // Package methods

    /**
     * Applies all of the groups from the given model to our RuleSetBuilder.
     */
    void apply(final Collection<GroupModel> groups) {
        if (groups != null) {
            for (final GroupModel group : groups) {
                apply(group);
            }
        }
    }

    /**
     * Apply the given group to the builder.
     */
    void apply(final GroupModel group) {
        requireNonNull(group, "null group");
        boolean errorsEncountered = false;
        final GroupBuilder gb = gsb.group();
        if (group.getName() != null) {
            try {
                gb.name(group.getName().toString());
            } catch (RuleBuildingException e) {
                this.errorLog.error(group.getLocation(), e);
                errorsEncountered = true;
            }
        }
        if (group.getIncludes() != null) {
            for (final SourceLocatableString include : group.getIncludes()) {
                try {
                    gb.include(include.toString());
                } catch (RuleBuildingException e) {
                    this.errorLog.error(include.getLocation(), e);
                    errorsEncountered = true;
                }
            }
        }
        if (group.getExcept() != null) {
            for (final SourceLocatableString except : group.getExcept()) {
                try {
                    gb.except(except.toString());
                } catch (RuleBuildingException e) {
                    this.errorLog.error(except.getLocation(), e);
                    errorsEncountered = true;
                }
            }
        }
        if (group.getPomDependencies() != null) {
            for (final SourceLocatableString pom : group.getPomDependencies()) {
                final Set<Coordinates> deps;
                final Path pathToPom = Paths.get(pom.toString());
                try {
                    deps = mavenHelper.getDependenciesFromPom(pathToPom);
                } catch (IOException e) {
                    this.errorLog.error(pom.getLocation(), e);
                    errorsEncountered = true;
                    continue;
                }
                if (deps != null) {
                    for (final Coordinates dep : deps) {
                        try {
                            gb.include(dep.getCanonicalString());
                        } catch (RuleBuildingException e) {
                            this.errorLog.error(pom.getLocation(), e);
                            errorsEncountered = true;
                        }
                    }
                }
            }
        }
        if (!errorsEncountered) {
            try {
                gb.build();
            } catch (RuleBuildingException e) {
                this.errorLog.error(group.getLocation(), e);
            }
        }
    }
}
