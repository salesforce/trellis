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
public class MockRuleSetBuilder implements RuleSetBuilder {

    private final StringWriter buffer;
    private final PrintWriter pw;

    public MockRuleSetBuilder() {
        this.buffer = new StringWriter();
        this.pw = new PrintWriter(buffer, true);
    }

    @Override
    public String toString() {
        return this.buffer.toString().trim();
    }

    @Override
    public RuleSetBuilder logger(Logger logger) {
        return this;
    }

    @Override
    public RuleSetBuilder groups(GroupSet groups) {
        return this;
    }

    @Override
    public RuleSetBuilder addRules(RuleSet otherRules) {
        return this;
    }

    @Override
    public RuleBuilder rule() {
        pw.println("rule:");
        return new RuleBuilder() {

            @Override
            public RuleBuilder action(RuleAction action) {
                pw.println("  action: " + action);
                return this;
            }

            @Override
            public RuleBuilder from(String expression) {
                pw.println("  from: " + expression);
                return this;
            }

            @Override
            public RuleBuilder exceptFrom(String expression) {
                pw.println("  exceptFrom: " + expression);
                return this;
            }

            @Override
            public RuleBuilder to(String expression) {
                pw.println("  to: " + expression);
                return this;
            }

            @Override
            public RuleBuilder exceptTo(String expression) {
                pw.println("  exceptTo: " + expression);
                return this;
            }

            @Override
            public RuleBuilder scope(DependencyScope scope) {
                pw.println("  scope: " + scope);
                return this;
            }

            @Override
            public RuleBuilder distance(final RuleDistance distance) {
                pw.println("  distance: " + distance);
                return this;
            }

            @Override
            public RuleBuilder optionality(RuleOptionality optionality) {
                pw.println("  optionality: " + optionality);
                return this;
            }

            @Override
            public RuleBuilder reason(String reason) {
                pw.println("  reason: " + reason);
                return this;
            }

            @Override
            public void build() {
                pw.println("  build()");
            }
        };
    }

    @Override
    public RuleSet build() {
        return (m) -> {
            throw new UnsupportedOperationException();
        };
    }
}

