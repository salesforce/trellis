/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.rules.builder;

import com.salesforce.trellis.rules.DependencyScope;
import com.salesforce.trellis.rules.RuleSet;
import org.slf4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * So we can capture events applyModel the parser and compare them against a gold file.
 *
 * @author pcal
 * @since 0.0.1
 */
public class MockGroupSetBuilder implements GroupSetBuilder {

    private final StringWriter buffer;
    private final PrintWriter pw;

    public MockGroupSetBuilder() {
        this.buffer = new StringWriter();
        this.pw = new PrintWriter(buffer, true);
    }

    @Override
    public String toString() {
        return this.buffer.toString().trim();
    }

    @Override
    public GroupBuilder group() {
        pw.println("group:");
        return new GroupBuilder() {

            @Override
            public GroupBuilder name(String name) {
                pw.println("  name: " + name);
                return this;
            }

            @Override
            public GroupBuilder include(String includeExpression) {
                pw.println("  include: " + includeExpression);
                return this;
            }

            @Override
            public GroupBuilder except(String includeExpression) throws RuleBuildingException {
                pw.println("  except: " + includeExpression);
                return this;
            }

            @Override
            public void build() {
                pw.println("  build()");
            }
        };
    }

    @Override
    public GroupSet build() {
        return new GroupSet() {};
    }

}

