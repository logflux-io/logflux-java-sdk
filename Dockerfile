# LogFlux Agent Java SDK Docker Build Environment
FROM openjdk:17-jdk-slim

# Install build tools
RUN apt-get update && apt-get install -y \
    maven \
    curl \
    wget \
    git \
    make \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /workspace

# Copy Maven configuration
COPY pom.xml .

# Download dependencies (for better caching)
RUN mvn dependency:go-offline -B || true

# Copy source code
COPY src/ src/
COPY Makefile .

# Set environment variables
ENV JAVA_HOME=/usr/local/openjdk-17
ENV MAVEN_HOME=/usr/share/maven
ENV PATH=$PATH:$MAVEN_HOME/bin

# Default command
CMD ["make", "help"]