/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.common;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import static java.util.Objects.requireNonNull;

/**
 * @author pcal
 * @since 0.0.3
 */
public class GoldFileSerializer {

    // ===================================================================
    // Fields

    private final ObjectMapper mapper;

    // ===================================================================
    // Constructor

    public static GoldFileSerializer create(GoldFileSerializerConfig... configs) {
        return new GoldFileSerializer(configs);
    }

    private GoldFileSerializer(GoldFileSerializerConfig[] configs) {
        requireNonNull(configs);
        this.mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
        for (final GoldFileSerializerConfig config : configs) {
            mapper.registerModule(config.getJacksonModule());
        }
    }

    // ===================================================================
    // Public methods

    public String toString(final Object o) throws Exception {
        return this.mapper.writeValueAsString(o);
    }

}
