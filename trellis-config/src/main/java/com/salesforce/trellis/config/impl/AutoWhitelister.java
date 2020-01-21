package com.salesforce.trellis.config.impl;

import com.google.common.base.Throwables;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.salesforce.trellis.config.FileAdapter;
import com.salesforce.trellis.config.impl.YamlModel.RuleModel;
import com.salesforce.trellis.rules.Coordinates;
import com.salesforce.trellis.rules.builder.RuleAction;
import com.salesforce.trellis.whitelist.builder.WhitelistListener;
import com.salesforce.trellis.whitelist.builder.WhitelistedDependency;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Listens for whitelisting events and updates the yaml config files as appropriate.  This is the core of the
 * implementation for whitelists that specify a 'file' property.
 *
 * @author pcal
 * @since 0.0.1
 */
class AutoWhitelister implements WhitelistListener {

    // ===================================================================
    // Fields

    private final FileAdapter whitelistFile;
    private final Set<Coordinates> modulesDone = new HashSet<>();
    private final SetMultimap<Coordinates, WhitelistedDependency> whitelistEntries = HashMultimap.create();
    private final String headerCommentOrNull;
    private final Logger logger;
    private final RuleAction action;

    // ===================================================================
    // Constructor

    AutoWhitelister(final FileAdapter whitelistFile,
                    final RuleAction action,
                    final String headerCommentOrNull,
                    final Logger logger) {
        this.whitelistFile = requireNonNull(whitelistFile);
        this.action = requireNonNull(action);
        this.headerCommentOrNull = headerCommentOrNull;
        this.logger = requireNonNull(logger);
    }

    // ===================================================================
    // WhitelistListener impl

    @Override
    public void notifyWhitelisted(WhitelistedDependency dep) {
        checkState(dep.getFromModule());
        logger.debug("whitelisting " + dep.getFromModule());
        whitelistEntries.put(dep.getFromModule(), dep);
    }

    @Override
    public void notifyModuleProcessed(final Coordinates fromModule) {
        synchronized (this) {
            checkState(fromModule);
            modulesDone.add(fromModule);
        }
        final Set<WhitelistedDependency> deps = this.whitelistEntries.removeAll(fromModule);
        if (deps == null || deps.size() == 0) {
            logger.debug("no whitelists notifications received for " + fromModule);
        }
        try {
            // FIXME synchronize on specific config instead
            synchronized (AutoWhitelister.class) {
                updateFile(fromModule, deps);
            }
        } catch (IOException e) {
            Throwables.propagate(e); //FIXME
        }
    }

    // ===================================================================
    // Exposed for unit testing

    void appendRules(final Set<WhitelistedDependency> whitelistedDeps, final YamlModel whitelistModel) {
        final List<RuleModel> rulesCopy =
            whitelistModel.getRules() == null ? new ArrayList<>() : new ArrayList<>(whitelistModel.getRules());
        for (final WhitelistedDependency dep : whitelistedDeps) {
            final RuleModel newRule = new RuleModel();
            newRule
                .setFrom(Collections.singletonList(SourceLocatableString.of(dep.getFromModule().getCanonicalString())));
            newRule.setTo(Collections.singletonList(SourceLocatableString.of(dep.getToModule().getCanonicalString())));
            newRule.setScope(SourceLocatableString.of(dep.getScope().toString()));
            newRule.setAction(SourceLocatableString.of(this.action.name()));
            if (dep.getReason() != null) newRule.setReason(SourceLocatableString.of(dep.getReason()));
            rulesCopy.add(newRule);
            whitelistModel.setRules(rulesCopy);
        }
    }

    void stripRulesFrom(final Coordinates fromModule, final YamlModel whitelistModel) {
        requireNonNull(fromModule, "null fromModule");
        requireNonNull(whitelistModel, "null whitelistModel");
        if (whitelistModel.getRules() == null) return;
        final List<RuleModel> rulesCopy = new ArrayList<>(whitelistModel.getRules());
        for (final Iterator<RuleModel> i = rulesCopy.iterator(); i.hasNext(); ) {
            final RuleModel rule = i.next();
            if (rule.getFrom() == null) continue;
            final List<SourceLocatableString> from = new ArrayList<>(rule.getFrom());
            for (final Iterator<SourceLocatableString> f = from.iterator(); f.hasNext(); ) {
                final Coordinates existing = Coordinates.parse(f.next().toString());
                if (fromModule.equals(existing)) f.remove();
            }
            if (from.isEmpty()) {
                i.remove();
            } else {
                rule.setFrom(from);
            }
        }
        whitelistModel.setRules(rulesCopy);
    }

    // ===================================================================
    // Private methods

    private void updateFile(final Coordinates fromModule, final Set<WhitelistedDependency> whitelistedDeps)
        throws IOException {
        requireNonNull(fromModule);
        requireNonNull(whitelistedDeps);
        try {
            // test for the file existence before we enter the execute block, because opening the FileChannel will
            // create an empty file to lock if it doesn't exist.
            final boolean whitelistFileExists = this.whitelistFile.exists();
            this.whitelistFile.executeExclusiveWrite(() -> {
                YamlModel whitelistModel;
                if (whitelistFileExists) {
                    try (final Reader fr = this.whitelistFile.getReader()) {
                        whitelistModel = new YamlParser(this.whitelistFile).readValue(fr, YamlModel.class);
                        stripRulesFrom(fromModule, whitelistModel);
                    }
                } else {
                    whitelistModel = new YamlModel();
                }
                appendRules(whitelistedDeps, whitelistModel);
                new YamlModelTransformer(whitelistModel).consolidate().canonicalize();
                try (final Writer fw = this.whitelistFile.getWriter()) {
                    if (this.headerCommentOrNull != null) {
                        writeYamlComment(this.headerCommentOrNull, fw);
                    }
                    new YamlParser(whitelistFile).writeValue(fw, whitelistModel);
                }
                return null;
            });
        } catch (final Exception e) {
            Throwables.propagateIfPossible(e, IOException.class);
            throw new RuntimeException(e);
        }
    }

    private void writeYamlComment(final String untrimmedComment, final Writer writer) throws IOException {
        final String comment = untrimmedComment.trim();
        final BufferedReader in = new BufferedReader(new StringReader(comment));
        writer.append("#\n");
        String line;
        while ((line = in.readLine()) != null) {
            writer.append("# ");
            writer.append(line);
            writer.append("\n");
        }
        writer.append("#\n");
    }

    private void checkState(Coordinates fromModule) {
        if (modulesDone.contains(fromModule)) {
            throw new IllegalStateException(fromModule + " already processed");
        }
    }
}
