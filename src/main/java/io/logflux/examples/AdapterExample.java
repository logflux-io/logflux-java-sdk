package io.logflux.examples;

import io.logflux.adapters.JulAdapter;
import io.logflux.adapters.Slf4jAdapter;
import io.logflux.client.ResilientClient;

import java.util.logging.Logger;

/**
 * Example demonstrating logger adapters.
 */
public class AdapterExample {
    public static void main(String[] args) {
        try {
            // Create LogFlux client
            ResilientClient client = ResilientClient.createFromEnv("my-app", "your-secret");
            
            // Example 1: Java Util Logging (JUL) adapter
            Logger julLogger = JulAdapter.createLogger(client, "jul-example");
            julLogger.info("JUL info message");
            julLogger.warning("JUL warning message");
            julLogger.severe("JUL severe message");
            
            // Example 2: SLF4J adapter
            Slf4jAdapter slf4jLogger = new Slf4jAdapter(client, "slf4j-example");
            slf4jLogger.info("SLF4J info message");
            slf4jLogger.warn("SLF4J warning message");
            slf4jLogger.error("SLF4J error message");
            
            // Example 3: Using SLF4J with parameters
            slf4jLogger.info("User {} logged in from IP {}", "john123", "192.168.1.1");
            slf4jLogger.warn("Rate limit reached: {} requests in {} seconds", 1000, 60);
            
            // Flush logs before shutdown
            client.flush(java.time.Duration.ofSeconds(10)).join();
            client.close();
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}