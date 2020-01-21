/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.rules.impl;


import com.salesforce.trellis.rules.Coordinates;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author pcal
 * @since 0.0.1
 */
public class CoordinatesTest {

    @Test
    public void testEquality() {
        final Coordinates a = Coordinates.parse("sfdc.core:platform-encryption");
        final Coordinates b = Coordinates.of("sfdc.core", "platform-encryption");
        final Coordinates c = Coordinates.of("sfdc.core", "platform-encryption");
        assertEquals(a, b);
        assertEquals(b, c);
        assertEquals(c, a);
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals(b.hashCode(), c.hashCode());
        assertEquals("sfdc.core:platform-encryption", b.toString());
        assertEquals("sfdc.core:platform-encryption", b.getCanonicalString());
        assertNotEquals(a, new Object());
    }

    @Test
    public void testComparisons() {
        final Coordinates a = Coordinates.parse("sfdc.core:platform-encryption");
        final Coordinates b = Coordinates.of("sfdc.core", "platform-encryption");
        final Coordinates c = Coordinates.of("sfdc.ui", "ui-platform-encryption");
        assertEquals(0, a.compareTo(b));
        assertTrue(b.compareTo(c) < 0);
    }

    @Test
    public void testInvalid() {
        try {
            Coordinates.parse("sfdc.core");
            fail("did not create expected exception");
        } catch (IllegalArgumentException expected) {
        }
    }
}
