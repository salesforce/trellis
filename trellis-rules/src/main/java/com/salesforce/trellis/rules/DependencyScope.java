/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.rules;

import com.salesforce.trellis.rules.impl.DependencyScopeFactory;

/**
 * Encapsulates a maven dependency scope, e.g. "compile" or "test".
 *
 * @author pcal
 * @since 0.0.1
 */
public interface DependencyScope {

    /**
     * @return a typed wrapper for the given dependency scope value.
     * @throws IllegalArgumentException if the value is a not a valid maven scope.
     */
    static DependencyScope parse(final String value) throws IllegalArgumentException {
        return DependencyScopeFactory.parse(value);
    }
}
