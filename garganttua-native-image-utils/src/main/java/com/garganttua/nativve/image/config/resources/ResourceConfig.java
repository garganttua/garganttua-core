package com.garganttua.nativve.image.config.resources;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ResourceConfig {

private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // Ajout d'une ressource
    @SuppressWarnings("unchecked")
    public static void addResource(File resourceConfigFile, Class<?> clazz) throws IOException {
        String classPath = clazz.getName().replace('.', '/') + ".class";
        String newPattern = "\\Q" + classPath + "\\E";

        // Si le fichier n'existe pas ou est vide, initialiser une nouvelle structure
        Map<String, Object> resourceConfig = new HashMap<>();
        if (resourceConfigFile.exists() && resourceConfigFile.length() > 0) {
            resourceConfig = objectMapper.readValue(resourceConfigFile, new TypeReference<Map<String, Object>>() {});
        }

        // Récupérer ou initialiser la section 'resources'
        Map<String, List<Map<String, String>>> resources = (Map<String, List<Map<String, String>>>) resourceConfig
                .computeIfAbsent("resources", k -> new HashMap<>());

        List<Map<String, String>> includes = resources.computeIfAbsent("includes", k -> new ArrayList<>());

        // Vérification si le pattern existe déjà
        boolean exists = includes.stream().anyMatch(entry -> newPattern.equals(entry.get("pattern")));

        if (!exists) {
            includes.add(Map.of("pattern", newPattern));
            // Écriture dans le fichier avec un format lisible
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(resourceConfigFile, resourceConfig);
            System.out.println("Pattern ajouté avec succès.");
        } else {
            System.out.println("Le pattern existe déjà.");
        }
    }

    // Suppression d'une ressource
    @SuppressWarnings("unchecked")
    public static void removeResource(File resourceConfigFile, Class<?> clazz) throws IOException {
        String classPath = clazz.getName().replace('.', '/') + ".class";
        String patternToRemove = "\\Q" + classPath + "\\E";

        if (!resourceConfigFile.exists()) {
            System.out.println("Le fichier n'existe pas : aucune suppression effectuée.");
            return;
        }

        // Si le fichier est vide, on s'arrête là
        if (resourceConfigFile.length() == 0) {
            System.out.println("Le fichier est vide : aucune suppression effectuée.");
            return;
        }

        Map<String, Object> resourceConfig = objectMapper.readValue(resourceConfigFile, new TypeReference<Map<String, Object>>() {});

        // Vérification de la présence des sections nécessaires
        Map<String, List<Map<String, String>>> resources = (Map<String, List<Map<String, String>>>) resourceConfig.get("resources");
        if (resources == null || !resources.containsKey("includes")) {
            System.out.println("Aucune section 'includes' trouvée dans le fichier : aucune suppression effectuée.");
            return;
        }

        List<Map<String, String>> includes = resources.get("includes");

        // Tentative de suppression du pattern
        boolean removed = includes.removeIf(entry -> patternToRemove.equals(entry.get("pattern")));

        if (removed) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(resourceConfigFile, resourceConfig);
            System.out.println("Pattern supprimé avec succès.");
        } else {
            System.out.println("Aucun pattern correspondant trouvé.");
        }
    }
}