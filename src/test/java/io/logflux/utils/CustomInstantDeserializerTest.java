package io.logflux.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class CustomInstantDeserializerTest {

    private CustomInstantDeserializer deserializer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        deserializer = new CustomInstantDeserializer();
        objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Instant.class, deserializer);
        objectMapper.registerModule(module);
    }

    @Test
    void testDeserializeWholeSeconds() throws IOException {
        String json = "1609459200.0";
        
        Instant instant = objectMapper.readValue(json, Instant.class);
        
        assertEquals(1609459200, instant.getEpochSecond());
        assertEquals(0, instant.getNano());
    }

    @Test
    void testDeserializeWithNanos() throws IOException {
        String json = "1609459200.123456789";
        
        Instant instant = objectMapper.readValue(json, Instant.class);
        
        assertEquals(1609459200, instant.getEpochSecond());
        // Due to floating-point precision, we allow for some tolerance
        assertTrue(Math.abs(instant.getNano() - 123456789) < 100);
    }

    @Test
    void testDeserializeWithMaxNanos() throws IOException {
        String json = "1609459200.999999999";
        
        Instant instant = objectMapper.readValue(json, Instant.class);
        
        // Due to floating-point precision, this might roll over to the next second
        assertTrue(instant.getEpochSecond() >= 1609459200 && instant.getEpochSecond() <= 1609459201);
        if (instant.getEpochSecond() == 1609459200) {
            assertTrue(instant.getNano() > 999999000); // Close to max nanos
        }
    }

    @Test
    void testDeserializeZero() throws IOException {
        String json = "0.0";
        
        Instant instant = objectMapper.readValue(json, Instant.class);
        
        assertEquals(0, instant.getEpochSecond());
        assertEquals(0, instant.getNano());
    }

    @Test
    void testDeserializeNegative() throws IOException {
        String json = "-1609459200.0";
        
        Instant instant = objectMapper.readValue(json, Instant.class);
        
        assertEquals(-1609459200, instant.getEpochSecond());
        assertEquals(0, instant.getNano());
    }

    @Test
    void testDeserializeNegativeWithNanos() throws IOException {
        String json = "-1609459200.5";
        
        Instant instant = objectMapper.readValue(json, Instant.class);
        
        // When deserializing negative values with fractional parts,
        // the behavior might be implementation-specific
        assertEquals(-1609459201, instant.getEpochSecond());
        assertEquals(500000000, instant.getNano());
    }

    @Test
    void testDeserializePartialNanos() throws IOException {
        testDeserializeWithFraction("1000.1", 1000, 100000000);
        testDeserializeWithFraction("1000.2", 1000, 200000000);
        testDeserializeWithFraction("1000.5", 1000, 500000000);
        testDeserializeWithFraction("1000.9", 1000, 900000000);
    }

    private void testDeserializeWithFraction(String json, long expectedSeconds, int expectedNanos) throws IOException {
        Instant instant = objectMapper.readValue(json, Instant.class);
        
        assertEquals(expectedSeconds, instant.getEpochSecond());
        // Allow for floating-point precision issues
        assertTrue(Math.abs(instant.getNano() - expectedNanos) < 1000, 
            "Expected nanos around " + expectedNanos + " but got " + instant.getNano());
    }

    @Test
    void testDeserializeIntegerValue() throws IOException {
        // Should handle integer values without decimal point
        String json = "1609459200";
        
        Instant instant = objectMapper.readValue(json, Instant.class);
        
        assertEquals(1609459200, instant.getEpochSecond());
        assertEquals(0, instant.getNano());
    }

    @Test
    void testDeserializeVeryPreciseValue() throws IOException {
        // Test with very precise nanoseconds
        String json = "1609459200.123456789";
        
        Instant instant = objectMapper.readValue(json, Instant.class);
        
        assertEquals(1609459200, instant.getEpochSecond());
        // Allow for floating-point precision issues
        assertTrue(Math.abs(instant.getNano() - 123456789) < 100);
    }

    @Test
    void testDeserializeRoundingBehavior() throws IOException {
        // Test that excess precision is handled correctly
        String json = "1609459200.1234567891234"; // More than 9 decimal places
        
        Instant instant = objectMapper.readValue(json, Instant.class);
        
        assertEquals(1609459200, instant.getEpochSecond());
        // Should be truncated to 9 decimal places, allowing for floating-point precision
        assertTrue(Math.abs(instant.getNano() - 123456789) < 1000);
    }

    @Test
    void testDeserializeInObject() throws IOException {
        String json = "{\"timestamp\":1609459200.123456789}";
        
        TestObject obj = objectMapper.readValue(json, TestObject.class);
        
        assertNotNull(obj.timestamp);
        assertEquals(1609459200, obj.timestamp.getEpochSecond());
        // Allow for floating-point precision issues
        assertTrue(Math.abs(obj.timestamp.getNano() - 123456789) < 100);
    }

    @Test
    void testRoundTrip() throws IOException {
        // Test serialization and deserialization together
        Instant original = Instant.ofEpochSecond(1609459200, 123456789);
        
        // Also register serializer for round-trip test
        SimpleModule module = new SimpleModule();
        module.addSerializer(Instant.class, new CustomInstantSerializer());
        module.addDeserializer(Instant.class, new CustomInstantDeserializer());
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(module);
        
        String json = mapper.writeValueAsString(original);
        Instant deserialized = mapper.readValue(json, Instant.class);
        
        // Due to floating-point precision, allow for small differences
        assertEquals(original.getEpochSecond(), deserialized.getEpochSecond());
        assertTrue(Math.abs(original.getNano() - deserialized.getNano()) < 1000);
    }

    static class TestObject {
        public Instant timestamp;
    }
}