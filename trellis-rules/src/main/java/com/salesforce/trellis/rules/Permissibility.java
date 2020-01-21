package com.salesforce.trellis.rules;

/**
 * Describes a judgement about whether a given dependency should be allowed.
 *
 * @author pcal
 * @since 0.0.1
 */
public interface Permissibility extends Comparable<Permissibility> {

    /**
     * @return true if the dependency in question should be allowed.
     */
    boolean isPermissible();

    /**
     * @return true if the dependency in question should be allowed but also discouraged.  The framework will log
     * a warning reason when discouraged dependencies are encountered.  This property is meaningful if and only if
     * isPermissible() is false.
     */
    boolean isDiscouraged();

    /**
     * @return an optional human-readable explanation coordinates why the dependency in question is not permissible or
     * is discouraged.  May return null.
     */
    String getReason();
}
