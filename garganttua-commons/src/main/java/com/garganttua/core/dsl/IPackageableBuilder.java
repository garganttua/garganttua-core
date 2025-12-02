package com.garganttua.core.dsl;

/**
 * Builder interface for components that support package scanning functionality.
 *
 * <p>
 * {@code IPackageableBuilder} extends the builder pattern to support configuration
 * of package scanning operations. This interface is typically implemented by builders
 * for dependency injection containers, component scanners, and other frameworks that
 * need to discover and register components from specific Java packages.
 * </p>
 *
 * <h2>Package Scanning</h2>
 * <p>
 * Package scanning allows frameworks to automatically discover and process classes
 * within specified packages and their sub-packages. This is commonly used for:
 * </p>
 * <ul>
 *   <li>Auto-discovery of annotated beans and components</li>
 *   <li>Configuration class detection</li>
 *   <li>Service provider discovery</li>
 *   <li>Plugin loading mechanisms</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Single package configuration
 * DiContext context = DiContextBuilder.create()
 *     .withPackage("com.example.services")
 *     .build();
 *
 * // Multiple packages configuration
 * DiContext context = DiContextBuilder.create()
 *     .withPackage("com.example.services")
 *     .withPackage("com.example.repositories")
 *     .withPackage("com.example.controllers")
 *     .build();
 *
 * // Array-based package configuration
 * String[] packages = {
 *     "com.example.services",
 *     "com.example.repositories",
 *     "com.example.controllers"
 * };
 * DiContext context = DiContextBuilder.create()
 *     .withPackages(packages)
 *     .build();
 *
 * // Retrieve configured packages
 * String[] configuredPackages = builder.getPackages();
 * }</pre>
 *
 * <h2>Type Parameters</h2>
 * <p>
 * The interface uses two type parameters to maintain type safety in the builder chain:
 * </p>
 * <ul>
 *   <li>{@code B} - The concrete builder type, enabling fluent method chaining</li>
 *   <li>{@code C} - The type of component being built</li>
 * </ul>
 *
 * <h2>Implementation Notes</h2>
 * <p>
 * Implementations should:
 * </p>
 * <ul>
 *   <li>Support recursive sub-package scanning</li>
 *   <li>Handle duplicate package registrations gracefully</li>
 *   <li>Validate package names for valid Java package naming conventions</li>
 *   <li>Return defensive copies from {@link #getPackages()} to prevent external modification</li>
 * </ul>
 *
 * @param <B> the concrete builder type for method chaining
 * @param <C> the type of component being built
 * @since 2.0.0-ALPHA01
 * @see IBuilder
 */
public interface IPackageableBuilder<B, C> extends IBuilder<C> {

    /**
     * Adds a package to scan for annotated beans and components.
     *
     * @param packageName the package name to scan
     * @return this builder for method chaining
     */
    B withPackage(String packageName);

    /**
     * Adds multiple packages to scan for annotated beans and components.
     *
     * @param packageNames array of package names to scan
     * @return this builder for method chainings
     */
    B withPackages(String[] packageNames);

    /**
     * Returns all packages configured for scanning.
     *
     * @return array of package names
     */
    String[] getPackages();

}
