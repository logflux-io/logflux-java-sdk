# LogFlux Java SDK

A comprehensive Java SDK for sending encrypted logs to LogFlux.io backend servers with enterprise-grade reliability and security.

## Features

- **🔒 Military-Grade Encryption**: All log messages encrypted using AES-256-GCM with PBKDF2-HMAC-SHA256 key derivation (600k iterations)
- **📊 Syslog-Compatible Levels**: Full syslog severity levels (1-8): Emergency, Alert, Critical, Error, Warning, Notice, Info, Debug
- **🔑 API Key Authentication**: Secure authentication with the LogFlux backend using API keys
- **⚡ Ultra-Simple API**: Get started with just 2 lines of code
- **🛠️ Flexible API**: Support for individual logs, batch operations, and async logging
- **🏭 Production Ready**: Comprehensive error handling and logging
- **🚀 Resilient Architecture**: In-memory queuing with background workers for high performance
- **🔄 Retry with Backoff**: Automatic retry with exponential backoff for failed requests
- **🛡️ Failsafe Mode**: Never crash your application due to logging failures
- **� Statistics**: Built-in metrics for monitoring queue size, success/failure rates
- **⚡ High Performance**: Async processing with configurable worker pools, key caching for <1ms encryption
- **🔌 Logger Adapters**: Drop-in replacements for popular Java logging frameworks
- **🔐 Enhanced Security**: PBKDF2 key derivation with 600,000 iterations (OWASP 2023+ standard)

## Log Levels

LogFlux supports the following log levels:

| Level | Value | Description |
|-------|-------|-------------|
| Debug | 0     | Detailed information for diagnosing problems |
| Info  | 1     | General informational messages |
| Warn  | 2     | Warning messages for potentially harmful situations |
| Error | 3     | Error events that still allow the application to continue |
| Fatal | 4     | Very severe error events that will lead the application to abort |

## Installation

### Maven

```xml
<dependency>
    <groupId>io.logflux</groupId>
    <artifactId>logflux-java-sdk</artifactId>
    <version>2.0.0</version>
</dependency>
```

### Gradle

```gradle
implementation 'io.logflux:logflux-java-sdk:2.0.0'
```

## Quick Start

### 1. Ultra-Simple Usage (2 lines!)

```java
import io.logflux.logger.LogFlux;

public class Example {
    public static void main(String[] args) {
        // Initialize LogFlux (one time setup)
        LogFlux.init("https://<customer-id>.ingest.<region>.logflux.io", "my-app", "lf_your_api_key", "your-secret");
        
        // Use it with proper log levels
        LogFlux.info("User login successful").join();
        LogFlux.warning("Rate limit approaching").join();
        LogFlux.error("Database connection failed").join();
        
        LogFlux.close();
    }
}
```

### 2. Advanced Client Usage

```java
import io.logflux.client.Client;

public class Example {
    public static void main(String[] args) {
        try (Client client = Client.create(
                "https://<customer-id>.ingest.<region>.logflux.io",  // LogFlux server URL
                "your-node-name",                       // Node identifier
                "lf_your_api_key",                      // API key
                "your-secret-key"                       // Encryption secret
        )) {
            // Use convenience methods with log levels
            client.info("User login successful");
            client.warning("High memory usage detected");
            client.error("Failed to process request");
            client.debug("Debug information");
            client.emergency("Critical system failure");
        }
    }
}
```

### 3. Using Environment Variables

Configure using environment variables:

```bash
# Required
export LOGFLUX_API_KEY="lf_your_api_key"
export LOGFLUX_SERVER_URL="https://<customer-id>.ingest.<region>.logflux.io"

# Optional configuration
export LOGFLUX_QUEUE_SIZE="1000"
export LOGFLUX_FLUSH_INTERVAL="5"        # seconds
export LOGFLUX_MAX_RETRIES="5"
export LOGFLUX_INITIAL_DELAY="100"       # milliseconds
export LOGFLUX_MAX_DELAY="30"            # seconds
export LOGFLUX_BACKOFF_FACTOR="2.0"
export LOGFLUX_HTTP_TIMEOUT="30"         # seconds
export LOGFLUX_FAILSAFE_MODE="true"
export LOGFLUX_WORKER_COUNT="2"
```

Then use the client:

```java
import io.logflux.client.Client;

public class Example {
    public static void main(String[] args) {
        try (Client client = Client.createFromEnv("node-1", "your-secret-key")) {
            // Send logs
            client.info("Application started");
            client.info("Processing user request");
        }
    }
}
```

### 4. Resilient Client (Recommended for Production)

```java
import io.logflux.client.ResilientClient;
import io.logflux.config.ResilientClientConfig;

public class Example {
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
            // Send logs - they're queued and sent asynchronously with retry
            client.info("Application started successfully").join();
            client.warning("High memory usage detected").join();
            client.error("Database connection failed - will retry").join();
            
            // Check statistics
            ClientStats stats = client.getStats();
            System.out.printf("Sent: %d, Failed: %d, Queued: %d/%d, Dropped: %d%n",
                    stats.getTotalSent(), stats.getTotalFailed(),
                    stats.getQueueSize(), stats.getQueueCapacity(),
                    stats.getTotalDropped());
            
            // Flush ensures all queued logs are sent before shutdown
            client.flush(Duration.ofSeconds(10)).join();
        }
    }
}
```

### 5. Batch Operations

```java
import io.logflux.client.ResilientClient;
import io.logflux.models.LogEntry;
import io.logflux.models.LogLevel;

public class Example {
    public static void main(String[] args) {
        try (ResilientClient client = ResilientClient.createFromEnv("batch-processor", "your-secret-key")) {
            // Prepare batch of log entries (messages are encrypted automatically)
            List<LogEntry> entries = Arrays.asList(
                    // Using the convenience method which handles encryption automatically
                    // For manual creation, you would need to encrypt each message first
            );
            
            // Better approach: use convenience methods that handle encryption
            CompletableFuture<?>[] futures = {
                client.info("Batch job started"),
                client.info("Processing 1000 records"),
                client.info("Batch job completed")
            };
            
            // Wait for all to complete
            CompletableFuture.allOf(futures).join();
            
            // Note: The above approach is preferred for simplicity
            // Direct batch sending is also possible but requires manual encryption
        }
    }
}
```

## Configuration

### Environment Variables

Configure the SDK using environment variables:

```bash
# Required
export LOGFLUX_API_KEY="lf_your_api_key"
export LOGFLUX_SERVER_URL="https://<customer-id>.ingest.<region>.logflux.io"

# Optional configuration
export LOGFLUX_QUEUE_SIZE="1000"
export LOGFLUX_FLUSH_INTERVAL="5"        # seconds
export LOGFLUX_MAX_RETRIES="5"
export LOGFLUX_INITIAL_DELAY="100"       # milliseconds
export LOGFLUX_MAX_DELAY="30"            # seconds
export LOGFLUX_BACKOFF_FACTOR="2.0"
export LOGFLUX_HTTP_TIMEOUT="30"         # seconds
export LOGFLUX_FAILSAFE_MODE="true"
export LOGFLUX_WORKER_COUNT="2"
```

### Programmatic Configuration

```java
ResilientClientConfig config = new ResilientClientConfig.Builder()
        .serverUrl("https://<customer-id>.ingest.<region>.logflux.io")
        .node("my-app")
        .apiKey("lf_your_api_key")
        .secret("your-encryption-secret")
        .queueSize(1000)
        .flushInterval(Duration.ofSeconds(5))
        .failsafeMode(true)
        .workerCount(2)
        .maxRetries(5)
        .initialDelay(Duration.ofMillis(100))
        .maxDelay(Duration.ofSeconds(30))
        .backoffFactor(2.0)
        .jitterEnabled(true)
        .build();

ResilientClient client = new ResilientClient(config);
```

## Logger Adapters

The LogFlux SDK provides adapters for popular Java logging frameworks, allowing you to use LogFlux as a drop-in replacement for your existing logging infrastructure.

### SLF4J Adapter

Use LogFlux with SLF4J-style logging:

```java
import io.logflux.adapters.Slf4jAdapter;
import io.logflux.client.ResilientClient;

ResilientClient client = ResilientClient.createFromEnv("my-app", "secret");
Slf4jAdapter logger = new Slf4jAdapter(client, "my-logger");

// Use like SLF4J
logger.info("SLF4J info message");
logger.warn("SLF4J warning message");
logger.error("SLF4J error message");

// With parameters
logger.info("User {} logged in from IP {}", "john123", "192.168.1.1");
logger.warn("Rate limit reached: {} requests in {} seconds", 1000, 60);
```

### Java Util Logging (JUL) Adapter

Use LogFlux with JUL:

```java
import io.logflux.adapters.JulAdapter;
import io.logflux.client.ResilientClient;

ResilientClient client = ResilientClient.createFromEnv("my-app", "secret");
Logger logger = JulAdapter.createLogger(client, "my-logger");

// Use like JUL
logger.info("JUL info message");
logger.warning("JUL warning message");
logger.severe("JUL severe message");
```

### Log4j2 Adapter

Use LogFlux with Log4j2:

```java
import io.logflux.adapters.Log4jAdapter;
import io.logflux.client.ResilientClient;

ResilientClient client = ResilientClient.createFromEnv("my-app", "secret");
Log4jAdapter appender = Log4jAdapter.createAppender(client, "logflux-appender");

// Configure Log4j2 to use the LogFlux appender
// (typically done in log4j2.xml configuration)
```

## Migration Guide

### From java.util.logging

```java
// Before
Logger logger = Logger.getLogger("MyClass");
logger.info("message");

// After
ResilientClient client = ResilientClient.createFromEnv("app", "secret");
Logger logger = JulAdapter.createLogger(client, "MyClass");
logger.info("message"); // Now goes to LogFlux
```

### From SLF4J

```java
// Before
Logger logger = LoggerFactory.getLogger(MyClass.class);
logger.info("message");

// After
ResilientClient client = ResilientClient.createFromEnv("app", "secret");
Slf4jAdapter logger = new Slf4jAdapter(client, "MyClass");
logger.info("message"); // Now goes to LogFlux
```

### From Log4j2

```java
// Before
Logger logger = LogManager.getLogger(MyClass.class);
logger.info("message");

// After
ResilientClient client = ResilientClient.createFromEnv("app", "secret");
Log4jAdapter appender = Log4jAdapter.createAppender(client, "logflux");
// Configure Log4j2 to use the LogFlux appender
```

## API Reference

### Client

#### `Client.create(serverUrl, node, apiKey, secret)`
Creates a new LogFlux client with explicit parameters.

#### `Client.createFromEnv(node, secret)`
Creates a client using configuration from environment variables.

#### `ResilientClient.createFromEnv(node, secret)`
Creates a production-ready client from environment variables.

#### Log Level Methods
- `debug(message)` - Send debug level log
- `info(message)` - Send info level log
- `notice(message)` - Send notice level log
- `warning(message)` - Send warning level log
- `error(message)` - Send error level log
- `critical(message)` - Send critical level log
- `alert(message)` - Send alert level log
- `emergency(message)` - Send emergency level log

#### Core Methods
- `sendLog(message, level, timestamp)` - Send a log with specific level and timestamp
- `sendLogBatch(entries)` - Send multiple logs in batch
- `flush(timeout)` - Flush all queued logs
- `getStats()` - Get client statistics
- `close()` - Close the client and flush any pending logs

## Architecture

### Core Components

The LogFlux Java SDK follows a layered architecture designed for reliability, performance, and security:

#### 1. Client Layer (`io.logflux.client`)

**Client (Basic)**
- Purpose: Simple HTTP-based log transmission
- Features: Synchronous operations, basic error handling
- Usage: Development and simple applications

**ResilientClient (Production)**
- Purpose: Production-ready client with advanced features
- Features: Async queuing, retry logic, failsafe mode, statistics
- Implementation: Multi-threaded with configurable worker pools

#### 2. Configuration Layer (`io.logflux.config`)

**ClientConfig**
- Purpose: Basic client configuration
- Validation: URL format, required fields, parameter ranges

**ResilientClientConfig**
- Purpose: Advanced configuration with builder pattern
- Features: Fluent API, comprehensive validation, environment variable support

#### 3. Encryption Layer (`io.logflux.crypto`)

**Encryptor**
- Algorithm: AES-256-GCM (authenticated encryption)
- Key Derivation: PBKDF2-HMAC-SHA256 with 600,000 iterations
- Security: Random IV per message, secure key derivation, salt handling
- Features: Thread-safe, high-performance encryption

**Security Improvements**
- **PBKDF2 Implementation**: Replaced vulnerable SHA-256 with industry-standard PBKDF2
- **Brute-force Resistance**: 600,000 iterations make attacks 600,000x more expensive
- **Key Caching**: Derived keys are cached for performance (<1ms after first encryption)
- **Enhanced Salt Size**: 32-byte salt exceeds OWASP minimum requirements
- **Separate IV/Salt Fields**: IV and salt sent as separate fields for better security analysis
- **Mandatory Encryption**: All payloads must be encrypted - no plaintext option

#### 4. Model Layer (`io.logflux.models`)

**LogEntry**
- Purpose: Structured log message representation
- Fields: node, payload (encrypted), level, timestamp, encryption_mode, iv, salt
- All payloads are encrypted - no plaintext option available

**LogLevel**
- Purpose: Enumeration of log severity levels
- Values: EMERGENCY(1), ALERT(2), CRITICAL(3), ERROR(4), WARNING(5), NOTICE(6), INFO(7), DEBUG(8)

**ClientStats**
- Purpose: Performance metrics and monitoring
- Metrics: sent, failed, dropped, queue size, queue capacity

#### 5. Queue Layer (`io.logflux.queue`)

**LogQueue**
- Purpose: Thread-safe in-memory queue for log buffering
- Features: Configurable capacity, failsafe mode, overflow handling
- Implementation: ArrayBlockingQueue with atomic counters
- Stores encrypted payloads with separate IV/salt fields

#### 6. Retry Layer (`io.logflux.retry`)

**RetryStrategy**
- Purpose: Exponential backoff retry logic
- Features: Configurable parameters, jitter, retryable error detection
- Algorithm: delay = initialDelay * (backoffFactor ^ attempt) + jitter

#### 7. Adapter Layer (`io.logflux.adapters`)

**SLF4J Adapter**
- Purpose: Drop-in replacement for SLF4J Logger interface
- Features: Parameter formatting, marker support, all log levels

**JUL Adapter**
- Purpose: Java Util Logging Handler implementation
- Features: Level mapping, record formatting, logger factory

**Log4j2 Adapter**
- Purpose: Log4j2 Appender implementation
- Features: Event processing, level conversion, plugin support

#### 8. Logger Layer (`io.logflux.logger`)

**LogFlux (Global Logger)**
- Purpose: Static interface for ultra-simple usage
- Features: Global client management, thread-safe initialization
- Pattern: Singleton with lazy initialization

### Threading Model

**Basic Client**
- Model: Single-threaded with optional async operations
- Concurrency: Thread-safe for concurrent access
- Lifecycle: Manual resource management

**Resilient Client**
- Model: Multi-threaded with worker pool and scheduler
- Workers: Configurable number of background worker threads
- Scheduler: Single-threaded scheduler for periodic flush operations
- Synchronization: Lock-free queues and atomic operations

### Data Flow

1. **Log Message Processing**
   ```
   Application → LogFlux.info() → ResilientClient.info() → Encryptor.encrypt() → LogQueue.offer() → Background Worker → HTTP Client → LogFlux Server
   ```

2. **Retry Processing**
   ```
   Failed Request → RetryStrategy.shouldRetry() → RetryStrategy.calculateDelay() → Thread.sleep() → Retry Request
   ```

3. **Queue Processing**
   ```
   LogQueue.offer() → Background Worker → LogQueue.poll() → Batch Processing → HTTP Request → Statistics Update
   ```

### Security Model

**Encryption**
- Algorithm: AES-256-GCM (authenticated encryption)
- Key Derivation: PBKDF2-HMAC-SHA256 with 600,000 iterations
- IV: Random 96-bit IV per message
- Authentication: Built-in authentication tag

**Transport Security**
- Protocol: HTTPS with TLS 1.2+
- Validation: Certificate validation
- Authentication: Bearer token (API key)

**Data Protection**
- At Rest: Messages encrypted before storage
- In Transit: HTTPS encryption
- In Memory: Encrypted payloads in queues

## Testing

### Test Execution

The LogFlux Java SDK includes a comprehensive test suite with 260+ tests covering all functionality:

```bash
# Run all tests
mvn test

# Run tests with coverage report
mvn clean test jacoco:report

# Run specific test class
mvn test -Dtest=EncryptorTest

# Run tests with detailed output
mvn test -X
```

### Test Structure

```
src/test/java/io/logflux/
├── adapters/           # Logger adapter tests
├── client/             # Client functionality tests
├── config/             # Configuration tests
├── crypto/             # Encryption and security tests
├── logger/             # Global logger tests
├── models/             # Data model tests
├── queue/              # Queue management tests
├── retry/              # Retry strategy tests
└── utils/              # Utility and serialization tests
```

### Test Coverage

The test suite provides comprehensive coverage:

**Core Functionality**
- ✅ **Encryption**: AES-256-GCM encryption/decryption with various message types
- ✅ **Basic Client**: HTTP communication, log levels, batch operations
- ✅ **Resilient Client**: Queuing, retry logic, failsafe mode, statistics
- ✅ **Configuration**: Environment variables, validation, builder pattern

**Advanced Features**
- ✅ **Logger Adapters**: SLF4J, JUL, Log4j integration
- ✅ **Global Logger**: Static interface for simple usage
- ✅ **Error Handling**: Network failures, authentication errors, graceful degradation
- ✅ **Concurrency**: Thread safety, async operations, high-volume logging

**Security Testing**
- ✅ **PBKDF2 Implementation**: Key derivation verification
- ✅ **Encryption Security**: IV randomness, no plaintext leakage
- ✅ **Authentication**: API key validation
- ✅ **Cross-instance Compatibility**: Encryption consistency

**Edge Cases**
- ✅ **Malformed Data**: Invalid JSON, corrupted encryption, network errors
- ✅ **Resource Management**: Proper cleanup, connection pooling, memory usage
- ✅ **Configuration Errors**: Missing parameters, invalid values, environment setup

### Mock Server

Tests use `MockLogFluxServer` which provides:
- HTTP/1.1 server simulation
- JSON request/response handling
- Configurable failure scenarios
- Request logging and validation
- Health and version endpoints

### Performance Benchmarks

The tests include performance validation:
- High-volume logging (50+ messages per second)
- Concurrent logging from multiple threads
- Queue processing efficiency
- Memory usage under load
- Network retry performance

### Test Results

Expected output when all tests pass:
```
[INFO] Tests run: 260, Failures: 0, Errors: 0, Skipped: 35
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 260, Failures: 0, Errors: 0, Skipped: 35
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

Note: 35 E2E tests are skipped unless a real LogFlux server is available.

## Best Practices

### 1. Use Resilient Client in Production

```java
// Always use resilient client for production
ResilientClient client = new ResilientClient(
    ResilientClientConfig.defaultConfig(
        "https://<customer-id>.ingest.<region>.logflux.io",
        "production-server",
        "lf_your_api_key",
        "your-encryption-secret"
    )
);
```

### 2. Handle Graceful Shutdown

```java
// Ensure logs are flushed on shutdown
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    try {
        client.flush(Duration.ofSeconds(10)).join();
        client.close();
    } catch (Exception e) {
        System.err.println("Error during shutdown: " + e.getMessage());
    }
}));
```

### 3. Use Appropriate Log Levels

```java
client.debug("Cache lookup for key: user_123");              // Debug info
client.info("User login successful");                        // General info
client.notice("New feature enabled for user");              // Notable events
client.warning("API rate limit approaching: 80% used");      // Warnings
client.error("Payment gateway timeout after 3 retries");     // Errors
client.critical("Core service unavailable");                 // Critical issues
client.alert("Disk space critically low");                   // Immediate action needed
client.emergency("System completely down");                  // System unusable
```

### 4. Structure Your Logs

```java
// Good: Structured JSON for better searchability
client.info("{\"event\": \"user_login\", \"user_id\": \"12345\", \"success\": true}");

// Avoid: Unstructured text
client.info("User 12345 logged in successfully");
```

## Performance and Monitoring

### Performance Characteristics

**Memory Usage**
- Queue: O(queue_size) for buffered messages
- Encryption: O(message_size) temporary allocation
- HTTP: Connection pooling for efficiency

**CPU Usage**
- Encryption: AES-256-GCM per message with PBKDF2 key derivation
- Serialization: JSON processing with Jackson
- Compression: HTTP gzip compression

**Network Usage**
- Batching: Automatic batching for efficiency
- Compression: HTTP gzip compression
- Keep-Alive: Connection reuse

### Monitoring and Statistics

Monitor your logging performance with built-in statistics:

```java
// Get current statistics
ClientStats stats = client.getStats();
System.out.printf("Sent: %d, Failed: %d, Queued: %d/%d, Dropped: %d%n",
    stats.getTotalSent(), 
    stats.getTotalFailed(),
    stats.getQueueSize(), 
    stats.getQueueCapacity(),
    stats.getTotalDropped()
);

// Monitor queue utilization
double utilization = (double) stats.getQueueSize() / stats.getQueueCapacity();
if (utilization > 0.8) {
    System.out.println("Warning: Queue utilization high: " + (utilization * 100) + "%");
}

// Monitor success rate
double successRate = (double) stats.getTotalSent() / 
    (stats.getTotalSent() + stats.getTotalFailed());
if (successRate < 0.95) {
    System.out.println("Warning: Success rate low: " + (successRate * 100) + "%");
}
```

### Health Checks

Implement health checks for your logging system:

```java
// Check client health
ClientStats stats = client.getStats();
boolean isHealthy = stats.getTotalFailed() == 0 && 
                   stats.getQueueSize() < stats.getQueueCapacity();

// Check server connectivity (if using basic client)
try {
    HealthResponse health = client.checkHealth();
    boolean serverHealthy = "ok".equals(health.getStatus());
} catch (Exception e) {
    // Server unreachable
}
```

## Examples

See the [examples directory](src/main/java/io/logflux/examples/) for complete working examples:

- **SimpleExample.java**: Ultra-simple API usage
- **ResilientClientExample.java**: Production-ready client
- **AdapterExample.java**: Logger adapter examples

## Support

- **GitHub Issues**: [logflux-io/logflux-java-sdk](https://github.com/logflux-io/logflux-java-sdk/issues)
- **Documentation**: [docs.logflux.io](https://docs.logflux.io)
- **Website**: [logflux.io](https://logflux.io)
- **Email**: [support@logflux.io](mailto:support@logflux.io)

## Requirements

- Java 11 or later
- Valid LogFlux API key from your [LogFlux Dashboard](https://dashboard.logflux.io)
- Network access to your LogFlux endpoint
- Maven 3.6+ (for building from source)

## Security Features

### Encryption
- **Algorithm**: AES-256-GCM (authenticated encryption)
- **Key Derivation**: PBKDF2-HMAC-SHA256 with 600,000 iterations (OWASP 2023+ recommended)
- **Authentication**: API keys with bearer token authentication
- **Privacy**: Secrets are never logged or exposed in error messages
- **Brute-force Protection**: PBKDF2 makes password cracking 600,000x more expensive

### Transport Security
- **Protocol**: HTTPS with TLS 1.2+ only
- **Certificate Validation**: Full certificate chain validation
- **Connection Security**: Connection pooling with secure defaults

### Data Protection
- **At Rest**: All messages encrypted before any storage
- **In Transit**: HTTPS encryption for all communications
- **In Memory**: Encrypted payloads stored in queues

### Security Best Practices
1. Use strong, randomly generated secrets (32+ characters recommended)
2. Store secrets in environment variables, not in code
3. Rotate API keys and secrets periodically
4. Monitor for any unusual performance impacts
5. Use HTTPS endpoints exclusively in production

## License

MIT License - see [LICENSE](LICENSE) for details.

Copyright (c) 2025 LogFlux.io