package com.salesforce.trellis.rules.impl;

import com.salesforce.trellis.common.GoldFileSerializer;
import com.salesforce.trellis.common.GoldFileValidator;
import com.salesforce.trellis.rules.Coordinates;
import com.salesforce.trellis.rules.DependencyScope;
import com.salesforce.trellis.rules.OutboundDependency;
import com.salesforce.trellis.rules.Permissibility;
import com.salesforce.trellis.rules.RuleSet;
import com.salesforce.trellis.rules.builder.GroupBuilder;
import com.salesforce.trellis.rules.builder.GroupSetBuilder;
import com.salesforce.trellis.rules.builder.RuleAction;
import com.salesforce.trellis.rules.builder.RuleDistance;
import com.salesforce.trellis.rules.builder.RuleOptionality;
import com.salesforce.trellis.rules.builder.RuleSetBuilder;
import com.salesforce.trellis.rules.builder.RuleSetBuilder.RuleBuilder;
import com.salesforce.trellis.rules.builder.RuleBuildingException;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

import static com.salesforce.trellis.rules.builder.RuleAction.ALLOW;
import static com.salesforce.trellis.rules.builder.RuleAction.DENY;
import static com.salesforce.trellis.rules.builder.RuleAction.WARN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests to ensure that RuleSetBuilder builds the right RuleSet.
 *
 * @author pcal
 * @since 0.0.1
 */
public class RulesBuilderTest {

    private static final Path RESOURCES_ROOT = Paths.get("src/test/resources/com/salesforce/trellis/rules/impl");

    private final DependencyScope compileScope = DependencyScope.parse("compile");
    private final DependencyScope testScope = DependencyScope.parse("test");
    private final DependencyScope runtimeScope = DependencyScope.parse("runtime");

    @Test
    public void testSimpleRule() throws Exception {
        final RuleSetBuilder b = RuleSetBuilder.create();
        b.logger(LoggerFactory.getLogger(this.getClass()));
        b.rule().action(ALLOW).from("sfdc.core:*-impl").to("sfdc.core:*-api").build();
        b.rule().action(DENY).from("*").to("*").build();
        final RuleSet rules = b.build();
        final RuleSet.PerModuleRules m = rules.getRulesFor(Coordinates.parse("sfdc.core:foo-impl"));
        assertNotNull(m, "null PerModuleRules");
        {
            final Permissibility p =
                m.checkDependency(dep(Coordinates.parse("sfdc.core:foo-api"), compileScope, true, false));
            assertNotNull(p, "null Permissibilty");
            assertTrue(p.isPermissible(), "wrong Permissibilty");
            assertFalse(p.isDiscouraged(), "wrong Discouraged");
            assertNull(p.getReason(), "expected null reason");
        }
        {
            final Permissibility p =
                m.checkDependency(dep(Coordinates.parse("sfdc.ui:ui-bar"), compileScope, true, false));
            assertNotNull(p, "null Permissibility");
            assertFalse(p.isPermissible());
            assertFalse(p.isDiscouraged());
            assertNull(p.getReason(), "expected null reason");
        }
        final Path TEST_RESOURCES = RESOURCES_ROOT.resolve("RulesBuilderTest-testSimpleRule");
        new GoldFileValidator(TEST_RESOURCES.resolve("testSimpleRule.goldfile")).validate(toString(rules));
    }

    @Test
    public void testExceptFrom() throws Exception {
        final RuleSetBuilder b = RuleSetBuilder.create();
        b.rule().action(DENY).exceptFrom("myapp:special-impl").to("myapp:special-api")
            .reason("Only special-impl can depend on special-api").build();
        final RuleSet rules = b.build();
        final Path TEST_RESOURCES = RESOURCES_ROOT.resolve("RulesBuilderTest-testExceptFrom");
        new GoldFileValidator(TEST_RESOURCES.resolve("testExceptFrom.goldfile")).validate(toString(rules));
    }

    @Test
    public void testExceptTo() throws Exception {
        final RuleSetBuilder b = RuleSetBuilder.create();
        b.rule().action(DENY).from("*:*").to("verboten:*").exceptTo("verboten:allowed")
            .reason("No one can depend on anything in verboten except for verboten:allowed").build();
        final RuleSet rules = b.build();
        final Path TEST_RESOURCES = RESOURCES_ROOT.resolve("RulesBuilderTest-testExceptTo");
        new GoldFileValidator(TEST_RESOURCES.resolve("testExceptTo.goldfile")).validate(toString(rules));
    }

    @Test
    public void testComplexGroups() throws Exception {
        final Path TEST_RESOURCES = RESOURCES_ROOT.resolve("RulesBuilderTest-testComplexGroups");
        final GroupSetBuilder g = GroupSetBuilder.create();
        g.group().name("FOO").include("junit:*").include("sfdc.core:*").build();
        g.group().name("BAR").include("FOO").include("sfdc.ui:*").build();
        g.group().name("BAZ").include("FOO").include("BAR").build();
        g.group().name("BOP").include("BAR").include("sfdc.core:bar").build();
        final RuleSetBuilder b = RuleSetBuilder.create().groups(g.build());
        b.rule().action(DENY).from("BOP").to("sfdc.core:bing").reason("BOP").build();
        b.rule().action(DENY).from("BAZ").to("sfdc.core:bing").reason("BAZ").build();
        b.rule().action(ALLOW).from("sfdc.ui:*").to("*").build();
        final RuleSet r = b.build();
        new GoldFileValidator(TEST_RESOURCES.resolve("rules.goldfile")).validate(toString(r));
        {
            final RuleSet.PerModuleRules m = r.getRulesFor(Coordinates.parse("sfdc.core:bar"));
            Permissibility p = m.checkDependency(dep(Coordinates.parse("sfdc.core:bing"), compileScope, true, false));
            assertFalse(p.isPermissible());
            assertEquals("BAZ", p.getReason());
            new GoldFileValidator(TEST_RESOURCES.resolve("moduleRules.goldfile")).validate(toString(m));
        }
    }

    @Test
    public void testGroupWithException() throws Exception {
        final GroupSetBuilder g = GroupSetBuilder.create();
        g.group().name("FOO").include("verboten:*").except("verboten:allowed").build();
        final RuleSetBuilder b = RuleSetBuilder.create().groups(g.build());
        b.rule().action(DENY).from("*:*").to("FOO")
            .reason("No one can depend on anything in verboten except for verboten:allowed").build();
        final RuleSet rules = b.build();
        final Path TEST_RESOURCES = RESOURCES_ROOT.resolve("RulesBuilderTest-testGroupWithException");
        new GoldFileValidator(TEST_RESOURCES.resolve("testGroupWithException.goldfile")).validate(toString(rules));
    }

    @Test
    public void testNoSuchGroup() throws Exception {
        final GroupSetBuilder g = GroupSetBuilder.create();
        try {
            g.group().name("FOO").include("BAR");
            fail("did not create expected exception on missing group reference");
        } catch (RuleBuildingException expected) {
        }
    }

    @Test
    public void testNoApplicableRules() throws Exception {
        {
            final RuleSetBuilder b = RuleSetBuilder.create();
            b.rule().action(DENY).from("sfdc.core:*").to("*").build();
            final RuleSet r = b.build();
            assertNull(r.getRulesFor(Coordinates.parse("sfdc.ui:ui-foo-impl")));
            assertNotNull(r.getRulesFor(Coordinates.parse("sfdc.core:cache-impl")));
        }
        {
            final RuleSetBuilder b = RuleSetBuilder.create();
            b.rule().action(DENY).from("sfdc.core:*").to("*").build();
            //sanity check that deny rule is working
            final RuleSet.PerModuleRules m2 = b.build().getRulesFor(Coordinates.parse("sfdc.core:bar-api"));
            assertNotNull(m2);
            assertFalse(
                m2.checkDependency(dep(Coordinates.parse("sfdc.ui:ui-foo-components"), compileScope, true, false))
                    .isPermissible());
        }
    }

    @Test
    public void testScopedRules() throws Exception {
        final RuleSetBuilder b = RuleSetBuilder.create();
        b.rule().action(ALLOW).from("sfdc.core:*").scope(testScope).to("org.junit.jupiter:*").build();
        b.rule().action(DENY).from("sfdc.core:*").scope(testScope).scope(compileScope).to("allbad:*").build();
        b.rule().action(DENY).from("sfdc.core:*").to("org.junit.jupiter:*").build();
        final RuleSet r = b.build();
        final RuleSet.PerModuleRules m = r.getRulesFor(Coordinates.parse("sfdc.core:foo-impl"));
        assertNotNull(m);
        assertTrue(
            m.checkDependency(dep(Coordinates.parse("org.junit.jupiter:junit-jupiter-api"), testScope, true, false))
                .isPermissible());
        assertFalse(m.checkDependency(OutboundDependency
            .create(Coordinates.parse("org.junit.jupiter:junit-jupiter-api"), compileScope, true, false))
            .isPermissible());
        assertFalse(
            m.checkDependency(dep(Coordinates.parse("allbad:thing"), compileScope, true, false)).isPermissible());
        assertFalse(m.checkDependency(dep(Coordinates.parse("allbad:thing"), testScope, true, false)).isPermissible());
        assertTrue(
            m.checkDependency(dep(Coordinates.parse("allbad:thing"), runtimeScope, true, false)).isPermissible());
    }

    @Test
    public void testDistance() throws Exception {
        final RuleSetBuilder b = RuleSetBuilder.create();
        b.rule().action(DENY).from("foo:*").to("junit:*").distance(RuleDistance.DIRECT_ONLY).build();
        b.rule().action(DENY).from("bar:*").to("junit:*").distance(RuleDistance.TRANSITIVE_ONLY).build();
        b.rule().action(DENY).from("baz:*").to("junit:*").distance(RuleDistance.ANY).build();
        b.rule().action(DENY).from("bop:*").to("junit:*").build();
        final RuleSet r = b.build();
        final Coordinates junit = Coordinates.of("junit", "junit");
        {
            final RuleSet.PerModuleRules m = r.getRulesFor(Coordinates.parse("foo:foo-impl"));
            assertFalse(m.checkDependency(dep(junit, compileScope, true, false)).isPermissible());
            assertTrue(m.checkDependency(dep(junit, compileScope, false, false)).isPermissible());
        }
        {
            final RuleSet.PerModuleRules m = r.getRulesFor(Coordinates.parse("bar:bar-impl"));
            assertTrue(m.checkDependency(dep(junit, compileScope, true, false)).isPermissible());
            assertFalse(m.checkDependency(dep(junit, compileScope, false, false)).isPermissible());
        }
        {
            final RuleSet.PerModuleRules m = r.getRulesFor(Coordinates.parse("baz:baz-impl"));
            assertFalse(m.checkDependency(dep(junit, compileScope, true, false)).isPermissible());
            assertFalse(m.checkDependency(dep(junit, compileScope, false, false)).isPermissible());
        }
        {
            final RuleSet.PerModuleRules m = r.getRulesFor(Coordinates.parse("bop:bop-impl"));
            assertFalse(m.checkDependency(dep(junit, compileScope, true, false)).isPermissible());
            assertFalse(m.checkDependency(dep(junit, compileScope, false, false)).isPermissible());
        }
    }

    @Test
    public void testAddRules() throws Exception {
        final RuleSet r1;
        {
            final RuleSetBuilder b = RuleSetBuilder.create();
            b.rule().action(DENY).from("foo1:*").to("bar1:*").build();
            r1 = b.build();
        }
        final RuleSet r2;
        {
            final RuleSetBuilder b = RuleSetBuilder.create();
            b.rule().action(DENY).from("foo2:*").to("bar2:*").build();
            r2 = b.build();
        }
        final RuleSet r3;
        {
            final RuleSetBuilder b = RuleSetBuilder.create();
            b.rule().action(DENY).from("foo3:*").to("bar3:*").build();
            r3 = b.build();
        }
        final RuleSet composite;
        {
            final RuleSetBuilder cb = RuleSetBuilder.create();
            cb.addRules(r1).addRules(r2).addRules(r3);
            composite = cb.build();
        }
        final Path TEST_RESOURCES = RESOURCES_ROOT.resolve("RulesBuilderTest-testAddRules");
        new GoldFileValidator(TEST_RESOURCES.resolve("testAddRules.goldfile")).validate(toString(composite));
    }

    @Test
    public void testOptionality() throws Exception {
        final RuleSetBuilder b = RuleSetBuilder.create();
        b.rule().action(DENY).from("*:*").to("private:thing").optionality(RuleOptionality.OPTIONAL_ONLY).build();
        b.rule().action(DENY).from("*:*").to("legacy:thing").optionality(RuleOptionality.NON_OPTIONAL_ONLY).build();
        b.rule().action(DENY).from("*:*").to("whatever:thing").optionality(RuleOptionality.ANY).build();
        final RuleSet r = b.build();
        final Coordinates priv = Coordinates.of("private", "thing");
        final Coordinates legacy = Coordinates.of("legacy", "thing");
        final Coordinates whatever = Coordinates.of("whatever", "thing");
        final boolean IS_OPTIONAL = true;
        final boolean IS_NOT_OPTIONAL = false;
        final RuleSet.PerModuleRules m = r.getRulesFor(Coordinates.of("my", "module"));
        // now ensure that the we get a denial when the right optionality is given:
        // check OPTIONAL_ONLY
        assertTrue(m.checkDependency(dep(priv, compileScope, true, IS_NOT_OPTIONAL)).isPermissible());
        assertFalse(m.checkDependency(dep(priv, compileScope, true, IS_OPTIONAL)).isPermissible());
        // check NON_OPTIONAL_ONLY
        assertFalse(m.checkDependency(dep(legacy, compileScope, true, IS_NOT_OPTIONAL)).isPermissible());
        assertTrue(m.checkDependency(dep(legacy, compileScope, true, IS_OPTIONAL)).isPermissible());
        // check WHATEVER
        assertFalse(m.checkDependency(dep(whatever, compileScope, true, IS_NOT_OPTIONAL)).isPermissible());
        assertFalse(m.checkDependency(dep(whatever, compileScope, true, IS_OPTIONAL)).isPermissible());
    }

    @Test
    public void testDiscourageAndReason() throws Exception {
        final DependencyScope scope = DependencyScope.parse("compile");
        final RuleSetBuilder b = RuleSetBuilder.create();
        b.rule().action(WARN).from("sfdc.core:bad-module").to("org.junit.jupiter:*").
            reason("Please stop depending on test code!!!").build();
        b.rule().action(DENY).from("sfdc.core:*").to("org.junit.jupiter:*").
            reason("Production code must not depend on test code").build();
        final RuleSet r = b.build();
        {
            final RuleSet.PerModuleRules m = r.getRulesFor(Coordinates.parse("sfdc.core:bad-module"));
            assertNotNull(m);
            final Permissibility p =
                m.checkDependency(dep(Coordinates.parse("org.junit.jupiter:junit-jupiter-api"), scope, true, false));
            assertNotNull(p);
            assertTrue(p.isPermissible());
            assertTrue(p.isDiscouraged());
            assertEquals("Please stop depending on test code!!!", p.getReason());
        }
        {
            final RuleSet.PerModuleRules m = r.getRulesFor(Coordinates.parse("sfdc.core:good-module"));
            assertNotNull(m);
            final Permissibility p =
                m.checkDependency(dep(Coordinates.parse("org.junit.jupiter:junit-jupiter-api"), scope, true, false));
            assertNotNull(p);
            assertFalse(p.isPermissible());
            assertEquals("Production code must not depend on test code", p.getReason());
        }
    }

    @Test
    public void testBadInputs() throws Exception {
        {
            final RuleSetBuilder b = RuleSetBuilder.create();
            try {
                b.rule().action(DENY).from("asdf1234%%%").to("sfdc.core:foo");
                fail("didn't create expected exception");
            } catch (RuleBuildingException expected) {
            }
        }
        {
            // missing rule action
            final RuleSetBuilder.RuleBuilder b = RuleSetBuilder.create().rule();
            b.from("sfdc.core:foo").to("sfdc.core:bar");
            try {
                b.build();
                fail("didn't create expected exception on missing action");
            } catch (RuleBuildingException expected) {
            }
        }
        {
            // no such group in rule
            final RuleSetBuilder.RuleBuilder b = RuleSetBuilder.create().rule();
            try {
                b.from("sfdc.core:foo").to("NO_SUCH_GROUP");
                fail("didn't create expected exception on bad group");
            } catch (RuleBuildingException expected) {
            }
        }
        {
            // bad group name
            final GroupBuilder g = GroupSetBuilder.create().group();
            g.include("sfdc.core:*");
            try {
                g.name("foo%abc");
                fail("didn't create expected exception on bad action");
            } catch (RuleBuildingException expected) {
            }
        }
        {
            // missing group name
            final GroupBuilder g = GroupSetBuilder.create().group();
            g.include("sfdc.core:*");
            try {
                g.build();
                fail("didn't create expected exception on missing group name");
            } catch (RuleBuildingException expected) {
            }
        }
    }


    /**
     * Ensure we behave correctly in the case where none of the rules apply.
     */
    @Test
    public void testDefaultPermissibiliy() throws RuleBuildingException {
        final RuleSetBuilder b = RuleSetBuilder.create();
        b.rule().action(ALLOW).from("sfdc.core:*-impl").to("sfdc.core:*-api").build();
        final RuleSet rules = b.build();
        final RuleSet.PerModuleRules m = rules.getRulesFor(Coordinates.parse("sfdc.core:some-impl"));
        final Permissibility p =
            m.checkDependency(dep(Coordinates.parse("sfdc.ui:other-module"), compileScope, true, false));
        assertNotNull(p);
        assertTrue(p.isPermissible());
        assertFalse(p.isDiscouraged());
        assertNull(p.getReason());
    }

    /**
     * Ensure we blow up correctly if they try to reuse a builder.
     */
    @Test
    public void testReuseBuilder() throws RuleBuildingException {
        {
            final RuleSetBuilder b = RuleSetBuilder.create();
            b.rule().action(ALLOW).from("sfdc.core:*-impl").to("sfdc.core:*-api").build();
            final RuleSet rules = b.build();
            try {
                b.build();
                fail("should not allow RuleSetBuilder reuse");
            } catch (IllegalStateException expected) {
            }
        }
        {
            final RuleBuilder b = RuleSetBuilder.create().rule().from("*:*").to("*:*").action(DENY);
            b.build();
            try {
                b.build();
                fail("should not allow RuleBuilder reuse");
            } catch (IllegalStateException expected) {
            }
        }
        {
            final GroupSetBuilder b = GroupSetBuilder.create();
            b.group().name("foo").include("foo:bar").build();
            b.build();
            try {
                b.build();
                fail("should not allow GroupSetBuilder reuse");
            } catch (IllegalStateException expected) {
            }
        }
        {
            final GroupBuilder g = GroupSetBuilder.create().group().name("ALL").include("*:*");
            g.build();
            try {
                g.build();
                fail("should not allow GroupBuilder reuse");
            } catch (IllegalStateException expected) {
            }
        }

    }

    /**
     * Ensure we blow up correctly if they add bad inputs
     */
    @Test
    public void testInvalidBuilderUse() throws RuleBuildingException {
        final GroupSetBuilder g = GroupSetBuilder.create();
        g.group().name("I_AM_DUP").include("foo:foo").build();
        try {
            g.group().name("I_AM_DUP").include("bar:bar").build();
            fail("did not get expected exception");
        } catch (RuleBuildingException expected) {
        }
        try {
            g.group().name("@#$@#!");
            fail("did not get expected exception");
        } catch (RuleBuildingException expected) {
        }
        try {
            g.group().include("NO_SUCH_GROUP");
            fail("did not get expected exception");
        } catch (RuleBuildingException expected) {
        }
        try {
            g.group().name("I_AM_EMPTY").build();
            fail("did not get expected exception about empty group");
        } catch (RuleBuildingException expected) {
        }
        final RuleSetBuilder b = RuleSetBuilder.create();
        try {
            b.rule().to("*:*").action(RuleAction.DENY).build();
            fail("did not get expected exception about missing from");
        } catch (RuleBuildingException expected) {
        }
        try {
            b.rule().from("*:*").action(RuleAction.DENY).build();
            fail("did not get expected exception about missing to");
        } catch (RuleBuildingException expected) {
        }
    }

    private String toString(Object o) throws Exception {
        return GoldFileSerializer.create(RulesGoldFileConfig.get()).toString(o);
    }

    private static OutboundDependency dep(final Coordinates toModule,
                                          final DependencyScope scope,
                                          final boolean isDirect,
                                          final boolean isOptional) {
        return OutboundDependency.create(toModule, scope, isDirect, isOptional);
    }
}
