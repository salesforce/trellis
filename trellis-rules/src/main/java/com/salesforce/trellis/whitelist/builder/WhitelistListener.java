package com.salesforce.trellis.whitelist.builder;

import com.salesforce.trellis.rules.Coordinates;

/**
 * Receives notifications about stuff that needs to get whitelisted.
 *
 * @author pcal
 * @since 0.0.1
 */
public interface WhitelistListener {

    /**
     * Receive notification about a dependency that violates our rules but has been whitelisted.
     */
    void notifyWhitelisted(final WhitelistedDependency dependency);

    /**
     * Notifies the listener that whitelist processing for the given module has been completed.  In practice, this is
     * a signal to the underyling system that it can go ahead and update all whitelist entries for dependencies from
     * the given module.
     * <p>
     * By calling this, the caller guarantees that no more dependencies from the given module will be whitelisted in
     * the current whitelisting session.
     * <p>
     * This must be called exactly once per module.  Note that it must be called for *all* modules, even those for
     * which notifyWhitelisted() was never called.
     *
     * @param fromModule The module we're done processing.
     */
    void notifyModuleProcessed(final Coordinates fromModule);

}
