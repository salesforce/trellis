/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.config.impl;

import com.salesforce.trellis.config.FileAdapter;
import com.salesforce.trellis.rules.builder.RuleAction;
import com.salesforce.trellis.whitelist.builder.WhitelistListener;

/**
 * @author pcal
 * @since 0.0.1
 */
interface AutoWhitelisterFactory {

    /**
     * Creates an WhitelistListener.  This is useful mainly as an override point for unit tests that need to alter the
     * way whitelist files get generated.
     *
     * @param whitelistFile the file that contains the whitelist
     * @param action the rule that should be used in the generated file
     * @param headerTextOrNull header comment for the generated file
     */
    WhitelistListener create(FileAdapter whitelistFile, RuleAction action, String headerTextOrNull);
}
