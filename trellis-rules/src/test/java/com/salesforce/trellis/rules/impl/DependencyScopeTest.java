/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.rules.impl;


import com.salesforce.trellis.rules.DependencyScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author pcal
 * @since 0.0.1
 */
public class DependencyScopeTest {

    @Test
    public void testEquality() {
        final DependencyScope a = DependencyScope.parse("compile");
        final DependencyScope b = DependencyScope.parse("compile");
        final DependencyScope c = DependencyScope.parse("test");
        final DependencyScope d = DependencyScope.parse("import");
        assertEquals(a, b);
        assertNotEquals(b, c);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(b.hashCode(), c.hashCode());
        assertEquals("compile", a.toString());
        assertNotEquals(a, new Object());
    }

    @Test
    public void testInvalid() {
        try {
            DependencyScope.parse("%$@#!@");
            fail("did not get expected exception");
        } catch (IllegalArgumentException expecteed) {}
    }
}
