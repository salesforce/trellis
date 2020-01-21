/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.rules.impl;

import com.salesforce.trellis.rules.Coordinates;
import com.salesforce.trellis.rules.DependencyScope;
import com.salesforce.trellis.rules.OutboundDependency;

import static java.util.Objects.requireNonNull;

public class OutboundDependencyImpl implements OutboundDependency {

    private final Coordinates coordinates;
    private final DependencyScope scope;
    private final boolean isDirect;
    private final boolean isOptional;

    public OutboundDependencyImpl(Coordinates coordinates,
                                  DependencyScope scope,
                                  boolean isDirect,
                                  boolean isOptional) {
        this.coordinates = requireNonNull(coordinates);
        this.scope = requireNonNull(scope);
        this.isDirect = isDirect;
        this.isOptional = isOptional;
    }

    @Override
    public boolean isOptional() {
        return isOptional;
    }

    @Override
    public boolean isDirect() {
        return isDirect;
    }

    @Override
    public DependencyScope getScope() {
        return scope;
    }

    @Override
    public Coordinates getTo() {
        return coordinates;
    }
}
