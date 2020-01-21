package com.salesforce.trellis.config.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableList;
import com.salesforce.trellis.common.CollectionComparator;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static java.util.Objects.requireNonNull;

/**
 * Model that the yaml config get parsed into.  Eventually we'll probably remove this in favor coordinates a
 * handcrafted parser that gives us better line number reporting.  But this makes for faster prototyping for now.
 *
 * @author pcal
 * @since 0.0.1
 */
class YamlModel {

    // ===================================================================
    // Constants

    private static final CollectionComparator COLLECTION_COMPARATOR = new CollectionComparator();

    // ===================================================================
    // Fields

    private Properties properties;
    private List<WhitelistModel> whitelists;
    private List<GroupModel> groups;
    private List<RuleModel> rules;

    // ===================================================================
    // Properties

    public Properties getProperties() {
        return this.properties;
    }

    public void setProperties(final Properties properties) {
        this.properties = properties;
    }

    public List<WhitelistModel> getWhitelists() {
        return whitelists;
    }

    public void setWhitelists(List<WhitelistModel> whitelists) {
        this.whitelists = whitelists == null ? null : ImmutableList.copyOf(whitelists);
    }

    public List<GroupModel> getGroups() {
        return groups;
    }

    public void setGroups(List<GroupModel> groups) {
        this.groups = groups == null ? null : ImmutableList.copyOf(groups);
    }

    public List<RuleModel> getRules() {
        return rules;
    }

    public void setRules(List<RuleModel> rules) {
        this.rules = rules == null ? null : ImmutableList.copyOf(rules);
    }

    // ===================================================================
    // Inner classes


    static abstract class ModelBase implements SourceLocatable {

        @JsonIgnore
        private transient SourceLocation sourceLocation;

        @JsonIgnore
        @Override
        public final SourceLocation getLocation() {
            return sourceLocation;
        }

        @JsonIgnore
        @Override
        public final void setLocation(SourceLocation sourceLocation) {
            this.sourceLocation = requireNonNull(sourceLocation);
        }
    }


    static class WhitelistModel extends ModelBase implements SourceLocatable {

        private SourceLocatableString file;
        private SourceLocatableString action;
        private SourceLocatableString comment;

        /**
         * @return a path to the whitelist file.  The path is relative to the rules file.  The file will be created if
         * it doesn't exist.  Maven properties in this value will be interpolated.
         */
        public SourceLocatableString getFile() {
            return file;
        }

        public void setFile(SourceLocatableString file) {
            this.file = file;
        }

        /**
         * @return a comment that should appear at the top of the generated whitelist.  Optional.
         */
        public SourceLocatableString getHeaderComment() {
            return comment;
        }

        public void setHeaderComment(SourceLocatableString comment) {
            this.comment = comment;
        }

        /**
         * @return the action that should be performed (ALLOW, DENY or WARN).  Optional.
         */
        public SourceLocatableString getAction() {
            return action;
        }

        public void setAction(SourceLocatableString action) {
            this.action = action;
        }

        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof WhitelistModel)) return false;
            final WhitelistModel that = (WhitelistModel) o;
            return new EqualsBuilder().append(this.file, that.file).append(this.comment, that.comment)
                .append(this.action, that.action).isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder().append(this.file).append(this.comment).append(this.action).toHashCode();
        }

    }


    static class GroupModel extends ModelBase implements Comparable<GroupModel> {

        private SourceLocatableString name;
        private List<SourceLocatableString> includes;
        private List<SourceLocatableString> except;
        private List<SourceLocatableString> pomDependencies;

        public SourceLocatableString getName() {
            return name;
        }

        public void setName(SourceLocatableString name) {
            this.name = name;
        }

        public List<SourceLocatableString> getIncludes() {
            return includes;
        }

        public void setIncludes(final List<SourceLocatableString> includes) {
            this.includes = includes == null ? null : ImmutableList.copyOf(includes);
        }

        public List<SourceLocatableString> getExcept() {
            return except;
        }

        public void setExcept(final List<SourceLocatableString> except) {
            this.except = except == null ? null : ImmutableList.copyOf(except);
        }

        public List<SourceLocatableString> getPomDependencies() {
            return pomDependencies;
        }

        public void setPomDependencies(final List<SourceLocatableString> pomDependencies) {
            this.pomDependencies = pomDependencies == null ? null : ImmutableList.copyOf(pomDependencies);
        }

        @Override
        public int compareTo(final GroupModel that) {
            return new CompareToBuilder().append(this.name, that.name)
                .append(this.includes, that.includes, COLLECTION_COMPARATOR).toComparison();
        }

        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof GroupModel)) return false;
            final GroupModel that = (GroupModel) o;
            return new EqualsBuilder().append(this.name, that.name).append(this.includes, that.includes)
                .append(this.except, that.except).append(this.pomDependencies, that.pomDependencies).isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder().append(this.name).append(this.includes).append(this.except)
                .append(this.pomDependencies).toHashCode();
        }
    }


    static class RuleModel extends ModelBase implements Comparable<RuleModel> {

        private SourceLocatableString action;
        private List<SourceLocatableString> from;
        private List<SourceLocatableString> exceptFrom;
        private List<SourceLocatableString> to;
        private List<SourceLocatableString> exceptTo;
        private SourceLocatableString reason;
        private SourceLocatableString distance;
        private SourceLocatableString scope;
        private SourceLocatableString optionality;
        private SourceLocatableString whitelist;

        RuleModel() {
        }

        RuleModel(String action, String from, String to, String scope) {
            this.action = SourceLocatableString.of(action);
            this.from = Collections.singletonList(SourceLocatableString.of(requireNonNull(from)));
            this.to = Collections.singletonList(SourceLocatableString.of(to));
            this.scope = SourceLocatableString.of(scope);
        }

        public SourceLocatableString getAction() {
            return action;
        }

        public void setAction(SourceLocatableString action) {
            this.action = action;
        }

        public SourceLocatableString getReason() {
            return reason;
        }

        public void setReason(SourceLocatableString reason) {
            this.reason = reason;
        }

        public List<SourceLocatableString> getFrom() {
            return from;
        }

        public void setFrom(List<SourceLocatableString> from) {
            this.from = from == null ? null : ImmutableList.copyOf(from);
        }

        public List<SourceLocatableString> getExceptFrom() {
            return exceptFrom;
        }

        public void setExceptFrom(List<SourceLocatableString> exceptFrom) {
            this.exceptFrom = exceptFrom == null ? null : ImmutableList.copyOf(exceptFrom);
        }

        public List<SourceLocatableString> getTo() {
            return to;
        }

        public void setTo(List<SourceLocatableString> to) {
            this.to = to == null ? null : ImmutableList.copyOf(to);
        }

        public List<SourceLocatableString> getExceptTo() {
            return exceptTo;
        }

        public void setExceptTo(List<SourceLocatableString> exceptTo) {
            this.exceptTo = exceptTo == null ? null : ImmutableList.copyOf(exceptTo);
        }

        public SourceLocatableString getScope() {
            return scope;
        }

        public void setScope(SourceLocatableString scope) {
            this.scope = scope;
        }

        public SourceLocatableString getOptionality() {
            return optionality;
        }

        public void setOptionality(SourceLocatableString optionality) {
            this.optionality = optionality;
        }

        public SourceLocatableString getDistance() {
            return distance;
        }

        public void setDistance(SourceLocatableString distance) {
            this.distance = distance;
        }

        public SourceLocatableString getWhitelist() {
            return this.whitelist;
        }

        public void setWhitelist(SourceLocatableString whitelist) {
            this.whitelist = whitelist;
        }

        @Override
        public int compareTo(RuleModel that) {
            return new CompareToBuilder().append(this.action, that.action)
                .append(this.from, that.from, COLLECTION_COMPARATOR)
                .append(this.exceptFrom, that.exceptFrom, COLLECTION_COMPARATOR)
                .append(this.to, that.to, COLLECTION_COMPARATOR)
                .append(this.exceptTo, that.exceptTo, COLLECTION_COMPARATOR).append(this.distance, that.distance)
                .append(this.optionality, that.optionality).append(this.reason, that.reason).toComparison();
        }

        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof RuleModel)) return false;
            final RuleModel that = (RuleModel) o;
            return new EqualsBuilder().append(this.action, that.action).append(this.from, that.from)
                .append(this.exceptFrom, that.exceptFrom).append(this.to, that.to).append(this.exceptTo, that.exceptTo)
                .append(this.distance, that.distance).append(this.optionality, that.optionality)
                .append(this.reason, that.reason).append(this.whitelist, that.whitelist).isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder().append(this.action).append(this.from).append(this.exceptFrom).append(this.to)
                .append(this.exceptTo).append(this.distance).append(this.optionality).append(this.reason)
                .append(this.whitelist).toHashCode();
        }
    }
}
