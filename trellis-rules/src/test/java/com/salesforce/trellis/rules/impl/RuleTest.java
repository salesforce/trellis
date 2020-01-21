package com.salesforce.trellis.rules.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.salesforce.trellis.common.OrderingTester;
import com.salesforce.trellis.rules.Coordinates;
import com.salesforce.trellis.rules.DependencyScope;
import com.salesforce.trellis.rules.OutboundDependency;
import com.salesforce.trellis.rules.Permissibility;
import com.salesforce.trellis.rules.builder.RuleAction;
import com.salesforce.trellis.rules.builder.RuleDistance;
import com.salesforce.trellis.rules.builder.RuleOptionality;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.salesforce.trellis.rules.builder.RuleAction.ALLOW;
import static com.salesforce.trellis.rules.builder.RuleAction.DENY;
import static com.salesforce.trellis.rules.builder.RuleAction.WARN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author pcal
 * @since 0.0.1
 */
public class RuleTest {

    // ===================================================================
    // Fields

    private final DependencyScope compileScope = DependencyScope.parse("compile");

    // ===================================================================
    // Test methods

    /**
     * Ensure that we impose a total ordering on Rule.
     */
    @Test
    public void testOrdering() throws Exception {
        final ImmutableList.Builder<Rule> builder = ImmutableList.builder();
        builder.add(createRule("foo:foo-impl", "foo:foo-api", ALLOW, "alpha good"));
        builder.add(
            createRule(new String[] {"foo:foo-impl", "baz:baz-impl"}, new String[] {"bop:bop-impl", "boo:boo-impl"},
                ALLOW, null));
        builder.add(
            createRule(new String[] {"zzz:foo-impl", "baz:baz-impl"}, new String[] {"bop:bop-impl", "boo:boo-impl"},
                ALLOW, null));
        builder.add(createRule("bar:bar-impl", "baz:baz-impl", WARN, "alpha"));
        builder.add(createRule("bar:bar-impl", "baz:baz-impl", DENY, "omega bad"));
        builder.add(createRule("bar:bar-impl", "foo:foo-impl", DENY));
        builder.add(createRule("foo:foo-impl", "bar:bar-impl", DENY));
        new OrderingTester().testOrdering(builder.build());
    }

    /**
     * Ensure that we properly order rules that differ only by their applicableScope.
     */
    @Test
    public void testScopeOrdering() {
        final ImmutableList.Builder<Rule> builder = ImmutableList.builder();
        builder.add(
            createRule("foo:foo-impl", "bar:bar-impl", DENY, ImmutableSet.of(DependencyScopeFactory.parse("compile"))));
        builder.add(
            createRule("foo:foo-impl", "bar:bar-impl", DENY, ImmutableSet.of(DependencyScopeFactory.parse("provided"))));
        builder.add(
            createRule("foo:foo-impl", "bar:bar-impl", DENY, DependencyScopeFactory.MavenDependencyScope.ANY));
        new OrderingTester().testOrdering(builder.build());
    }

    @Test
    public void testEquality() throws Exception {
        final Rule r1a = createRule("foo:foo-impl", "foo:foo-api", ALLOW, "alpha good");
        final Rule r1b = createRule("foo:foo-impl", "foo:foo-api", ALLOW, "alpha good");
        final Rule r2 = createRule("foo:foo-impl", "bra:bar-api", DENY);
        assertEquals(r1a, r1b);
        assertEquals(r1b, r1a);
        assertNotEquals(r2, r1a);
        assertNotEquals(r1a, r2);
        assertNotEquals(r1a, new Object());
        assertNotEquals(new Object(), r1a);
    }


    @Test
    public void testEvaluation() throws Exception {
        final ImmutableList.Builder<Rule> builder = ImmutableList.builder();
        builder.add(createRule("foo:foo-impl", "baz:baz-impl", DENY, "omega bad"));
        builder.add(createRule("foo:foo-impl", "foo:*", DENY, "NONE SHALL PASS"));
        builder.add(createRule("foo:foo-impl", "bar:bar-impl", DENY));
        builder.add(createRule("foo:foo-impl", "foo:bar-api", ALLOW));
        builder.add(createRule("foo:OOPS", "foo:foo-api", ALLOW, "alpha good")); //shouldnt matter

        final PerModuleRulesImpl rules =
            new PerModuleRulesImpl(builder.build(), LoggerFactory.getLogger(this.getClass()));
        {
            final Permissibility p =
                rules.checkDependency(dep(Coordinates.of("foo", "foo-api"), compileScope, true, false));
            assertNotNull(p);
            assertTrue(p.isPermissible());
            assertFalse(p.isDiscouraged());
            assertEquals("alpha good", p.getReason());
        }
        {
            final Permissibility p =
                rules.checkDependency(dep(Coordinates.of("foo", "other-matches-star-api"), compileScope, true, false));
            assertNotNull(p);
            assertFalse(p.isPermissible());
            assertFalse(p.isDiscouraged());
            assertEquals("NONE SHALL PASS", p.getReason());
        }
    }

    // ===================================================================
    // Private methods

    private Rule createRule(String from, String to, RuleAction action) {
        return createRule(from, to, action, DependencyScopeFactory.MavenDependencyScope.ANY);
    }

    private Rule createRule(String from,
                            String to,
                            RuleAction action,
                            final Set<? extends DependencyScope> applicableScopes) {
        final Rule r =
            new Rule(new WildcardMatcher(from), new WildcardMatcher(to), PermissibilityImpl.create(action, null),
                applicableScopes, RuleDistance.ANY, RuleOptionality.ANY);
        assertTrue(r.toString().contains(action.toString()));
        return r;
    }

    private Rule createRule(String from, String to, RuleAction action, String reason) {
        return new Rule(new WildcardMatcher(from), new WildcardMatcher(to), PermissibilityImpl.create(action, reason),
            DependencyScopeFactory.MavenDependencyScope.ANY, RuleDistance.ANY, RuleOptionality.ANY);
    }

    private Rule createRule(String[] from, String to[], RuleAction action, String reason) {
        return new Rule(orMatcher(from), orMatcher(to), PermissibilityImpl.create(action, reason),
            DependencyScopeFactory.MavenDependencyScope.ANY, RuleDistance.ANY, RuleOptionality.ANY);
    }

    private Matcher orMatcher(String[] expressions) {
        final List<Matcher> matchers = new ArrayList<>();
        for (String e : expressions) {
            matchers.add(new SimpleMatcher(Coordinates.parse(e)));
        }
        return OrMatcher.get(matchers);
    }

    private static OutboundDependency dep(final Coordinates toModule,
                                          final DependencyScope scope,
                                          final boolean isDirect,
                                          final boolean isOptional) {
        return OutboundDependency.create(toModule, scope, isDirect, isOptional);
    }
}
