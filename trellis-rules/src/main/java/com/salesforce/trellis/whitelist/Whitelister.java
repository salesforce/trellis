package com.salesforce.trellis.whitelist;

import com.salesforce.trellis.rules.Coordinates;
import com.salesforce.trellis.rules.OutboundDependency;

/**
 * Session-scoped object for processing dependency whitelists.
 *
 * @author pcal
 * @since 0.0.1
 */
public interface Whitelister {

    /**
     * @return A whitelister that can be used to evaluate dependencies from the given module.  Returns null if no
     * dependency rules from the given module require whitelisting (in which case whitelist processing for the given
     * module should be skipped).
     */
    PerModuleWhitelister getWhitelister(Coordinates fromModule);

    /**
     * Whitelists dependencies for a given module.  The expectation is that this will be called in a tight loop for
     * all dependencies of a module.
     */
    interface PerModuleWhitelister {

        /**
         * Should be called once for each dependency from the context module to the given module.  Internally, checks
         * will be made to determine if the given dependency is a violation that needs to be whitelisted.
         *
         * @param dependency the dependency that was encountered in the project.
         */
        void notifyDependency(OutboundDependency dependency);

        /**
         * Must be called exactly once after all dependencies have been processed for the context module.
         */
        void notifyDone();
    }
}
