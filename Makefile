# LogFlux Java SDK Makefile

# Default target
.PHONY: help
help:
	@echo "Available targets:"
	@echo "  install       - Install dependencies and build"
	@echo "  test          - Run all tests"
	@echo "  test-unit     - Run unit tests only"
	@echo "  test-e2e      - Run E2E tests"
	@echo "  coverage      - Run tests with coverage report"
	@echo "  build         - Build the project"
	@echo "  clean         - Clean build artifacts"
	@echo "  package       - Package JAR file"
	@echo "  verify        - Run verification (tests + checks)"
	@echo "  deploy        - Deploy to Maven repository"
	@echo "  docs          - Generate Javadoc"
	@echo "  format        - Format code with Google Java Format"
	@echo "  lint          - Run static analysis"

# Maven command
MVN := mvn

# Build the project
.PHONY: build
build:
	$(MVN) clean compile

# Install dependencies and build
.PHONY: install
install:
	$(MVN) clean install

# Run all tests
.PHONY: test
test:
	$(MVN) clean test

# Run unit tests only
.PHONY: test-unit
test-unit:
	$(MVN) test -Dtest="!*E2ETest"

# Run E2E tests
.PHONY: test-e2e
test-e2e:
	$(MVN) test -Dtest="*E2ETest"

# Run tests with coverage
.PHONY: coverage
coverage:
	$(MVN) clean test jacoco:report
	@echo "Coverage report generated at: target/site/jacoco/index.html"

# Package JAR
.PHONY: package
package:
	$(MVN) clean package

# Clean build artifacts
.PHONY: clean
clean:
	$(MVN) clean
	rm -rf target/

# Run verification
.PHONY: verify
verify:
	$(MVN) clean verify

# Check dependencies
.PHONY: deps-check
deps-check:
	$(MVN) dependency:tree
	$(MVN) dependency:analyze

# Check for dependency updates
.PHONY: deps-update
deps-update:
	$(MVN) versions:display-dependency-updates
	$(MVN) versions:display-plugin-updates

# Security check
.PHONY: security
security:
	$(MVN) dependency-check:check

# Deploy to Maven repository
.PHONY: deploy
deploy:
	$(MVN) clean deploy -P release

# Deploy snapshot
.PHONY: deploy-snapshot
deploy-snapshot:
	$(MVN) clean deploy

# Generate Javadoc
.PHONY: docs
docs:
	$(MVN) javadoc:javadoc
	@echo "Javadoc generated at: target/site/apidocs/index.html"

# Format code (requires google-java-format)
.PHONY: format
format:
	@echo "Formatting Java files..."
	@find src -name "*.java" -print0 | xargs -0 google-java-format -i

# Run static analysis
.PHONY: lint
lint:
	$(MVN) spotbugs:check
	$(MVN) pmd:check

# Quick build without tests
.PHONY: quick
quick:
	$(MVN) clean compile -DskipTests

# Run a specific test class
.PHONY: test-class
test-class:
	@if [ -z "$(CLASS)" ]; then \
		echo "Usage: make test-class CLASS=TestClassName"; \
		exit 1; \
	fi
	$(MVN) test -Dtest=$(CLASS)

# Development workflow
.PHONY: dev
dev: format build test

# CI workflow
.PHONY: ci
ci: clean verify coverage security

# Release workflow
.PHONY: release
release: clean verify docs deploy
	@echo "Release completed successfully"

# All targets
.PHONY: all
all: clean format verify coverage docs