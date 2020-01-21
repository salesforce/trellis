package com.salesforce.trellis.config.impl;

import com.google.common.collect.ImmutableList;
import com.salesforce.trellis.config.impl.YamlModel.GroupModel;
import com.salesforce.trellis.config.impl.YamlModel.RuleModel;
import com.salesforce.trellis.config.impl.YamlModel.WhitelistModel;
import org.junit.jupiter.api.Test;

import static com.salesforce.trellis.config.impl.ConfigTestUtils.string;
import static com.salesforce.trellis.config.impl.ConfigTestUtils.stringList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author pcal
 * @since 0.0.1
 */
public class YamlModelTest {

    @Test
    public void testRules() throws Exception {
        final RuleModel r1;
        {
            r1 = new RuleModel();
            r1.setAction(string("DENY"));
            r1.setScope(string("compile"));
            r1.setFrom(stringList("sfdc.core:*"));
            r1.setTo(stringList("junit:junit"));
        }
        final RuleModel r2;
        {
            r2 = new RuleModel();
            r2.setAction(string("DENY"));
            r2.setScope(string("compile"));
            r2.setFrom(stringList("sfdc.core:*"));
            r2.setTo(stringList("junit:junit"));
        }
        final RuleModel r3;
        {
            r3 = new RuleModel();
            r3.setAction(string("ALLOW"));
            r3.setScope(string("compile"));
            r3.setFrom(stringList("sfdc.core:*"));
            r3.setTo(stringList("sfdc.shared:*"));
            r3.setOptionality(string("NON_OPTIONAL_ONLY"));
        }
        assertEquals(r1, r1);
        assertEquals(r1, r2);
        assertNotEquals(r1, r3);
        assertNotEquals(r2, r3);
        assertEquals(0, r1.compareTo(r1));
        assertEquals(0, r2.compareTo(r2));
        assertEquals(0, r1.compareTo(r2));
        assertEquals(0, r2.compareTo(r1));
        assertTrue(r1.compareTo(r3) > 0);
        assertTrue(r3.compareTo(r1) < 0);
        final YamlModelTransformer t = new YamlModelTransformer(new YamlModel());
        assertEquals(1, t.stripDupsAndSort(ImmutableList.of(r1, r2)).size());
        assertEquals(2, t.stripDupsAndSort(ImmutableList.of(r1, r3)).size());
    }

    @Test
    public void testGroups() throws Exception {
        final GroupModel g1;
        {
            g1 = new GroupModel();
            g1.setName(string("PRODUCTION_CODE"));
            g1.setIncludes(stringList("sfdc.core:*"));
        }
        final GroupModel g2;
        {
            g2 = new GroupModel();
            g2.setName(string("PRODUCTION_CODE"));
            g2.setIncludes(stringList("sfdc.core:*"));
        }
        final GroupModel g3;
        {
            g3 = new GroupModel();
            g3.setName(string("TEST_CODE"));
            g3.setIncludes(stringList("sfdc.core:*"));
        }
        assertEquals(g1, g1);
        assertEquals(g1, g2);
        assertNotEquals(g1, g3);
        assertNotEquals(g2, g3);
        assertEquals(0, g1.compareTo(g1));
        assertEquals(0, g1.compareTo(g2));
        assertEquals(0, g2.compareTo(g1));
        assertTrue(g1.compareTo(g3) < 0);
        assertTrue(g3.compareTo(g1) > 0);
        final YamlModelTransformer t = new YamlModelTransformer(new YamlModel());
        assertEquals(1, t.stripDupsAndSort(ImmutableList.of(g1, g2)).size());
        assertEquals(2, t.stripDupsAndSort(ImmutableList.of(g1, g3)).size());
    }

    @Test
    public void testWhitelist() throws Exception {
        final YamlModel.WhitelistModel w1;
        {
            w1 = new WhitelistModel();
            w1.setFile(string("foo/bar/baz"));
            w1.setAction(string("WARN"));
        }
        final WhitelistModel w2;
        {
            w2 = new WhitelistModel();
            w2.setFile(string("foo/bar/baz"));
            w2.setAction(string("WARN"));
        }
        final WhitelistModel w3;
        {
            w3 = new WhitelistModel();
            w3.setFile(string("xxx/yyy/zzz"));
            w3.setAction(string("WARN"));
        }
        assertEquals(w1, w1);
        assertEquals(w1, w2);
        assertNotEquals(w1, w3);
        assertNotEquals(w2, w3);
        assertEquals(w1.hashCode(), w2.hashCode());
    }
}
