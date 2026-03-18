package io.logflux.sdk.payload;

import java.util.List;
import java.util.Map;

/**
 * Minimal hand-written JSON serializer. Zero external dependencies.
 * Handles string, number, boolean, null, Map, and List values.
 */
public final class Json {

    private Json() {}

    /**
     * Serializes a map to a JSON object string.
     */
    public static String toJson(Map<String, Object> map) {
        if (map == null) return "null";
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() == null) continue; // omit null fields
            if (!first) sb.append(',');
            first = false;
            writeString(sb, entry.getKey());
            sb.append(':');
            writeValue(sb, entry.getValue());
        }
        sb.append('}');
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void writeValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String) {
            writeString(sb, (String) value);
        } else if (value instanceof Number) {
            Number n = (Number) value;
            if (value instanceof Double || value instanceof Float) {
                double d = n.doubleValue();
                if (d == Math.floor(d) && !Double.isInfinite(d) && Math.abs(d) < 1e15) {
                    sb.append((long) d);
                } else {
                    sb.append(d);
                }
            } else {
                sb.append(n.longValue());
            }
        } else if (value instanceof Boolean) {
            sb.append(value);
        } else if (value instanceof Map) {
            sb.append(toJson((Map<String, Object>) value));
        } else if (value instanceof List) {
            writeList(sb, (List<?>) value);
        } else {
            writeString(sb, value.toString());
        }
    }

    private static void writeList(StringBuilder sb, List<?> list) {
        sb.append('[');
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(',');
            writeValue(sb, list.get(i));
        }
        sb.append(']');
    }

    public static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b");  break;
                case '\f': sb.append("\\f");  break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    /**
     * Parses a JSON object string into a simple nested map structure.
     * Supports strings, numbers, booleans, null, objects, and arrays.
     * Used for parsing handshake and discovery responses.
     */
    public static Map<String, Object> parseObject(String json) {
        if (json == null || json.trim().isEmpty()) return null;
        JsonParser parser = new JsonParser(json.trim());
        Object result = parser.parseValue();
        if (result instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            return map;
        }
        return null;
    }

    /**
     * Safely get a string value from a parsed JSON map.
     */
    public static String getString(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object v = map.get(key);
        if (v instanceof String) return (String) v;
        return null;
    }

    /**
     * Safely get a nested map from a parsed JSON map.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getObject(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object v = map.get(key);
        if (v instanceof Map) return (Map<String, Object>) v;
        return null;
    }

    /**
     * Safely get an int value from a parsed JSON map.
     */
    public static int getInt(Map<String, Object> map, String key, int defaultValue) {
        if (map == null) return defaultValue;
        Object v = map.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        return defaultValue;
    }

    /**
     * Safely get a boolean value from a parsed JSON map.
     */
    public static boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        if (map == null) return defaultValue;
        Object v = map.get(key);
        if (v instanceof Boolean) return (Boolean) v;
        return defaultValue;
    }

    /**
     * Minimal recursive-descent JSON parser.
     */
    private static class JsonParser {
        private final String input;
        private int pos;

        JsonParser(String input) {
            this.input = input;
            this.pos = 0;
        }

        Object parseValue() {
            skipWhitespace();
            if (pos >= input.length()) return null;
            char c = input.charAt(pos);
            switch (c) {
                case '{': return parseObjectInternal();
                case '[': return parseArray();
                case '"': return parseString();
                case 't': case 'f': return parseBoolean();
                case 'n': return parseNull();
                default:
                    if (c == '-' || (c >= '0' && c <= '9')) return parseNumber();
                    return null;
            }
        }

        private Map<String, Object> parseObjectInternal() {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            pos++; // skip '{'
            skipWhitespace();
            if (pos < input.length() && input.charAt(pos) == '}') { pos++; return map; }
            while (pos < input.length()) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                if (pos < input.length() && input.charAt(pos) == ':') pos++;
                skipWhitespace();
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                if (pos < input.length() && input.charAt(pos) == ',') { pos++; continue; }
                if (pos < input.length() && input.charAt(pos) == '}') { pos++; break; }
                break;
            }
            return map;
        }

        private java.util.List<Object> parseArray() {
            java.util.List<Object> list = new java.util.ArrayList<>();
            pos++; // skip '['
            skipWhitespace();
            if (pos < input.length() && input.charAt(pos) == ']') { pos++; return list; }
            while (pos < input.length()) {
                skipWhitespace();
                list.add(parseValue());
                skipWhitespace();
                if (pos < input.length() && input.charAt(pos) == ',') { pos++; continue; }
                if (pos < input.length() && input.charAt(pos) == ']') { pos++; break; }
                break;
            }
            return list;
        }

        private String parseString() {
            if (pos >= input.length() || input.charAt(pos) != '"') return "";
            pos++; // skip opening quote
            StringBuilder sb = new StringBuilder();
            while (pos < input.length()) {
                char c = input.charAt(pos);
                if (c == '"') { pos++; return sb.toString(); }
                if (c == '\\' && pos + 1 < input.length()) {
                    pos++;
                    char esc = input.charAt(pos);
                    switch (esc) {
                        case '"':  sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/':  sb.append('/'); break;
                        case 'b':  sb.append('\b'); break;
                        case 'f':  sb.append('\f'); break;
                        case 'n':  sb.append('\n'); break;
                        case 'r':  sb.append('\r'); break;
                        case 't':  sb.append('\t'); break;
                        case 'u':
                            if (pos + 4 < input.length()) {
                                String hex = input.substring(pos + 1, pos + 5);
                                sb.append((char) Integer.parseInt(hex, 16));
                                pos += 4;
                            }
                            break;
                        default: sb.append(esc);
                    }
                } else {
                    sb.append(c);
                }
                pos++;
            }
            return sb.toString();
        }

        private Number parseNumber() {
            int start = pos;
            if (pos < input.length() && input.charAt(pos) == '-') pos++;
            while (pos < input.length() && input.charAt(pos) >= '0' && input.charAt(pos) <= '9') pos++;
            boolean isFloat = false;
            if (pos < input.length() && input.charAt(pos) == '.') {
                isFloat = true;
                pos++;
                while (pos < input.length() && input.charAt(pos) >= '0' && input.charAt(pos) <= '9') pos++;
            }
            if (pos < input.length() && (input.charAt(pos) == 'e' || input.charAt(pos) == 'E')) {
                isFloat = true;
                pos++;
                if (pos < input.length() && (input.charAt(pos) == '+' || input.charAt(pos) == '-')) pos++;
                while (pos < input.length() && input.charAt(pos) >= '0' && input.charAt(pos) <= '9') pos++;
            }
            String numStr = input.substring(start, pos);
            if (isFloat) return Double.parseDouble(numStr);
            long val = Long.parseLong(numStr);
            if (val >= Integer.MIN_VALUE && val <= Integer.MAX_VALUE) return (int) val;
            return val;
        }

        private Boolean parseBoolean() {
            if (input.startsWith("true", pos)) { pos += 4; return Boolean.TRUE; }
            if (input.startsWith("false", pos)) { pos += 5; return Boolean.FALSE; }
            return Boolean.FALSE;
        }

        private Object parseNull() {
            if (input.startsWith("null", pos)) { pos += 4; }
            return null;
        }

        private void skipWhitespace() {
            while (pos < input.length()) {
                char c = input.charAt(pos);
                if (c != ' ' && c != '\t' && c != '\n' && c != '\r') break;
                pos++;
            }
        }
    }
}
