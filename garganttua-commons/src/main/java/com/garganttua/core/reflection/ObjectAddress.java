package com.garganttua.core.reflection;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ObjectAddress implements Cloneable {

    public final static String MAP_KEY_INDICATOR = "#key";
    public final static String MAP_VALUE_INDICATOR = "#value";
    public final static String ELEMENT_SEPARATOR = ".";

    @Getter
    private String[] fields;
    private boolean detectLoops = true;

    public ObjectAddress(String address, boolean detectLoops) throws ReflectionException {
        log.atTrace().log("Entering ObjectAddress constructor with address='{}', detectLoops={}", address, detectLoops);
        this.detectLoops = detectLoops;
        if (address == null || address.startsWith(".") || address.endsWith(".") || address.isEmpty()) {
            log.atError().log("Invalid address: '{}'", address);
            throw new IllegalArgumentException("Address cannot start or end with a dot, or be empty");
        }
        this.fields = address.split("\\.");
        log.atDebug().log("Parsed fields: {}", Arrays.toString(fields));

        if (this.detectLoops) {
            try {
                this.detectLoop();
            } catch (ReflectionException e) {
                log.atError().log("Loop detected in constructor", e);
                throw e;
            }
        }
        log.atTrace().log("Exiting ObjectAddress constructor");
    }

    public ObjectAddress(String address) throws ReflectionException {
        this(address, true);
    }

    public int length() {
        log.atTrace().log("length() called, returning {}", fields.length);
        return fields.length;
    }

    public String getElement(int index) {
        log.atTrace().log("getElement() called with index={}", index);
        if (index >= 0 && index < fields.length) {
            String element = fields[index];
            log.atDebug().log("Returning element: {}", element);
            return element;
        } else {
            log.atError().log("Index out of bounds: {}", index);
            throw new IllegalArgumentException("Index out of bounds");
        }
    }

    @Override
    public String toString() {
        String result = String.join(ELEMENT_SEPARATOR, fields);
        log.atTrace().log("toString() called, returning '{}'", result);
        return result;
    }

    @Override
    public int hashCode() {
        int hash = Arrays.hashCode(fields);
        log.atTrace().log("hashCode() called, returning {}", hash);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        log.atTrace().log("equals() called with obj={}", obj);
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        ObjectAddress address = (ObjectAddress) obj;
        boolean eq = Arrays.equals(fields, address.fields);
        log.atDebug().log("Equality result: {}", eq);
        return eq;
    }

    public ObjectAddress subAddress(int endIndex) throws ReflectionException {
        log.atTrace().log("subAddress() called with endIndex={}", endIndex);
        if (endIndex < 0 || endIndex >= fields.length) {
            log.atError().log("Invalid end index: {}", endIndex);
            throw new IllegalArgumentException("Invalid end index");
        }
        String subAddress = String.join(ELEMENT_SEPARATOR, Arrays.copyOfRange(fields, 0, endIndex + 1));
        log.atDebug().log("Created subAddress: {}", subAddress);
        return new ObjectAddress(subAddress);
    }

    private void detectLoop() throws ReflectionException {
        log.atTrace().log("detectLoop() called");
        Set<String> visitedElements = Collections.synchronizedSet(new HashSet<>());
        boolean loop = Arrays.stream(fields)
                .parallel()
                .filter(field -> !field.equals(MAP_KEY_INDICATOR) && !field.equals(MAP_VALUE_INDICATOR))
                .anyMatch(field -> !visitedElements.add(field));
        if (loop) {
            log.atError().log("Loop detected in fields: {}", Arrays.toString(fields));
            throw new ReflectionException("Loop detected! " + this.toString());
        }
        log.atDebug().log("No loop detected");
    }

    public ObjectAddress addElement(String newElement) throws ReflectionException {
        log.atTrace().log("addElement() called with newElement='{}'", newElement);
        if (newElement == null || newElement.isEmpty()) {
            log.atError().log("Element cannot be null or empty");
            throw new IllegalArgumentException("Element cannot be null or empty");
        }

        String[] newFields = Arrays.copyOf(fields, fields.length + 1);
        newFields[newFields.length - 1] = newElement;
        this.fields = newFields;
        log.atDebug().log("Fields after adding new element: {}", Arrays.toString(fields));

        if (this.detectLoops) {
            try {
                this.detectLoop();
            } catch (ReflectionException e) {
                log.atError().log("Loop detected after adding element '{}'", newElement, e);
                throw e;
            }
        }
        log.atTrace().log("Exiting addElement()");
        return this;
    }

    @Override
    public ObjectAddress clone() {
        log.atTrace().log("clone() called");
        try {
            ObjectAddress clone = new ObjectAddress(this.toString());
            log.atDebug().log("Clone created: {}", clone);
            return clone;
        } catch (ReflectionException e) {
            log.atError().log("Exception during clone", e);
            throw new IllegalArgumentException(e);
        }
    }
}
