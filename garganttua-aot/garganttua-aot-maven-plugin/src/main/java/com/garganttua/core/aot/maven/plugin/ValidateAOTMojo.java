package com.garganttua.core.aot.maven.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.garganttua.core.aot.commons.AOTMetadataConstants;

/**
 * Validates that all {@code @Reflected} types have corresponding AOT class
 * descriptors. Issues warnings for missing descriptors but does not fail the
 * build, allowing gradual AOT adoption.
 */
@Mojo(name = "validate-aot", defaultPhase = LifecyclePhase.VERIFY)
public class ValidateAOTMojo extends AbstractMojo {

    private static final String REFLECTED_ANNOTATION_FQN = "com.garganttua.core.reflection.annotations.Reflected";

    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true, readonly = true)
    private File outputDirectory;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        Path indexFile = outputDirectory.toPath()
                .resolve(AOTMetadataConstants.INDEX_DIR)
                .resolve(REFLECTED_ANNOTATION_FQN);

        if (!Files.exists(indexFile)) {
            getLog().info("No @Reflected index file found — skipping AOT validation.");
            return;
        }

        // Read all @Reflected class entries
        List<String> reflectedClasses = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(indexFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith(AOTMetadataConstants.CLASS_ENTRY_PREFIX)) {
                    String fqn = trimmed.substring(AOTMetadataConstants.CLASS_ENTRY_PREFIX.length());
                    reflectedClasses.add(fqn);
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to read @Reflected index file: " + indexFile, e);
        }

        if (reflectedClasses.isEmpty()) {
            getLog().info("No @Reflected class entries found — nothing to validate.");
            return;
        }

        // Check for corresponding AOT descriptors
        Path aotClassesDir = outputDirectory.toPath().resolve(AOTMetadataConstants.AOT_CLASSES_DIR);
        int withDescriptor = 0;
        int withoutDescriptor = 0;

        for (String fqn : reflectedClasses) {
            Path descriptorFile = aotClassesDir.resolve(fqn);
            if (Files.exists(descriptorFile)) {
                withDescriptor++;
            } else {
                withoutDescriptor++;
                getLog().warn("@Reflected class missing AOT descriptor: " + fqn);
            }
        }

        int total = reflectedClasses.size();
        getLog().info("AOT validation: " + withDescriptor + "/" + total + " @Reflected classes have AOT descriptors.");

        if (withoutDescriptor > 0) {
            getLog().warn(withoutDescriptor + " @Reflected class(es) lack AOT descriptors. "
                    + "Run the AOT annotation processor to generate them.");
        }
    }
}
