# LogFlux Java SDK

[![License](https://img.shields.io/badge/License-ELv2-blue.svg)](LICENSE)
[![Java Version](https://img.shields.io/badge/Java-11%2B-blue.svg)](build.gradle)

Zero-dependency Java SDK for [LogFlux.io](https://logflux.io) with end-to-end encryption, multipart/mixed transport, and async batching.

## Features

- **End-to-end encryption**: AES-256-GCM with RSA-OAEP key exchange
- **Zero dependencies**: Uses only Java stdlib (`javax.crypto`, `java.net.http`, `java.util.zip`, `java.security`)
- **All 7 entry types**: Log, Metric, Trace, Event, Audit, Telemetry, TelemetryManaged
- **Async batching**: Background workers with bounded queue and configurable flush
- **Multipart/mixed**: Binary transport (no base64 overhead)
- **Distributed tracing**: Span creation, child spans, header propagation
- **Scopes**: Per-request context isolation with breadcrumbs
- **Sampling**: Configurable probabilistic sampling (audit entries always sent)
- **Rate limit handling**: Automatic 429 backoff with Retry-After support
- **Quota awareness**: Per-category 507 blocking
- **BeforeSend hooks**: Per-type payload inspection and modification
- **Failsafe mode**: SDK never crashes the host application

## Requirements

- Java 11 or later (built with JDK 17)
- No external dependencies

## Quick Start

### Gradle

```kotlin
implementation("io.logflux:logflux-java-sdk:3.0.0")
```

### Maven

```xml
<dependency>
    <groupId>io.logflux</groupId>
    <artifactId>logflux-java-sdk</artifactId>
    <version>3.0.0</version>
</dependency>
```

### Basic Usage

```java
import io.logflux.sdk.*;

import java.util.Map;

public class MyApp {
    public static void main(String[] args) throws Exception {
        // Initialize
        LogFlux.init(Options.builder("eu-lf_your_api_key")
            .source("my-service")
            .environment("production")
            .release("v1.2.0")
            .build());

        // Logging (Type 1)
        LogFlux.info("Server started on port 8080");
        LogFlux.warn("High memory usage", Map.of("usage_pct", "85"));
        LogFlux.error("Connection failed", Map.of("host", "db.example.com"));

        // Metrics (Type 2)
        LogFlux.counter("http.requests", 1, Map.of("method", "GET"));
        LogFlux.gauge("memory.usage", 85.5, Map.of("unit", "percent"));

        // Events (Type 4)
        LogFlux.event("user.login", Map.of("user_id", "usr_123"));

        // Audit (Type 5 - Object Lock, never sampled)
        LogFlux.audit("delete", "admin@example.com", "user", "usr_456", null);

        // Error capture with stack trace + breadcrumbs
        try {
            riskyOperation();
        } catch (Exception e) {
            LogFlux.captureError(e);
        }

        // Shutdown
        LogFlux.close();
    }
}
```

### Environment Variable Configuration

```bash
export LOGFLUX_API_KEY="eu-lf_your_api_key"
export LOGFLUX_ENVIRONMENT="production"
export LOGFLUX_NODE="server-01"
```

```java
LogFlux.initFromEnv();
// or
LogFlux.initFromEnv("custom-node-name");
```

### Distributed Tracing (Type 3)

```java
// Create a root span
Span span = LogFlux.startSpan("http.server", "GET /api/users");
try {
    span.setAttribute("http.method", "GET");
    span.setAttribute("http.url", "/api/users");

    // Create child span for DB query
    Span dbSpan = span.startChild("db.query", "SELECT users");
    try {
        // ... database query ...
    } finally {
        dbSpan.end();
    }

    span.setAttribute("http.status_code", "200");
} catch (Exception e) {
    span.setError(e);
    throw e;
} finally {
    span.end();
}
```

### Trace Propagation

```java
// Inject trace context into outgoing request headers
Map<String, String> headers = new HashMap<>();
span.injectHeaders(headers);

// Continue trace from incoming request headers
Span serverSpan = LogFlux.continueFromHeaders(
    incomingHeaders, "http.server", "POST /api/orders");
```

### Scopes (Per-Request Context)

```java
LogFlux.withScope(scope -> {
    scope.setAttribute("request_id", "req-abc-123");
    scope.setUser("usr_456");
    scope.setRequest("GET", "/api/users", "req-abc-123");

    scope.addBreadcrumb("http", "GET /api/users", null);
    scope.info("Processing request");

    try {
        processRequest();
    } catch (Exception e) {
        scope.captureError(e); // includes scope attrs + breadcrumbs
    }
});
```

### BeforeSend Hooks

```java
Options opts = Options.builder("eu-lf_your_api_key")
    // Global hook - runs on all entries
    .beforeSend(payload -> {
        payload.put("custom_tag", "injected");
        return payload;
    })
    // Type-specific hook - return null to drop
    .beforeSendLog(payload -> {
        int level = ((Number) payload.get("level")).intValue();
        if (level > LogLevel.WARNING) return null; // drop debug/info
        return payload;
    })
    .build();
```

### Sampling

```java
Options opts = Options.builder("eu-lf_your_api_key")
    .sampleRate(0.1) // Send 10% of entries
    .build();
// Note: Audit entries (Type 5) are always sent regardless of sample rate
```

## Spring Boot Integration

```java
import io.logflux.sdk.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@SpringBootApplication
public class Application {

    @PostConstruct
    public void initLogFlux() throws Exception {
        LogFlux.init(Options.builder(System.getenv("LOGFLUX_API_KEY"))
            .source("spring-app")
            .environment(System.getenv("SPRING_PROFILES_ACTIVE"))
            .build());
    }

    @PreDestroy
    public void shutdownLogFlux() {
        LogFlux.close();
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

## Android Integration

```java
import io.logflux.sdk.*;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            LogFlux.init(Options.builder(BuildConfig.LOGFLUX_API_KEY)
                .source("android-app")
                .environment(BuildConfig.DEBUG ? "debug" : "release")
                .release(BuildConfig.VERSION_NAME)
                .node(Build.MODEL)
                .queueSize(500)
                .failsafe(true) // never crash the app
                .build());
        } catch (Exception e) {
            // Failsafe: log locally but don't crash
            Log.w("LogFlux", "Failed to initialize", e);
        }

        // Set global exception handler
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            LogFlux.captureError(throwable, Map.of("thread", thread.getName()));
            LogFlux.flush(3000);
        });
    }
}
```

## Configuration Reference

| Option | Default | Description |
|--------|---------|-------------|
| `apiKey` | *required* | API key with region prefix (e.g., `eu-lf_...`) |
| `source` | `""` | Source identifier attached to all payloads |
| `environment` | `""` | Environment name (production, staging, etc.) |
| `release` | `""` | Release/version identifier |
| `node` | `""` | Node/host identifier |
| `queueSize` | `1000` | Maximum entries in the async queue |
| `flushInterval` | `5000` | Flush interval in milliseconds |
| `batchSize` | `100` | Maximum entries per batch request |
| `workerCount` | `2` | Number of background sender threads |
| `maxRetries` | `3` | Maximum retry attempts per batch |
| `httpTimeout` | `30000` | HTTP request timeout in milliseconds |
| `failsafe` | `true` | Silently drop on errors (never crash) |
| `enableCompression` | `true` | Enable gzip compression before encryption |
| `sampleRate` | `1.0` | Probability of sending (0.0-1.0) |
| `maxBreadcrumbs` | `100` | Ring buffer size for breadcrumbs |

## Entry Types

| Type | Value | Category | Encryption | Description |
|------|-------|----------|------------|-------------|
| Log | 1 | events | AES-256-GCM | Application logs |
| Metric | 2 | events | AES-256-GCM | Counters, gauges |
| Trace | 3 | traces | AES-256-GCM | Distributed tracing spans |
| Event | 4 | events | AES-256-GCM | Application events |
| Audit | 5 | audit | AES-256-GCM | Audit log (Object Lock, never sampled) |
| Telemetry | 6 | traces | AES-256-GCM | IoT/device telemetry |
| TelemetryManaged | 7 | traces | gzip only | Server-managed telemetry |

## Build

All builds run in Docker (no local Java/Gradle required):

```bash
make docker-build   # Build Docker dev image
make build          # Build SDK
make test           # Run tests
make shell          # Interactive shell
make clean          # Clean build artifacts
```

## License

[Elastic License 2.0 (ELv2)](LICENSE)

## Support

- [Documentation](https://docs.logflux.io)
- [Issue Tracker](https://github.com/logflux-io/logflux-java-sdk/issues)
