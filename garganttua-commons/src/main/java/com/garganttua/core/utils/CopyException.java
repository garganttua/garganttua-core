package com.garganttua.core.utils;

import com.garganttua.core.CoreException;

/**
 * Exception thrown when an error occurs during a copy operation.
 *
 * <p>
 * {@code CopyException} is thrown by implementations of the {@link Copyable} interface
 * when an object cannot be successfully copied. This can occur due to various reasons
 * such as reflection errors, cloning failures, or serialization issues.
 * </p>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * </p>
 * <pre>{@code
 * public class ComplexObject implements Copyable<ComplexObject> {
 *     private Object internalState;
 *
 *     @Override
 *     public ComplexObject copy() throws CopyException {
 *         try {
 *             // Attempt to copy using serialization
 *             ByteArrayOutputStream baos = new ByteArrayOutputStream();
 *             ObjectOutputStream oos = new ObjectOutputStream(baos);
 *             oos.writeObject(this);
 *             oos.close();
 *
 *             ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
 *             ObjectInputStream ois = new ObjectInputStream(bais);
 *             return (ComplexObject) ois.readObject();
 *         } catch (IOException | ClassNotFoundException e) {
 *             throw new CopyException(e);
 *         }
 *     }
 * }
 *
 * // Handling copy exceptions
 * try {
 *     ComplexObject copy = original.copy();
 * } catch (CopyException e) {
 *     logger.error("Failed to copy object", e);
 * }
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see Copyable
 * @see CoreException
 */
public class CopyException extends CoreException {

    /**
     * Constructs a new CopyException with the specified detail message.
     *
     * @param string the detail message explaining the copy failure
     */
    public CopyException(String string) {
        super(CoreException.COPY_ERROR, string);
    }

    /**
     * Constructs a new CopyException with the specified cause.
     *
     * @param e the exception that caused the copy failure
     */
    public CopyException(Exception e) {
        super(CoreException.COPY_ERROR, e);
    }

}
