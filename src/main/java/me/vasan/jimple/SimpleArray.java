package me.vasan.jimple;

import java.util.ArrayList;
import java.util.List;

public class SimpleArray {
    private List<Object> elements;

    public SimpleArray() {
        this.elements = new ArrayList<>();
    }

    public SimpleArray(List<Object> elements) {
        this.elements = new ArrayList<>(elements);
    }

    public Object get(int index) {
        if (index < 0 || index >= elements.size()) {
            return null; // Or throw an exception based on language semantics
        }
        return elements.get(index);
    }

    public void set(int index, Object value) {
        if (index < 0) {
            return; // Or throw an exception
        }
        
        // Expand array if necessary
        while (index >= elements.size()) {
            elements.add(null);
        }
        
        elements.set(index, value);
    }

    public void add(Object value) {
        elements.add(value);
    }

    public int size() {
        return elements.size();
    }

    public Object remove(int index) {
        if (index < 0 || index >= elements.size()) {
            return null;
        }
        return elements.remove(index);
    }

    public List<Object> getElements() {
        return new ArrayList<>(elements);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < elements.size(); i++) {
            Object value = elements.get(i);
            if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else {
                sb.append(value == null ? "nil" : value.toString());
            }
            if (i < elements.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        SimpleArray that = (SimpleArray) other;
        return elements.equals(that.elements);
    }

    @Override
    public int hashCode() {
        return elements.hashCode();
    }
}