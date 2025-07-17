package io.logflux.examples;

import io.logflux.logger.LogFlux;

/**
 * Simple example demonstrating ultra-simple LogFlux usage.
 */
public class SimpleExample {
    public static void main(String[] args) {
        try {
            // Initialize LogFlux (one time setup)
            LogFlux.init("https://<customer-id>.ingest.<region>.logflux.io", "my-app", "lf_your_api_key", "your-secret");
            
            // Use it with proper log levels
            LogFlux.info("User login successful").join();
            LogFlux.warn("Rate limit approaching").join();
            LogFlux.error("Database connection failed").join();
            LogFlux.debug("Processing user data").join();
            LogFlux.fatal("System critical error").join();
            
            // Flush all logs before shutdown
            LogFlux.flush().join();
            
        } finally {
            // Clean up
            LogFlux.close();
        }
    }
}