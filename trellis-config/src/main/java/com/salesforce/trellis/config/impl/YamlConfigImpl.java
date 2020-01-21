package com.salesforce.trellis.config.impl;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.salesforce.trellis.config.Config;
import com.salesforce.trellis.config.ConfigException;
import com.salesforce.trellis.config.FileAdapter;
import com.salesforce.trellis.config.MavenHelper;
import com.salesforce.trellis.config.ParserListener;
import com.salesforce.trellis.config.impl.YamlModel.RuleModel;
import com.salesforce.trellis.config.impl.YamlModel.WhitelistModel;
import com.salesforce.trellis.rules.RuleSet;
import com.salesforce.trellis.rules.builder.GroupSet;
import com.salesforce.trellis.rules.builder.GroupSetBuilder;
import com.salesforce.trellis.rules.builder.RuleAction;
import com.salesforce.trellis.rules.builder.RuleBuildingException;
import com.salesforce.trellis.rules.builder.RuleSetBuilder;
import com.salesforce.trellis.whitelist.builder.WhitelistListener;
import com.salesforce.trellis.whitelist.builder.WhitelisterBuilder;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static com.salesforce.trellis.config.impl.SourceLocatableString.unwrap;
import static java.util.Objects.requireNonNull;

/**
 * Use this to set up the config for a set coordinates config to be parsed into a single set coordinates rules.
 *
 * @author pcal
 * @since 0.0.1
 */
public class YamlConfigImpl implements Config {

    // ===================================================================
    // Constants

    /**
     * The action that is given to generated whitelist rules when nothing is specified via config or properties.
     */
    private static final RuleAction DEFAULT_WHITELIST_ACTION = RuleAction.WARN;

    // ===================================================================
    // Fields

    private final List<FileAdapter> files;
    private final MavenHelper mavenHelper;
    private final AutoWhitelisterFactory listenerFactory;
    private final Logger logger;
    private final Supplier<RuleSetBuilder> rsbSupplier;
    private final Supplier<GroupSetBuilder> gsbSupplier;

    // ===================================================================
    // Constructors

    YamlConfigImpl(final List<FileAdapter> files,
                   final MavenHelper mavenHelper,
                   final AutoWhitelisterFactory listenerFactory,
                   final Supplier<RuleSetBuilder> rsbSupplier,
                   final Supplier<GroupSetBuilder> gsbSupplier,
                   final Logger logger) {
        this.files = requireNonNull(files);
        this.mavenHelper = requireNonNull(mavenHelper);
        this.rsbSupplier = requireNonNull(rsbSupplier);
        this.gsbSupplier = requireNonNull(gsbSupplier);
        this.listenerFactory = requireNonNull(listenerFactory);
        this.logger = requireNonNull(logger);
    }

    // ===================================================================
    // Config impl

    @Override
    public void applyTo(final RuleSetBuilder rulesBuilder, final ParserListener listenerOrNull) throws ConfigException {
        requireNonNull(rulesBuilder, "rulesBuilder may not be null");
        final ConfigErrorReporter errorReporter = new ConfigErrorReporter(listenerOrNull);
        final List<ParsedYamlFile> parsed = parseFiles(errorReporter, true);
        for (final ParsedYamlFile yamlFile : parsed) {
            final GroupSetBuilder gsb = gsbSupplier.get();
            final YamlModel model = requireNonNull(yamlFile.getModel());
            new YamlGroupsApplier(gsb, mavenHelper, errorReporter).apply(model.getGroups());
            final RuleSetBuilder rsb = rsbSupplier.get().groups(gsb.build());
            new YamlRulesApplier(rsb, errorReporter).apply(model.getRules());
            try {
                rulesBuilder.addRules(rsb.build());
            } catch (RuleBuildingException e) {
                errorReporter.error(e);
                continue;
            }
        }
        if (errorReporter.isFatalErrorEncountered()) {
            throw new ConfigException(
                "Fatal errors were encountered building the configuration.  See log for details.");
        }
    }

    @Override
    public void applyTo(final WhitelisterBuilder wb, final ParserListener listenerOrNull) throws ConfigException {
        requireNonNull(wb, WhitelisterBuilder.class + " is required");
        //
        // set up some basic stuff we're going to need
        //
        final RuleSetBuilder fullRulesBuilder = rsbSupplier.get();
        final ConfigErrorReporter errorReporter = new ConfigErrorReporter(listenerOrNull);

        //
        // Ok, first step is to build up a set of rules based on this configuration.
        //
        final List<ParsedYamlFile> parsed = parseFiles(errorReporter, true);
        //
        // Now we need to build up a mapping that will tell us which rule definitions correspond to which
        // whitelist files.  Note that more than one rule can be whitelisted in the same file.  We ignore any
        // rules that that aren't getting whitelisted.
        //
        final Set<FileAdapter> usedFiles = new HashSet<>();
        //
        // Ok, go through all of the files
        //
        for (final ParsedYamlFile ypf : parsed) {
            final YamlModel model = ypf.getModel();
            //
            // For each file, parse out the group definitions.  They will apply only to rules in the current file.
            //
            final GroupSet groups;
            {
                final GroupSetBuilder gsb = this.gsbSupplier.get();
                new YamlGroupsApplier(gsb, mavenHelper, errorReporter).apply(model.getGroups());
                groups = gsb.build();
            }
            //
            // If there are any declared whitelist configurations, make note of each and which file they apply to.
            //
            final BiMap<FileAdapter, WhitelistModel> file2whitelist = HashBiMap.create();
            if (model.getWhitelists() != null) {
                for (WhitelistModel whitelist : model.getWhitelists()) {

                    final FileAdapter whitelistFile;
                    final Path whitelistPath = Paths.get(whitelist.getFile().toString());
                    if (whitelistPath.isAbsolute()) {
                        whitelistFile = FileAdapter.forPath(whitelistPath);
                    } else {
                        whitelistFile = ypf.getFile().getRelativeFile(whitelistPath);
                    }
                    if (!usedFiles.add(whitelistFile)) {
                        errorReporter.error(whitelist.getLocation(),
                            "multiple whitelist configurations reference the same file: " + whitelist.getFile());
                    } else {
                        file2whitelist.put(whitelistFile, whitelist);
                    }
                }
            }
            //
            // Go through all of the rules and process any of them that specify a 'whitelist.'  Figure out if they
            // match to a whitelist configuration we noted above; synthesize a default one if not.  Build a set of
            // rules for all of the rules that use each whitelist
            //
            final Map<WhitelistModel, RuleSetBuilder> rulesPerWhitelistModel = new HashMap<>();
            if (model.getRules() != null) {
                for (final RuleModel rule : model.getRules()) {
                    if (rule.getWhitelist() != null) {
                        final Path whitelistPath = Paths.get(rule.getWhitelist().toString());
                        final FileAdapter whitelistFile;
                        if (whitelistPath.isAbsolute()) {
                            whitelistFile = FileAdapter.forPath(whitelistPath);

                        } else {
                            whitelistFile = ypf.getFile().getRelativeFile(whitelistPath);
                        }
                        WhitelistModel whitelist = file2whitelist.get(whitelistFile);
                        if (whitelist == null) {
                            logger.debug("creating default whitelist config for " + rule.getWhitelist() + " " + rule
                                .getLocation());
                            whitelist = new WhitelistModel();
                            whitelist.setFile(rule.getWhitelist());
                            whitelist.setLocation(rule.getWhitelist().getLocation());
                            whitelist = ypf.getInterpolator().interpolate(whitelist);
                            file2whitelist.put(whitelistFile, whitelist);
                        }
                        final RuleSetBuilder rsb;
                        if (rulesPerWhitelistModel.containsKey(whitelist)) {
                            rsb = rulesPerWhitelistModel.get(whitelist);
                        } else {
                            rsb = rsbSupplier.get().groups(groups);
                            rulesPerWhitelistModel.put(whitelist, rsb);
                        }
                        new YamlRulesApplier(rsb, errorReporter).apply(rule);
                    }
                }
            }
            //
            // At this point, if we have any rules to whitelist, build the RuleSets (from RuleSetBuilders we created in
            // the previous step) and then register WhitelistListeners for each one.
            //
            if (rulesPerWhitelistModel.isEmpty()) {
                logger.debug("no whitelists to process in " + ypf.getFile());
                continue;
            } else {
                for (final WhitelistModel whitelist : rulesPerWhitelistModel.keySet()) {
                    final RuleSet rules;
                    try {
                        rules = requireNonNull(rulesPerWhitelistModel.get(whitelist)).build();
                    } catch (RuleBuildingException e) {
                        errorReporter.error(e);
                        continue;
                    }
                    final RuleAction action;
                    if (whitelist.getAction() == null) {
                        action = DEFAULT_WHITELIST_ACTION;
                    } else {
                        try {
                            action = RuleAction.valueOf(unwrap(whitelist.getAction()));
                        } catch (IllegalArgumentException | NullPointerException e) {
                            errorReporter.error(whitelist.getLocation(), e);
                            continue;
                        }
                    }
                    final FileAdapter file = file2whitelist.inverse().get(whitelist);
                    final String headerCommentOrNull = unwrap(whitelist.getHeaderComment());
                    final WhitelistListener listener =
                        requireNonNull(this.listenerFactory.create(file, action, headerCommentOrNull));
                    wb.add(rules, listener);
                }
            }
        }
        //
        // Bail if we found any fatal errors
        //
        if (errorReporter.isFatalErrorEncountered()) {
            throw new ConfigException(
                "Fatal errors were encountered building the configuration.  See log for details.");
        }
    }

    // ===================================================================
    // Private methods

    /**
     * Parse all our files and return them as a list.
     */
    private List<ParsedYamlFile> parseFiles(final ConfigErrorReporter errorReporter,
                                            final boolean tolerateMisingWhitelists) {
        final ParsedYamlFiles files =
            new ParsedYamlFiles(this.mavenHelper, errorReporter, tolerateMisingWhitelists, logger);
        files.parseAll(this.files);
        return files.getFiles();
    }
}
