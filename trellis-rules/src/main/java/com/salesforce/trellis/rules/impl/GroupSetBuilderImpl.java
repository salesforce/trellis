package com.salesforce.trellis.rules.impl;

import com.google.common.collect.ImmutableMap;
import com.salesforce.trellis.rules.builder.GroupBuilder;
import com.salesforce.trellis.rules.builder.GroupSet;
import com.salesforce.trellis.rules.builder.GroupSetBuilder;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author pcal
 * @since 0.0.5
 */
public class GroupSetBuilderImpl implements GroupSetBuilder, Consumer<Pair<String, Matcher>> {

    private final ExpressionResolver resolver;
    private boolean isBuilt = false;
    private final Map<String, Matcher> groupMatchers;

    public GroupSetBuilderImpl() {
        this.groupMatchers = new HashMap<>();
        this.resolver = new ExpressionResolver.WithGroupsExpressionResolver(groupMatchers);
    }

    @Override
    public GroupBuilder group() {
        assertNotBuilt();
        return new GroupBuilderImpl(this.resolver, this.groupMatchers.keySet(), this);
    }

    @Override
    public GroupSet build() {
        assertNotBuilt();
        isBuilt = true;
        return new GroupSetImpl(ImmutableMap.copyOf(this.groupMatchers));
    }

    @Override
    public void accept(final Pair<String, Matcher> pair) {
        assertNotBuilt();
        this.groupMatchers.put(pair.getKey(), pair.getValue());
    }

    private void assertNotBuilt() {
        if (isBuilt) throw new IllegalStateException("has already been built");
    }
}
