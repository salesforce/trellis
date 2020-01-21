package com.salesforce.trellis.rules.impl;

import com.google.common.collect.ImmutableList;
import com.salesforce.trellis.common.OrderingTester;
import com.salesforce.trellis.rules.Permissibility;
import com.salesforce.trellis.rules.builder.RuleAction;
import org.junit.jupiter.api.Test;

import static com.salesforce.trellis.rules.builder.RuleAction.ALLOW;
import static com.salesforce.trellis.rules.builder.RuleAction.DENY;
import static com.salesforce.trellis.rules.builder.RuleAction.WARN;

/**
 * @author pcal
 * @since 0.0.1
 */
public class PermissibilityTest {

    // ===================================================================
    // Test methods

    /**
     * Ensure that we impose a total ordering on PermissibilityImpl.
     */
    @Test
    public void testOrdering() throws Exception {
        final ImmutableList.Builder<Permissibility> builder = ImmutableList.builder();
        builder.add(perm(ALLOW, "alpha"));
        builder.add(perm(ALLOW, "beta"));
        builder.add(perm(ALLOW));
        builder.add(perm(WARN, "alpha"));
        builder.add(perm(WARN));
        builder.add(perm(DENY, "alpha"));
        builder.add(perm(DENY, "beta"));
        builder.add(perm(DENY));
        new OrderingTester().testOrdering(builder.build());
    }

    // ===================================================================
    // Private methods

    private Permissibility perm(RuleAction action, String reason) {
        return PermissibilityImpl.create(action, reason);
    }

    private Permissibility perm(RuleAction action) {
        return PermissibilityImpl.create(action, null);
    }

}
