package com.salesforce.trellis.whitelist.impl;

import com.salesforce.trellis.rules.RuleSet;
import com.salesforce.trellis.rules.builder.RuleSetBuilder;
import com.salesforce.trellis.rules.builder.RuleBuildingException;
import com.salesforce.trellis.whitelist.Whitelister;
import com.salesforce.trellis.whitelist.builder.WhitelistListener;
import com.salesforce.trellis.whitelist.builder.WhitelisterBuilder;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * @author pcal
 * @since 0.0.1
 */
public class WhitelisterBuilderImpl implements WhitelisterBuilder {

    private final Supplier<RuleSetBuilder> rbSupplier;
    private final List<Pair<RuleSet, WhitelistListener>> whitelists = new ArrayList();

    public WhitelisterBuilderImpl() {
        this(() -> RuleSetBuilder.create());
    }

    public WhitelisterBuilderImpl(final Supplier<RuleSetBuilder> rulesBuilderSupplier) {
        this.rbSupplier = requireNonNull(rulesBuilderSupplier);
    }

    @Override
    public WhitelisterBuilder add(final RuleSet rules, final WhitelistListener listener) {
        requireNonNull(rules, "rules must not be null");
        requireNonNull(listener, "listener must not be null");
        final Pair<RuleSet, WhitelistListener> pair = Pair.of(rules, listener);
        this.whitelists.add(pair);
        return this;
    }

    @Override
    public Whitelister build() throws RuleBuildingException {
        return new WhitelisterImpl(whitelists);
    }
}
