/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.rules.builder;

/**
 * Thrown to indicate a failure to build rules caused by invalid build directives.
 *
 * @author pcal
 * @since 0.0.1
 */
public class RuleBuildingException extends Exception {

    public RuleBuildingException(String m) {
        super(m);
    }

    public RuleBuildingException(Throwable throwable) {
        super(throwable);
    }
}
