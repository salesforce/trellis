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
