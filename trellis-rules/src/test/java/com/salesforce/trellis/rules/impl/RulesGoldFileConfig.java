package com.salesforce.trellis.rules.impl;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.salesforce.trellis.common.GoldFileSerializerConfig;
import com.salesforce.trellis.common.SortedCollectionSerializer;
import com.salesforce.trellis.rules.DependencyScope;
import com.salesforce.trellis.rules.builder.RuleAction;

import java.util.Collection;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Serializes RulesImpls instances for gold file comparisons
 *
 * @author pcal
 * @since 0.0.3
 */
public class RulesGoldFileConfig implements GoldFileSerializerConfig {

    // ===================================================================
    // Factory

    public static RulesGoldFileConfig get() {
        return new RulesGoldFileConfig();
    }

    private RulesGoldFileConfig() {
    }

    // ===================================================================
    // SerializationConfig impl

    @Override
    public Module getJacksonModule() {
        final SimpleModule module = new SimpleModule();
        module.setMixInAnnotation(Rule.class, RuleMixin.class);
        module.setMixInAnnotation(RuleSetImpl.class, RulesImplMixin.class);
        module.setMixInAnnotation(Matcher.class, MatcherMixin.class);
        module.setMixInAnnotation(PerModuleRulesImpl.class, PerModuleRulesImplSerializerMixin.class);
        module.setMixInAnnotation(PermissibilityImpl.class, PermissibilityMixin.class);
        return module;
    }

    // ===================================================================
    // Private stuff

    private static abstract class PerModuleRulesImplSerializerMixin {
        @JsonIgnore
        private Logger logger;
    }


    private static abstract class RuleMixin {
        @JsonSerialize(using = SortedCollectionSerializer.class, as = Collection.class)
        private Set<? extends DependencyScope> applicableScopes;
    }


    private static abstract class RulesImplMixin {
        @JsonIgnore
        private Logger logger;
    }


    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
    private static abstract class MatcherMixin {}


    private static abstract class PermissibilityMixin {

        @JsonIgnore
        RuleAction action;

        @JsonIgnore
        String reason;

        @JsonSerialize
        abstract boolean isPermissible();

        @JsonSerialize
        abstract boolean isDiscouraged();

        @JsonSerialize
        @JsonInclude(Include.NON_NULL)
        abstract String getReason();
    }

}

