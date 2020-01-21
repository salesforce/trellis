/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.config.impl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.salesforce.trellis.config.FileAdapter;
import com.salesforce.trellis.config.impl.SourceLocatable.SourceLocation;
import com.salesforce.trellis.config.impl.SourceLocatableString.SourceLocatableStringDeserializer;
import com.salesforce.trellis.config.impl.SourceLocatableString.SourceLocatableStringSerializer;

import java.io.BufferedWriter;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import static java.util.Objects.requireNonNull;

/**
 * Configuration for jackson de/serializers.
 *
 * @author pcal
 * @since 0.0.1
 */
class YamlParser {

    // ===================================================================
    // Fields

    private final ObjectMapper mapper;
    private final FileAdapter sourceFile;

    // ===================================================================
    // Constructors

    YamlParser(final FileAdapter sourceFile) {
        this.sourceFile = requireNonNull(sourceFile);
        this.mapper = initObjectMapper(requireNonNull(sourceFile));
    }

    private ObjectMapper initObjectMapper(final FileAdapter sourceFileOrNull) {
        final YAMLFactory yf = new YAMLFactory();
        yf.configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true);
        yf.configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false);
        final ObjectMapper out = new ObjectMapper(yf);
        final SimpleModule module = new SimpleModule();
        module.addSerializer(SourceLocatableString.class, new SourceLocatableStringSerializer());
        module.addDeserializer(SourceLocatableString.class, new SourceLocatableStringDeserializer(this.sourceFile));
        module.setDeserializerModifier(new SourceLocatingDeserializerModifier());
        out.registerModule(module);
        out.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return out;
    }

    // ===================================================================
    // Package methods

    <T> T readValue(final Reader in, final Class<T> clazz) throws IOException {
        return this.mapper.readValue(in, clazz);
    }

    void writeValue(Writer out, Object o) throws IOException {
        this.mapper.writeValue(new BufferedWriter(new BreakingWriter(out)), o);
    }

    // ===================================================================
    // Inner classes


    /**
     * Moderately ridiculous hack to add extra line breaks before top-level elements in the yaml.  Because I think it
     * looks better.
     */
    private static class BreakingWriter extends FilterWriter {

        BreakingWriter(Writer out) {
            super(out);
        }

        private boolean atLineStart = false;

        public void write(int c) throws IOException {
            if (atLineStart) {
                if (c == '-' || c != ' ') out.write('\n');
            }
            atLineStart = (c == '\n');
            out.write(c);
        }

        public void write(char[] cbuf, int off, int len) throws IOException {
            for (int i = off; i < off + len; i++) write(cbuf[i]);
        }

        public void write(String str, int off, int len) throws IOException {
            for (int i = off; i < off + len; i++) write(str.charAt(i));
        }
    }


    private class SourceLocatingDeserializerModifier extends BeanDeserializerModifier {

        @Override
        public JsonDeserializer<?> modifyDeserializer(final DeserializationConfig config,
                                                      final BeanDescription beanDesc,
                                                      final JsonDeserializer<?> deserializer) {
            return new SourceLocatingDeserializer(deserializer);
        }
    }


    private class SourceLocatingDeserializer extends DelegatingDeserializer {

        public SourceLocatingDeserializer(JsonDeserializer<?> delegate) {
            super(delegate);
        }

        @Override
        protected JsonDeserializer<?> newDelegatingInstance(final JsonDeserializer<?> newDelegatee) {
            return this._delegatee;
        }

        @Override
        public Object deserialize(final JsonParser jp, final DeserializationContext ctxt) throws IOException {
            try {
                final Object out = super.deserialize(jp, ctxt);
                if (out instanceof SourceLocatable) {
                    final JsonLocation jloc = jp.getCurrentLocation();
                    final SourceLocation loc = new YamlSourceLocation(YamlParser.this.sourceFile, jloc);
                    ((SourceLocatable) out).setLocation(loc);
                }
                return out;
            } catch (final JsonMappingException e) {
                throw new JsonMappingException(jp, e.getMessage(), e);
            }
        }

    }
}
