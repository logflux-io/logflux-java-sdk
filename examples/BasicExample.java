import io.logflux.agent.LogEntry;
import io.logflux.agent.LogFluxClient;

/**
 * Basic usage example for LogFlux Java SDK
 */
public class BasicExample {
    public static void main(String[] args) {
        System.out.println("LogFlux Java SDK - Basic Example");
        System.out.println("================================");
        
        // This is a demonstration - would normally connect to actual agent
        try {
            // Create log entries to demonstrate API
            LogEntry basicEntry = new LogEntry("Hello from Java SDK!");
            
            LogEntry detailedEntry = new LogEntry("User login attempt")
                .withSource("java-example")
                .withLevel(LogEntry.LEVEL_INFO)
                .withLabel("user_id", "12345")
                .withLabel("ip_address", "192.168.1.100");
            
            LogEntry jsonEntry = LogEntry.newGenericEntry("{\"event\": \"user_login\", \"success\": true}");
            LogEntry metricEntry = LogEntry.newMetricEntry("{\"cpu_usage\": 45.2, \"memory\": 1024}");
            
            // Display the entries (since we can't connect without an agent)
            System.out.println("Created log entries:");
            System.out.println("1. Basic: " + basicEntry.getMessage());
            System.out.println("2. Detailed: " + detailedEntry.getMessage() + " (labels: " + detailedEntry.getLabels().size() + ")");
            System.out.println("3. JSON: " + jsonEntry.getMessage() + " (type: " + jsonEntry.getLabels().get("payload_type") + ")");
            System.out.println("4. Metric: " + metricEntry.getMessage() + " (type: " + metricEntry.getLabels().get("payload_type") + ")");
            
            System.out.println("\n✅ Java SDK basic example completed successfully!");
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}