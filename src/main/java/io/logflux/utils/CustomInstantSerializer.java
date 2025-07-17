package io.logflux.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.Instant;

/**
 * Custom serializer for Instant that outputs seconds since epoch as a decimal number.
 */
public class CustomInstantSerializer extends JsonSerializer<Instant> {
    @Override
    public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
        } else {
            // Convert to seconds with nanosecond precision
            double seconds = value.getEpochSecond() + (value.getNano() / 1_000_000_000.0);
            gen.writeNumber(seconds);
        }
    }
}