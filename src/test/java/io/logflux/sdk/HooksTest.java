package io.logflux.sdk;

import io.logflux.sdk.payload.Json;
import io.logflux.sdk.payload.PayloadLog;
import io.logflux.sdk.payload.PayloadContext;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class HooksTest {

    @Test
    void beforeSendCanModifyPayload() {
        Function<Map<String, Object>, Map<String, Object>> hook = p -> {
            p.put("custom_field", "added_by_hook");
            return p;
        };

        Map<String, Object> payload = PayloadLog.create("src", "test", LogLevel.INFO, null);
        payload = hook.apply(payload);

        assertNotNull(payload);
        assertEquals("added_by_hook", payload.get("custom_field"));
    }

    @Test
    void beforeSendCanDropPayload() {
        Function<Map<String, Object>, Map<String, Object>> hook = p -> null; // drop

        Map<String, Object> payload = PayloadLog.create("src", "test", LogLevel.INFO, null);
        payload = hook.apply(payload);

        assertNull(payload);
    }

    @Test
    void beforeSendCanFilterByLevel() {
        Function<Map<String, Object>, Map<String, Object>> hook = p -> {
            Object level = p.get("level");
            if (level instanceof Number && ((Number) level).intValue() > LogLevel.WARNING) {
                return null; // drop info/debug
            }
            return p;
        };

        // Info (7) - should be dropped
        Map<String, Object> infoPayload = PayloadLog.create("src", "info msg", LogLevel.INFO, null);
        assertNull(hook.apply(infoPayload));

        // Error (4) - should pass
        Map<String, Object> errorPayload = PayloadLog.create("src", "error msg", LogLevel.ERROR, null);
        assertNotNull(hook.apply(errorPayload));
    }

    @Test
    void beforeSendCanAddAttributes() {
        Function<Map<String, Object>, Map<String, Object>> hook = p -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> attrs = (Map<String, Object>) p.get("attributes");
            if (attrs == null) {
                attrs = new HashMap<>();
                p.put("attributes", attrs);
            }
            attrs.put("hostname", "server-01");
            return p;
        };

        Map<String, Object> payload = PayloadLog.create("src", "test", LogLevel.INFO, null);
        payload = hook.apply(payload);

        @SuppressWarnings("unchecked")
        Map<String, Object> attrs = (Map<String, Object>) payload.get("attributes");
        assertEquals("server-01", attrs.get("hostname"));
    }

    @Test
    void beforeSendCanScrubSensitiveData() {
        Function<Map<String, Object>, Map<String, Object>> hook = p -> {
            String msg = (String) p.get("message");
            if (msg != null) {
                p.put("message", msg.replaceAll("\\d{4}-\\d{4}-\\d{4}-\\d{4}", "[REDACTED]"));
            }
            return p;
        };

        Map<String, Object> payload = PayloadLog.create("src",
                "Card: 1234-5678-9012-3456", LogLevel.INFO, null);
        payload = hook.apply(payload);
        assertEquals("Card: [REDACTED]", payload.get("message"));
    }

    @Test
    void payloadContextAppliesGlobalContext() {
        PayloadContext.configure("global-source", "production", "v1.0.0");

        Map<String, Object> payload = PayloadLog.create("", "test", LogLevel.INFO, null);
        PayloadContext.apply(payload);

        assertEquals("global-source", payload.get("source"));
        @SuppressWarnings("unchecked")
        Map<String, Object> meta = (Map<String, Object>) payload.get("meta");
        assertNotNull(meta);
        assertEquals("production", meta.get("environment"));
        assertEquals("v1.0.0", meta.get("release"));

        PayloadContext.reset();
    }

    @Test
    void payloadContextDoesNotOverwriteExisting() {
        PayloadContext.configure("default-source", "staging", "v2.0.0");

        Map<String, Object> payload = PayloadLog.create("custom-source", "test", LogLevel.INFO, null);
        Map<String, Object> meta = new HashMap<>();
        meta.put("environment", "custom-env");
        payload.put("meta", meta);
        PayloadContext.apply(payload);

        // Should keep custom-source, not overwrite
        assertEquals("custom-source", payload.get("source"));
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMeta = (Map<String, Object>) payload.get("meta");
        assertEquals("custom-env", resultMeta.get("environment"));
        // Release should be added since it wasn't set
        assertEquals("v2.0.0", resultMeta.get("release"));

        PayloadContext.reset();
    }
}
