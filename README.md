# LogFlux Java SDK

Official Java SDK for LogFlux Agent - A lightweight, high-performance log collection and forwarding agent.

## Installation

### For Java

```bash
# Installation instructions will be added based on the language
```

## Quick Start

```
// Quick start example will be added
```

## Features

- Lightweight client for communicating with LogFlux Agent local server
- Support for both Unix socket and TCP connections
- Automatic batching of log entries
- Built-in retry logic with exponential backoff
- Thread-safe operations
- Auto-discovery of agent configuration

## Documentation

For full documentation, visit [LogFlux Documentation](https://docs.logflux.io)

## License

This SDK is distributed under the Apache License, Version 2.0. See the LICENSE file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

For issues and questions, please use the GitHub issue tracker.

## Requirements

- Java 11 or higher
- Maven 3.6+ or Gradle 6+

## Installation

### Maven

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.logflux</groupId>
    <artifactId>logflux-sdk</artifactId>
    <version>0.1.0</version>
</dependency>
```

### Gradle

Add the following to your `build.gradle`:

```gradle
implementation 'io.logflux:logflux-sdk:0.1.0'
```

## Usage Example

```java
import io.logflux.agent.LogFluxClient;
import io.logflux.agent.LogEntry;

public class Example {
    public static void main(String[] args) {
        // Create a client for Unix socket connection
        try (LogFluxClient client = new LogFluxClient("/tmp/logflux-agent.sock")) {
            // Connect to the agent
            client.connect();
            
            // Send a simple log message
            client.sendLog("Hello from Java SDK!");
            
            // Send a structured log entry
            LogEntry entry = new LogEntry()
                .withMessage("Application started")
                .withLevel(LogEntry.Level.INFO)
                .withSource("my-app")
                .withLabel("component", "web-server")
                .withLabel("version", "1.0.0");
            
            client.sendLogEntry(entry);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## Batch Client Example

```java
import io.logflux.agent.BatchLogFluxClient;
import io.logflux.agent.LogEntry;

public class BatchExample {
    public static void main(String[] args) {
        // Create a batch client for better performance
        try (BatchLogFluxClient client = new BatchLogFluxClient("/tmp/logflux-agent.sock")) {
            client.setBatchSize(100);
            client.setFlushInterval(5000); // 5 seconds
            
            client.connect();
            
            // Logs are automatically batched
            for (int i = 0; i < 1000; i++) {
                client.sendLog("Log message " + i);
            }
            
            // Force flush any pending logs
            client.flush();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## Features

- Support for both Unix socket and TCP connections
- Automatic reconnection with exponential backoff
- Batch processing for high-throughput scenarios
- Thread-safe operations
- Zero dependencies (except Jackson for JSON)
- Comprehensive logging via SLF4J
