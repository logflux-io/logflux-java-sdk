package io.logflux.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;

/**
 * Custom deserializer for Instant that reads seconds since epoch as a decimal number.
 */
public class CustomInstantDeserializer extends JsonDeserializer<Instant> {
    @Override
    public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        double seconds = p.getDoubleValue();
        long epochSecond = (long) seconds;
        int nano = (int) ((seconds - epochSecond) * 1_000_000_000);
        return Instant.ofEpochSecond(epochSecond, nano);
    }
}