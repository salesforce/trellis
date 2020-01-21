/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.config.impl;

import com.salesforce.trellis.config.MavenHelper;
import com.salesforce.trellis.config.impl.SourceLocatable.SourceLocation;
import com.salesforce.trellis.config.impl.YamlModel.GroupModel;
import com.salesforce.trellis.config.impl.YamlModel.RuleModel;
import com.salesforce.trellis.config.impl.YamlModel.WhitelistModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * Helper class that substitutes environmental and declared property values into the parsed model.
 *
 * @author pcal
 * @since 0.0.5
 */
final class YamlModelInterpolator {

    // ===================================================================
    // Constants

    private static final String DEFAULT_WHITELIST_HEADER_PROP = "${trellis.whitelist.headerComment}";
    private static final String DEFAULT_WHITELIST_ACTION_PROP = "${trellis.whitelist.action}";
    private static final String DEFAULT_RULE_REASON_PROP = "${trellis.rule.reason}";

    // ===================================================================
    // Fields

    private final Function<String, String> stringInterpolator;

    // ===================================================================
    // Constructor

    YamlModelInterpolator(final MavenHelper helper, final Properties propertiesOrNull) {
        requireNonNull(helper, "helper");
        if (propertiesOrNull == null) {
            this.stringInterpolator = helper.getInterpolator();
        } else {
            this.stringInterpolator = helper.createInterpolator(propertiesOrNull);
        }
    }

    // ===================================================================
    // Packge methods

    YamlModel interpolate(final YamlModel in) {
        requireNonNull(in);
        final YamlModel out = new YamlModel();
        if (in.getRules() != null) {
            final List<RuleModel> rules = new ArrayList<>();
            in.getRules().forEach(r -> rules.add(interpolate(r)));
            out.setRules(rules);
        }
        if (in.getGroups() != null) {
            final List<GroupModel> groups = new ArrayList<>();
            in.getGroups().forEach(g -> groups.add(interpolate(g)));
            out.setGroups(groups);
        }
        if (in.getWhitelists() != null) {
            final List<WhitelistModel> whitelists = new ArrayList<>();
            in.getWhitelists().forEach(w -> whitelists.add(interpolate(w)));
            out.setWhitelists(whitelists);
        }
        return out;
    }

    WhitelistModel interpolate(final WhitelistModel in) {
        requireNonNull(in);
        final WhitelistModel out = new WhitelistModel();
        // these are NOT interpolated:
        out.setLocation(in.getLocation());
        // these are interpolated:
        out.setAction(interpolate(in.getAction(), in.getLocation(), DEFAULT_WHITELIST_ACTION_PROP));
        out.setFile(interpolate(in.getFile()));
        out.setHeaderComment(interpolate(in.getHeaderComment(), in.getLocation(), DEFAULT_WHITELIST_HEADER_PROP));
        return out;
    }

    private GroupModel interpolate(final GroupModel in) {
        requireNonNull(in);
        final GroupModel out = new GroupModel();
        // these are NOT interpolated:
        out.setName(in.getName());
        out.setLocation(in.getLocation());
        // these are interpolated:
        out.setIncludes(interpolate(in.getIncludes()));
        out.setExcept(interpolate(in.getExcept()));
        out.setPomDependencies(interpolate(in.getPomDependencies()));
        return out;
    }

    private RuleModel interpolate(final RuleModel in) {
        requireNonNull(in);
        final RuleModel out = new RuleModel();
        if (in.getWhitelist() != null) {
            out.setWhitelist(interpolate(in.getWhitelist()));
        }
        // these are NOT interpolated:
        out.setLocation(in.getLocation());
        // these are interpolated:
        out.setReason(interpolate(in.getReason(), in.getLocation(), DEFAULT_RULE_REASON_PROP));
        out.setFrom(interpolate(in.getFrom()));
        out.setExceptFrom(interpolate(in.getExceptFrom()));
        out.setTo(interpolate(in.getTo()));
        out.setExceptTo(interpolate(in.getExceptTo()));
        out.setOptionality(interpolate(in.getOptionality()));
        out.setScope(interpolate(in.getScope()));
        out.setAction(interpolate(in.getAction()));
        out.setDistance(interpolate(in.getDistance()));
        out.setWhitelist(interpolate(in.getWhitelist()));
        return out;
    }


    private List<SourceLocatableString> interpolate(final List<SourceLocatableString> in) {
        if (in == null) {
            return null;
        } else {
            final List<SourceLocatableString> out = new ArrayList<>();
            in.forEach(s -> out.add(interpolate(s)));
            return out;
        }
    }

    // exposed for unit testing
    SourceLocatableString interpolate(final SourceLocatableString in) {
        if (in == null) {
            return null;
        } else {
            final String value = this.stringInterpolator.apply(in.toString());
            final SourceLocatableString out = SourceLocatableString.of(value);
            out.setLocation(in.getLocation());
            return out;
        }
    }

    private SourceLocatableString interpolate(final SourceLocatableString in,
                                              final SourceLocation location,
                                              final String defaultProperty) {
        if (in != null) {
            return interpolate(in);
        } else {
            // yuck this is kind of gross
            final String sub = defaultProperty;
            final String value = this.stringInterpolator.apply(sub);
            if (value.equals(sub)) {
                return null;
            } else {
                final SourceLocatableString out = SourceLocatableString.of(value);
                out.setLocation(location);
                return out;
            }
        }
    }
}
