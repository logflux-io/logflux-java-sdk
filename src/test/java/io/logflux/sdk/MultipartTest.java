package io.logflux.sdk;

import io.logflux.sdk.crypto.Crypto;
import io.logflux.sdk.transport.MultipartBuilder;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MultipartTest {

    @Test
    void singlePartMultipart() throws Exception {
        byte[] key = Crypto.generateAESKey();
        Crypto crypto = new Crypto(key);

        List<MultipartBuilder.Part> parts = new ArrayList<>();
        parts.add(new MultipartBuilder.Part("{\"v\":\"2.0\",\"type\":\"log\"}", EntryType.LOG, LogLevel.INFO));

        MultipartBuilder.Result result = MultipartBuilder.build(parts, crypto, "key-uuid-1", true);

        assertNotNull(result.body);
        assertTrue(result.body.length > 0);
        assertTrue(result.contentType.startsWith("multipart/mixed; boundary="));

        // Verify body contains boundary markers
        String bodyStr = new String(result.body, StandardCharsets.US_ASCII);
        String boundary = result.contentType.substring("multipart/mixed; boundary=".length());
        assertTrue(bodyStr.contains("--" + boundary));
        assertTrue(bodyStr.contains("--" + boundary + "--"));

        // Verify headers present
        assertTrue(bodyStr.contains("X-LF-Entry-Type: 1"));
        assertTrue(bodyStr.contains("X-LF-Payload-Type: 1"));
        assertTrue(bodyStr.contains("X-LF-Key-ID: key-uuid-1"));
        assertTrue(bodyStr.contains("X-LF-Nonce:"));
        assertTrue(bodyStr.contains("X-LF-Timestamp:"));

        crypto.close();
    }

    @Test
    void multiPartMultipart() throws Exception {
        byte[] key = Crypto.generateAESKey();
        Crypto crypto = new Crypto(key);

        List<MultipartBuilder.Part> parts = new ArrayList<>();
        parts.add(new MultipartBuilder.Part("{\"type\":\"log\"}", EntryType.LOG, LogLevel.INFO));
        parts.add(new MultipartBuilder.Part("{\"type\":\"metric\"}", EntryType.METRIC, LogLevel.INFO));
        parts.add(new MultipartBuilder.Part("{\"type\":\"event\"}", EntryType.EVENT, LogLevel.INFO));

        MultipartBuilder.Result result = MultipartBuilder.build(parts, crypto, "key-uuid-2", false);

        String bodyStr = new String(result.body, StandardCharsets.US_ASCII);
        String boundary = result.contentType.substring("multipart/mixed; boundary=".length());

        // Count boundary markers (3 parts + 1 closing)
        int count = 0;
        int idx = 0;
        while ((idx = bodyStr.indexOf("--" + boundary, idx)) != -1) {
            count++;
            idx += boundary.length();
        }
        assertEquals(4, count, "Should have 3 part boundaries + 1 closing boundary");

        crypto.close();
    }

    @Test
    void telemetryManagedSkipsEncryption() throws Exception {
        byte[] key = Crypto.generateAESKey();
        Crypto crypto = new Crypto(key);

        List<MultipartBuilder.Part> parts = new ArrayList<>();
        parts.add(new MultipartBuilder.Part("{\"type\":\"telemetry\"}", EntryType.TELEMETRY_MANAGED, LogLevel.INFO));

        MultipartBuilder.Result result = MultipartBuilder.build(parts, crypto, "key-uuid-3", true);

        String bodyStr = new String(result.body, StandardCharsets.US_ASCII);
        // Type 7 should NOT have Key-ID or Nonce headers
        assertFalse(bodyStr.contains("X-LF-Key-ID:"));
        assertFalse(bodyStr.contains("X-LF-Nonce:"));
        // But should still have Entry-Type and Payload-Type
        assertTrue(bodyStr.contains("X-LF-Entry-Type: 7"));
        assertTrue(bodyStr.contains("X-LF-Payload-Type: 3")); // gzip only

        crypto.close();
    }

    @Test
    void encryptedTypesHaveKeyIdAndNonce() throws Exception {
        byte[] key = Crypto.generateAESKey();
        Crypto crypto = new Crypto(key);

        // Test all encrypted types (1-6)
        for (int type = 1; type <= 6; type++) {
            List<MultipartBuilder.Part> parts = new ArrayList<>();
            parts.add(new MultipartBuilder.Part("{\"test\":true}", type, LogLevel.INFO));

            MultipartBuilder.Result result = MultipartBuilder.build(parts, crypto, "uuid", true);
            String bodyStr = new String(result.body, StandardCharsets.US_ASCII);
            assertTrue(bodyStr.contains("X-LF-Key-ID: uuid"),
                    "Type " + type + " should have Key-ID");
            assertTrue(bodyStr.contains("X-LF-Nonce:"),
                    "Type " + type + " should have Nonce");
        }

        crypto.close();
    }

    @Test
    void uniqueBoundaries() throws Exception {
        byte[] key = Crypto.generateAESKey();
        Crypto crypto = new Crypto(key);

        List<MultipartBuilder.Part> parts = new ArrayList<>();
        parts.add(new MultipartBuilder.Part("{}", EntryType.LOG, LogLevel.INFO));

        MultipartBuilder.Result r1 = MultipartBuilder.build(parts, crypto, "uuid", false);
        MultipartBuilder.Result r2 = MultipartBuilder.build(parts, crypto, "uuid", false);

        assertNotEquals(r1.contentType, r2.contentType, "Boundaries should be unique");

        crypto.close();
    }
}
