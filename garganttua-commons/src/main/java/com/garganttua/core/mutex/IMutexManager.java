package com.garganttua.core.mutex;

import java.util.Optional;

import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.SupplyException;

/**
 * Manager interface for creating and retrieving named mutexes.
 *
 * <p>
 * {@code IMutexManager} provides a registry of mutexes identified by string names.
 * It extends {@link IContextualSupplier} to integrate with the Garganttua supply framework,
 * allowing mutexes to be injected and managed as contextual resources.
 * </p>
 *
 * <h2>Usage Patterns</h2>
 * <ul>
 *   <li>Centralized mutex management across application</li>
 *   <li>Named mutex retrieval for specific resources</li>
 *   <li>Integration with dependency injection contexts</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see IMutex
 * @see IContextualSupplier
 */
public interface IMutexManager extends IContextualSupplier<IMutex, MutexName> {

    /**
     * Retrieves or creates a mutex with the specified name.
     *
     * <p>
     * The manager maintains a registry of mutexes keyed by name. If a mutex
     * with the given name already exists, it is returned. Otherwise, a new
     * mutex is created and registered.
     * </p>
     *
     * @param name the unique name identifying the mutex
     * @return the mutex associated with the given name
     * @throws MutexException if mutex creation or retrieval fails
     */
    IMutex mutex(MutexName name) throws MutexException;

    /**
     * Supplies a mutex identified by name within the supply framework context.
     *
     * <p>
     * This method implements {@link IContextualSupplier#supply} to provide
     * integration with the Garganttua dependency injection system. It delegates
     * to {@link #mutex(String)} and wraps the result in an {@link Optional}.
     * </p>
     *
     * @param mutexName the name of the mutex to supply
     * @param otherContexts additional context parameters (unused)
     * @return an {@link Optional} containing the mutex, or empty if creation fails
     * @throws SupplyException if mutex retrieval fails
     */
    @Override
    default Optional<IMutex> supply(MutexName mutexName, Object... otherContexts) throws SupplyException {
        return Optional.ofNullable(this.mutex(mutexName));
    }

}
