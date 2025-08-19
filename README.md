# LogFlux Agent Java SDK 

A lightweight Java SDK for communicating with the LogFlux Agent via Unix socket or TCP protocols.


## Requirements

- Java 16 or higher (for Unix Domain Socket support)
- Maven 3.6+

## Installation

### Maven

Add this dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.logflux</groupId>
    <artifactId>logflux-agent-java-sdk</artifactId>
    <version>1.1.0</version>
</dependency>
```

### Gradle

Add this to your `build.gradle`:

```gradle
implementation 'io.logflux:logflux-agent-java-sdk:1.1.0'
```

## Quick Start

### Basic Usage

```java
import io.logflux.agent.LogFluxClient;
import io.logflux.agent.LogEntry;

public class Example {
    public static void main(String[] args) {
        // Create a Unix socket client (recommended)
        LogFluxClient client = new LogFluxClient("/tmp/logflux-agent.sock");
        
        try {
            // Connect to the agent
            client.connect();
            
            // Create and send a log entry
            LogEntry entry = new LogEntry("Hello from Java!")
                .withSource("my-java-app")
                .withLevel(LogEntry.LEVEL_INFO)
                .withLabel("component", "example");
            
            client.sendLogEntry(entry);
            
            System.out.println("Log sent successfully!");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            client.close();
        }
    }
}
```

### TCP Connection

```java
// Create a TCP client
LogFluxClient client = new LogFluxClient("localhost", 9999);
```

### Using Try-with-Resources

```java
try (LogFluxClient client = new LogFluxClient("/tmp/logflux-agent.sock")) {
    client.connect();
    
    LogEntry entry = LogEntry.newApplicationEntry("Application started")
        .withSource("my-service")
        .withLabel("version", "1.0.0");
    
    client.sendLogEntry(entry);
}
```

## Log Levels

The SDK supports standard syslog levels:

```java
LogEntry.LEVEL_EMERGENCY  // 0 - Emergency
LogEntry.LEVEL_ALERT      // 1 - Alert  
LogEntry.LEVEL_CRITICAL   // 2 - Critical
LogEntry.LEVEL_ERROR      // 3 - Error
LogEntry.LEVEL_WARNING    // 4 - Warning
LogEntry.LEVEL_NOTICE     // 5 - Notice
LogEntry.LEVEL_INFO       // 6 - Info
LogEntry.LEVEL_DEBUG      // 7 - Debug
```

## Entry Types

```java
LogEntry.TYPE_LOG    // 1 - Standard log messages
LogEntry.TYPE_METRIC // 2 - Metrics data
LogEntry.TYPE_TRACE  // 3 - Distributed tracing
LogEntry.TYPE_EVENT  // 4 - Application events
LogEntry.TYPE_AUDIT  // 5 - Audit logs
```

## Payload Types

The SDK supports payload type hints for better log processing:

```java
// Specific payload types with convenience methods
LogEntry syslogEntry = LogEntry.newSyslogEntry("kernel: USB disconnect");
LogEntry journalEntry = LogEntry.newSystemdJournalEntry("Started SSH daemon");
LogEntry metricEntry = LogEntry.newMetricEntry("{\"cpu_usage\": 45.2}");
LogEntry containerEntry = LogEntry.newContainerEntry("[nginx] GET /health");

// Manual payload type assignment
LogEntry entry = new LogEntry("Custom log message")
    .withPayloadType(LogEntry.PAYLOAD_TYPE_APPLICATION);

// Automatic JSON detection
LogEntry jsonEntry = LogEntry.newGenericEntry("{\"user\": \"admin\"}"); 
// Automatically detected as PAYLOAD_TYPE_GENERIC_JSON
```

## Advanced Usage

### Custom Labels and Metadata

```java
LogEntry entry = new LogEntry("User login attempt")
    .withSource("auth-service")
    .withLevel(LogEntry.LEVEL_INFO)
    .withLabel("user_id", "12345")
    .withLabel("ip_address", "192.168.1.100")
    .withLabel("success", "true")
    .withPayloadType(LogEntry.PAYLOAD_TYPE_AUDIT);
```

### Error Handling

```java
try (LogFluxClient client = new LogFluxClient("/tmp/logflux-agent.sock")) {
    client.connect();
    
    if (client.isConnected()) {
        LogEntry entry = new LogEntry("Operation completed");
        client.sendLogEntry(entry);
    }
    
} catch (IOException e) {
    System.err.println("Failed to send log: " + e.getMessage());
    // Handle connection issues, retry logic, etc.
}
```

### Metrics Logging

```java
LogEntry metricEntry = LogEntry.newMetricEntry(
    "{\"memory_usage\": 1024, \"cpu_percent\": 45.2, \"disk_io\": 123456}"
)
.withSource("monitoring-agent")
.withLabel("host", "web-server-01")
.withLabel("service", "monitoring");

client.sendLogEntry(metricEntry);
```

## Building from Source

```bash
# Clone the repository
git clone https://github.com/logflux-io/logflux-agent.git
cd logflux-agent/sdk/java

# Build the project
mvn clean compile

# Run tests
mvn test

# Create JAR
mvn package
```

## Best Practices

1. **Use Unix sockets** for local communication (faster and more secure)
2. **Use try-with-resources** to ensure proper cleanup
3. **Handle connection errors** gracefully with appropriate retry logic
4. **Set meaningful labels** for better log filtering and analysis
5. **Use appropriate log levels** to control verbosity
6. **Choose correct payload types** to help LogFlux route logs appropriately

## Thread Safety

The `LogFluxClient` is **not thread-safe**. If you need to send logs from multiple threads, either:

1. Create separate client instances for each thread
2. Synchronize access to a shared client instance
3. Use a connection pool pattern

## License

This SDK is part of the LogFlux Agent project. See the main repository for license information.