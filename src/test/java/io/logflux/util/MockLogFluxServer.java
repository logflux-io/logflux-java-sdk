package io.logflux.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.logflux.models.LogEntry;
import io.logflux.models.LogResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Mock LogFlux server for testing purposes.
 */
public class MockLogFluxServer {
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final List<LogEntry> receivedLogs = Collections.synchronizedList(new ArrayList<>());
    private final AtomicLong idCounter = new AtomicLong(1);
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;
    private ServerSocket serverSocket;
    private Thread serverThread;
    private int port;
    private boolean shouldFail = false;
    private int failureCount = 0;
    private int currentFailures = 0;

    public MockLogFluxServer() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.executorService = Executors.newCachedThreadPool();
    }

    /**
     * Starts the mock server on an available port.
     *
     * @return The port number the server is listening on
     * @throws IOException if the server cannot be started
     */
    public int start() throws IOException {
        if (running.get()) {
            throw new IllegalStateException("Server is already running");
        }

        serverSocket = new ServerSocket(0); // Use any available port
        port = serverSocket.getLocalPort();
        running.set(true);

        serverThread = new Thread(this::serverLoop);
        serverThread.setDaemon(true);
        serverThread.start();

        return port;
    }

    /**
     * Stops the mock server.
     */
    public void stop() {
        if (!running.get()) {
            return;
        }

        running.set(false);
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            // Ignore
        }

        if (serverThread != null) {
            try {
                serverThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        executorService.shutdown();
    }

    /**
     * Configures the server to fail requests.
     *
     * @param shouldFail    Whether to fail requests
     * @param failureCount  Number of requests to fail
     */
    public void configureFailures(boolean shouldFail, int failureCount) {
        this.shouldFail = shouldFail;
        this.failureCount = failureCount;
        this.currentFailures = 0;
    }

    /**
     * Gets the server URL.
     *
     * @return The server URL
     */
    public String getUrl() {
        return "http://localhost:" + port;
    }

    /**
     * Gets the port number.
     *
     * @return The port number
     */
    public int getPort() {
        return port;
    }

    /**
     * Gets all received log entries.
     *
     * @return List of received log entries
     */
    public List<LogEntry> getReceivedLogs() {
        return new ArrayList<>(receivedLogs);
    }

    /**
     * Clears all received log entries.
     */
    public void clearLogs() {
        receivedLogs.clear();
    }

    /**
     * Gets the number of received log entries.
     *
     * @return The count of received logs
     */
    public int getLogCount() {
        return receivedLogs.size();
    }

    /**
     * Main server loop that accepts and handles connections.
     */
    private void serverLoop() {
        while (running.get()) {
            try {
                Socket clientSocket = serverSocket.accept();
                executorService.submit(() -> handleClient(clientSocket));
            } catch (IOException e) {
                if (running.get()) {
                    System.err.println("Error accepting connection: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Handles a client connection.
     *
     * @param clientSocket The client socket
     */
    private void handleClient(Socket clientSocket) {
        try {
            // Read HTTP request headers
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            StringBuilder headers = new StringBuilder();
            String line;
            int contentLength = 0;
            
            // Read headers
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                headers.append(line).append("\r\n");
                if (line.toLowerCase().startsWith("content-length:")) {
                    contentLength = Integer.parseInt(line.substring(15).trim());
                }
            }
            headers.append("\r\n");
            
            // Read body if present
            StringBuilder body = new StringBuilder();
            if (contentLength > 0) {
                char[] bodyBuffer = new char[contentLength];
                int totalRead = 0;
                while (totalRead < contentLength) {
                    int read = reader.read(bodyBuffer, totalRead, contentLength - totalRead);
                    if (read == -1) break;
                    totalRead += read;
                }
                body.append(bodyBuffer, 0, totalRead);
            }
            
            String requestStr = headers.toString() + body.toString();
            String[] lines = requestStr.split("\r\n");
            String requestLine = lines[0];
            String[] requestParts = requestLine.split(" ");
            
            if (requestParts.length < 2) {
                sendErrorResponse(clientSocket, 400, "Bad Request");
                return;
            }

            String method = requestParts[0];
            String path = requestParts[1];

            // Check if we should fail this request
            if (shouldFail && currentFailures < failureCount) {
                currentFailures++;
                sendErrorResponse(clientSocket, 500, "Internal Server Error");
                return;
            }

            // Route the request
            if ("GET".equals(method)) {
                handleGetRequest(clientSocket, path);
            } else if ("POST".equals(method)) {
                handlePostRequest(clientSocket, path, requestStr);
            } else {
                sendErrorResponse(clientSocket, 405, "Method Not Allowed");
            }

        } catch (Exception e) {
            System.err.println("Error handling client: " + e.getMessage());
            try {
                sendErrorResponse(clientSocket, 500, "Internal Server Error");
            } catch (IOException ioException) {
                // Ignore
            }
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    /**
     * Handles GET requests.
     *
     * @param clientSocket The client socket
     * @param path         The request path
     */
    private void handleGetRequest(Socket clientSocket, String path) throws IOException {
        if ("/health".equals(path)) {
            sendResponse(clientSocket, 200, "OK");
        } else if ("/version".equals(path)) {
            Map<String, Object> version = Map.of(
                    "api_version", "1.0.0",
                    "service", "logflux-ingestor",
                    "supported_versions", List.of("v1"),
                    "deprecated_versions", List.of()
            );
            sendJsonResponse(clientSocket, 200, version);
        } else {
            sendErrorResponse(clientSocket, 404, "Not Found");
        }
    }

    /**
     * Handles POST requests.
     *
     * @param clientSocket The client socket
     * @param path         The request path
     * @param requestStr   The full request string
     */
    private void handlePostRequest(Socket clientSocket, String path, String requestStr) throws IOException {
        if ("/v1/logs".equals(path)) {
            handleSingleLogRequest(clientSocket, requestStr);
        } else if ("/v1/logs/batch".equals(path)) {
            handleBatchLogRequest(clientSocket, requestStr);
        } else {
            sendErrorResponse(clientSocket, 404, "Not Found");
        }
    }

    /**
     * Handles single log entry requests.
     *
     * @param clientSocket The client socket
     * @param requestStr   The full request string
     */
    private void handleSingleLogRequest(Socket clientSocket, String requestStr) throws IOException {
        String body = extractRequestBody(requestStr);
        if (body == null) {
            sendErrorResponse(clientSocket, 400, "Bad Request");
            return;
        }

        try {
            LogEntry logEntry = objectMapper.readValue(body, LogEntry.class);
            receivedLogs.add(logEntry);

            LogResponse response = new LogResponse("accepted", idCounter.getAndIncrement(), Instant.now(), null);
            sendJsonResponse(clientSocket, 200, response);
        } catch (Exception e) {
            sendErrorResponse(clientSocket, 400, "Invalid JSON");
        }
    }

    /**
     * Handles batch log entry requests.
     *
     * @param clientSocket The client socket
     * @param requestStr   The full request string
     */
    private void handleBatchLogRequest(Socket clientSocket, String requestStr) throws IOException {
        String body = extractRequestBody(requestStr);
        if (body == null) {
            sendErrorResponse(clientSocket, 400, "Bad Request");
            return;
        }

        try {
            LogEntry[] logEntries = objectMapper.readValue(body, LogEntry[].class);
            List<LogResponse> responses = new ArrayList<>();

            for (LogEntry logEntry : logEntries) {
                receivedLogs.add(logEntry);
                responses.add(new LogResponse("accepted", idCounter.getAndIncrement(), Instant.now(), null));
            }

            sendJsonResponse(clientSocket, 200, responses);
        } catch (Exception e) {
            sendErrorResponse(clientSocket, 400, "Invalid JSON");
        }
    }

    /**
     * Extracts the request body from the HTTP request string.
     *
     * @param requestStr The full request string
     * @return The request body or null if not found
     */
    private String extractRequestBody(String requestStr) {
        String[] parts = requestStr.split("\r\n\r\n");
        return parts.length > 1 ? parts[1] : null;
    }

    /**
     * Sends a simple text response.
     *
     * @param clientSocket The client socket
     * @param statusCode   The HTTP status code
     * @param body         The response body
     */
    private void sendResponse(Socket clientSocket, int statusCode, String body) throws IOException {
        String response = String.format(
                "HTTP/1.1 %d %s\r\n" +
                        "Content-Length: %d\r\n" +
                        "Content-Type: text/plain\r\n" +
                        "\r\n" +
                        "%s",
                statusCode, getStatusText(statusCode), body.length(), body
        );

        clientSocket.getOutputStream().write(response.getBytes());
        clientSocket.getOutputStream().flush();
    }

    /**
     * Sends a JSON response.
     *
     * @param clientSocket The client socket
     * @param statusCode   The HTTP status code
     * @param object       The object to serialize as JSON
     */
    private void sendJsonResponse(Socket clientSocket, int statusCode, Object object) throws IOException {
        String json = objectMapper.writeValueAsString(object);
        String response = String.format(
                "HTTP/1.1 %d %s\r\n" +
                        "Content-Length: %d\r\n" +
                        "Content-Type: application/json\r\n" +
                        "\r\n" +
                        "%s",
                statusCode, getStatusText(statusCode), json.length(), json
        );

        clientSocket.getOutputStream().write(response.getBytes());
        clientSocket.getOutputStream().flush();
    }

    /**
     * Sends an error response.
     *
     * @param clientSocket The client socket
     * @param statusCode   The HTTP status code
     * @param message      The error message
     */
    private void sendErrorResponse(Socket clientSocket, int statusCode, String message) throws IOException {
        sendResponse(clientSocket, statusCode, message);
    }

    /**
     * Gets the status text for a given HTTP status code.
     *
     * @param statusCode The HTTP status code
     * @return The status text
     */
    private String getStatusText(int statusCode) {
        switch (statusCode) {
            case 200: return "OK";
            case 400: return "Bad Request";
            case 404: return "Not Found";
            case 405: return "Method Not Allowed";
            case 500: return "Internal Server Error";
            default: return "Unknown";
        }
    }
}