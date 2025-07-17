# Contributing to LogFlux Java SDK

Thank you for your interest in contributing to the LogFlux Java SDK! This document provides guidelines and information for contributors.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Project Structure](#project-structure)
- [Running Tests](#running-tests)
- [Coding Standards](#coding-standards)
- [Pull Request Process](#pull-request-process)
- [Issue Guidelines](#issue-guidelines)
- [Release Process](#release-process)

## Code of Conduct

By participating in this project, you are expected to uphold our code of conduct. Please be respectful and professional in all interactions.

## Getting Started

### Prerequisites

- Java 11 or later
- Maven 3.6 or later
- Git
- An IDE (IntelliJ IDEA, Eclipse, VS Code, etc.)

### Fork and Clone

1. Fork the repository on GitHub
2. Clone your fork locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/logflux-java-sdk.git
   cd logflux-java-sdk
   ```

3. Add the original repository as upstream:
   ```bash
   git remote add upstream https://github.com/logflux-io/logflux-java-sdk.git
   ```

## Development Setup

1. Install dependencies:
   ```bash
   mvn clean install
   ```

2. Run tests to ensure everything works:
   ```bash
   mvn test
   ```

3. Generate code coverage report:
   ```bash
   mvn jacoco:report
   ```

## Project Structure

```
logflux-java-sdk/
├── src/main/java/io/logflux/
│   ├── client/            # Client implementations
│   │   ├── Client.java    # Basic client
│   │   └── ResilientClient.java # Production client
│   ├── config/            # Configuration classes
│   ├── crypto/            # Encryption utilities
│   ├── models/            # Data models
│   ├── queue/             # Queue implementation
│   ├── retry/             # Retry logic
│   ├── adapters/          # Logger adapters
│   ├── logger/            # Simple logger interface
│   └── examples/          # Usage examples
├── src/test/java/         # Test files
├── pom.xml                # Maven configuration
└── README.md
```

### Key Components

- **Client**: Basic LogFlux client (`io.logflux.client.Client`)
- **ResilientClient**: Production-ready client with queuing and retry logic (`io.logflux.client.ResilientClient`)
- **Encryptor**: AES-256-GCM encryption implementation (`io.logflux.crypto.Encryptor`)
- **Adapters**: Logger adapters for popular Java frameworks (`io.logflux.adapters.*`)
- **LogFlux**: Simple global logger interface (`io.logflux.logger.LogFlux`)

## Running Tests

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify
```

### Test Coverage
```bash
mvn jacoco:report
```

View coverage report at `target/site/jacoco/index.html`

### Test with Different Java Versions
```bash
mvn test -Dmaven.compiler.source=11 -Dmaven.compiler.target=11
mvn test -Dmaven.compiler.source=17 -Dmaven.compiler.target=17
```

## Coding Standards

### Java Style

- Follow Oracle Java Code Conventions
- Use 4 spaces for indentation
- Maximum line length: 120 characters
- Use meaningful variable and method names
- Add Javadoc for all public classes and methods

### Code Quality

Run code quality checks:
```bash
mvn checkstyle:check
mvn spotbugs:check
mvn pmd:check
```

### Example Code Style:

```java
/**
 * Represents a log entry to be sent to LogFlux.
 */
public class LogEntry {
    private final String node;
    private final String payload;
    private final LogLevel logLevel;
    private final Instant timestamp;

    /**
     * Creates a new LogEntry.
     *
     * @param node      The node identifier
     * @param payload   The encrypted log message
     * @param logLevel  The log level
     * @param timestamp The timestamp
     */
    public LogEntry(String node, String payload, LogLevel logLevel, Instant timestamp) {
        this.node = Objects.requireNonNull(node, "Node cannot be null");
        this.payload = Objects.requireNonNull(payload, "Payload cannot be null");
        this.logLevel = Objects.requireNonNull(logLevel, "LogLevel cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
    }

    // Getters, equals, hashCode, toString methods...
}
```

### Documentation

- Use Javadoc for all public APIs
- Include @param and @return tags
- Provide usage examples in class-level Javadoc
- Use proper HTML formatting in Javadoc

## Pull Request Process

### Before Submitting

1. **Update from upstream**:
   ```bash
   git fetch upstream
   git checkout main
   git merge upstream/main
   ```

2. **Create a feature branch**:
   ```bash
   git checkout -b feature/your-feature-name
   ```

3. **Make your changes**:
   - Write code following our standards
   - Add tests for new functionality
   - Update documentation if needed

4. **Run quality checks**:
   ```bash
   mvn clean compile
   mvn test
   mvn jacoco:report
   mvn checkstyle:check
   ```

5. **Commit your changes**:
   ```bash
   git add .
   git commit -m "feat: add new feature description"
   ```

### Commit Message Format

Follow conventional commits:

- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation changes
- `style:` Code style changes
- `refactor:` Code refactoring
- `test:` Test changes
- `chore:` Build/tool changes

Examples:
```
feat: add retry mechanism to resilient client
fix: handle network timeout errors properly
docs: update API documentation for new methods
test: add integration tests for batch operations
```

### Submit Pull Request

1. Push your branch:
   ```bash
   git push origin feature/your-feature-name
   ```

2. Create a pull request on GitHub with:
   - Clear title and description
   - Reference any related issues
   - Include testing instructions
   - Add screenshots if applicable

### PR Review Process

- All PRs require at least one review
- Tests must pass
- Code coverage should not decrease
- Documentation must be updated for API changes
- All quality checks must pass

## Issue Guidelines

### Before Creating an Issue

1. Search existing issues to avoid duplicates
2. Check if it's already fixed in the latest version
3. Reproduce the issue with minimal code

### Issue Types

**Bug Reports** should include:
- Java version
- SDK version
- Operating system
- Steps to reproduce
- Expected vs actual behavior
- Minimal code example
- Stack trace if applicable

**Feature Requests** should include:
- Use case description
- Proposed API (if applicable)
- Alternative solutions considered
- Willingness to implement

**Questions** should include:
- What you're trying to achieve
- What you've already tried
- Relevant code snippets

### Labels

- `bug`: Something isn't working
- `enhancement`: New feature or improvement
- `documentation`: Documentation improvements
- `good first issue`: Good for newcomers
- `help wanted`: Extra attention needed

## Release Process

### Versioning

We follow [Semantic Versioning](https://semver.org/):

- **MAJOR**: Breaking changes
- **MINOR**: New features (backward compatible)
- **PATCH**: Bug fixes (backward compatible)

### Release Steps

1. **Update version** in `pom.xml`:
   ```xml
   <version>1.2.3</version>
   ```

2. **Update CHANGELOG.md**:
   - Add new version section
   - List all changes since last version
   - Follow [Keep a Changelog](https://keepachangelog.com/) format

3. **Run quality checks**:
   ```bash
   mvn clean compile test jacoco:report
   ```

4. **Build and package**:
   ```bash
   mvn clean package
   ```

5. **Deploy to Maven Central**:
   ```bash
   mvn clean deploy -P release
   ```

6. **Create GitHub release**:
   - Tag the version
   - Copy changelog entry
   - Attach built artifacts

## Development Guidelines

### Adding New Features

1. **Design first**: Discuss major changes in issues
2. **Write tests**: Test-driven development preferred
3. **Document**: Add Javadoc and update README
4. **Examples**: Add usage examples if applicable
5. **Compatibility**: Ensure backward compatibility

### Performance Considerations

- Use efficient data structures
- Minimize object creation in hot paths
- Use async operations for I/O
- Consider memory usage for high-volume scenarios
- Profile critical paths

### Security

- Never log secrets or sensitive data
- Use secure encryption practices
- Validate all inputs
- Handle errors gracefully without exposing internals
- Follow OWASP guidelines

### Testing Guidelines

- Write unit tests for all new methods
- Add integration tests for complex features
- Use JUnit 5 for testing
- Mock external dependencies
- Test both success and failure cases

Example test:
```java
@Test
void shouldEncryptMessage() {
    // Arrange
    Encryptor encryptor = new Encryptor("test-secret");
    String message = "Hello, LogFlux!";
    
    // Act
    String encrypted = encryptor.encrypt(message);
    
    // Assert
    assertThat(encrypted).isNotNull();
    assertThat(encrypted).isNotEqualTo(message);
    assertThat(encrypted).matches("^[A-Za-z0-9+/]+=*$");
}
```

## Getting Help

- **GitHub Issues**: [logflux-io/logflux-java-sdk/issues](https://github.com/logflux-io/logflux-java-sdk/issues)
- **Documentation**: [docs.logflux.io](https://docs.logflux.io)
- **Website**: [logflux.io](https://logflux.io)
- **Email**: [support@logflux.io](mailto:support@logflux.io)

## Recognition

Contributors will be acknowledged in:
- CHANGELOG.md
- GitHub contributors list
- Release notes (for significant contributions)

Thank you for contributing to LogFlux Java SDK! 🚀