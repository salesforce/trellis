package com.salesforce.trellis.whitelist.builder;

import com.salesforce.trellis.rules.Coordinates;
import com.salesforce.trellis.rules.DependencyScope;

/**
 * Describes a dependency that needs to be whitelisted.
 *
 * @author pcal
 * @since 0.0.1
 */
public interface WhitelistedDependency {
    /**
     * @return the 'from' side of the dependency that needs to get whitelisted.  May not be null.
     */
    Coordinates getFromModule();

    /**
     * @return the 'to' side of the dependency that needs to get whitelisted.  May not be null.
     */
    Coordinates getToModule();

    /**
     * @return the scope in which the dependency needs to be whitelisted.  May not be null.
     */
    DependencyScope getScope();

    /**
     * @return a human-readable description of why this dependency is not desirable and/or why it has been
     * whitelisted.  May return null
     */
    String getReason();
}
