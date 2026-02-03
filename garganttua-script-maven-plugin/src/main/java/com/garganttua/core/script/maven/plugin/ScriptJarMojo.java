package com.garganttua.core.script.maven.plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

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
 * Maven plugin Mojo that creates a JAR file configured for Garganttua script inclusion.
 *
 * <p>
 * This Mojo creates a JAR file with the {@code Garganttua-Packages} manifest attribute
 * set to the packages containing Garganttua annotations. This JAR can then be included
 * in Garganttua scripts using the {@code include()} function.
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
 *                 <goal>script-jar</goal>
 *             </goals>
 *         </execution>
 *     </executions>
 *     <configuration>
 *         <packages>
 *             <package>com.myapp.expressions</package>
 *         </packages>
 *     </configuration>
 * </plugin>
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 */
@Mojo(name = "script-jar", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE)
public class ScriptJarMojo extends AbstractMojo {

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

    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    private File buildDirectory;

    /**
     * The name of the JAR file to create. Defaults to ${project.artifactId}-${project.version}-script.jar
     */
    @Parameter(property = "jarName", defaultValue = "${project.artifactId}-${project.version}-script.jar")
    private String jarName;

    /**
     * Explicit list of packages to include in the manifest.
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
     */
    @Parameter(property = "scanPackages")
    private List<String> scanPackages;

    /**
     * Whether to include resources in the JAR.
     */
    @Parameter(property = "includeResources", defaultValue = "true")
    private boolean includeResources;

    /**
     * Resources directory.
     */
    @Parameter(defaultValue = "${project.build.resources[0].directory}", readonly = true)
    private File resourcesDirectory;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Garganttua Script Maven Plugin - Creating script-includable JAR");

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
        getLog().info("Packages to include: " + packagesValue);

        // Create the JAR with the manifest
        File jarFile = new File(buildDirectory, jarName);
        try {
            createJarWithManifest(jarFile, packagesValue);
            getLog().info("Created script JAR: " + jarFile.getAbsolutePath());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to create JAR file", e);
        }

        // Attach the JAR as a project artifact with classifier "script"
        project.getArtifact().setFile(jarFile);
    }

    /**
     * Creates a JAR file with the Garganttua-Packages manifest attribute.
     *
     * @param jarFile the JAR file to create
     * @param packagesValue the comma-separated packages value
     * @throws IOException if JAR creation fails
     */
    private void createJarWithManifest(File jarFile, String packagesValue) throws IOException {
        // Create the manifest
        Manifest manifest = new Manifest();
        Attributes mainAttrs = manifest.getMainAttributes();
        mainAttrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        mainAttrs.putValue(PACKAGES_ATTRIBUTE, packagesValue);

        // Add project information
        mainAttrs.putValue("Built-By", System.getProperty("user.name"));
        mainAttrs.putValue("Implementation-Title", project.getName());
        mainAttrs.putValue("Implementation-Version", project.getVersion());
        mainAttrs.putValue("Implementation-Vendor", project.getGroupId());

        // Ensure parent directory exists
        if (!jarFile.getParentFile().exists()) {
            jarFile.getParentFile().mkdirs();
        }

        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile), manifest)) {
            // Add compiled classes
            if (outputDirectory.exists()) {
                addDirectoryToJar(jos, outputDirectory.toPath(), outputDirectory.toPath());
            }

            // Add resources if configured
            if (includeResources && resourcesDirectory != null && resourcesDirectory.exists()) {
                addDirectoryToJar(jos, resourcesDirectory.toPath(), resourcesDirectory.toPath());
            }
        }
    }

    /**
     * Adds all files from a directory to the JAR.
     *
     * @param jos the JAR output stream
     * @param root the root directory
     * @param dir the current directory
     * @throws IOException if an I/O error occurs
     */
    private void addDirectoryToJar(JarOutputStream jos, Path root, Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String entryName = root.relativize(file).toString().replace(File.separatorChar, '/');

                // Skip if already added (e.g., MANIFEST.MF)
                if (entryName.equals("META-INF/MANIFEST.MF")) {
                    return FileVisitResult.CONTINUE;
                }

                JarEntry entry = new JarEntry(entryName);
                entry.setTime(attrs.lastModifiedTime().toMillis());
                jos.putNextEntry(entry);
                Files.copy(file, jos);
                jos.closeEntry();

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                String entryName = root.relativize(dir).toString().replace(File.separatorChar, '/');
                if (!entryName.isEmpty()) {
                    if (!entryName.endsWith("/")) {
                        entryName += "/";
                    }
                    JarEntry entry = new JarEntry(entryName);
                    entry.setTime(attrs.lastModifiedTime().toMillis());
                    jos.putNextEntry(entry);
                    jos.closeEntry();
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Auto-detects packages containing Garganttua annotations.
     */
    private Set<String> autoDetectPackages() throws Exception {
        Set<String> detectedPackages = new HashSet<>();

        if (!outputDirectory.exists()) {
            getLog().warn("Output directory does not exist: " + outputDirectory);
            return detectedPackages;
        }

        URL[] urls = { outputDirectory.toURI().toURL() };
        try (URLClassLoader classLoader = new URLClassLoader(urls, getClass().getClassLoader())) {
            ObjectReflectionHelper.setAnnotationScanner(new ReflectionsAnnotationScanner());

            List<String> packagesToScan = new ArrayList<>();
            if (scanPackages != null && !scanPackages.isEmpty()) {
                packagesToScan.addAll(scanPackages);
            } else {
                packagesToScan.addAll(findAllPackages(outputDirectory, ""));
            }

            getLog().debug("Scanning " + packagesToScan.size() + " packages for Garganttua annotations");

            for (String packageName : packagesToScan) {
                if (hasGarganttuaAnnotations(packageName, classLoader)) {
                    detectedPackages.add(packageName);
                    getLog().debug("Found Garganttua annotations in package: " + packageName);
                }
            }
        }

        return detectedPackages;
    }

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

        if (hasClasses && !prefix.isEmpty()) {
            packages.add(prefix);
        }

        return packages;
    }

    @SuppressWarnings("unchecked")
    private boolean hasGarganttuaAnnotations(String packageName, ClassLoader classLoader) {
        for (String annotationClassName : GARGANTTUA_ANNOTATIONS) {
            try {
                Class<? extends Annotation> annotationClass =
                        (Class<? extends Annotation>) classLoader.loadClass(annotationClassName);

                List<Class<?>> annotatedClasses =
                        ObjectReflectionHelper.getClassesWithAnnotation(packageName, annotationClass);
                if (!annotatedClasses.isEmpty()) {
                    return true;
                }

                List<Method> annotatedMethods =
                        ObjectReflectionHelper.getMethodsWithAnnotation(packageName, annotationClass);
                if (!annotatedMethods.isEmpty()) {
                    return true;
                }
            } catch (ClassNotFoundException e) {
                getLog().debug("Annotation class not found: " + annotationClassName);
            } catch (Exception e) {
                getLog().debug("Error scanning for annotation " + annotationClassName + ": " + e.getMessage());
            }
        }
        return false;
    }
}
