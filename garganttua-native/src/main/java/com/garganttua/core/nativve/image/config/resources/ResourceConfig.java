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

public class ResourceConfig {

private static final ObjectMapper objectMapper = new ObjectMapper();
    
    public static void addResource(File resourceConfigFile, Class<?> clazz) throws IOException {
        String classPath = clazz.getName().replace('.', '/') + ".class";
        addResource(resourceConfigFile, classPath);
    }

    @SuppressWarnings("unchecked")
	public static void addResource(File resourceConfigFile, String resource)
			throws IOException, StreamReadException, DatabindException, StreamWriteException {
		String newPattern = "\\Q" + resource + "\\E";

        Map<String, Object> resourceConfig = new HashMap<>();
        if (resourceConfigFile.exists() && resourceConfigFile.length() > 0) {
            resourceConfig = objectMapper.readValue(resourceConfigFile, new TypeReference<Map<String, Object>>() {});
        }

        Map<String, List<Map<String, String>>> resources = (Map<String, List<Map<String, String>>>) resourceConfig
                .computeIfAbsent("resources", k -> new HashMap<>());

        List<Map<String, String>> includes = resources.computeIfAbsent("includes", k -> new ArrayList<>());

        boolean exists = includes.stream().anyMatch(entry -> newPattern.equals(entry.get("pattern")));

        if (!exists) {
            includes.add(Map.of("pattern", newPattern));
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(resourceConfigFile, resourceConfig);
            System.out.println("Pattern ajouté avec succès.");
        } else {
            System.out.println("Le pattern existe déjà.");
        }
	}

    public static void removeResource(File resourceConfigFile, Class<?> clazz) throws IOException {
        String classPath = clazz.getName().replace('.', '/') + ".class";
        removeResource(resourceConfigFile, classPath);
    }

    @SuppressWarnings("unchecked")
	public static void removeResource(File resourceConfigFile, String resource)
			throws IOException, StreamReadException, DatabindException, StreamWriteException {
		String patternToRemove = "\\Q" + resource + "\\E";

        if (!resourceConfigFile.exists()) {
            System.out.println("Le fichier n'existe pas : aucune suppression effectuée.");
            return;
        }

        if (resourceConfigFile.length() == 0) {
            System.out.println("Le fichier est vide : aucune suppression effectuée.");
            return;
        }

        Map<String, Object> resourceConfig = objectMapper.readValue(resourceConfigFile, new TypeReference<Map<String, Object>>() {});

        Map<String, List<Map<String, String>>> resources = (Map<String, List<Map<String, String>>>) resourceConfig.get("resources");
        if (resources == null || !resources.containsKey("includes")) {
            System.out.println("Aucune section 'includes' trouvée dans le fichier : aucune suppression effectuée.");
            return;
        }

        List<Map<String, String>> includes = resources.get("includes");

        boolean removed = includes.removeIf(entry -> patternToRemove.equals(entry.get("pattern")));

        if (removed) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(resourceConfigFile, resourceConfig);
            System.out.println("Pattern supprimé avec succès.");
        } else {
            System.out.println("Aucun pattern correspondant trouvé.");
        }
	}
}