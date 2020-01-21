package com.salesforce.trellis.config.impl;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.salesforce.trellis.config.FileAdapter;
import org.apache.commons.lang3.builder.CompareToBuilder;

import java.io.IOException;

import static java.util.Objects.requireNonNull;

/**
 * A single string value that knows exactly where it came from.
 *
 * @author pcal
 * @since 0.0.3
 */
class SourceLocatableString implements SourceLocatable, Comparable<SourceLocatableString> {

    static String unwrap(SourceLocatableString slsOrNull) {
        return slsOrNull == null ? null : slsOrNull.toString();
    }

    static SourceLocatableString of(String string) {
        return string == null ? null : new SourceLocatableString(string);
    }

    private final String string;
    private SourceLocation sourceLocation;

    private SourceLocatableString(String string) {
        this.string = requireNonNull(string);
    }

    @Override
    public String toString() {
        if (string == null) throw new IllegalStateException();
        return string;
    }

    @Override
    public void setLocation(SourceLocation sourceLocation) {
        this.sourceLocation = sourceLocation;
    }

    @Override
    public SourceLocation getLocation() {
        return this.sourceLocation;
    }

    @Override
    public int compareTo(final SourceLocatableString that) {
        return new CompareToBuilder().append(this.toString(), that.toString()).toComparison();
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SourceLocatableString)) return false;
        return this.string.equals(((SourceLocatableString) o).string);
    }

    static class SourceLocatableStringSerializer extends JsonSerializer<SourceLocatableString> {

        @Override
        public void serialize(SourceLocatableString value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
            gen.writeString(value.toString());
        }
    }


    static class SourceLocatableStringDeserializer extends JsonDeserializer<SourceLocatableString> {

        private final FileAdapter location;

        SourceLocatableStringDeserializer(final FileAdapter location) {
            this.location = requireNonNull(location);
        }

        @Override
        public SourceLocatableString deserialize(final JsonParser p, final DeserializationContext ctxt)
            throws IOException {
            final SourceLocatableString out = new SourceLocatableString(p.getText());
            out.setLocation(new YamlSourceLocation(this.location, p.getCurrentLocation()));
            return out;
        }
    }
}
