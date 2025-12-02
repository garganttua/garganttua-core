package com.garganttua.core.mapper;

import com.garganttua.core.CoreException;

/**
 * Exception thrown when an error occurs during object mapping operations.
 * <p>
 * This exception indicates failures in the mapping process, such as:
 * </p>
 * <ul>
 *   <li>Invalid mapping configuration</li>
 *   <li>Type conversion failures</li>
 *   <li>Missing or inaccessible fields</li>
 *   <li>Instantiation errors</li>
 *   <li>Custom mapping rule execution failures</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>
 * try {
 *     UserDTO dto = mapper.map(user, UserDTO.class);
 * } catch (MapperException e) {
 *     System.err.println("Mapping failed: " + e.getMessage());
 *     // Handle mapping error
 * }
 *
 * // In custom mapping rule
 * IMappingRuleExecutor customRule = (destClass, destObj, sourceObj) -&gt; {
 *     if (sourceObj == null) {
 *         throw new MapperException("Source object cannot be null");
 *     }
 *     // Perform transformation
 *     return transformed;
 * };
 * </pre>
 *
 * @since 2.0.0-ALPHA01
 */
public class MapperException extends CoreException {
    private static final long serialVersionUID = 3629256996026750672L;

    /**
     * Creates a new MapperException with a descriptive message.
     *
     * @param string the error message describing the mapping failure
     */
    public MapperException(String string) {
        super(CoreException.MAPPER_ERROR, string);
    }

    /**
     * Creates a new MapperException with a message and a cause.
     *
     * @param string the error message describing the mapping failure
     * @param e the underlying exception that caused the mapping failure
     */
    public MapperException(String string, Exception e) {
        super(CoreException.MAPPER_ERROR, string, e);
    }

    /**
     * Creates a new MapperException wrapping an existing exception.
     *
     * @param e the underlying exception that caused the mapping failure
     */
    public MapperException(Exception e) {
        super(e);
    }
}
