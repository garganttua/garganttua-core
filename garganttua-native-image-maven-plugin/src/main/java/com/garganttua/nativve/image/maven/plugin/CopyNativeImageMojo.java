package com.garganttua.nativve.image.maven.plugin;

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
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.io.InputStreamFacade;

/**
 * Maven Plugin to copy META-INF/native-image files from dependencies.
 */
@Mojo(name = "copy-native-image", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class CopyNativeImageMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project.build.directory}", readonly = true)
	private File buildDirectory;

	@Parameter(defaultValue = "${project.artifacts}", readonly = true)
	private Set<Artifact> artifacts;

	@Parameter(property = "dependencies")
	private List<Dependency> dependencies;

	@Override
	public void execute() throws MojoExecutionException {
		getLog().info("Looking for Native-image files into " + artifacts.size() + " artefacts");
		File outputDir = new File(buildDirectory, "classes/META-INF/native-image");
		if (!outputDir.exists() && !outputDir.mkdirs()) {
			throw new MojoExecutionException("Failed to create directory: " + outputDir.getAbsolutePath());
		}

		for (Artifact artifact : artifacts) {
			try {
				processArtifact(artifact, outputDir);
			} catch (IOException e) {
				throw new MojoExecutionException("Failed to process artifact: " + artifact, e);
			}
		}

		getLog().info("Native-image files copied successfully to: " + outputDir.getAbsolutePath());
	}

	private void processArtifact(Artifact artifact, File outputDir) throws IOException {
		File artifactFile = artifact.getFile();
		getLog().info("Looking for Native-image files into: " + artifact.getArtifactId());
		if (artifactFile == null || !artifactFile.exists() || !artifactFile.getName().endsWith(".jar")) {
			return;
		}

		try (ZipFile zipFile = new ZipFile(artifactFile)) {
			ZipEntry nativeImageDir = zipFile.getEntry("META-INF/native-image/");
			if (nativeImageDir == null) {
				return; // No native-image directory in this artifact
			}

			getLog().info("Native-image files detected into: " + artifact.getArtifactId());

			String artifactPath = artifact.getGroupId()/*.replace('.', File.separatorChar) */+ File.separator+ artifact.getArtifactId() + File.separator + artifact.getVersion();

			File destinationDir = new File(outputDir, artifactPath);
			if (!destinationDir.exists() && !destinationDir.mkdirs()) {
				throw new IOException("Failed to create directory: " + destinationDir.getAbsolutePath());
			}

			zipFile.stream()
					.filter(entry -> entry.getName().startsWith("META-INF/native-image/") && !entry.isDirectory())
					.forEach(entry -> {
						try {
							File destFile = new File(destinationDir,
									entry.getName().substring("META-INF/native-image/".length()));
							FileUtils.copyStreamToFile(new SimpleInputStreamFacade(zipFile.getInputStream(entry)),
									destFile);
						} catch (IOException e) {
							getLog().error("Failed to copy file: " + entry.getName(), e);
						}
					});
		}
	}

	public class SimpleInputStreamFacade implements InputStreamFacade {
		private final InputStream inputStream;

		public SimpleInputStreamFacade(InputStream inputStream) {
			this.inputStream = inputStream;
		}

		@Override
		public InputStream getInputStream() {
			return inputStream;
		}
	}
}