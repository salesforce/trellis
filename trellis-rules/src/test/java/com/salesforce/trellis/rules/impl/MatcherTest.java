/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.rules.impl;

import com.salesforce.trellis.common.OrderingTester;
import com.salesforce.trellis.rules.Coordinates;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author pcal
 * @since 0.0.1
 */
public class MatcherTest {

    @Test
    public void testExpressionMatcher() {
        {
            final WildcardMatcher m = new WildcardMatcher("sfdc.core:*");
            assertTrue(m.matches(Coordinates.parse("sfdc.core:foo")));
            assertFalse(m.matches(Coordinates.parse("sfdc.ui:foo")));
        }
        {
            final WildcardMatcher m = new WildcardMatcher("*:*-api");
            assertTrue(m.matches(Coordinates.parse("sfdc.core:foo-api")));
            assertTrue(m.matches(Coordinates.parse("sfdc.ui:bar-api")));
            assertFalse(m.matches(Coordinates.parse("sfdc.core:foo-impl")));
            assertFalse(m.matches(Coordinates.parse("sfdc.core:foo-api-wut")));
        }
    }

    @Test
    public void testCoordinateMatcher() {
        final SimpleMatcher m = new SimpleMatcher(Coordinates.parse("sfdc.core:foo-api"));
        assertTrue(m.matches(Coordinates.parse("sfdc.core:foo-api")));
        assertFalse(m.matches(Coordinates.parse("sfdc.core:foo")));
        assertFalse(m.matches(Coordinates.parse("sfdc.core:foo-impl")));
    }

    @Test
    public void testOrMatcher() {
        try {
            OrMatcher.get(Collections.emptyList());
            fail("didn't get expected exception on empty list");
        } catch (IllegalArgumentException expected) {}
        {
            final Matcher all = new WildcardMatcher("*:*");
            final Matcher and = OrMatcher.get(all);
            assertTrue(all == and);
            assertTrue(and.matches(Coordinates.parse("myapp:rando")));
        }
        final Matcher or1 = OrMatcher.get(
            new SimpleMatcher(Coordinates.parse("myapp:module1a")),
            new SimpleMatcher(Coordinates.parse("myapp:module1b")),
            new SimpleMatcher(Coordinates.parse("myapp:module1c")));
        assertTrue(or1.matches(Coordinates.parse("myapp:module1a")));
        assertTrue(or1.matches(Coordinates.parse("myapp:module1b")));
        assertTrue(or1.matches(Coordinates.parse("myapp:module1c")));
        assertFalse(or1.matches(Coordinates.parse("myapp:module2a")));
        //
        final Matcher or2 = OrMatcher.get(
            new SimpleMatcher(Coordinates.parse("myapp:module2a")),
            new SimpleMatcher(Coordinates.parse("myapp:module2b")),
            new SimpleMatcher(Coordinates.parse("myapp:module2c")));
        assertTrue(or2.matches(Coordinates.parse("myapp:module2a")));
        assertTrue(or2.matches(Coordinates.parse("myapp:module2b")));
        assertTrue(or2.matches(Coordinates.parse("myapp:module2c")));
        assertFalse(or2.matches(Coordinates.parse("myapp:module1a")));
        //
        // exercise the flattening mechanisms
        final Matcher or3 = OrMatcher.get(or1);
        assertTrue(or3.matches(Coordinates.parse("myapp:module1a")));
        assertTrue(or3.matches(Coordinates.parse("myapp:module1b")));
        assertTrue(or3.matches(Coordinates.parse("myapp:module1c")));
        assertFalse(or3.matches(Coordinates.parse("myapp:module2a")));
        //
        // more exercise the flattening mechanisms
        final Matcher or4 = OrMatcher.get(or3, or2);
        assertTrue(or4.matches(Coordinates.parse("myapp:module1a")));
        assertTrue(or4.matches(Coordinates.parse("myapp:module1b")));
        assertTrue(or4.matches(Coordinates.parse("myapp:module1c")));
        assertTrue(or4.matches(Coordinates.parse("myapp:module2a")));
        assertTrue(or4.matches(Coordinates.parse("myapp:module2b")));
        assertTrue(or4.matches(Coordinates.parse("myapp:module2c")));
        assertFalse(or4.matches(Coordinates.parse("myapp:module9z")));
    }

    @Test
    public void testAndMatcher() {
        try {
            AndMatcher.get(Collections.emptyList());
            fail("didn't get expected exception on empty list");
        } catch (IllegalArgumentException expected) {}
        {
            final Matcher all = new WildcardMatcher("*:*");
            final Matcher and = AndMatcher.get(all);
            assertTrue(all == and);
            assertTrue(and.matches(Coordinates.parse("myapp:module1a")));
        }
        {
            final Matcher and = AndMatcher.get(new WildcardMatcher("*:bar"), new WildcardMatcher("foo:*"));
            assertTrue(and.matches(Coordinates.parse("foo:bar")));
            assertFalse(and.matches(Coordinates.parse("zzz:bar")));
            assertFalse(and.matches(Coordinates.parse("foo:zzz")));
        }
    }

    @Test
    public void testNotMatcher() {
        final Matcher not = NotMatcher.get(new WildcardMatcher("foo:*"));
        assertTrue(not.matches(Coordinates.parse("bar:bar")));
        assertFalse(not.matches(Coordinates.parse("foo:bar")));
    }


    @Test
    public void textMatcherWithExceptions() {
        final Matcher e1 = new SimpleMatcher(Coordinates.parse("myapp:exception1"));
        final Matcher e2 = new SimpleMatcher(Coordinates.parse("myapp:exception2"));
        final Matcher all = new WildcardMatcher("myapp:*");
        {
            // simple case, empty matchers, single exception
            final Matcher m =
                RuleSetBuilderImpl.mergeMatcherWithException(Collections.emptyList(), Arrays.asList(e1));
            assertTrue(m.matches(Coordinates.parse("myapp:rando")));
            assertFalse(m.matches(Coordinates.parse("myapp:exception1")));
        }
        {
            // same thing but with wildcard matcher
            final Matcher m =
                RuleSetBuilderImpl.mergeMatcherWithException(Arrays.asList(all), Arrays.asList(e1));
            assertTrue(m.matches(Coordinates.parse("myapp:rando")));
            assertFalse(m.matches(Coordinates.parse("myapp:exception1")));
        }
        {
            // simple case with two exceptions
            final Matcher m =
                RuleSetBuilderImpl.mergeMatcherWithException(Arrays.asList(all), Arrays.asList(e1, e2));
            assertTrue(m.matches(Coordinates.parse("myapp:rando1")));
            assertTrue(m.matches(Coordinates.parse("myapp:rando2")));
            assertFalse(m.matches(Coordinates.parse("myapp:exception1")));
            assertFalse(m.matches(Coordinates.parse("myapp:exception2")));
        }
    }

    @Test
    public void testMatcherOrdering() {
        final Matcher s1 = SimpleMatcher.get("myapp:module1");
        final Matcher s2 = SimpleMatcher.get("myapp:module2");
        final Matcher n1 = NotMatcher.get(s1);
        final Matcher n2 = NotMatcher.get(s2);
        final Matcher c1 = AndMatcher.get(s1, s2, n1);
        final Matcher c2 = AndMatcher.get(s1, s2);
        final Matcher c3 = OrMatcher.get(s1, s2);
        new OrderingTester().testOrdering(Arrays.asList(c1, c2, n1, n2, c3, s1, s2));
    }
}

