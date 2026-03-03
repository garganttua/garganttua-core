package com.garganttua.core.reflection;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Immutable symbolic address for navigating object graphs through field paths.
 *
 * <p>
 * {@code ObjectAddress} represents a dot-separated path for accessing nested
 * fields, map values, and collection elements within object structures. It
 * provides loop detection, path manipulation, and validation capabilities,
 * making it safe for traversing complex object graphs.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Simple field path
 * ObjectAddress address = new ObjectAddress("user.profile.email");
 *
 * // Dynamic path building (returns new instance)
 * ObjectAddress extended = address.addElement("verified");
 * // extended = "user.profile.email.verified"
 * // address  = "user.profile.email"  (unchanged)
 *
 * // Sub-address extraction
 * ObjectAddress partial = address.subAddress(1); // "user.profile"
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * {@code ObjectAddress} instances are immutable and therefore thread-safe.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 * @see IObjectQuery
 */
@Slf4j
public class ObjectAddress implements Cloneable {

    /**
     * Indicator for accessing map keys in an address path.
     */
    public final static String MAP_KEY_INDICATOR = "#key";

    /**
     * Indicator for accessing map values in an address path.
     */
    public final static String MAP_VALUE_INDICATOR = "#value";

    /**
     * Separator character for address elements.
     */
    public final static String ELEMENT_SEPARATOR = ".";

    @Getter
    private final String[] fields;

    private final boolean detectLoops;

    /**
     * Constructs a new object address with optional loop detection.
     *
     * @param address     the dot-separated field path
     * @param detectLoops {@code true} to enable loop detection
     * @throws ReflectionException      if the address contains loops
     * @throws IllegalArgumentException if the address is invalid
     */
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
            detectLoop(this.fields);
        }
        log.atTrace().log("Exiting ObjectAddress constructor");
    }

    /**
     * Constructs a new object address with loop detection enabled.
     *
     * @param address the dot-separated field path
     * @throws ReflectionException      if the address contains loops
     * @throws IllegalArgumentException if the address is invalid
     */
    public ObjectAddress(String address) throws ReflectionException {
        this(address, true);
    }

    /**
     * Internal constructor for creating addresses from pre-built field arrays.
     */
    private ObjectAddress(String[] fields, boolean detectLoops) throws ReflectionException {
        this.fields = fields;
        this.detectLoops = detectLoops;
        if (this.detectLoops) {
            detectLoop(this.fields);
        }
    }

    /**
     * Returns the number of elements in this address path.
     */
    public int length() {
        return fields.length;
    }

    /**
     * Retrieves the field element at the specified index.
     *
     * @param index the zero-based index
     * @return the field name at the specified position
     * @throws IllegalArgumentException if the index is out of bounds
     */
    public String getElement(int index) {
        if (index >= 0 && index < fields.length) {
            return fields[index];
        } else {
            throw new IllegalArgumentException("Index out of bounds");
        }
    }

    @Override
    public String toString() {
        return String.join(ELEMENT_SEPARATOR, fields);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(fields);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        ObjectAddress address = (ObjectAddress) obj;
        return Arrays.equals(fields, address.fields);
    }

    /**
     * Creates a sub-address from the beginning up to and including the specified index.
     *
     * @param endIndex the zero-based index of the last element to include (inclusive)
     * @return a new ObjectAddress containing elements from 0 to endIndex
     * @throws ReflectionException if loop detection fails
     */
    public ObjectAddress subAddress(int endIndex) throws ReflectionException {
        if (endIndex < 0 || endIndex >= fields.length) {
            throw new IllegalArgumentException("Invalid end index");
        }
        String subAddress = String.join(ELEMENT_SEPARATOR, Arrays.copyOfRange(fields, 0, endIndex + 1));
        return new ObjectAddress(subAddress);
    }

    /**
     * Detects loops (circular references) in the given fields.
     */
    private static void detectLoop(String[] fields) throws ReflectionException {
        Set<String> visitedElements = Collections.synchronizedSet(new HashSet<>());
        boolean loop = Arrays.stream(fields)
                .parallel()
                .filter(field -> !field.equals(MAP_KEY_INDICATOR) && !field.equals(MAP_VALUE_INDICATOR))
                .anyMatch(field -> !visitedElements.add(field));
        if (loop) {
            throw new ReflectionException("Loop detected! " + String.join(ELEMENT_SEPARATOR, fields));
        }
    }

    /**
     * Returns a new ObjectAddress with the given element appended.
     *
     * <p>
     * This instance is <b>not</b> modified (immutable).
     * </p>
     *
     * @param newElement the field name to append
     * @return a new ObjectAddress with the element appended
     * @throws ReflectionException      if adding the element creates a loop
     * @throws IllegalArgumentException if newElement is null or empty
     */
    public ObjectAddress addElement(String newElement) throws ReflectionException {
        if (newElement == null || newElement.isEmpty()) {
            throw new IllegalArgumentException("Element cannot be null or empty");
        }
        String[] newFields = Arrays.copyOf(fields, fields.length + 1);
        newFields[newFields.length - 1] = newElement;
        return new ObjectAddress(newFields, this.detectLoops);
    }

    /**
     * Creates a copy of this object address.
     * Since ObjectAddress is immutable, returns {@code this}.
     *
     * @return this instance
     */
    @Override
    public ObjectAddress clone() {
        return this;
    }

    /**
     * Returns the last element in the address path.
     */
    public String getLastElement() {
        return this.fields[this.fields.length - 1];
    }
}
