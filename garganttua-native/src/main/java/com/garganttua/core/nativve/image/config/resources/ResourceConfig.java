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

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResourceConfig {

private static final ObjectMapper objectMapper = new ObjectMapper();
    
    public static void addResource(File resourceConfigFile, Class<?> clazz) throws IOException {
        log.atTrace().log("Entering addResource with file: {} and class: {}", resourceConfigFile, clazz.getName());
        String classPath = clazz.getName().replace('.', '/') + ".class";
        log.atDebug().log("Converted class {} to resource path: {}", clazz.getName(), classPath);
        addResource(resourceConfigFile, classPath);
        log.atTrace().log("Exiting addResource");
    }

	public static void addResource(File resourceConfigFile, String resource)
			throws IOException, StreamReadException, DatabindException, StreamWriteException {
		log.atTrace().log("Entering addResource with file: {} and resource: {}", resourceConfigFile, resource);
		String newPattern = "\\Q" + resource + "\\E";
		log.atDebug().log("Generated pattern for resource {}: {}", resource, newPattern);

        Map<String, Object> resourceConfig = new HashMap<>();
        if (resourceConfigFile.exists() && resourceConfigFile.length() > 0) {
            log.atDebug().log("Loading existing resource config from file: {}", resourceConfigFile);
            resourceConfig = objectMapper.readValue(resourceConfigFile, new TypeReference<Map<String, Object>>() {});
        }

        Map<String, List<Map<String, String>>> resources = (Map<String, List<Map<String, String>>>) resourceConfig
                .computeIfAbsent("resources", k -> new HashMap<>());

        List<Map<String, String>> includes = resources.computeIfAbsent("includes", k -> new ArrayList<>());

        boolean exists = includes.stream().anyMatch(entry -> newPattern.equals(entry.get("pattern")));

        if (!exists) {
            log.atDebug().log("Adding new pattern to resource config: {}", newPattern);
            includes.add(Map.of("pattern", newPattern));
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(resourceConfigFile, resourceConfig);
            log.atDebug().log("Resource pattern added successfully: {}", newPattern);
        } else {
            log.atDebug().log("Pattern already exists in resource config: {}", newPattern);
        }
		log.atTrace().log("Exiting addResource");
	}

    public static void removeResource(File resourceConfigFile, Class<?> clazz) throws IOException {
        log.atTrace().log("Entering removeResource with file: {} and class: {}", resourceConfigFile, clazz.getName());
        String classPath = clazz.getName().replace('.', '/') + ".class";
        log.atDebug().log("Converted class {} to resource path: {}", clazz.getName(), classPath);
        removeResource(resourceConfigFile, classPath);
        log.atTrace().log("Exiting removeResource");
    }

	public static void removeResource(File resourceConfigFile, String resource)
			throws IOException, StreamReadException, DatabindException, StreamWriteException {
		log.atTrace().log("Entering removeResource with file: {} and resource: {}", resourceConfigFile, resource);
		String patternToRemove = "\\Q" + resource + "\\E";
		log.atDebug().log("Pattern to remove: {}", patternToRemove);

        if (!resourceConfigFile.exists()) {
            log.atWarn().log("Resource config file does not exist, no removal performed: {}", resourceConfigFile);
            return;
        }

        if (resourceConfigFile.length() == 0) {
            log.atWarn().log("Resource config file is empty, no removal performed: {}", resourceConfigFile);
            return;
        }

        log.atDebug().log("Loading resource config from file: {}", resourceConfigFile);
        Map<String, Object> resourceConfig = objectMapper.readValue(resourceConfigFile, new TypeReference<Map<String, Object>>() {});

        Map<String, List<Map<String, String>>> resources = (Map<String, List<Map<String, String>>>) resourceConfig.get("resources");
        if (resources == null || !resources.containsKey("includes")) {
            log.atWarn().log("No 'includes' section found in resource config, no removal performed");
            return;
        }

        List<Map<String, String>> includes = resources.get("includes");

        boolean removed = includes.removeIf(entry -> patternToRemove.equals(entry.get("pattern")));

        if (removed) {
            log.atDebug().log("Removing pattern from resource config: {}", patternToRemove);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(resourceConfigFile, resourceConfig);
            log.atDebug().log("Resource pattern removed successfully: {}", patternToRemove);
        } else {
            log.atWarn().log("No matching pattern found for removal: {}", patternToRemove);
        }
		log.atTrace().log("Exiting removeResource");
	}
}