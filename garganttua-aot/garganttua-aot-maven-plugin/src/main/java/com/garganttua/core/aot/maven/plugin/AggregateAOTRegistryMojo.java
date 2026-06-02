package com.garganttua.core.aot.maven.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import com.garganttua.core.aot.commons.AOTMetadataConstants;

/**
 * Merges AOT class descriptor listings from dependency JARs into the current
 * module's output directory. Descriptor listings are located under
 * {@code META-INF/garganttua/aot/classes/}.
 */
@Mojo(name = "aggregate-registry", defaultPhase = LifecyclePhase.PROCESS_CLASSES,
        requiresDependencyResolution = ResolutionScope.COMPILE)
public class AggregateAOTRegistryMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true, readonly = true)
    private File outputDirectory;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /**
     * Scans dependency JARs for AOT class descriptor listings, merges them with
     * any local listings into the module output directory, and emits GraalVM
     * native-image reachability metadata for the collected descriptors.
     *
     * @throws MojoExecutionException if reading or writing a registry file, or
     *                                emitting native-image metadata, fails
     */
    @Override
    public void execute() throws MojoExecutionException {
        Map<String, Set<String>> dependencyEntries = ArtifactScanner.scan(project, AOTMetadataConstants.AOT_CLASSES_DIR);

        if (dependencyEntries.isEmpty()) {
            getLog().info("No AOT class descriptor entries found in dependencies.");
            return;
        }

        int totalFiles = 0;
        int totalEntries = 0;

        for (Map.Entry<String, Set<String>> entry : dependencyEntries.entrySet()) {
            String relativePath = entry.getKey();
            Set<String> mergedEntries = new LinkedHashSet<>(entry.getValue());

            // Merge with existing local entries if present
            Path localFile = outputDirectory.toPath().resolve(AOTMetadataConstants.AOT_CLASSES_DIR).resolve(relativePath);
            if (Files.exists(localFile)) {
                try (BufferedReader reader = Files.newBufferedReader(localFile, StandardCharsets.UTF_8)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String trimmed = line.trim();
                        if (!trimmed.isEmpty()) {
                            mergedEntries.add(trimmed);
                        }
                    }
                } catch (IOException e) {
                    throw new MojoExecutionException("Failed to read existing AOT registry file: " + localFile, e);
                }
            }

            // Write merged registry
            try {
                Files.createDirectories(localFile.getParent());
                Files.write(localFile, mergedEntries, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to write merged AOT registry file: " + localFile, e);
            }

            totalFiles++;
            totalEntries += mergedEntries.size();
        }

        getLog().info("Aggregated " + totalEntries + " entries across " + totalFiles + " AOT class descriptor files.");

        // Generate GraalVM native-image reachability metadata so the
        // AOTClass_* descriptors stay in the closed-world image, and the
        // META-INF/garganttua/aot/classes/ + META-INF/services/...
        // resources are visible at runtime in the native binary.
        try {
            generateNativeImageMetadata(dependencyEntries);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to write native-image reachability metadata", e);
        }
    }

    /**
     * Collect every AOTClass_* FQN from the merged listings + the local
     * compile output, then emit {@code reflect-config.json} and
     * {@code resource-config.json} under
     * {@code META-INF/native-image/<groupId>/<artifactId>/}. GraalVM
     * native-image consumes these automatically — no extra configuration on
     * the user side.
     */
    private void generateNativeImageMetadata(Map<String, Set<String>> dependencyEntries) throws IOException {
        Set<String> allDescriptors = new LinkedHashSet<>();
        for (Set<String> entries : dependencyEntries.values()) {
            allDescriptors.addAll(entries);
        }

        // Re-walk the local output to catch entries written in the same build
        // (the loop above only saw dependency JARs).
        Path classesDir = outputDirectory.toPath().resolve(AOTMetadataConstants.AOT_CLASSES_DIR);
        if (Files.isDirectory(classesDir)) {
            try (var stream = Files.list(classesDir)) {
                stream.filter(Files::isRegularFile).forEach(p -> {
                    try (BufferedReader br = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            String t = line.trim();
                            if (!t.isEmpty() && !t.startsWith("#")) {
                                allDescriptors.add(t);
                            }
                        }
                    } catch (IOException ignored) {
                        // best-effort
                    }
                });
            }
        }

        if (allDescriptors.isEmpty()) {
            getLog().info("No AOT descriptors → skipping native-image metadata generation.");
            return;
        }

        String groupId = project.getGroupId();
        String artifactId = project.getArtifactId();
        Path metaDir = outputDirectory.toPath()
                .resolve("META-INF/native-image")
                .resolve(groupId)
                .resolve(artifactId);
        Files.createDirectories(metaDir);

        writeReflectConfig(metaDir.resolve("reflect-config.json"), allDescriptors);
        writeResourceConfig(metaDir.resolve("resource-config.json"));

        getLog().info("Wrote native-image reachability metadata for "
                + allDescriptors.size() + " AOTClass_* descriptors under "
                + outputDirectory.toPath().relativize(metaDir));
    }

    private void writeReflectConfig(Path target, Set<String> descriptors) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        int i = 0;
        for (String fqn : descriptors) {
            if (i++ > 0) {
                sb.append(",\n");
            }
            sb.append("  {\n")
              .append("    \"name\": \"").append(fqn).append("\",\n")
              .append("    \"allDeclaredConstructors\": true,\n")
              .append("    \"allDeclaredMethods\": false,\n")
              .append("    \"allDeclaredFields\": false\n")
              .append("  }");
        }
        sb.append("\n]\n");
        Files.writeString(target, sb.toString(), StandardCharsets.UTF_8);
    }

    private void writeResourceConfig(Path target) throws IOException {
        String json = "{\n"
                + "  \"resources\": {\n"
                + "    \"includes\": [\n"
                + "      { \"pattern\": \"" + AOTMetadataConstants.AOT_CLASSES_DIR + ".*\" },\n"
                + "      { \"pattern\": \"META-INF/services/com\\\\.garganttua\\\\.core\\\\.aot\\\\.commons\\\\.IAOTSelfRegistering\" }\n"
                + "    ]\n"
                + "  }\n"
                + "}\n";
        Files.writeString(target, json, StandardCharsets.UTF_8);
    }
}
