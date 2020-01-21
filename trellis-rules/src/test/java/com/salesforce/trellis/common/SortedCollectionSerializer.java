package com.salesforce.trellis.common;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author pcal
 * @since 0.0.1
 */
public class SortedCollectionSerializer extends JsonSerializer<Collection> {

    @Override
    public void serialize(Collection value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {
        final List list = new ArrayList<>(value);
        Collections.sort(list);
        gen.writeObject(list);
    }
}

