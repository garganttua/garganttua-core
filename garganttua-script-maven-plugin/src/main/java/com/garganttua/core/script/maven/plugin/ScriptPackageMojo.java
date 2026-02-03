package com.garganttua.core.script.maven.plugin;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;

/**
 * Maven plugin Mojo that configures JARs for inclusion in Garganttua scripts.
 *
 * <p>
 * This plugin scans the project's compiled classes for Garganttua annotations
 * ({@code @Expression}, {@code @Bootstrap}, {@code @Prototype}, etc.) and
 * adds the {@code Garganttua-Packages} attribute to the JAR manifest.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * <plugin>
 *     <groupId>com.garganttua.core</groupId>
 *     <artifactId>garganttua-script-maven-plugin</artifactId>
 *     <version>${garganttua.version}</version>
 *     <executions>
 *         <execution>
 *             <goals>
 *                 <goal>prepare-script-jar</goal>
 *             </goals>
 *         </execution>
 *     </executions>
 *     <configuration>
 *         <!-- Optional: explicitly specify packages -->
 *         <packages>
 *             <package>com.myapp.expressions</package>
 *             <package>com.myapp.beans</package>
 *         </packages>
 *         <!-- Optional: auto-detect packages with Garganttua annotations -->
 *         <autoDetect>true</autoDetect>
 *     </configuration>
 * </plugin>
 * }</pre>
 *
 * <h2>Generated Manifest Attribute</h2>
 * <pre>
 * Garganttua-Packages: com.myapp.expressions,com.myapp.beans
 * </pre>
 *
 * @since 2.0.0-ALPHA01
 */
@Mojo(name = "prepare-script-jar", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE)
public class ScriptPackageMojo extends AbstractMojo {

    /**
     * The manifest attribute name for Garganttua packages.
     */
    public static final String PACKAGES_ATTRIBUTE = "Garganttua-Packages";

    /**
     * Garganttua annotation class names to scan for.
     * This list includes all annotations that enable auto-detection in Garganttua builders.
     */
    private static final List<String> GARGANTTUA_ANNOTATIONS = List.of(
            // Expression annotations
            "com.garganttua.core.expression.annotations.Expression",

            // Bootstrap annotations
            "com.garganttua.core.bootstrap.annotations.Bootstrap",

            // Injection annotations
            "com.garganttua.core.injection.annotations.Prototype",
            "com.garganttua.core.injection.annotations.Property",
            "com.garganttua.core.injection.annotations.ChildContext",
            "com.garganttua.core.injection.annotations.Provider",
            "com.garganttua.core.injection.annotations.Resolver",

            // JSR-330 standard annotations (javax.inject)
            "javax.inject.Singleton",
            "javax.inject.Inject",
            "javax.inject.Qualifier",
            "javax.inject.Named",

            // Runtime annotations
            "com.garganttua.core.runtime.annotations.RuntimeDefinition",
            "com.garganttua.core.runtime.annotations.Step",
            "com.garganttua.core.runtime.annotations.Steps",

            // Mutex annotations
            "com.garganttua.core.mutex.annotations.MutexFactory",
            "com.garganttua.core.mutex.annotations.Mutex",

            // Native annotations
            "com.garganttua.core.nativve.annotations.NativeConfigurationBuilder",
            "com.garganttua.core.nativve.annotations.Native",

            // DSL annotations
            "com.garganttua.core.dsl.annotations.Scan",

            // Mapper annotations
            "com.garganttua.core.mapper.annotations.ObjectMappingRule",
            "com.garganttua.core.mapper.annotations.FieldMappingRule"
    );

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true)
    private File outputDirectory;

    /**
     * Explicit list of packages to include in the manifest.
     * If specified, these packages are always included.
     */
    @Parameter(property = "packages")
    private List<String> packages;

    /**
     * Whether to automatically detect packages containing Garganttua annotations.
     * Defaults to true.
     */
    @Parameter(property = "autoDetect", defaultValue = "true")
    private boolean autoDetect;

    /**
     * Base packages to scan for auto-detection.
     * If not specified, scans all packages in the output directory.
     */
    @Parameter(property = "scanPackages")
    private List<String> scanPackages;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Garganttua Script Maven Plugin - Preparing JAR for script inclusion");

        Set<String> allPackages = new HashSet<>();

        // Add explicitly configured packages
        if (packages != null && !packages.isEmpty()) {
            allPackages.addAll(packages);
            getLog().info("Added " + packages.size() + " explicitly configured packages");
        }

        // Auto-detect packages with Garganttua annotations
        if (autoDetect) {
            try {
                Set<String> detectedPackages = autoDetectPackages();
                allPackages.addAll(detectedPackages);
                getLog().info("Auto-detected " + detectedPackages.size() + " packages with Garganttua annotations");
            } catch (Exception e) {
                throw new MojoExecutionException("Failed to auto-detect packages", e);
            }
        }

        if (allPackages.isEmpty()) {
            getLog().warn("No packages found to include in manifest. "
                    + "Specify packages explicitly or ensure classes have Garganttua annotations.");
            return;
        }

        // Build the packages string
        String packagesValue = String.join(",", allPackages);
        getLog().info("Setting manifest attribute: " + PACKAGES_ATTRIBUTE + "=" + packagesValue);

        // Configure the maven-jar-plugin manifest
        configureJarPluginManifest(packagesValue);

        getLog().info("JAR configured for Garganttua script inclusion with " + allPackages.size() + " packages");
    }

    /**
     * Auto-detects packages containing Garganttua annotations.
     *
     * @return set of package names containing Garganttua-annotated classes/methods
     * @throws Exception if scanning fails
     */
    private Set<String> autoDetectPackages() throws Exception {
        Set<String> detectedPackages = new HashSet<>();

        if (!outputDirectory.exists()) {
            getLog().warn("Output directory does not exist: " + outputDirectory);
            return detectedPackages;
        }

        // Create a classloader with the output directory
        URL[] urls = { outputDirectory.toURI().toURL() };
        try (URLClassLoader classLoader = new URLClassLoader(urls, getClass().getClassLoader())) {

            // Set up the annotation scanner
            ObjectReflectionHelper.setAnnotationScanner(new ReflectionsAnnotationScanner());

            // Determine packages to scan
            List<String> packagesToScan = new ArrayList<>();
            if (scanPackages != null && !scanPackages.isEmpty()) {
                packagesToScan.addAll(scanPackages);
            } else {
                // Scan all packages by finding package directories
                packagesToScan.addAll(findAllPackages(outputDirectory, ""));
            }

            getLog().debug("Scanning " + packagesToScan.size() + " packages for Garganttua annotations");

            // Scan each package for Garganttua annotations
            for (String packageName : packagesToScan) {
                if (hasGarganttuaAnnotations(packageName, classLoader)) {
                    detectedPackages.add(packageName);
                    getLog().debug("Found Garganttua annotations in package: " + packageName);
                }
            }
        }

        return detectedPackages;
    }

    /**
     * Finds all packages in the output directory.
     *
     * @param dir the directory to scan
     * @param prefix the current package prefix
     * @return list of package names
     */
    private List<String> findAllPackages(File dir, String prefix) {
        List<String> packages = new ArrayList<>();

        File[] files = dir.listFiles();
        if (files == null) {
            return packages;
        }

        boolean hasClasses = false;
        for (File file : files) {
            if (file.isDirectory()) {
                String subPackage = prefix.isEmpty() ? file.getName() : prefix + "." + file.getName();
                packages.addAll(findAllPackages(file, subPackage));
            } else if (file.getName().endsWith(".class")) {
                hasClasses = true;
            }
        }

        // Add this package if it contains classes
        if (hasClasses && !prefix.isEmpty()) {
            packages.add(prefix);
        }

        return packages;
    }

    /**
     * Checks if a package contains classes with Garganttua annotations.
     *
     * @param packageName the package to check
     * @param classLoader the classloader to use
     * @return true if the package has Garganttua-annotated elements
     */
    @SuppressWarnings("unchecked")
    private boolean hasGarganttuaAnnotations(String packageName, ClassLoader classLoader) {
        for (String annotationClassName : GARGANTTUA_ANNOTATIONS) {
            try {
                Class<? extends Annotation> annotationClass =
                        (Class<? extends Annotation>) classLoader.loadClass(annotationClassName);

                // Check for annotated classes
                List<Class<?>> annotatedClasses =
                        ObjectReflectionHelper.getClassesWithAnnotation(packageName, annotationClass);
                if (!annotatedClasses.isEmpty()) {
                    return true;
                }

                // Check for annotated methods (e.g., @Expression)
                List<Method> annotatedMethods =
                        ObjectReflectionHelper.getMethodsWithAnnotation(packageName, annotationClass);
                if (!annotatedMethods.isEmpty()) {
                    return true;
                }
            } catch (ClassNotFoundException e) {
                // Annotation class not available, skip
                getLog().debug("Annotation class not found: " + annotationClassName);
            } catch (Exception e) {
                getLog().debug("Error scanning for annotation " + annotationClassName + ": " + e.getMessage());
            }
        }
        return false;
    }

    /**
     * Configures the maven-jar-plugin to add the Garganttua-Packages manifest attribute.
     *
     * @param packagesValue the comma-separated list of packages
     */
    private void configureJarPluginManifest(String packagesValue) {
        // Get or create the manifest entries in project properties
        // This will be picked up by the maven-jar-plugin
        project.getProperties().setProperty("garganttua.packages", packagesValue);

        // Also set it directly in the project's manifest configuration
        // by adding to the archive configuration
        Object jarPluginConfig = project.getBuild().getPluginsAsMap()
                .get("org.apache.maven.plugins:maven-jar-plugin");

        if (jarPluginConfig == null) {
            getLog().info("maven-jar-plugin not explicitly configured. "
                    + "Add the following to your pom.xml to include the manifest attribute:");
            getLog().info("");
            getLog().info("<plugin>");
            getLog().info("    <groupId>org.apache.maven.plugins</groupId>");
            getLog().info("    <artifactId>maven-jar-plugin</artifactId>");
            getLog().info("    <configuration>");
            getLog().info("        <archive>");
            getLog().info("            <manifestEntries>");
            getLog().info("                <Garganttua-Packages>" + packagesValue + "</Garganttua-Packages>");
            getLog().info("            </manifestEntries>");
            getLog().info("        </archive>");
            getLog().info("    </configuration>");
            getLog().info("</plugin>");
            getLog().info("");
            getLog().info("Or use the garganttua-script-maven-plugin with jar goal:");
        }

        // Store the packages value for use by the jar goal
        project.getProperties().setProperty(PACKAGES_ATTRIBUTE, packagesValue);
    }
}
