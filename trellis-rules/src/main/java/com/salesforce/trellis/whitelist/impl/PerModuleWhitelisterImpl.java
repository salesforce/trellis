package com.salesforce.trellis.whitelist.impl;

import com.salesforce.trellis.rules.Coordinates;
import com.salesforce.trellis.rules.OutboundDependency;
import com.salesforce.trellis.rules.Permissibility;
import com.salesforce.trellis.rules.RuleSet;
import com.salesforce.trellis.whitelist.Whitelister;
import com.salesforce.trellis.whitelist.builder.WhitelistListener;
import com.salesforce.trellis.whitelist.builder.WhitelistedDependency;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * @author pcal
 * @since 0.0.1
 */
class PerModuleWhitelisterImpl implements Whitelister.PerModuleWhitelister {

    // ===================================================================
    // Fields

    private final Coordinates fromModule;
    private final List<Pair<RuleSet.PerModuleRules, WhitelistListener>> whitelists;
    private boolean isDone = false;

    // ===================================================================
    // Factory method

    static PerModuleWhitelisterImpl createFor(final Coordinates fromModule,
                                              final List<Pair<RuleSet, WhitelistListener>> whitelists) {
        List<Pair<RuleSet.PerModuleRules, WhitelistListener>> applicableWhitelists = null;
        for (final Pair<RuleSet, WhitelistListener> whitelist : whitelists) {
            final RuleSet.PerModuleRules moduleRules = whitelist.getLeft().getRulesFor(fromModule);
            if (moduleRules != null) {
                if (applicableWhitelists == null) applicableWhitelists = new ArrayList();
                applicableWhitelists.add(Pair.of(moduleRules, whitelist.getRight()));
            }
        }
        return applicableWhitelists == null ? null : new PerModuleWhitelisterImpl(fromModule, applicableWhitelists);
    }

    // ===================================================================
    // Constructors

    private PerModuleWhitelisterImpl(final Coordinates fromModule,
                                     final List<Pair<RuleSet.PerModuleRules, WhitelistListener>> whitelists) {
        this.whitelists = requireNonNull(whitelists);
        this.fromModule = requireNonNull(fromModule);
    }

    // ===================================================================
    // PerModuleWhitelister impl

    @Override
    public void notifyDependency(final OutboundDependency dependency) {
        checkState();
        for (final Pair<RuleSet.PerModuleRules, WhitelistListener> whitelist : this.whitelists) {
            final Permissibility pw = whitelist.getLeft().checkDependency(dependency);
            if (!pw.isPermissible()) {
                // FIXME pass in the whole dep + deal with optionality
                final WhitelistedDependency wp =
                    new WhitelistedDependencyImpl(this.fromModule, dependency.getTo(), dependency.getScope(),
                        pw.getReason());
                whitelist.getRight().notifyWhitelisted(wp);
            }
        }
    }

    @Override
    public void notifyDone() {
        checkState();
        isDone = true;
        this.whitelists.forEach(p -> p.getRight().notifyModuleProcessed(this.fromModule));
    }

    // ===================================================================
    // Private methods

    private void checkState() {
        if (isDone) throw new IllegalStateException("whitelister already used");
    }
}
