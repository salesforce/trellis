/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.config.impl;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.salesforce.trellis.config.FileAdapter;
import com.salesforce.trellis.config.MavenHelper;
import com.salesforce.trellis.config.impl.YamlModel.WhitelistModel;
import com.salesforce.trellis.config.impl.YamlModel.RuleModel;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Stack;

import static java.util.Objects.requireNonNull;

/**
 * Encapsulates a self-contained set of parsed config files.
 *
 * @author pcal
 * @since 0.0.1
 */
class ParsedYamlFiles {

    // ===================================================================
    // Fields

    private final ConfigErrorReporter errorLog;
    private final MavenHelper mavenHelper;
    private final LinkedHashMap<FileAdapter, ParsedYamlFile> parsedFiles;
    private final Stack<ParsedYamlFile> cycleGuard;
    private final boolean tolerateMissingWhitelists;
    private final Logger logger;

    // ===================================================================
    // Constructor

    ParsedYamlFiles(final MavenHelper mavenHelper,
                    final ConfigErrorReporter errorLog,
                    boolean tolerateMissingWhitelists,
                    final Logger logger) {
        this.parsedFiles = new LinkedHashMap<>();
        this.mavenHelper = requireNonNull(mavenHelper);
        this.errorLog = requireNonNull(errorLog);
        this.tolerateMissingWhitelists = tolerateMissingWhitelists;
        this.logger = requireNonNull(logger);
        this.cycleGuard = new Stack<>();
    }

    // ===================================================================
    // Package methods

    /**
     * Parses the given files and all of their imports and whitelists, recursively.  When this method returns, we will
     * contain the transitive closure of the referenced configuation rooted in the given files.
     * <p/>
     * Any errors encountered will be repor        <dependency>
     * <groupId>org.apache.commons</groupId>
     * <artifactId>commons-lang3</artifactId>
     * </dependency>
     * ted to the provided ConfigErrorReporter; this method never throws.
     */
    void parseAll(Collection<FileAdapter> files) {
        cycleGuard.clear();
        final Stopwatch sw = Stopwatch.createStarted();
        files.forEach(f -> add(f));
        logger.info("Parsed " + files.size() + " trellis config file(s) in " + sw);
    }

    /**
     * @return a topologically-sorted list of all of the parsed files, including any that were dragged in via imports or
     * whitelisting.
     */
    List<ParsedYamlFile> getFiles() {
        return ImmutableList.copyOf(parsedFiles.values()); //they're already topo-sorted
    }

    // ===================================================================
    // Private methods

    private ParsedYamlFile add(final FileAdapter file) {
        //
        // Check for cycles in the dependency graph
        //
        for (ParsedYamlFile pyf : cycleGuard) {
            if (pyf.getFile().equals(file)) {
                this.errorLog.error(file, "cyclical imports detected " + this.cycleGuard);
                return null;
            }
        }
        if (parsedFiles.containsKey(file)) {
            logger.debug("skipping " + file + " because it has already been processed.");
            final ParsedYamlFile pyf = this.parsedFiles.get(file);
            if (pyf == null) throw new IllegalStateException("should have parsed file " + file);
            return pyf;
        }
        logger.debug("processing " + file);
        final ParsedYamlFile pyf;
        try {
            final Stopwatch sw = Stopwatch.createStarted();
            pyf = ParsedYamlFile.parse(file, mavenHelper);
            logger.debug("parsed and interpolated " + file + " in " + sw);
        } catch (IOException e) {
            errorLog.error(file, e);
            return null;
        }
        cycleGuard.push(pyf);
        final YamlModel model = pyf.getModel();
        try {
            if (model.getWhitelists() != null) {
                for (final WhitelistModel whitelist : model.getWhitelists()) {
                    if (whitelist.getFile() != null) {
                        checkAndAddFile(pyf.getFile(), whitelist.getFile());
                    } else {
                        this.errorLog.error(whitelist.getLocation(), "'whitelist' is missing required 'file'");
                    }
                }
            }
            if (model.getRules() != null) {
                for (final RuleModel rule : model.getRules()) {
                    if (rule.getWhitelist() != null) checkAndAddFile(pyf.getFile(), rule.getWhitelist());
                }
            }
        } finally {
            logger.debug("done processing " + file);
            cycleGuard.pop();
        }
        //
        // Finally, add the file we've built.
        //
        this.parsedFiles.put(file, pyf);
        return pyf;
    }

    private void checkAndAddFile(final FileAdapter base, final SourceLocatableString file) {
        this.logger.debug("processing file " + file);
        final Path wlPath = Paths.get(file.toString());
        final FileAdapter wlFile;
        if (!wlPath.isAbsolute()) {
            wlFile = base.getRelativeFile(wlPath);
        } else {
            wlFile = FileAdapter.forPath(wlPath);
        }

        if (wlFile.exists()) {
            add(wlFile);
        } else {
            if (!this.tolerateMissingWhitelists) {
                this.errorLog.error(file.getLocation(), "whitelist file does not exist: " + wlFile.getLocation());
            } else {
                this.logger.warn("whitelist file does not exist: " + wlFile.getLocation());

            }
        }
    }
}
