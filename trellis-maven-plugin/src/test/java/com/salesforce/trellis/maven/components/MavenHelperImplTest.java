/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.maven.components;

import org.junit.Test;

import java.util.Properties;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;

/**
 * Unit test of MavenHelperImpl
 *
 * @author pcal
 * @since 0.0.1
 */
public class MavenHelperImplTest {

    /**
     * Sanity test the interpolation mechanism we expose to the config layer.
     */
    @Test
    public void testInterpolation() {
        final Properties p1 = new Properties();
        {
            p1.setProperty("foo", "foo oops 1!");
            p1.setProperty("bar", "bar oops 1!");
            p1.setProperty("baz", "baz");
        }
        final Properties p2 = new Properties();
        {
            p2.setProperty("foo", "foo oops 2!");
            p2.setProperty("bar", "bar");
        }
        final Properties p3 = new Properties();
        {
            p3.setProperty("foo", "foo");
            p3.setProperty("bop", "bop oops 3!");
        }
        MavenHelperImpl helper = new MavenHelperImpl(null, p1, null, p2, p3);
        assertEquals("foo = foo", helper.getInterpolator().apply("foo = ${foo}"));
        assertEquals("bar = bar", helper.getInterpolator().apply("bar = ${bar}"));
        assertEquals("baz = baz", helper.getInterpolator().apply("baz = ${baz}"));
        assertEquals("bop = bop oops 3!", helper.getInterpolator().apply("bop = ${bop}"));

        Properties p4 = new Properties();
        p4.setProperty("bop", "bop");
        Function<String, String> custom = helper.createInterpolator(p4);
        assertEquals("foo = foo", custom.apply("foo = ${foo}"));
        assertEquals("bar = bar", custom.apply("bar = ${bar}"));
        assertEquals("baz = baz", custom.apply("baz = ${baz}"));
        assertEquals("bop = bop", custom.apply("bop = ${bop}"));
    }
}
