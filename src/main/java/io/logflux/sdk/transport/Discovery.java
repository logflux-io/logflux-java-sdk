package io.logflux.sdk.transport;

import io.logflux.sdk.Version;
import io.logflux.sdk.payload.Json;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Endpoint discovery for LogFlux services.
 * Supports static regional discovery and authenticated API gateway discovery.
 */
public final class Discovery {

    /** Recognized region prefixes for API keys. */
    private static final String[] REGION_PREFIXES = {"eu-", "us-", "ca-", "au-", "ap-"};
    private static final int MAX_RESPONSE_SIZE = 64 * 1024; // 64 KiB

    /** API endpoint paths. */
    public static final String INGEST_PATH = "/v1/ingest";
    public static final String HEALTH_PATH = "/health";
    public static final String VERSION_PATH = "/version";
    public static final String HANDSHAKE_BASE_PATH = "/v1/handshake";
    public static final String HANDSHAKE_INIT_SUFFIX = "/init";
    public static final String HANDSHAKE_COMPLETE_SUFFIX = "/complete";

    private Discovery() {}

    /**
     * Discovered endpoint information.
     */
    public static final class EndpointInfo {
        public final String baseUrl;
        public final String region;

        public EndpointInfo(String baseUrl, String region) {
            this.baseUrl = baseUrl;
            this.region = region;
        }

        public String getIngestUrl() { return baseUrl + INGEST_PATH; }
        public String getHealthUrl() { return baseUrl + HEALTH_PATH; }
        public String getVersionUrl() { return baseUrl + VERSION_PATH; }
        public String getHandshakeUrl() { return baseUrl + HANDSHAKE_BASE_PATH; }
    }

    /**
     * Extracts the region from an API key with region prefix.
     * Returns null if no recognized prefix found.
     */
    public static String extractRegion(String apiKey) {
        if (apiKey == null) return null;
        for (String prefix : REGION_PREFIXES) {
            if (apiKey.startsWith(prefix)) {
                return prefix.substring(0, prefix.length() - 1);
            }
        }
        return null;
    }

    /**
     * Discovers endpoints for the given API key.
     * Tries static regional discovery first, then authenticated API gateway.
     */
    public static EndpointInfo discover(String apiKey, HttpClient httpClient, Duration timeout)
            throws Exception {
        // Try static discovery if key has region prefix
        String region = extractRegion(apiKey);
        if (region != null) {
            try {
                return tryStaticDiscovery(region, httpClient, timeout);
            } catch (Exception e) {
                // Fall through to authenticated discovery
            }
        }

        // Authenticated discovery
        String[] urls = {
                "https://api.logflux.io/api/discovery",
                "https://eu.api.logflux.io/api/discovery",
                "https://us.api.logflux.io/api/discovery",
        };
        Exception lastError = null;
        for (String url : urls) {
            try {
                return tryAuthenticatedDiscovery(url, apiKey, httpClient, timeout);
            } catch (Exception e) {
                lastError = e;
            }
        }
        throw new RuntimeException("All discovery URLs failed: " +
                (lastError != null ? lastError.getMessage() : "unknown"));
    }

    /**
     * Creates an EndpointInfo for a custom endpoint URL (bypasses discovery).
     */
    public static EndpointInfo customEndpoint(String baseUrl) {
        return new EndpointInfo(baseUrl, "custom");
    }

    private static EndpointInfo tryStaticDiscovery(String region, HttpClient httpClient,
                                                    Duration timeout) throws Exception {
        String url = "https://discover." + region + ".logflux.io";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", Version.USER_AGENT)
                .timeout(timeout)
                .GET()
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new RuntimeException("Static discovery returned " + resp.statusCode());
        }

        String body = truncateBody(resp.body());
        Map<String, Object> parsed = Json.parseObject(body);
        Map<String, Object> endpoints = Json.getObject(parsed, "endpoints");
        String ingestorUrl = Json.getString(endpoints, "ingestor_url");
        if (ingestorUrl == null || ingestorUrl.isEmpty()) {
            throw new RuntimeException("Static discovery missing ingestor_url");
        }

        return new EndpointInfo(ingestorUrl, Json.getString(parsed, "region"));
    }

    private static EndpointInfo tryAuthenticatedDiscovery(String url, String apiKey,
                                                           HttpClient httpClient,
                                                           Duration timeout) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "application/json")
                .header("User-Agent", Version.USER_AGENT)
                .timeout(timeout)
                .GET()
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new RuntimeException("Discovery returned " + resp.statusCode());
        }

        String body = truncateBody(resp.body());
        Map<String, Object> parsed = Json.parseObject(body);

        // Try spec format
        String ingestorUrl = Json.getString(parsed, "ingestor_url");
        if (ingestorUrl != null && !ingestorUrl.isEmpty()) {
            return new EndpointInfo(ingestorUrl,
                    Json.getString(parsed, "data_residency"));
        }

        // Try wrapped format
        Map<String, Object> data = Json.getObject(parsed, "data");
        if (data != null) {
            String baseUrl = Json.getString(data, "base_url");
            if (baseUrl != null && !baseUrl.isEmpty()) {
                return new EndpointInfo(baseUrl, Json.getString(data, "region"));
            }
        }

        throw new RuntimeException("Failed to parse discovery response");
    }

    private static String truncateBody(String body) {
        if (body != null && body.length() > MAX_RESPONSE_SIZE) {
            return body.substring(0, MAX_RESPONSE_SIZE);
        }
        return body;
    }
}
