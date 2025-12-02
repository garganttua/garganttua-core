package com.garganttua.core.dsl;

public interface IPackageableBuilder<B, C> extends IAutomaticBuilder<B, C> {

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
