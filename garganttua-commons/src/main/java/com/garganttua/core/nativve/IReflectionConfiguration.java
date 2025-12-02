package com.garganttua.core.nativve;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Interface for managing GraalVM native image reflection configuration.
 *
 * <p>
 * {@code IReflectionConfiguration} provides methods to manage reflection configuration
 * entries for GraalVM native images. This includes adding, removing, updating entries,
 * and saving the configuration to a reflect-config.json file required by GraalVM.
 * </p>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * </p>
 * <pre>{@code
 * IReflectionConfiguration config = new ReflectionConfiguration();
 *
 * // Add entries
 * IReflectionConfigurationEntry entry1 = createEntry(MyService.class);
 * config.addEntry(entry1);
 *
 * IReflectionConfigurationEntry entry2 = createEntry(MyRepository.class);
 * config.addEntry(entry2);
 *
 * // Find and update entry
 * config.findEntryByType(MyService.class).ifPresent(entry -> {
 *     entry.setQueryAllDeclaredMethods(true);
 *     config.updateEntry(entry);
 * });
 *
 * // Save to file for native image build
 * config.saveToFile(new File("META-INF/native-image/reflect-config.json"));
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see IReflectionConfigurationEntry
 * @see IReflectionConfigurationEntryBuilder
 */
public interface IReflectionConfiguration {

    /**
     * Replaces all existing entries with the provided list of entries.
     *
     * @param entries the new list of reflection configuration entries
     */
    void setEntries(List<IReflectionConfigurationEntry> entries);

    /**
     * Adds a new reflection configuration entry.
     *
     * @param entry the reflection configuration entry to add
     */
    void addEntry(IReflectionConfigurationEntry entry);

    /**
     * Removes the specified reflection configuration entry.
     *
     * @param entry the reflection configuration entry to remove
     */
    void removeEntry(IReflectionConfigurationEntry entry);

    /**
     * Updates an existing reflection configuration entry.
     *
     * <p>
     * The entry is matched by its type name and replaced with the updated version.
     * </p>
     *
     * @param updatedEntry the updated reflection configuration entry
     */
    void updateEntry(IReflectionConfigurationEntry updatedEntry);

    /**
     * Saves the reflection configuration to a JSON file.
     *
     * <p>
     * The output file follows the GraalVM reflect-config.json format and can be used
     * directly in native image builds. Typical location is
     * {@code META-INF/native-image/<group-id>/<artifact-id>/reflect-config.json}.
     * </p>
     *
     * @param file the file to write the configuration to
     * @throws IOException if an I/O error occurs during writing
     */
    void saveToFile(File file) throws IOException;

    /**
     * Finds a reflection configuration entry by its associated class type.
     *
     * @param type the class type to search for
     * @return an {@link Optional} containing the entry if found, or empty if not found
     */
    Optional<IReflectionConfigurationEntry> findEntryByType(Class<?> type);

}
