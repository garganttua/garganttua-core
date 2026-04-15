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
 * Merges annotation index files from dependency JARs into the current module's
 * output directory. Index files are located under {@code META-INF/garganttua/index/}
 * and contain one entry per line (class or method reference).
 */
@Mojo(name = "aggregate-index", defaultPhase = LifecyclePhase.PROCESS_CLASSES,
        requiresDependencyResolution = ResolutionScope.COMPILE)
public class AggregateIndexMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true, readonly = true)
    private File outputDirectory;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        Map<String, Set<String>> dependencyEntries = ArtifactScanner.scan(project, AOTMetadataConstants.INDEX_DIR);

        if (dependencyEntries.isEmpty()) {
            getLog().info("No annotation index entries found in dependencies.");
            return;
        }

        int totalFiles = 0;
        int totalEntries = 0;

        for (Map.Entry<String, Set<String>> entry : dependencyEntries.entrySet()) {
            String relativePath = entry.getKey();
            Set<String> mergedEntries = new LinkedHashSet<>(entry.getValue());

            // Merge with existing local entries if present
            Path localFile = outputDirectory.toPath().resolve(AOTMetadataConstants.INDEX_DIR).resolve(relativePath);
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
                    throw new MojoExecutionException("Failed to read existing index file: " + localFile, e);
                }
            }

            // Write merged index
            try {
                Files.createDirectories(localFile.getParent());
                Files.write(localFile, mergedEntries, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to write merged index file: " + localFile, e);
            }

            totalFiles++;
            totalEntries += mergedEntries.size();
        }

        getLog().info("Aggregated " + totalEntries + " index entries across " + totalFiles + " annotation index files.");
    }
}
