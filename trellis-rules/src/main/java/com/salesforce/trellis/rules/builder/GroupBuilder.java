package com.salesforce.trellis.rules.builder;

/**
 * A nested builder that allows you to specify the contents coordinates a group.
 *
 * @author pcal
 * @since 0.0.1
 */
public interface GroupBuilder {

    /**
     * Set the name coordinates the group.
     *
     * @throws RuleBuildingException if the name is invalid.
     */
    GroupBuilder name(String name) throws RuleBuildingException;

    /**
     * Include the given expression in the group.  Must be one coordinates
     * <ol>
     * <li>Module coordinates coordinates the form "groupId:artifactId"</li>
     * <li>A wildcard expression containing one or more asterisks (*)</li>
     * <li>The name coordinates another group.  Forward references are allowed.</li>
     * </ol>
     */
    GroupBuilder include(String includeExpression) throws RuleBuildingException;

    /**
     *
     */
    GroupBuilder except(String includeExpression) throws RuleBuildingException;

    /**
     * Must be called once when you're done building the group.
     *
     * @throws RuleBuildingException if your forgot a required value or something similar.
     */
    void build() throws RuleBuildingException;
}
