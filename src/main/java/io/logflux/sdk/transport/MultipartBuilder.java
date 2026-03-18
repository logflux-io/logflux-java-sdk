package io.logflux.sdk.transport;

import io.logflux.sdk.EntryType;
import io.logflux.sdk.crypto.Crypto;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

/**
 * Builds multipart/mixed request bodies for the LogFlux ingestor.
 */
public final class MultipartBuilder {

    private static final SecureRandom RANDOM = new SecureRandom();

    private MultipartBuilder() {}

    /**
     * Represents one MIME part to be sent.
     */
    public static final class Part {
        public final String jsonPayload;
        public final int entryType;
        public final int level;

        public Part(String jsonPayload, int entryType, int level) {
            this.jsonPayload = jsonPayload;
            this.entryType = entryType;
            this.level = level;
        }
    }

    /**
     * Result of building a multipart body.
     */
    public static final class Result {
        public final byte[] body;
        public final String contentType;

        public Result(byte[] body, String contentType) {
            this.body = body;
            this.contentType = contentType;
        }
    }

    /**
     * Builds a multipart/mixed body from a list of parts.
     */
    public static Result build(List<Part> parts, Crypto encryptor, String keyUuid,
                                boolean enableCompression) throws Exception {
        String boundary = generateBoundary();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        for (Part part : parts) {
            int payloadType = EntryType.defaultPayloadType(part.entryType);

            baos.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.US_ASCII));

            // Headers
            baos.write("Content-Type: application/octet-stream\r\n".getBytes(StandardCharsets.US_ASCII));
            baos.write(("X-LF-Entry-Type: " + part.entryType + "\r\n").getBytes(StandardCharsets.US_ASCII));
            baos.write(("X-LF-Payload-Type: " + payloadType + "\r\n").getBytes(StandardCharsets.US_ASCII));

            String ts = Instant.now().atOffset(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_INSTANT);
            baos.write(("X-LF-Timestamp: " + ts + "\r\n").getBytes(StandardCharsets.US_ASCII));

            byte[] partBody;
            if (EntryType.requiresEncryption(part.entryType)) {
                Crypto.RawResult raw = encryptor.encryptRaw(
                        part.jsonPayload.getBytes(StandardCharsets.UTF_8), enableCompression);
                baos.write(("X-LF-Key-ID: " + keyUuid + "\r\n").getBytes(StandardCharsets.US_ASCII));
                baos.write(("X-LF-Nonce: " +
                        Base64.getEncoder().encodeToString(raw.nonce) + "\r\n")
                        .getBytes(StandardCharsets.US_ASCII));
                partBody = raw.ciphertext;
            } else {
                // Type 7 (telemetry managed): compress only
                if (enableCompression) {
                    partBody = Crypto.gzipCompress(
                            part.jsonPayload.getBytes(StandardCharsets.UTF_8));
                } else {
                    partBody = part.jsonPayload.getBytes(StandardCharsets.UTF_8);
                }
            }

            baos.write("\r\n".getBytes(StandardCharsets.US_ASCII));
            baos.write(partBody);
            baos.write("\r\n".getBytes(StandardCharsets.US_ASCII));
        }

        baos.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.US_ASCII));
        return new Result(baos.toByteArray(), "multipart/mixed; boundary=" + boundary);
    }

    private static String generateBoundary() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder("logflux-");
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
