package com.salesforce.trellis.whitelist;

import com.salesforce.trellis.rules.Coordinates;
import com.salesforce.trellis.rules.DependencyScope;
import com.salesforce.trellis.rules.OutboundDependency;
import com.salesforce.trellis.rules.builder.GroupSetBuilder;
import com.salesforce.trellis.rules.builder.RuleSetBuilder;
import com.salesforce.trellis.whitelist.builder.WhitelistListener;
import com.salesforce.trellis.whitelist.builder.WhitelistedDependency;
import com.salesforce.trellis.whitelist.builder.WhitelisterBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.salesforce.trellis.rules.builder.RuleAction.DENY;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author pcal
 * @since 0.0.1
 */
public class WhitelisterTest {

    private static final DependencyScope COMPILE_SCOPE = DependencyScope.parse("compile");

    @Test
    public void testBasic() throws Exception {

        final MockWhitelistListener listener = new MockWhitelistListener();
        final Whitelister whitelister;
        final WhitelisterBuilder wb = WhitelisterBuilder.create();
        {
            {
                final GroupSetBuilder g = GroupSetBuilder.create();
                g.group().name("PRODUCTION_MODULES").include("sfdc.core:*").build();
                final RuleSetBuilder wr = RuleSetBuilder.create().groups(g.build());
                wr.rule().action(DENY).from("PRODUCTION_MODULES").scope(DependencyScope.parse("compile")).to("junit:*")
                    .reason("wat?").build();
                wb.add(wr.build(), listener);
            }
            whitelister = wb.build();
        }
        {
            final Whitelister.PerModuleWhitelister mw =
                whitelister.getWhitelister(Coordinates.of("sfdc.core", "platform-encryption"));
            mw.notifyDependency(dep(Coordinates.of("sfdc.core", "sfdc"), COMPILE_SCOPE, true, false));
            mw.notifyDependency(dep(Coordinates.of("sfdc.ui", "oops"), COMPILE_SCOPE, true, false));
            mw.notifyDependency(dep(Coordinates.of("junit", "junit"), COMPILE_SCOPE, true, false));
            mw.notifyDone();
            try {
                mw.notifyDone();
                fail("did not get expected exception");
            } catch (IllegalStateException expected) {}
        }
        assertEquals(0, listener.whitelisted.size());
        assertEquals(1, listener.done.size());
    }

    private static class MockWhitelistListener implements WhitelistListener {
        final Set<WhitelistedDependency> whitelisted = new LinkedHashSet<>();
        final List<WhitelistedDependency> done = new ArrayList<>();

        @Override
        public void notifyWhitelisted(WhitelistedDependency dep) {
            assertNotNull(dep.getFromModule());
            assertNotNull(dep.getToModule());
            assertNotNull(dep.getScope());
            assertNotNull(dep.getReason());
            assertEquals(dep, dep);
            whitelisted.add(dep);
        }

        @Override
        public void notifyModuleProcessed(final Coordinates module) {
            requireNonNull(module);
            final Iterator<WhitelistedDependency> i = whitelisted.iterator();
            while (i.hasNext()) {
                final WhitelistedDependency next = i.next();
                if (next.getFromModule().equals(module)) {
                    i.remove();
                    done.add(next);
                }
            }
        }
    }

    private static OutboundDependency dep(final Coordinates toModule,
                                          final DependencyScope scope,
                                          final boolean isDirect,
                                          final boolean isOptional) {
        return OutboundDependency.create(toModule, scope, isDirect, isOptional);
    }
}
