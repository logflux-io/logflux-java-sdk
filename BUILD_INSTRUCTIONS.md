# Building and Running LogFlux Java SDK Examples

## Prerequisites

- Java 11 or later
- Maven 3.6+

## Building the Project

### 1. Compile the Project
```bash
mvn compile
```

### 2. Build the Complete JAR Package
```bash
mvn package
```

This creates the following artifacts in the `target/` directory:
- `logflux-java-sdk-1.0.0.jar` - Main JAR with all classes
- `logflux-java-sdk-1.0.0-sources.jar` - Source code JAR
- `logflux-java-sdk-1.0.0-javadoc.jar` - Javadoc JAR

## Running the Simple Example

The SimpleExample demonstrates the ultra-simple LogFlux API:

### Source Code Location
```
src/main/java/io/logflux/examples/SimpleExample.java
```

### Compiled Class Location
```
target/classes/io/logflux/examples/SimpleExample.class
```

### Running with Maven
```bash
# Run the simple example (requires valid LogFlux credentials)
mvn exec:java -Dexec.mainClass="io.logflux.examples.SimpleExample"
```

### Running with Java (after building)
```bash
# Add dependencies to classpath and run
java -cp "target/logflux-java-sdk-1.0.0.jar:target/dependency/*" io.logflux.examples.SimpleExample
```

## Example Code Structure

The SimpleExample demonstrates:

1. **Initialization**: One-time setup with LogFlux credentials
2. **Logging**: Using different log levels (info, warn, error, debug, fatal)
3. **Cleanup**: Proper shutdown with flush and close

```java
// Initialize LogFlux (one time setup)
LogFlux.init("https://<customer-id>.ingest.<region>.logflux.io", "my-app", "lf_your_api_key", "your-secret");

// Use it with proper log levels
LogFlux.info("User login successful").join();
LogFlux.warn("Rate limit approaching").join();
LogFlux.error("Database connection failed").join();

// Clean up
LogFlux.close();
```

## Configuration

To run the example with real LogFlux credentials:

1. Replace `<customer-id>` with your actual customer ID
2. Replace `<region>` with your region (e.g., `eu`, `us`)
3. Replace `lf_your_api_key` with your actual API key
4. Replace `your-secret` with your encryption secret

## Other Examples

- `ResilientClientExample.java` - Production-ready client with configuration
- `AdapterExample.java` - Logger adapter examples for SLF4J, JUL, and Log4j2

## Testing

Run the full test suite:
```bash
mvn test
```

Run with coverage:
```bash
mvn clean test jacoco:report
```
