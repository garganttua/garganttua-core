package com.garganttua.core.nativve.image.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.nativve.INativeConfiguration;
import com.garganttua.core.nativve.INativeConfigurationBuilder;
import com.garganttua.core.nativve.NativeConfigurationBuilder;
import com.garganttua.core.nativve.image.config.reflection.ReflectConfigEntry;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

/**
 * Maven {@link AbstractMojo} that generates GraalVM Native Image configuration for the
 * current project.
 * <p>
 * Bound to the {@code prepare-package} phase, the {@code native-config} goal scans the
 * project classpath for reflection metadata, validates and registers declared resources,
 * aggregates {@code META-INF/native-image} configs found in dependency JARs, and writes
 * {@code reflect-config.json} and {@code resource-config.json} under
 * {@code META-INF/native-image/<namespace>}.
 */
@Mojo(name = "native-config", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class NativeConfigMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project.build.directory}", readonly = true)
	private File buildDirectory;

	@Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true)
	private String buildOutputDirectory;

	@Parameter(defaultValue = "${project.artifacts}", readonly = true)
	private Set<Artifact> artifacts;

	@Parameter(defaultValue = "${project.basedir}", readonly = true)
	private String baseDir;

	@Parameter(defaultValue = "${project.build.resources[0].directory}", readonly = true)
	private File resourcesDirectory;

	@Parameter(property = "resources")
	private List<String> resources;

	@Parameter(property = "reflections")
	private List<ReflectConfigEntry> reflections;

	@Parameter(property = "packages")
	private List<String> packages;

	@Parameter(property = "dependencies")
	private List<Dependency> dependencies;

	@Parameter(defaultValue = "${project.groupId}", readonly = true)
	private String projectGroupId;

	@Parameter(defaultValue = "${project.artifactId}", readonly = true)
	private String projectArtifactId;

	/**
	 * Sub-path under {@code META-INF/native-image/} where configs are written.
	 * Defaults to {@code <groupId>/<artifactId>} (GraalVM-recommended, unique per
	 * artifact — avoids collisions in uber-jars / aggregated builds). Set to an
	 * empty value to fall back to the flat (legacy) layout.
	 */
	@Parameter(property = "configOutputNamespace")
	private String configOutputNamespace;

	/**
	 * Runs the {@code native-config} goal: installs the reflection facade, builds the
	 * {@link INativeConfiguration} from declared reflections/resources/packages, merges
	 * native-image configs from project artifacts, then writes the reflection and resource
	 * configuration files.
	 *
	 * @throws MojoExecutionException if the output directory cannot be created, a declared
	 *         resource is missing, an artifact cannot be processed, or the native
	 *         configuration cannot be configured or built
	 */
	@Override
	public void execute() throws MojoExecutionException {
		getLog().info("Generating Native-image configuration in directory: " + this.buildOutputDirectory);

		IReflectionBuilder reflectionBuilder = ReflectionBuilder.builder()
				.withProvider(new RuntimeReflectionProvider())
				.withScanner(new ReflectionsAnnotationScanner());
		// Install the reflection facade before constructing NativeConfigurationBuilder:
		// its constructor calls IClass.getClass(...), which needs IClass.setReflection()
		// already set (otherwise: "No IReflection available").
		IClass.setReflection(reflectionBuilder.build());

		File outputDir = new File(buildDirectory, "classes/META-INF/native-image");
		if (!outputDir.exists() && !outputDir.mkdirs()) {
			throw new MojoExecutionException("Failed to create directory: " +
					outputDir.getAbsolutePath());
		}

		INativeConfigurationBuilder builder;
		try {
			builder = NativeConfigurationBuilder.builder()
					.reflectionPath(this.buildOutputDirectory)
					.resourcesPath(this.buildOutputDirectory)
					.configNamespace(resolveConfigNamespace())
					.autoDetect(true)
					.withPackages(this.packages.toArray(new String[0]));
			builder.provide(reflectionBuilder);
		} catch (DslException e) {
			throw new MojoExecutionException("Failed to configure native builder", e);
		}

		this.reflections.forEach(builder::reflectionEntry);

		try {
			this.validateFiles(resources, builder);
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage());
		}

		for (Artifact artifact : artifacts) {
			try {
				processArtifact(artifact, outputDir);
			} catch (IOException e) {
				throw new MojoExecutionException("Failed to process artifact: " + artifact,
						e);
			}
		}

		INativeConfiguration configuration;
		try {
			configuration = builder.build();
		} catch (DslException e) {
			throw new MojoExecutionException("Failed to build native configuration", e);
		}

		getLog().info("Writing Native-image configuration");
		configuration.writeReflectionConfiguration();
		configuration.writeResourcesConfiguration();
	}

	/**
	 * Resolves the {@code META-INF/native-image/} sub-path: the explicit
	 * {@code configOutputNamespace} when provided (empty string = flat layout),
	 * otherwise {@code <groupId>/<artifactId>}.
	 */
	private String resolveConfigNamespace() {
		if (this.configOutputNamespace != null) {
			return this.configOutputNamespace.trim();
		}
		return this.projectGroupId + "/" + this.projectArtifactId;
	}

	/**
	 * Validates that each declared resource exists under the project's resources directory
	 * and registers it on the given builder.
	 *
	 * @param filePaths the resource paths (relative to the resources directory) to register
	 * @param builder   the native configuration builder to register the resources on
	 * @throws IOException if any declared resource does not exist
	 */
	public void validateFiles(List<String> filePaths, INativeConfigurationBuilder builder) throws IOException {

		String resourcesPath = resourcesDirectory.getAbsolutePath();

		for (String filePath : filePaths) {
			getLog().debug("Native-image adding resource " + filePath);
			File file = new File(resourcesPath + File.separator + filePath);
			if (file.exists()) {
				builder.resource(filePath);
			} else {
				throw new IOException("Native-image resource " + filePath + " does not exist in " + resourcesPath);
			}
			getLog().info("Native-image resource added " + filePath);
		}
	}

	private void processArtifact(Artifact artifact, File outputDir) throws IOException {
		File artifactFile = artifact.getFile();
		getLog().info("Looking for Native-image files into: " +
				artifact.getArtifactId());
		if (artifactFile == null || !artifactFile.exists() ||
				!artifactFile.getName().endsWith(".jar")) {
			return;
		}

		try (ZipFile zipFile = new ZipFile(artifactFile)) {
			ZipEntry nativeImageDir = zipFile.getEntry("META-INF/native-image/");
			if (nativeImageDir == null) {
				return;
			}

			getLog().info("Native-image files detected into: " +
					artifact.getArtifactId());

			String artifactPath = artifact.getGroupId() + File.separator
					+ artifact.getArtifactId() + File.separator + artifact.getVersion();

			File destinationDir = new File(outputDir, artifactPath);
			if (!destinationDir.exists() && !destinationDir.mkdirs()) {
				throw new IOException("Failed to create directory: " +
						destinationDir.getAbsolutePath());
			}

			zipFile.stream()
					.filter(entry -> entry.getName().startsWith("META-INF/native-image/") && !entry.isDirectory())
					.forEach(entry -> {
						try {
							File destFile = new File(destinationDir,
									entry.getName().substring("META-INF/native-image/".length()));

							// Crée les dossiers parents si nécessaire
							File parentDir = destFile.getParentFile();
							if (!parentDir.exists() && !parentDir.mkdirs()) {
								getLog().error("Failed to create directory: " + parentDir.getAbsolutePath());
								return;
							}

							try (InputStream is = zipFile.getInputStream(entry)) {
								java.nio.file.Files.copy(is, destFile.toPath(),
										java.nio.file.StandardCopyOption.REPLACE_EXISTING);
							}

						} catch (IOException e) {
							getLog().error("Failed to copy file: " + entry.getName(), e);
						}
					});
		}
	}

}