package com.garganttua.core.aot.maven.plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

/**
 * Scans dependency JARs for resource entries under a given prefix and collects
 * their contents as sets of lines, keyed by relative path.
 */
final class ArtifactScanner {

    private ArtifactScanner() {
    }

    /**
     * Scans all resolved dependency JARs of the given project for entries under
     * {@code resourcePrefix}. For each matching entry, reads all non-blank lines
     * and collects them into a set, keyed by the entry path relative to the prefix.
     *
     * @param project        the Maven project whose artifacts to scan
     * @param resourcePrefix the resource directory prefix (e.g. "META-INF/garganttua/index/")
     * @return a map from relative path to collected entry lines
     */
    static Map<String, Set<String>> scan(MavenProject project, String resourcePrefix) {
        Map<String, Set<String>> result = new LinkedHashMap<>();

        for (Artifact artifact : project.getArtifacts()) {
            if (artifact.getFile() == null || !artifact.getFile().getName().endsWith(".jar")) {
                continue;
            }

            try (JarFile jar = new JarFile(artifact.getFile())) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (!name.startsWith(resourcePrefix) || entry.isDirectory()) {
                        continue;
                    }

                    String relativePath = name.substring(resourcePrefix.length());
                    if (relativePath.isEmpty()) {
                        continue;
                    }

                    Set<String> lines = result.computeIfAbsent(relativePath, k -> new LinkedHashSet<>());
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(jar.getInputStream(entry), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String trimmed = line.trim();
                            if (!trimmed.isEmpty()) {
                                lines.add(trimmed);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                // Skip unreadable JARs silently — they may be directories or non-standard artifacts
            }
        }

        return result;
    }
}
