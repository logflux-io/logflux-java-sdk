package io.logflux.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class LogResponseTest {

    private ObjectMapper objectMapper;
    private Instant testTimestamp;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        testTimestamp = Instant.parse("2025-01-01T00:00:00Z");
    }

    @Test
    void testDefaultConstructor() {
        LogResponse response = new LogResponse();
        
        assertNull(response.getSuccess());
        assertNull(response.getStatus());
        assertNull(response.getId());
        assertNull(response.getTimestamp());
        assertNull(response.getMessage());
    }

    @Test
    void testParameterizedConstructor() {
        LogResponse response = new LogResponse("accepted", 123L, testTimestamp, "Log accepted");
        
        assertTrue(response.getSuccess());
        assertEquals("accepted", response.getStatus());
        assertEquals(123L, response.getId());
        assertEquals(testTimestamp, response.getTimestamp());
        assertEquals("Log accepted", response.getMessage());
    }

    @Test
    void testConstructorWithNonAcceptedStatus() {
        LogResponse response = new LogResponse("rejected", 456L, testTimestamp, "Error occurred");
        
        assertFalse(response.getSuccess());
        assertEquals("rejected", response.getStatus());
        assertEquals(456L, response.getId());
        assertEquals(testTimestamp, response.getTimestamp());
        assertEquals("Error occurred", response.getMessage());
    }

    @Test
    void testConstructorWithNullValues() {
        LogResponse response = new LogResponse(null, null, null, null);
        
        assertFalse(response.getSuccess());
        assertNull(response.getStatus());
        assertNull(response.getId());
        assertNull(response.getTimestamp());
        assertNull(response.getMessage());
    }

    @Test
    void testSettersAndGetters() {
        LogResponse response = new LogResponse();
        
        response.setSuccess(true);
        response.setStatus("accepted");
        response.setId(789L);
        response.setTimestamp(testTimestamp);
        response.setMessage("Test message");
        
        assertTrue(response.getSuccess());
        assertEquals("accepted", response.getStatus());
        assertEquals(789L, response.getId());
        assertEquals(testTimestamp, response.getTimestamp());
        assertEquals("Test message", response.getMessage());
    }

    @Test
    void testIsSuccess() {
        LogResponse response1 = new LogResponse();
        response1.setSuccess(true);
        assertTrue(response1.isSuccess());
        
        LogResponse response2 = new LogResponse();
        response2.setSuccess(false);
        assertFalse(response2.isSuccess());
        
        LogResponse response3 = new LogResponse();
        response3.setStatus("accepted");
        assertTrue(response3.isSuccess());
        
        LogResponse response4 = new LogResponse();
        response4.setStatus("rejected");
        assertFalse(response4.isSuccess());
        
        LogResponse response5 = new LogResponse();
        response5.setSuccess(false);
        response5.setStatus("accepted");
        assertTrue(response5.isSuccess());
    }

    @Test
    void testEquals() {
        LogResponse response1 = new LogResponse("accepted", 123L, testTimestamp, "Message");
        LogResponse response2 = new LogResponse("accepted", 123L, testTimestamp, "Message");
        LogResponse response3 = new LogResponse("rejected", 123L, testTimestamp, "Message");
        LogResponse response4 = new LogResponse("accepted", 456L, testTimestamp, "Message");
        LogResponse response5 = new LogResponse("accepted", 123L, testTimestamp.plusSeconds(1), "Message");
        LogResponse response6 = new LogResponse("accepted", 123L, testTimestamp, "Different");

        assertEquals(response1, response2);
        assertNotEquals(response1, response3);
        assertNotEquals(response1, response4);
        assertNotEquals(response1, response5);
        assertNotEquals(response1, response6);
        assertNotEquals(response1, null);
        assertNotEquals(response1, new Object());
        assertEquals(response1, response1);
    }

    @Test
    void testHashCode() {
        LogResponse response1 = new LogResponse("accepted", 123L, testTimestamp, "Message");
        LogResponse response2 = new LogResponse("accepted", 123L, testTimestamp, "Message");
        LogResponse response3 = new LogResponse("rejected", 123L, testTimestamp, "Message");

        assertEquals(response1.hashCode(), response2.hashCode());
        assertNotEquals(response1.hashCode(), response3.hashCode());
    }

    @Test
    void testToString() {
        LogResponse response = new LogResponse("accepted", 123L, testTimestamp, "Test message");
        String toString = response.toString();

        assertTrue(toString.contains("success=true"));
        assertTrue(toString.contains("status='accepted'"));
        assertTrue(toString.contains("id=123"));
        assertTrue(toString.contains(testTimestamp.toString()));
        assertTrue(toString.contains("message='Test message'"));
    }

    @Test
    void testJsonSerialization() throws Exception {
        LogResponse response = new LogResponse("accepted", 123L, testTimestamp, "Success");
        
        String json = objectMapper.writeValueAsString(response);
        
        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("\"status\":\"accepted\""));
        assertTrue(json.contains("\"id\":123"));
        assertTrue(json.contains("\"timestamp\":"));
        assertTrue(json.contains("\"message\":\"Success\""));
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = "{\"success\":true,\"status\":\"accepted\",\"id\":456,\"timestamp\":1735689600.0,\"message\":\"Log received\"}";
        
        LogResponse response = objectMapper.readValue(json, LogResponse.class);
        
        assertTrue(response.getSuccess());
        assertEquals("accepted", response.getStatus());
        assertEquals(456L, response.getId());
        assertEquals(testTimestamp, response.getTimestamp());
        assertEquals("Log received", response.getMessage());
    }

    @Test
    void testJsonDeserializationWithNullFields() throws Exception {
        String json = "{\"success\":false,\"status\":\"error\"}";
        
        LogResponse response = objectMapper.readValue(json, LogResponse.class);
        
        assertFalse(response.getSuccess());
        assertEquals("error", response.getStatus());
        assertNull(response.getId());
        assertNull(response.getTimestamp());
        assertNull(response.getMessage());
    }
}