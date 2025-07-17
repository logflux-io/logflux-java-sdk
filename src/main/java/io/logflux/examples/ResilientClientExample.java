package io.logflux.examples;

import io.logflux.client.ResilientClient;
import io.logflux.config.ResilientClientConfig;
import io.logflux.models.ClientStats;

import java.time.Duration;

/**
 * Example demonstrating the resilient client with custom configuration.
 */
public class ResilientClientExample {
    public static void main(String[] args) {
        // Create resilient client with custom configuration
        ResilientClientConfig config = new ResilientClientConfig.Builder()
                .serverUrl("https://<customer-id>.ingest.<region>.logflux.io")
                .node("production-server")
                .apiKey("lf_your_api_key")
                .secret("your-encryption-secret")
                .queueSize(1000)
                .flushInterval(Duration.ofSeconds(5))
                .workerCount(3)
                .failsafeMode(true)
                .maxRetries(5)
                .initialDelay(Duration.ofMillis(100))
                .maxDelay(Duration.ofSeconds(30))
                .backoffFactor(2.0)
                .jitterEnabled(true)
                .build();

        try (ResilientClient client = new ResilientClient(config)) {
            
            // Send logs - they're queued and sent asynchronously
            client.info("Application started successfully").join();
            client.warn("High memory usage detected").join();
            client.error("Database connection failed - will retry").join();
            
            // Check statistics
            ClientStats stats = client.getStats();
            System.out.printf("Sent: %d, Failed: %d, Queued: %d/%d, Dropped: %d%n",
                    stats.getTotalSent(), stats.getTotalFailed(),
                    stats.getQueueSize(), stats.getQueueCapacity(),
                    stats.getTotalDropped());
            
            // Flush ensures all logs are sent before shutdown
            client.flush(Duration.ofSeconds(10)).join();
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}