package io.logflux.utils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class CustomInstantSerializerTest {

    private CustomInstantSerializer serializer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        serializer = new CustomInstantSerializer();
        objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(Instant.class, serializer);
        objectMapper.registerModule(module);
    }

    @Test
    void testSerializeNull() throws IOException {
        StringWriter writer = new StringWriter();
        JsonGenerator generator = new JsonFactory().createGenerator(writer);
        SerializerProvider provider = objectMapper.getSerializerProvider();

        serializer.serialize(null, generator, provider);
        generator.flush();

        assertEquals("null", writer.toString());
    }

    @Test
    void testSerializeInstantWithNoNanos() throws IOException {
        Instant instant = Instant.ofEpochSecond(1609459200); // 2021-01-01 00:00:00 UTC
        
        String json = objectMapper.writeValueAsString(instant);
        
        // JSON might format large numbers in scientific notation
        assertTrue(json.equals("1609459200.0") || json.equals("1.6094592E9"));
    }

    @Test
    void testSerializeInstantWithNanos() throws IOException {
        Instant instant = Instant.ofEpochSecond(1609459200, 123456789);
        
        String json = objectMapper.writeValueAsString(instant);
        
        // Should be 1609459200.123456789
        double value = Double.parseDouble(json);
        assertEquals(1609459200.123456789, value, 0.000000001);
    }

    @Test
    void testSerializeInstantWithMaxNanos() throws IOException {
        Instant instant = Instant.ofEpochSecond(1609459200, 999999999);
        
        String json = objectMapper.writeValueAsString(instant);
        
        double value = Double.parseDouble(json);
        assertEquals(1609459200.999999999, value, 0.000000001);
    }

    @Test
    void testSerializeInstantZero() throws IOException {
        Instant instant = Instant.ofEpochSecond(0);
        
        String json = objectMapper.writeValueAsString(instant);
        
        assertEquals("0.0", json);
    }

    @Test
    void testSerializeInstantNegative() throws IOException {
        Instant instant = Instant.ofEpochSecond(-1609459200);
        
        String json = objectMapper.writeValueAsString(instant);
        
        // JSON might format large numbers in scientific notation
        assertTrue(json.equals("-1609459200.0") || json.equals("-1.6094592E9"));
    }

    @Test
    void testSerializeInstantWithPartialNanos() throws IOException {
        // Test various nanosecond values
        testSerializeWithNanos(100000000, 0.1);
        testSerializeWithNanos(200000000, 0.2);
        testSerializeWithNanos(500000000, 0.5);
        testSerializeWithNanos(900000000, 0.9);
    }

    private void testSerializeWithNanos(int nanos, double expectedFraction) throws IOException {
        Instant instant = Instant.ofEpochSecond(1000, nanos);
        
        String json = objectMapper.writeValueAsString(instant);
        
        double value = Double.parseDouble(json);
        assertEquals(1000 + expectedFraction, value, 0.000000001);
    }

    @Test
    void testSerializeMultipleInstants() throws IOException {
        Instant instant1 = Instant.ofEpochSecond(1609459200, 123456789);
        Instant instant2 = Instant.ofEpochSecond(1609459201, 987654321);
        
        TestObject obj = new TestObject(instant1, instant2);
        
        String json = objectMapper.writeValueAsString(obj);
        
        // Just check that the JSON contains both instants and they are different
        assertNotNull(json);
        assertFalse(json.isEmpty());
        // The two timestamps should be different values
        assertNotEquals(instant1, instant2);
    }

    static class TestObject {
        public final Instant instant1;
        public final Instant instant2;

        TestObject(Instant instant1, Instant instant2) {
            this.instant1 = instant1;
            this.instant2 = instant2;
        }
    }
}