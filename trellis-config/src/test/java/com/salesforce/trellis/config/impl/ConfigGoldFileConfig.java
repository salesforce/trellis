package com.salesforce.trellis.config.impl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.salesforce.trellis.common.GoldFileSerializerConfig;
import com.salesforce.trellis.config.FileAdapter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Serializes RulesImpls instances for gold file comparisons
 *
 * @author pcal
 * @since 0.0.3
 */
public class ConfigGoldFileConfig implements GoldFileSerializerConfig {

    // ===================================================================
    // Factory

    public static GoldFileSerializerConfig get() {
        return new ConfigGoldFileConfig();
    }

    private ConfigGoldFileConfig() {
    }

    // ===================================================================
    // ConfigGoldFileConfig impl

    @Override
    public Module getJacksonModule() {
        final SimpleModule module = new SimpleModule();
        module.setMixInAnnotation(ConfigErrorReporter.ParserEventImpl.class, ParserEventImplMixin.class);
        module.addSerializer(FileAdapter.class, new FileAdapterSerializer());
        module.addSerializer(PathFileAdapter.class, new FileAdapterSerializer());
        return module;
    }

    // ===================================================================
    // Private stuff


    private static abstract class ParserEventImplMixin {

        @JsonSerialize(using = FileAdapterSerializer.class)
        private FileAdapter file;

        @JsonSerialize(using = ThrowableSerializer.class)
        @JsonInclude(Include.NON_NULL)
        private Throwable causeOrNull;

        @JsonInclude(Include.NON_NULL)
        private Integer lineNumberOrNull;

        @JsonInclude(Include.NON_NULL)
        private Integer columnNumberOrNull;
    }


    private static class FileAdapterSerializer extends JsonSerializer<FileAdapter> {
        @Override
        public void serialize(FileAdapter value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            final Path p = Paths.get(value.getLocation());
            gen.writeString(String.valueOf(p.getFileName()));
        }
    }


    private static class ThrowableSerializer extends JsonSerializer<Throwable> {
        @Override
        public void serialize(Throwable value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value.getClass().getSimpleName() + ": " + value.getMessage());
        }
    }
}



