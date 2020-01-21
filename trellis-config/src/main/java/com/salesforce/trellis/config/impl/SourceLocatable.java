/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.config.impl;

import com.salesforce.trellis.config.FileAdapter;

/**
 * Implemented by model objects that can remember which part of which document they came from.
 *
 * @author pcal
 * @since 0.0.3
 */
interface SourceLocatable {

    /**
     *
     */
    void setLocation(SourceLocation loc);

    /**
     *
     */
    SourceLocation getLocation();

    /**
     *
     */
    interface SourceLocation {

        FileAdapter getSourceFile();

        int getLineNumber();

        int getColumnNumber();
    }

}
