/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.config.impl;

import com.fasterxml.jackson.core.JsonLocation;
import com.salesforce.trellis.config.FileAdapter;
import com.salesforce.trellis.config.impl.SourceLocatable.SourceLocation;

import static java.util.Objects.requireNonNull;

/**
 * @author pcal
 * @since 0.0.3
 */
class YamlSourceLocation implements SourceLocation {

    private final FileAdapter sourceFile;
    private final JsonLocation jloc;

    YamlSourceLocation(FileAdapter sourceFile, JsonLocation jloc) {
        this.sourceFile = requireNonNull(sourceFile);
        this.jloc = requireNonNull(jloc);
    }

    @Override
    public FileAdapter getSourceFile() {
        return this.sourceFile;
    }

    @Override
    public int getLineNumber() {
        return jloc.getLineNr();
    }

    @Override
    public int getColumnNumber() {
        return jloc.getColumnNr();
    }

    @Override
    public String toString() {
        return this.sourceFile + " " + getLineNumber() + ":" + getColumnNumber();
    }
}
