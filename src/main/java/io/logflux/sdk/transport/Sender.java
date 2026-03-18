package io.logflux.sdk.transport;

import io.logflux.sdk.Version;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * HTTP sender for multipart/mixed payloads.
 * Handles rate limiting (429) and quota (507) responses.
 */
public final class Sender {

    /** Maximum error response body to read (1 MiB). */
    private static final int MAX_ERROR_RESPONSE = 1024 * 1024;

    private Sender() {}

    /**
     * Result of sending a request.
     */
    public static final class SendResult {
        public final int statusCode;
        public final String errorMessage;
        public final int retryAfterSeconds;
        public final int rateLimitLimit;
        public final int rateLimitRemaining;
        public final long rateLimitReset;

        public SendResult(int statusCode, String errorMessage, int retryAfterSeconds,
                          int rateLimitLimit, int rateLimitRemaining, long rateLimitReset) {
            this.statusCode = statusCode;
            this.errorMessage = errorMessage;
            this.retryAfterSeconds = retryAfterSeconds;
            this.rateLimitLimit = rateLimitLimit;
            this.rateLimitRemaining = rateLimitRemaining;
            this.rateLimitReset = rateLimitReset;
        }

        public boolean isSuccess() {
            return statusCode >= 200 && statusCode < 300;
        }

        public boolean isRateLimited() {
            return statusCode == 429;
        }

        public boolean isQuotaExceeded() {
            return statusCode == 507;
        }

        public boolean isRetryable() {
            return statusCode == 429 || statusCode == 500 || statusCode == 502 ||
                   statusCode == 503 || statusCode == 504;
        }
    }

    /**
     * Sends a multipart/mixed request to the ingest endpoint.
     */
    public static SendResult send(String ingestUrl, String apiKey, byte[] body,
                                   String contentType, HttpClient httpClient,
                                   Duration timeout) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ingestUrl))
                    .header("Content-Type", contentType)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("User-Agent", Version.USER_AGENT)
                    .timeout(timeout)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpResponse<String> resp = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            // Parse rate limit headers
            int rlLimit = parseIntHeader(resp, "X-RateLimit-Limit", 0);
            int rlRemaining = parseIntHeader(resp, "X-RateLimit-Remaining", 0);
            long rlReset = parseLongHeader(resp, "X-RateLimit-Reset", 0);
            int retryAfter = parseIntHeader(resp, "Retry-After", 0);

            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                return new SendResult(resp.statusCode(), null, 0,
                        rlLimit, rlRemaining, rlReset);
            }

            String errorBody = resp.body();
            if (errorBody != null && errorBody.length() > MAX_ERROR_RESPONSE) {
                errorBody = errorBody.substring(0, MAX_ERROR_RESPONSE);
            }

            if (resp.statusCode() == 429 && retryAfter == 0) {
                retryAfter = 60; // default for bare 429
            }

            return new SendResult(resp.statusCode(), errorBody, retryAfter,
                    rlLimit, rlRemaining, rlReset);

        } catch (Exception e) {
            // Network error — retryable
            return new SendResult(-1, e.getMessage(), 0, 0, 0, 0);
        }
    }

    private static int parseIntHeader(HttpResponse<?> resp, String name, int defaultValue) {
        return resp.headers().firstValue(name)
                .map(v -> {
                    try { return Integer.parseInt(v); }
                    catch (NumberFormatException e) { return defaultValue; }
                })
                .orElse(defaultValue);
    }

    private static long parseLongHeader(HttpResponse<?> resp, String name, long defaultValue) {
        return resp.headers().firstValue(name)
                .map(v -> {
                    try { return Long.parseLong(v); }
                    catch (NumberFormatException e) { return defaultValue; }
                })
                .orElse(defaultValue);
    }
}
