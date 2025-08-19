package io.logflux.agent;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LogFluxClientTest {

    @Test
    void testUnixSocketClientCreation() {
        LogFluxClient client = new LogFluxClient("/tmp/logflux.sock");
        assertNotNull(client);
        assertFalse(client.isConnected());
    }

    @Test
    void testTcpClientCreation() {
        LogFluxClient client = new LogFluxClient("localhost", 8080);
        assertNotNull(client);
        assertFalse(client.isConnected());
    }

    @Test
    void testSendLogEntryWhenNotConnected() {
        LogFluxClient client = new LogFluxClient("/tmp/logflux.sock");
        LogEntry entry = new LogEntry("test message");
        
        assertThrows(IllegalStateException.class, () -> client.sendLogEntry(entry));
    }

    @Test
    void testCloseClient() {
        LogFluxClient client = new LogFluxClient("/tmp/logflux.sock");
        client.close(); // Should not throw exception
        assertFalse(client.isConnected());
    }

    @Test
    void testAutoCloseable() {
        assertDoesNotThrow(() -> {
            try (LogFluxClient client = new LogFluxClient("/tmp/logflux.sock")) {
                // Client should close automatically
            }
        });
    }
}