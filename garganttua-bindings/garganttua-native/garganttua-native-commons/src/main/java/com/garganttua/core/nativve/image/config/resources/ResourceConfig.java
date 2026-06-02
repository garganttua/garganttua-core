package com.garganttua.core.nativve.image.config.resources;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IClass;

/**
 * Utility for reading and writing GraalVM {@code resource-config.json} files,
 * adding and removing literal-quoted ({@code \\Q...\\E}) include patterns for
 * classes and arbitrary resource paths.
 */
public class ResourceConfig {
    private static final Logger log = Logger.getLogger(ResourceConfig.class);

private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Adds the {@code .class} resource path of the given class to the resource config.
     *
     * @param resourceConfigFile the {@code resource-config.json} file to update
     * @param clazz the class whose compiled resource should be included
     * @throws IOException if the file cannot be read or written
     */
    public static void addResource(File resourceConfigFile, IClass<?> clazz) throws IOException {
        log.trace("Entering addResource with file: {} and class: {}", resourceConfigFile, clazz.getName());
        String classPath = clazz.getName().replace('.', '/') + ".class";
        log.debug("Converted class {} to resource path: {}", clazz.getName(), classPath);
        addResource(resourceConfigFile, classPath);
        log.trace("Exiting addResource");
    }

	/**
	 * Adds a resource include pattern to the resource config, creating the file's
	 * {@code resources.includes} structure as needed and skipping duplicates.
	 *
	 * @param resourceConfigFile the {@code resource-config.json} file to update
	 * @param resource the resource path to include (wrapped as a literal-quoted pattern)
	 * @throws IOException if the file cannot be read or written
	 */
	public static void addResource(File resourceConfigFile, String resource)
			throws IOException, StreamReadException, DatabindException, StreamWriteException {
		log.trace("Entering addResource with file: {} and resource: {}", resourceConfigFile, resource);
		String newPattern = "\\Q" + resource + "\\E";
		log.debug("Generated pattern for resource {}: {}", resource, newPattern);

        Map<String, Object> resourceConfig = new HashMap<>();
        if (resourceConfigFile.exists() && resourceConfigFile.length() > 0) {
            log.debug("Loading existing resource config from file: {}", resourceConfigFile);
            resourceConfig = objectMapper.readValue(resourceConfigFile, new TypeReference<Map<String, Object>>() {});
        }

        Map<String, List<Map<String, String>>> resources = (Map<String, List<Map<String, String>>>) resourceConfig
                .computeIfAbsent("resources", k -> new HashMap<>());

        List<Map<String, String>> includes = resources.computeIfAbsent("includes", k -> new ArrayList<>());

        boolean exists = includes.stream().anyMatch(entry -> newPattern.equals(entry.get("pattern")));

        if (!exists) {
            log.debug("Adding new pattern to resource config: {}", newPattern);
            includes.add(Map.of("pattern", newPattern));
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(resourceConfigFile, resourceConfig);
            log.debug("Resource pattern added successfully: {}", newPattern);
        } else {
            log.debug("Pattern already exists in resource config: {}", newPattern);
        }
		log.trace("Exiting addResource");
	}

    /**
     * Removes the {@code .class} resource path of the given class from the resource config.
     *
     * @param resourceConfigFile the {@code resource-config.json} file to update
     * @param clazz the class whose compiled resource should be removed
     * @throws IOException if the file cannot be read or written
     */
    public static void removeResource(File resourceConfigFile, IClass<?> clazz) throws IOException {
        log.trace("Entering removeResource with file: {} and class: {}", resourceConfigFile, clazz.getName());
        String classPath = clazz.getName().replace('.', '/') + ".class";
        log.debug("Converted class {} to resource path: {}", clazz.getName(), classPath);
        removeResource(resourceConfigFile, classPath);
        log.trace("Exiting removeResource");
    }

	/**
	 * Removes a resource include pattern from the resource config. No-ops when the
	 * file is missing, empty, lacks an {@code includes} section, or has no match.
	 *
	 * @param resourceConfigFile the {@code resource-config.json} file to update
	 * @param resource the resource path to remove (matched as a literal-quoted pattern)
	 * @throws IOException if the file cannot be read or written
	 */
	public static void removeResource(File resourceConfigFile, String resource)
			throws IOException, StreamReadException, DatabindException, StreamWriteException {
		log.trace("Entering removeResource with file: {} and resource: {}", resourceConfigFile, resource);
		String patternToRemove = "\\Q" + resource + "\\E";
		log.debug("Pattern to remove: {}", patternToRemove);

        if (!resourceConfigFile.exists()) {
            log.warn("Resource config file does not exist, no removal performed: {}", resourceConfigFile);
            return;
        }

        if (resourceConfigFile.length() == 0) {
            log.warn("Resource config file is empty, no removal performed: {}", resourceConfigFile);
            return;
        }

        log.debug("Loading resource config from file: {}", resourceConfigFile);
        Map<String, Object> resourceConfig = objectMapper.readValue(resourceConfigFile, new TypeReference<Map<String, Object>>() {});

        Map<String, List<Map<String, String>>> resources = (Map<String, List<Map<String, String>>>) resourceConfig.get("resources");
        if (resources == null || !resources.containsKey("includes")) {
            log.warn("No 'includes' section found in resource config, no removal performed");
            return;
        }

        List<Map<String, String>> includes = resources.get("includes");

        boolean removed = includes.removeIf(entry -> patternToRemove.equals(entry.get("pattern")));

        if (removed) {
            log.debug("Removing pattern from resource config: {}", patternToRemove);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(resourceConfigFile, resourceConfig);
            log.debug("Resource pattern removed successfully: {}", patternToRemove);
        } else {
            log.warn("No matching pattern found for removal: {}", patternToRemove);
        }
		log.trace("Exiting removeResource");
	}
}