package me.vasan.jimple;

import java.util.HashMap;
import java.util.Map;

public class SimpleObject {
    private Map<String, Object> properties;

    public SimpleObject() {
        this.properties = new HashMap<>();
    }

    public SimpleObject(Map<String, Object> properties) {
        this.properties = new HashMap<>(properties);
    }

    public Object get(String key) {
        return properties.get(key);
    }

    public void set(String key, Object value) {
        properties.put(key, value);
    }

    public boolean has(String key) {
        return properties.containsKey(key);
    }

    public Object remove(String key) {
        return properties.remove(key);
    }

    public Map<String, Object> getProperties() {
        return new HashMap<>(properties);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(entry.getKey()).append(": ");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else {
                sb.append(value == null ? "nil" : value.toString());
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        SimpleObject that = (SimpleObject) other;
        return properties.equals(that.properties);
    }

    @Override
    public int hashCode() {
        return properties.hashCode();
    }
}