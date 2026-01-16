package com.garganttua.core.reflection;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Symbolic address for navigating object graphs through field paths.
 *
 * <p>
 * {@code ObjectAddress} represents a dot-separated path for accessing nested
 * fields,
 * map values, and collection elements within object structures. It provides
 * loop
 * detection, path manipulation, and validation capabilities, making it safe for
 * traversing complex object graphs. This is fundamental for property path
 * expressions,
 * data binding, and reflective field access.
 * </p>
 *
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * // Simple field path
 * ObjectAddress address = new ObjectAddress("user.profile.email");
 * // Navigates: object.getUser().getProfile().getEmail()
 *
 * // Map navigation
 * ObjectAddress mapAddress = new ObjectAddress("config.properties.database#key");
 * // Navigates to map key in config.properties.database
 *
 * ObjectAddress mapValueAddress = new ObjectAddress("cache.users.admin#value");
 * // Navigates to map value for key "admin"
 *
 * // Sub-address extraction
 * ObjectAddress fullPath = new ObjectAddress("app.module.service.method");
 * ObjectAddress partial = fullPath.subAddress(2); // "app.module.service"
 *
 * // Dynamic path building
 * ObjectAddress dynamic = new ObjectAddress("user.profile");
 * dynamic.addElement("contactInfo");
 * dynamic.addElement("phoneNumber");
 * // Results in: "user.profile.contactInfo.phoneNumber"
 * }</pre>
 *
 * <h2>Address Format</h2>
 * <ul>
 * <li><b>Field navigation</b>: "field1.field2.field3" - Dot-separated field
 * names</li>
 * <li><b>Map keys</b>: "map#key" - Access map keys</li>
 * <li><b>Map values</b>: "map#value" - Access map values</li>
 * <li><b>Combined</b>: "user.settings.theme#value" - Nested navigation with map
 * access</li>
 * </ul>
 *
 * <h2>Loop Detection</h2>
 * <p>
 * The address validates against circular references during construction and
 * modification.
 * This prevents infinite loops when traversing object graphs with cyclic
 * dependencies.
 * Loop detection can be disabled for performance when paths are known to be
 * safe.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * {@code ObjectAddress} instances are mutable but use synchronized collections
 * during
 * loop detection. For concurrent use, create separate instances per thread or
 * use
 * the {@link #clone()} method.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 * @see IObjectQuery
 */
@Slf4j
public class ObjectAddress implements Cloneable {

    /**
     * Indicator for accessing map keys in an address path.
     * Use "field#key" to access the keys of a map field.
     */
    public final static String MAP_KEY_INDICATOR = "#key";

    /**
     * Indicator for accessing map values in an address path.
     * Use "field#value" to access the values of a map field.
     */
    public final static String MAP_VALUE_INDICATOR = "#value";

    /**
     * Separator character for address elements.
     * Fields are separated by dots in address strings.
     */
    public final static String ELEMENT_SEPARATOR = ".";

    /**
     * Array of field names representing the address path.
     * Retrieved via Lombok-generated getter.
     */
    @Getter
    private String[] fields;

    private boolean detectLoops = true;

    /**
     * Constructs a new object address with optional loop detection.
     *
     * @param address     the dot-separated field path (must not be null, empty, or
     *                    start/end with dots)
     * @param detectLoops {@code true} to enable loop detection, {@code false} to
     *                    disable
     * @throws ReflectionException      if the address is invalid or contains loops
     *                                  (when detection enabled)
     * @throws IllegalArgumentException if the address is null, empty, or has
     *                                  invalid format
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
            try {
                this.detectLoop();
            } catch (ReflectionException e) {
                log.atError().log("Loop detected in constructor", e);
                throw e;
            }
        }
        log.atTrace().log("Exiting ObjectAddress constructor");
    }

    /**
     * Constructs a new object address with loop detection enabled.
     *
     * @param address the dot-separated field path (must not be null, empty, or
     *                start/end with dots)
     * @throws ReflectionException      if the address is invalid or contains loops
     * @throws IllegalArgumentException if the address is null, empty, or has
     *                                  invalid format
     */
    public ObjectAddress(String address) throws ReflectionException {
        this(address, true);
    }

    /**
     * Returns the number of elements in this address path.
     *
     * @return the count of field elements in the path
     */
    public int length() {
        log.atTrace().log("length() called, returning {}", fields.length);
        return fields.length;
    }

    /**
     * Retrieves the field element at the specified index.
     *
     * @param index the zero-based index of the element to retrieve
     * @return the field name at the specified position
     * @throws IllegalArgumentException if the index is out of bounds
     */
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

    /**
     * Returns the string representation of this address.
     *
     * @return the dot-separated field path (e.g., "user.profile.email")
     */
    @Override
    public String toString() {
        String result = String.join(ELEMENT_SEPARATOR, fields);
        log.atTrace().log("toString() called, returning '{}'", result);
        return result;
    }

    /**
     * Computes the hash code for this address.
     *
     * @return hash code based on the field array
     */
    @Override
    public int hashCode() {
        int hash = Arrays.hashCode(fields);
        log.atTrace().log("hashCode() called, returning {}", hash);
        return hash;
    }

    /**
     * Compares this address with another object for equality.
     *
     * @param obj the object to compare with
     * @return {@code true} if the objects represent the same address path
     */
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

    /**
     * Creates a sub-address from the beginning up to and including the specified
     * index.
     *
     * @param endIndex the zero-based index of the last element to include
     *                 (inclusive)
     * @return a new ObjectAddress containing elements from 0 to endIndex
     * @throws ReflectionException      if loop detection fails on the new address
     * @throws IllegalArgumentException if endIndex is invalid
     */
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

    /**
     * Detects loops (circular references) in the address path.
     *
     * @throws ReflectionException if a loop is detected (same field appears
     *                             multiple times)
     */
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

    /**
     * Adds a new element to the end of this address path.
     *
     * <p>
     * This method mutates the current address by appending a new field element.
     * Loop detection is performed if enabled.
     * </p>
     *
     * @param newElement the field name to append (must not be null or empty)
     * @return this address instance for method chaining
     * @throws ReflectionException      if adding the element creates a loop (when
     *                                  detection enabled)
     * @throws IllegalArgumentException if newElement is null or empty
     */
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

    /**
     * Creates a deep copy of this object address.
     *
     * @return a new ObjectAddress with the same field path
     * @throws IllegalArgumentException if cloning fails due to address validation
     */
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

    public String getLastElement() {
        return this.fields[this.fields.length - 1];
    }
}
