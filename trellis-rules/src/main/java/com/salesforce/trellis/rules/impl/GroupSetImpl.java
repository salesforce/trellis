package com.salesforce.trellis.rules.impl;

import com.google.common.collect.ImmutableMap;
import com.salesforce.trellis.rules.builder.GroupSet;

import static java.util.Objects.requireNonNull;

/**
 *
 * @author pcal
 * @since 0.0.1
 */
class GroupSetImpl implements GroupSet {

    private final ImmutableMap<String, Matcher> groupMatchers;

    GroupSetImpl(final ImmutableMap<String, Matcher> groupMatchers) {
        this.groupMatchers = requireNonNull(groupMatchers);
    }

    ImmutableMap<String, Matcher> getGroupMatchers() {
        return this.groupMatchers;
    }
}
