/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.rules.impl;

import com.salesforce.trellis.rules.DependencyScope;

import java.util.EnumSet;

import static java.util.Objects.requireNonNull;

/**
 * @author pcal
 * @since 0.0.1
 */
public class DependencyScopeFactory {

    public static DependencyScope parse(String mavenScope) throws IllegalArgumentException {
        requireNonNull(mavenScope, "null scope");
        if ("import".equals(mavenScope)) return MavenDependencyScope._import;
        return MavenDependencyScope.valueOf(mavenScope);
    }

    /**
     * All of the known maven dependency scopes.  This is hidden from the public API just in case we have to support
     * weird cases someday.
     * <p>
     * But as a practical matter, these are currently the only valid impls of DependencyScope.
     */
    enum MavenDependencyScope implements DependencyScope {

        compile,
        _import,
        provided,
        runtime,
        system,
        test;

        static final EnumSet<MavenDependencyScope> ANY = EnumSet.allOf(MavenDependencyScope.class);

    }

}
