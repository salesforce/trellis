package com.salesforce.trellis.rules.builder;

/**
 * A valid value for a Rule's 'optionality' property.  This can be used to limit the dependencies to which a Rule
 * applies based on whether or not they are optional.
 *
 * @author pcal
 * @since 0.0.4
 */
public enum RuleOptionality {

    /**
     * The rule applies to all dependencies.  This is the default.
     */
    ANY,

    /**
     * The rule applies only to optional dependencies.
     */
    OPTIONAL_ONLY,

    /**
     * The rule applies only to non-optional dependencies.
     */
    NON_OPTIONAL_ONLY;

}
