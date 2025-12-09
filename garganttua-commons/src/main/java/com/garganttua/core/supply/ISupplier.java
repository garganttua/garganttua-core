package com.garganttua.core.supply;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Optional;

import com.garganttua.core.supply.dsl.ISupplierBuilder;

/**
 * Supplier interface for providing object instances on demand.
 *
 * <p>
 * {@code ISupplier} defines a contract for objects that can provide instances
 * of a specific type. Unlike standard Java suppliers, this interface supports optional
 * return values, exception handling, and type introspection. Suppliers can be stateless
 * (returning new instances each time) or stateful (caching and reusing instances).
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Simple supplier implementation
 * ISupplier<DatabaseConnection> supplier = new ISupplier<>() {
 *     @Override
 *     public Optional<DatabaseConnection> supply() throws SupplyException {
 *         try {
 *             return Optional.of(new DatabaseConnection("jdbc:mysql://localhost:3306/db"));
 *         } catch (SQLException e) {
 *             throw new SupplyException(e);
 *         }
 *     }
 *
 *     @Override
 *     public Type getSuppliedType() {
 *         return DatabaseConnection.class;
 *     }
 * };
 *
 * // Using the supplier
 * Optional<DatabaseConnection> connection = supplier.supply();
 * connection.ifPresent(conn -> {
 *     // Use the connection
 * });
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Thread safety depends on the implementation. Stateless suppliers are typically
 * thread-safe, while stateful suppliers may require synchronization.
 * </p>
 *
 * @param <Supplied> the type of object this supplier provides
 * @since 2.0.0-ALPHA01
 * @see IContextualSupplier
 * @see Supplier
 */
public interface ISupplier<Supplied> {

    /**
     * Supplies an instance of the specified type.
     *
     * <p>
     * This method creates or retrieves an instance of the supplied type. The returned
     * {@link Optional} will be empty if the supplier cannot provide an instance
     * (e.g., due to configuration, availability, or conditional logic).
     * </p>
     *
     * @return an {@link Optional} containing the supplied instance, or empty if unavailable
     * @throws SupplyException if an error occurs during instance creation or retrieval
     */
    Optional<Supplied> supply() throws SupplyException;

    /**
     * Returns the runtime type of objects supplied by this supplier.
     *
     * <p>
     * This method enables type introspection and validation without requiring
     * an actual instance to be created.
     * </p>
     *
     * @return the {@link Type} representing the supplied type
     */
    Type getSuppliedType();

    /**
     * Returns the runtime class of objects supplied by this supplier.
     *
     * <p>
     * This method extracts the raw {@link Class} from the {@link Type} returned
     * by {@link #getSuppliedType()}. It handles parameterized types, arrays,
     * type variables, and wildcards.
     * </p>
     *
     * @return the {@link Class} object representing the supplied type
     */
    @SuppressWarnings("unchecked")
    default Class<Supplied> getSuppliedClass() {
        Type type = this.getSuppliedType();
        return (Class<Supplied>) ISupplierBuilder.extractClass(type);
    }

}
