package com.garganttua.core.nativve;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Interface for writing GraalVM native image configuration files.
 *
 * <p>
 * {@code INativeConfiguration} provides methods for generating and writing native image
 * configuration files required by GraalVM for proper native compilation. It supports writing
 * both reflection configuration (reflect-config.json) and resource configuration (resource-config.json)
 * files to the META-INF/native-image directory structure.
 * </p>
 *
 * <p>
 * The configuration can be written in two ways:
 * </p>
 * <ul>
 *   <li>Automatic file-based writing using default paths configured during build</li>
 *   <li>Stream-based writing for custom I/O handling</li>
 * </ul>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * </p>
 * <pre>{@code
 * // Create configuration using builder
 * INativeConfiguration config = NativeConfigurationBuilder.builder()
 *     .reflectionPath("target/classes")
 *     .resourcesPath("target/classes")
 *     .reflectionEntry(MyService.class)
 *         .queryAllDeclaredConstructors(true)
 *         .queryAllDeclaredMethods(true)
 *     .resource("application.properties")
 *     .resource(MyConfig.class)
 *     .mode(NativeConfigurationMode.override)
 *     .build();
 *
 * // Write configuration files to default locations
 * config.writeReflectionConfiguration();
 * config.writeResourcesConfiguration();
 *
 * // Or write to custom streams
 * try (InputStream input = new FileInputStream("existing-config.json");
 *      OutputStream output = new FileOutputStream("reflect-config.json")) {
 *     config.writeReflectionConfiguration(input, output);
 * }
 * }</pre>
 *
 * <p>
 * The generated files follow the GraalVM native image configuration format:
 * </p>
 * <ul>
 *   <li><strong>reflect-config.json</strong> - Specifies classes, methods, fields, and constructors
 *       that should be accessible via reflection at runtime</li>
 *   <li><strong>resource-config.json</strong> - Defines resource files that should be included
 *       in the native image</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see INativeConfigurationBuilder
 * @see IReflectionConfigurationEntry
 * @see NativeConfigurationMode
 */
public interface INativeConfiguration {

    /**
     * Writes the reflection configuration to the default reflection path.
     *
     * <p>
     * This method generates the reflect-config.json file in the META-INF/native-image
     * directory at the configured reflection path. The file contains all reflection
     * configuration entries registered with the builder, formatted according to GraalVM
     * native image specifications.
     * </p>
     *
     * <p>
     * The method automatically:
     * </p>
     * <ul>
     *   <li>Creates the META-INF/native-image directory structure if it doesn't exist</li>
     *   <li>Creates or overwrites the reflect-config.json file</li>
     *   <li>Writes all registered reflection entries in JSON format</li>
     * </ul>
     *
     * <p>
     * <strong>Example:</strong>
     * </p>
     * <pre>{@code
     * INativeConfiguration config = NativeConfigurationBuilder.builder()
     *     .reflectionPath("target/classes")
     *     .reflectionEntry(UserService.class)
     *         .queryAllDeclaredConstructors(true)
     *         .queryAllDeclaredMethods(true)
     *     .reflectionEntry(DataRepository.class)
     *         .allDeclaredFields(true)
     *     .build();
     *
     * // Writes to target/classes/META-INF/native-image/reflect-config.json
     * config.writeReflectionConfiguration();
     * }</pre>
     *
     * @throws com.garganttua.core.CoreException if an I/O error occurs during file writing
     */
    void writeReflectionConfiguration();

    /**
     * Writes the resources configuration to the default resources path.
     *
     * <p>
     * This method generates the resource-config.json file in the META-INF/native-image
     * directory at the configured resources path. The file contains resource patterns
     * that specify which application resources (property files, configuration files, etc.)
     * should be included in the native image.
     * </p>
     *
     * <p>
     * The method automatically:
     * </p>
     * <ul>
     *   <li>Creates the META-INF/native-image directory structure if it doesn't exist</li>
     *   <li>Creates or overwrites the resource-config.json file</li>
     *   <li>Writes all registered resource patterns in JSON format</li>
     * </ul>
     *
     * <p>
     * <strong>Example:</strong>
     * </p>
     * <pre>{@code
     * INativeConfiguration config = NativeConfigurationBuilder.builder()
     *     .resourcesPath("target/classes")
     *     .resource("application.properties")
     *     .resource("config/database.yml")
     *     .resource(MyConfiguration.class)
     *     .build();
     *
     * // Writes to target/classes/META-INF/native-image/resource-config.json
     * config.writeResourcesConfiguration();
     * }</pre>
     *
     * @throws com.garganttua.core.CoreException if an I/O error occurs during file writing
     */
    void writeResourcesConfiguration();

    /**
     * Writes the reflection configuration using custom input and output streams.
     *
     * <p>
     * This method provides fine-grained control over the reflection configuration writing
     * process by accepting custom streams. This is useful for:
     * </p>
     * <ul>
     *   <li>Merging with existing configuration from the input stream</li>
     *   <li>Writing to non-standard locations or formats</li>
     *   <li>Testing and validation scenarios</li>
     *   <li>Custom processing pipelines</li>
     * </ul>
     *
     * <p>
     * <strong>Note:</strong> The caller is responsible for managing stream lifecycle
     * (opening, closing, and error handling).
     * </p>
     *
     * <p>
     * <strong>Example:</strong>
     * </p>
     * <pre>{@code
     * INativeConfiguration config = createConfiguration();
     *
     * // Write to custom location with merge capability
     * try (InputStream existingConfig = new FileInputStream("existing-reflect-config.json");
     *      OutputStream output = new FileOutputStream("merged-reflect-config.json")) {
     *     config.writeReflectionConfiguration(existingConfig, output);
     * } catch (IOException e) {
     *     // Handle error
     * }
     *
     * // Write to memory for testing
     * ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);
     * ByteArrayOutputStream output = new ByteArrayOutputStream();
     * config.writeReflectionConfiguration(input, output);
     * String jsonConfig = output.toString(StandardCharsets.UTF_8);
     * }</pre>
     *
     * @param inputStream the input stream to read existing configuration from (may be empty)
     * @param outputStream the output stream to write the configuration to
     * @throws com.garganttua.core.CoreException if an I/O error occurs during reading or writing
     */
    void writeReflectionConfiguration(InputStream inputStream, OutputStream outputStream);

    /**
     * Writes the resources configuration using custom input and output streams.
     *
     * <p>
     * This method provides fine-grained control over the resources configuration writing
     * process by accepting custom streams. This is useful for:
     * </p>
     * <ul>
     *   <li>Merging with existing resource configuration from the input stream</li>
     *   <li>Writing to non-standard locations or formats</li>
     *   <li>Testing and validation scenarios</li>
     *   <li>Custom processing pipelines</li>
     * </ul>
     *
     * <p>
     * <strong>Note:</strong> The caller is responsible for managing stream lifecycle
     * (opening, closing, and error handling).
     * </p>
     *
     * <p>
     * <strong>Example:</strong>
     * </p>
     * <pre>{@code
     * INativeConfiguration config = createConfiguration();
     *
     * // Write to custom location
     * try (InputStream existingConfig = new FileInputStream("existing-resource-config.json");
     *      OutputStream output = new FileOutputStream("merged-resource-config.json")) {
     *     config.writeResourcesConfiguration(existingConfig, output);
     * } catch (IOException e) {
     *     // Handle error
     * }
     *
     * // Write to string for inspection
     * ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);
     * ByteArrayOutputStream output = new ByteArrayOutputStream();
     * config.writeResourcesConfiguration(input, output);
     * String jsonConfig = output.toString(StandardCharsets.UTF_8);
     * }</pre>
     *
     * @param inputStream the input stream to read existing configuration from (may be empty)
     * @param outputStream the output stream to write the configuration to
     * @throws com.garganttua.core.CoreException if an I/O error occurs during reading or writing
     */
    void writeResourcesConfiguration(InputStream inputStream, OutputStream outputStream);

}
