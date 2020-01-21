/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.config.impl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.salesforce.trellis.config.impl.YamlModel.RuleModel;
import org.apache.commons.lang3.builder.EqualsBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Performs transformations on a YamlModel.
 *
 * @author pcal
 * @since 0.0.1
 */
class YamlModelTransformer {

    // ===================================================================
    // Fields

    private final YamlModel target;

    // ===================================================================
    // Constructor

    YamlModelTransformer(YamlModel target) {
        this.target = requireNonNull(target);
    }

    // ===================================================================
    // Package methods

    /**
     * Dedupe and sort everything.
     */
    YamlModelTransformer canonicalize() {
        if (target.getGroups() != null) {
            target.getGroups().forEach(g -> {
                g.setIncludes(stripDupsAndSort(g.getIncludes()));
            });
            target.setGroups(stripDupsAndSort(target.getGroups()));
        }
        if (target.getRules() != null) {
            target.getRules().forEach(r -> {
                r.setTo(stripDupsAndSort(r.getTo()));
                r.setFrom(stripDupsAndSort(r.getFrom()));
            });
            target.setRules(stripDupsAndSort(target.getRules()));
        }
        return this;
    }

    /**
     * Reorganize the rules such that there is one rule per unique 'from' coordinates
     * in the existing from set.  The intent here is to make whitelists more readable.
     * <p/>
     * It's recommended that the rules be canonicalized after this transform is applied.
     */
    YamlModelTransformer consolidate() {
        if (target.getRules() == null) return this;
        final List<RuleModel> oldRules = stripDupsAndSort(target.getRules());
        if (oldRules.size() <= 1) {
            return this;
        }
        final List<RuleModel> newRules = new ArrayList<>();
        while (oldRules.size() > 0) {
            final Iterator<RuleModel> i = oldRules.iterator();
            final List<RuleModel> subset = new ArrayList<>();
            subset.add(i.next());
            i.remove();
            while (i.hasNext()) {
                final RuleModel next = i.next();
                if (matchesExceptToFrom(subset.get(0), next)) {
                    subset.add(next);
                    i.remove();
                }
            }
            final SetMultimap<SourceLocatableString, SourceLocatableString> smm = HashMultimap.create();
            subset.forEach(r -> {
                if (r.getFrom() == null || r.getTo() == null) {
                    //log warning?
                } else {
                    r.getTo().forEach(to -> smm.putAll(to, r.getFrom()));
                }
            });
            smm.keySet().forEach(k -> {
                final RuleModel newRule = copyRule(subset.get(0));
                newRule.setFrom(new ArrayList<>(smm.get(k)));
                newRule.setTo(Collections.singletonList(k));
                newRules.add(newRule);
            });
        }
        target.setRules(newRules);
        return this;
    }

    // ===================================================================
    // Private methods

    private RuleModel copyRule(final RuleModel r) {
        final RuleModel out = new RuleModel();
        out.setAction(r.getAction());
        out.setScope(r.getScope());
        out.setReason(r.getReason());
        out.setDistance(r.getDistance());
        out.setWhitelist(r.getWhitelist());
        return out;
    }

    private boolean matchesExceptToFrom(final RuleModel r1, final RuleModel r2) {
        return new EqualsBuilder().//
            append(r1.getAction(), r2.getAction()).//
            append(r1.getDistance(), r2.getDistance()).//
            append(r1.getScope(), r2.getScope()).//
            append(r1.getWhitelist(), r2.getWhitelist()).//
            append(r1.getReason(), r2.getReason()).isEquals();
    }

    /**
     * Exposed only for unit testing.
     */
    <T> List<T> stripDupsAndSort(List<T> list) {
        final List<T> out = new ArrayList<T>(new HashSet<>(list));
        Collections.sort((List) out);
        return out;
    }

}
