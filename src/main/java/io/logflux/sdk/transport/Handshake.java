package io.logflux.sdk.transport;

import io.logflux.sdk.Version;
import io.logflux.sdk.crypto.Crypto;
import io.logflux.sdk.payload.Json;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.PublicKey;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;

/**
 * RSA handshake protocol for AES-256 key negotiation.
 *
 * Protocol:
 * 1. POST /v1/handshake/init -> server returns RSA public key
 * 2. Client generates AES-256 key, encrypts with RSA-OAEP-SHA256
 * 3. POST /v1/handshake/complete -> server returns key_uuid
 */
public final class Handshake {

    private static final int MAX_RESPONSE_SIZE = 64 * 1024; // 64 KiB

    private Handshake() {}

    /**
     * Result of a successful handshake.
     */
    public static final class Result {
        public final byte[] aesKey;
        public final String keyUuid;
        public final String serverPublicKeyPem;
        public final String serverKeyFingerprint;
        public final boolean supportsMultipart;
        public final int maxBatchSize;
        public final int maxPayloadSize;
        public final int maxRequestSize;

        public Result(byte[] aesKey, String keyUuid, String serverPublicKeyPem,
                      String serverKeyFingerprint, boolean supportsMultipart,
                      int maxBatchSize, int maxPayloadSize, int maxRequestSize) {
            this.aesKey = aesKey;
            this.keyUuid = keyUuid;
            this.serverPublicKeyPem = serverPublicKeyPem;
            this.serverKeyFingerprint = serverKeyFingerprint;
            this.supportsMultipart = supportsMultipart;
            this.maxBatchSize = maxBatchSize;
            this.maxPayloadSize = maxPayloadSize;
            this.maxRequestSize = maxRequestSize;
        }
    }

    /**
     * Performs the complete handshake protocol.
     */
    public static Result perform(String handshakeBaseUrl, String apiKey,
                                  HttpClient httpClient, Duration timeout) throws Exception {
        // Step 1: Request server's public key
        String initUrl = handshakeBaseUrl + Discovery.HANDSHAKE_INIT_SUFFIX;
        String initBody = "{\"api_key\":" + jsonString(apiKey) + "}";

        HttpRequest initReq = HttpRequest.newBuilder()
                .uri(URI.create(initUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .header("User-Agent", Version.USER_AGENT)
                .timeout(timeout)
                .POST(HttpRequest.BodyPublishers.ofString(initBody))
                .build();

        HttpResponse<String> initResp = httpClient.send(initReq,
                HttpResponse.BodyHandlers.ofString());
        if (initResp.statusCode() != 200) {
            throw new RuntimeException("Handshake init failed: HTTP " + initResp.statusCode() +
                    ": " + truncate(initResp.body(), 200));
        }

        // Parse init response (try top-level then wrapped format)
        Map<String, Object> initParsed = Json.parseObject(initResp.body());
        String publicKeyPem = Json.getString(initParsed, "public_key");
        boolean supportsMultipart = Json.getBoolean(initParsed, "supports_multipart", false);
        Map<String, Object> limits = Json.getObject(initParsed, "limits");

        // Try wrapped format if top-level has no public_key
        if (publicKeyPem == null || publicKeyPem.trim().isEmpty()) {
            Map<String, Object> data = Json.getObject(initParsed, "data");
            if (data != null) {
                publicKeyPem = Json.getString(data, "public_key");
                supportsMultipart = Json.getBoolean(data, "supports_multipart", false);
                limits = Json.getObject(data, "limits");
            }
        }

        if (publicKeyPem == null || !publicKeyPem.contains("-----BEGIN PUBLIC KEY-----")) {
            throw new RuntimeException("Handshake init: invalid or missing public_key");
        }

        int maxBatchSize = limits != null ? Json.getInt(limits, "max_batch_size", 0) : 0;
        int maxPayloadSize = limits != null ? Json.getInt(limits, "max_payload_size", 0) : 0;
        int maxRequestSize = limits != null ? Json.getInt(limits, "max_request_size", 0) : 0;

        // Step 2: Generate AES key
        byte[] aesKey = Crypto.generateAESKey();

        // Step 3: Encrypt AES key with server's RSA public key
        PublicKey rsaKey = Crypto.parseRSAPublicKey(publicKeyPem);
        String fingerprint = Crypto.publicKeyFingerprint(rsaKey);
        String encryptedSecret = Crypto.encryptWithRSA(rsaKey, aesKey);

        // Step 4: Send encrypted key to server
        String completeUrl = handshakeBaseUrl + Discovery.HANDSHAKE_COMPLETE_SUFFIX;
        String completeBody = "{\"api_key\":" + jsonString(apiKey) +
                ",\"encrypted_secret\":" + jsonString(encryptedSecret) + "}";

        HttpRequest completeReq = HttpRequest.newBuilder()
                .uri(URI.create(completeUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .header("User-Agent", Version.USER_AGENT)
                .timeout(timeout)
                .POST(HttpRequest.BodyPublishers.ofString(completeBody))
                .build();

        HttpResponse<String> completeResp = httpClient.send(completeReq,
                HttpResponse.BodyHandlers.ofString());
        if (completeResp.statusCode() != 200) {
            // Zero key on failure
            Arrays.fill(aesKey, (byte) 0);
            throw new RuntimeException("Handshake complete failed: HTTP " +
                    completeResp.statusCode() + ": " + truncate(completeResp.body(), 200));
        }

        // Parse complete response (try top-level then wrapped format)
        Map<String, Object> completeParsed = Json.parseObject(completeResp.body());
        String keyUuid = Json.getString(completeParsed, "key_uuid");
        if (keyUuid == null || keyUuid.trim().isEmpty()) {
            Map<String, Object> data = Json.getObject(completeParsed, "data");
            if (data != null) {
                keyUuid = Json.getString(data, "key_uuid");
            }
        }
        if (keyUuid == null || keyUuid.trim().isEmpty()) {
            Arrays.fill(aesKey, (byte) 0);
            throw new RuntimeException("Handshake complete: no key_uuid in response");
        }

        return new Result(aesKey, keyUuid, publicKeyPem, fingerprint, supportsMultipart,
                maxBatchSize, maxPayloadSize, maxRequestSize);
    }

    private static String jsonString(String s) {
        StringBuilder sb = new StringBuilder();
        Json.writeString(sb, s);
        return sb.toString();
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen) + "...";
    }
}
