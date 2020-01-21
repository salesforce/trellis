/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.config;

/**
 * Optionally implemented by callers who want to report detailed information about problems
 * encountered during the getRules.
 *
 * @author pcal
 * @since 0.0.1
 */
public interface ParserListener {

    /**
     * Implement this.  It's how you find out what happened.
     */
    void notify(ParserEvent event);

    /**
     * Don't implement this.  Tells you what happened.
     */
    interface ParserEvent {

        /**
         * Category coordinates the event.  ERRORs mean that the getRules is going to fail and an exception
         * will be thrown.
         */
        ParserEventType getType();

        /**
         * Human-readable description coordinates the problem.
         */
        String getMessage();

        /**
         * Human-readable information about the file where the error occurred.  This is probably just the absolute
         * path to the file but don't count on it.
         */
        String getFileLocation();

        /**
         * @return line number where the error occurred, or null if we don't know.
         */
        Integer getLineNumber();

        /**
         * @return character offset where the error occurred, or null if we don't know.
         */
        Integer getColumnNumber();

        /**
         * @return the exception that produced the event, or null if there was none.
         */
        Throwable getCause();
    }


    enum ParserEventType {
        WARNING,
        ERROR
    }


}
