package com.salesforce.trellis.whitelist.impl;


import com.salesforce.trellis.rules.Coordinates;
import com.salesforce.trellis.rules.DependencyScope;
import com.salesforce.trellis.whitelist.builder.WhitelistedDependency;

/**
 * This just allows us to get the package-private impl class for testing purposes.
 *
 * @author pcal
 * @since 0.0.1
 */
public class MockWhitelistedDependencyFactory {

    public static WhitelistedDependency create(final Coordinates fromModule,
                                 final Coordinates toModule,
                                 final DependencyScope scope,
                                 final String reasonOrNull) {
        return new WhitelistedDependencyImpl(fromModule, toModule, scope, reasonOrNull);
    }

}
