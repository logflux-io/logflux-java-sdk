package io.logflux.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.*;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * LogFlux Agent client for Java applications
 */
public class LogFluxClient implements AutoCloseable {
    private final String socketPath;
    private final String host;
    private final int port;
    private final boolean isUnixSocket;
    private final ObjectMapper objectMapper;
    private Socket socket;
    private OutputStream outputStream;
    private final AtomicBoolean connected = new AtomicBoolean(false);

    /**
     * Create a Unix socket client
     */
    public LogFluxClient(String socketPath) {
        this.socketPath = socketPath;
        this.host = null;
        this.port = 0;
        this.isUnixSocket = true;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Create a TCP client
     */
    public LogFluxClient(String host, int port) {
        this.socketPath = null;
        this.host = host;
        this.port = port;
        this.isUnixSocket = false;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Connect to the LogFlux agent
     */
    public synchronized void connect() throws IOException {
        if (connected.get()) {
            return;
        }

        try {
            if (isUnixSocket) {
                // Unix socket connection (Java 16+)
                SocketAddress socketAddress = UnixDomainSocketAddress.of(Path.of(socketPath));
                SocketChannel channel = SocketChannel.open(socketAddress);
                socket = channel.socket();
            } else {
                // TCP connection
                socket = new Socket(host, port);
            }
            
            outputStream = socket.getOutputStream();
            connected.set(true);
            
        } catch (IOException e) {
            cleanup();
            throw new IOException("Failed to connect to LogFlux agent: " + e.getMessage(), e);
        }
    }

    /**
     * Send a log entry to the agent
     */
    public synchronized void sendLogEntry(LogEntry entry) throws IOException {
        if (!connected.get()) {
            throw new IllegalStateException("Client not connected. Call connect() first.");
        }

        try {
            // Create message object
            ObjectNode message = objectMapper.createObjectNode();
            message.put("id", entry.getId());
            message.put("message", entry.getMessage());
            message.put("source", entry.getSource());
            message.put("entry_type", entry.getEntryType());
            message.put("level", entry.getLevel());
            message.put("timestamp", entry.getTimestamp());
            
            // Add labels
            ObjectNode labelsNode = objectMapper.createObjectNode();
            for (var labelEntry : entry.getLabels().entrySet()) {
                labelsNode.put(labelEntry.getKey(), labelEntry.getValue());
            }
            message.set("labels", labelsNode);

            // Serialize to JSON
            String jsonMessage = objectMapper.writeValueAsString(message);
            
            // Send with newline delimiter
            String messageWithNewline = jsonMessage + "\n";
            outputStream.write(messageWithNewline.getBytes("UTF-8"));
            outputStream.flush();
            
        } catch (JsonProcessingException e) {
            throw new IOException("Failed to serialize log entry: " + e.getMessage(), e);
        } catch (IOException e) {
            connected.set(false);
            cleanup();
            throw new IOException("Failed to send log entry: " + e.getMessage(), e);
        }
    }

    /**
     * Check if client is connected
     */
    public boolean isConnected() {
        return connected.get() && socket != null && socket.isConnected() && !socket.isClosed();
    }

    /**
     * Close the connection
     */
    @Override
    public synchronized void close() {
        connected.set(false);
        cleanup();
    }

    private void cleanup() {
        try {
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (IOException ignored) {}

        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {}

        outputStream = null;
        socket = null;
    }
}