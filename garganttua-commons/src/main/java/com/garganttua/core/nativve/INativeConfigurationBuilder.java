package com.garganttua.core.nativve;

import com.garganttua.core.dsl.IAutomaticBuilder;
import com.garganttua.core.dsl.IPackageableBuilder;

/**
 * Builder interface for constructing GraalVM native image configurations.
 *
 * <p>
 * {@code INativeConfigurationBuilder} provides a fluent DSL for building native image
 * configurations that specify reflection requirements and resource inclusions for GraalVM
 * native image compilation. It combines package scanning capabilities with manual configuration
 * to create comprehensive native image metadata.
 * </p>
 *
 * <p>
 * This builder supports:
 * </p>
 * <ul>
 *   <li>Configuring output paths for generated configuration files</li>
 *   <li>Manually registering classes for reflection access</li>
 *   <li>Registering application resources for inclusion in the native image</li>
 *   <li>Automatic detection of classes annotated with {@code @Native}</li>
 *   <li>Integrating with other {@code INativeBuilder} implementations</li>
 *   <li>Choosing configuration merge strategies (override or merge modes)</li>
 * </ul>
 *
 * <p>
 * <strong>Basic Usage Example:</strong>
 * </p>
 * <pre>{@code
 * // Simple configuration with manual registration
 * INativeConfiguration config = NativeConfigurationBuilder.builder()
 *     .reflectionPath("target/classes")
 *     .resourcesPath("target/classes")
 *     .reflectionEntry(UserService.class)
 *         .queryAllDeclaredConstructors(true)
 *         .queryAllDeclaredMethods(true)
 *     .reflectionEntry(DataRepository.class)
 *         .allDeclaredFields(true)
 *     .resource("application.properties")
 *     .resource("config/database.yml")
 *     .mode(NativeConfigurationMode.override)
 *     .build();
 *
 * // Write configuration files
 * config.writeReflectionConfiguration();
 * config.writeResourcesConfiguration();
 * }</pre>
 *
 * <p>
 * <strong>Advanced Usage with Package Scanning:</strong>
 * </p>
 * <pre>{@code
 * // Automatic detection with package scanning
 * INativeConfiguration config = NativeConfigurationBuilder.builder()
 *     .reflectionPath("target/classes")
 *     .resourcesPath("target/classes")
 *     .withPackages(new String[]{"com.example.service", "com.example.repository"})
 *     // Classes annotated with @Native will be auto-detected
 *     .reflectionEntry(SpecialCase.class)
 *         .queryAllDeclaredConstructors(true)
 *     .resource(MyConfig.class)
 *     .build();
 * }</pre>
 *
 * <p>
 * <strong>Integration with Custom Builders:</strong>
 * </p>
 * <pre>{@code
 * // Integrate configurations from other builders
 * INativeBuilder<?, ?> customBuilder = new CustomNativeBuilder();
 *
 * INativeConfiguration config = NativeConfigurationBuilder.builder()
 *     .reflectionPath("target/classes")
 *     .resourcesPath("target/classes")
 *     .withPackage("com.example")
 *     .configurationBuilder(customBuilder)
 *     .build();
 * }</pre>
 *
 * <p>
 * <strong>Modifying Existing Entries:</strong>
 * </p>
 * <pre>{@code
 * // Load and modify existing configuration
 * IReflectionConfigurationEntry existingEntry = loadExistingEntry();
 *
 * INativeConfiguration config = NativeConfigurationBuilder.builder()
 *     .reflectionPath("target/classes")
 *     .resourcesPath("target/classes")
 *     .reflectionEntry(existingEntry)
 *         .field("newField")
 *         .method("newMethod", String.class)
 *     .build();
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see INativeConfiguration
 * @see IReflectionConfigurationEntry
 * @see IReflectionConfigurationEntryBuilder
 * @see INativeBuilder
 * @see NativeConfigurationMode
 * @see IPackageableBuilder
 * @see IAutomaticBuilder
 */
public interface INativeConfigurationBuilder extends IPackageableBuilder<INativeConfigurationBuilder, INativeConfiguration>, IAutomaticBuilder<INativeConfigurationBuilder, INativeConfiguration> {

    /**
     * Sets the path where resource configuration files will be written.
     *
     * <p>
     * This method specifies the base directory for generating the resource-config.json file.
     * The actual file will be created at {@code path/META-INF/native-image/resource-config.json}.
     * Typically, this path points to the build output directory (e.g., "target/classes" for Maven).
     * </p>
     *
     * <p>
     * <strong>Required:</strong> This method must be called before building the configuration.
     * </p>
     *
     * <p>
     * <strong>Example:</strong>
     * </p>
     * <pre>{@code
     * builder.resourcesPath("target/classes");
     * // Will write to: target/classes/META-INF/native-image/resource-config.json
     * }</pre>
     *
     * @param path the base path for writing resource configuration files, must not be null
     * @return this builder for method chaining
     * @throws NullPointerException if path is null
     */
    INativeConfigurationBuilder resourcesPath(String path);

    /**
     * Sets the path where reflection configuration files will be written.
     *
     * <p>
     * This method specifies the base directory for generating the reflect-config.json file.
     * The actual file will be created at {@code path/META-INF/native-image/reflect-config.json}.
     * Typically, this path points to the build output directory (e.g., "target/classes" for Maven).
     * </p>
     *
     * <p>
     * <strong>Required:</strong> This method must be called before building the configuration.
     * </p>
     *
     * <p>
     * <strong>Example:</strong>
     * </p>
     * <pre>{@code
     * builder.reflectionPath("target/classes");
     * // Will write to: target/classes/META-INF/native-image/reflect-config.json
     * }</pre>
     *
     * @param path the base path for writing reflection configuration files, must not be null
     * @return this builder for method chaining
     * @throws NullPointerException if path is null
     */
    INativeConfigurationBuilder reflectionPath(String path);

    /**
     * Registers a class for reflection access and returns a builder to configure it.
     *
     * <p>
     * This method creates a new reflection configuration entry for the specified class
     * and returns a builder that allows fine-grained control over which members (fields,
     * methods, constructors) should be accessible via reflection in the native image.
     * </p>
     *
     * <p>
     * The returned builder enables chaining to configure reflection settings, after which
     * you can continue building the main configuration.
     * </p>
     *
     * <p>
     * <strong>Example:</strong>
     * </p>
     * <pre>{@code
     * builder.reflectionEntry(UserService.class)
     *     .queryAllDeclaredConstructors(true)
     *     .queryAllDeclaredMethods(true)
     *     .field("username")
     *     .field("password")
     *     .fieldsAnnotatedWith(Inject.class);
     *
     * // Continue with other configuration
     * builder.reflectionEntry(DataRepository.class)
     *     .allDeclaredFields(true);
     * }</pre>
     *
     * @param clazz the class to register for reflection access, must not be null
     * @return a builder for configuring the reflection entry
     * @throws NullPointerException if clazz is null
     * @see IReflectionConfigurationEntryBuilder
     */
    IReflectionConfigurationEntryBuilder reflectionEntry(Class<?> clazz);

    /**
     * Registers an existing reflection configuration entry and returns a builder to modify it.
     *
     * <p>
     * This method is useful for modifying or extending existing reflection configuration
     * entries loaded from external sources or previous configurations. The returned builder
     * allows adding or removing reflection members from the entry.
     * </p>
     *
     * <p>
     * <strong>Example:</strong>
     * </p>
     * <pre>{@code
     * // Load existing entry from somewhere
     * IReflectionConfigurationEntry existingEntry = loadFromFile();
     *
     * builder.reflectionEntry(existingEntry)
     *     .field("additionalField")
     *     .method("additionalMethod", String.class)
     *     .removeField("obsoleteField");
     * }</pre>
     *
     * @param clazz the existing reflection configuration entry to modify, must not be null
     * @return a builder for configuring the reflection entry
     * @throws NullPointerException if clazz is null
     * @see IReflectionConfigurationEntryBuilder
     */
   IReflectionConfigurationEntryBuilder reflectionEntry(IReflectionConfigurationEntry clazz);

    /**
     * Integrates reflection configuration entries from another native builder.
     *
     * <p>
     * This method allows composing configurations from multiple sources by incorporating
     * all reflection entries generated by the specified builder. The builder will be invoked
     * with the same package configuration and its reflection entries will be merged into
     * this configuration.
     * </p>
     *
     * <p>
     * This is particularly useful for:
     * </p>
     * <ul>
     *   <li>Modular configuration management</li>
     *   <li>Reusing configuration logic across projects</li>
     *   <li>Integrating library-provided configurations</li>
     *   <li>Separating concerns in large applications</li>
     * </ul>
     *
     * <p>
     * <strong>Example:</strong>
     * </p>
     * <pre>{@code
     * // Define a reusable configuration builder
     * public class JpaEntitiesNativeBuilder implements INativeBuilder<JpaEntitiesNativeBuilder, INativeReflectionConfiguration> {
     *     @Override
     *     public INativeReflectionConfiguration build() {
     *         // Return JPA entity reflection configurations
     *     }
     * }
     *
     * // Integrate into main configuration
     * INativeConfiguration config = NativeConfigurationBuilder.builder()
     *     .reflectionPath("target/classes")
     *     .resourcesPath("target/classes")
     *     .withPackage("com.example")
     *     .configurationBuilder(new JpaEntitiesNativeBuilder())
     *     .configurationBuilder(new SerializationNativeBuilder())
     *     .build();
     * }</pre>
     *
     * @param nativeConfigurationBuilder the builder to integrate, must not be null
     * @return this builder for method chaining
     * @throws NullPointerException if nativeConfigurationBuilder is null
     * @see INativeBuilder
     */
    INativeConfigurationBuilder configurationBuilder(INativeBuilder<?,?> nativeConfigurationBuilder);

    /**
     * Registers a resource file pattern for inclusion in the native image.
     *
     * <p>
     * This method adds a resource pattern that will be included in the generated
     * resource-config.json file. The pattern can be a specific file path or a regex pattern
     * for matching multiple resources. The path should be relative to the classpath root.
     * </p>
     *
     * <p>
     * Resource patterns are automatically escaped using {@code \Q...\E} to ensure exact matching.
     * </p>
     *
     * <p>
     * <strong>Examples:</strong>
     * </p>
     * <pre>{@code
     * // Include specific configuration files
     * builder.resource("application.properties")
     *        .resource("config/database.yml")
     *        .resource("templates/email.html");
     *
     * // Include files in a directory
     * builder.resource("static/images/logo.png");
     *
     * // Resources will be accessible via Class.getResourceAsStream() at runtime
     * }</pre>
     *
     * @param resourcePath the resource file path or pattern, must not be null
     * @return this builder for method chaining
     * @throws NullPointerException if resourcePath is null
     */
    INativeConfigurationBuilder resource(String resourcePath);

    /**
     * Registers a class file as a resource for inclusion in the native image.
     *
     * <p>
     * This method automatically determines the class file path from the provided class
     * and registers it as a resource. This is useful when you need the actual .class file
     * to be accessible as a resource at runtime (e.g., for bytecode manipulation libraries
     * or frameworks that inspect class files).
     * </p>
     *
     * <p>
     * The class name is converted to a resource path by replacing dots with slashes and
     * appending ".class" (e.g., {@code com.example.MyClass} becomes {@code com/example/MyClass.class}).
     * </p>
     *
     * <p>
     * <strong>Example:</strong>
     * </p>
     * <pre>{@code
     * // Register class files as resources
     * builder.resource(MyConfiguration.class)
     *        .resource(CustomAnnotation.class);
     *
     * // At runtime, you can access the class file:
     * InputStream classFile = MyConfiguration.class.getResourceAsStream("/com/example/MyConfiguration.class");
     * }</pre>
     *
     * @param resource the class whose .class file should be included as a resource, must not be null
     * @return this builder for method chaining
     * @throws NullPointerException if resource is null
     */
    INativeConfigurationBuilder resource(Class<?> resource);

    /**
     * Sets the configuration merge mode.
     *
     * <p>
     * This method determines how the generated configuration should be combined with
     * existing configurations. Two modes are available:
     * </p>
     * <ul>
     *   <li><strong>OVERRIDE</strong> - Replaces any existing configuration completely
     *       (default and currently the only fully supported mode)</li>
     *   <li><strong>MERGE</strong> - Merges new configuration with existing entries
     *       (not yet fully implemented)</li>
     * </ul>
     *
     * <p>
     * <strong>Example:</strong>
     * </p>
     * <pre>{@code
     * // Override any existing configuration
     * builder.mode(NativeConfigurationMode.override);
     *
     * // Merge with existing configuration (when fully implemented)
     * builder.mode(NativeConfigurationMode.merge);
     * }</pre>
     *
     * @param mode the configuration merge mode, must not be null
     * @return this builder for method chaining
     * @throws NullPointerException if mode is null
     * @see NativeConfigurationMode
     */
    INativeConfigurationBuilder mode(NativeConfigurationMode mode);

}
