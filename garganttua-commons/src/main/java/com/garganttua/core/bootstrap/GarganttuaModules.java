package com.garganttua.core.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Classpath-wide discovery of every {@code garganttua-*} module that ended up
 * on the runtime classpath, together with its version.
 *
 * <p>Each module's JAR ships a {@code META-INF/MANIFEST.MF} with the entries
 * injected by the parent POM's {@code maven-jar-plugin} configuration:
 * <ul>
 *   <li>{@code Implementation-Title}</li>
 *   <li>{@code Implementation-Version}</li>
 *   <li>{@code Implementation-Vendor}</li>
 *   <li>{@code Garganttua-Module}</li>
 * </ul>
 * Modules are identified by {@code Implementation-Vendor} starting with
 * {@code "com.garganttua"} so third-party JARs don't pollute the listing.
 *
 * <p>{@link #discover()} walks {@link ClassLoader#getResources(String)} for
 * {@code META-INF/MANIFEST.MF}, which returns one URL per JAR on the
 * classpath, parses each manifest, and keeps the qualifying ones. Failures on
 * individual manifests are swallowed — a corrupt JAR never breaks the
 * inventory.
 *
 * @since 2.0.0-ALPHA02
 */
public final class GarganttuaModules {

	private static final String GARGANTTUA_VENDOR_PREFIX = "com.garganttua";
	private static final String MANIFEST_PATH = "META-INF/MANIFEST.MF";

	private GarganttuaModules() {
	}

	/**
	 * Carrier record for a single discovered garganttua-* module.
	 *
	 * @param artifactId the Maven artifactId (e.g. {@code "garganttua-runtime"})
	 * @param version    the {@code project.version} string
	 * @param groupId    the Maven groupId (e.g. {@code "com.garganttua.core"})
	 * @param title      the human-readable {@code project.name}, or
	 *                   {@code artifactId} if absent
	 */
	public record ModuleInfo(String artifactId, String version, String groupId, String title) {
		/**
		 * Validates that every component is non-null.
		 *
		 * @throws NullPointerException if any argument is {@code null}
		 */
		public ModuleInfo {
			Objects.requireNonNull(artifactId, "artifactId");
			Objects.requireNonNull(version, "version");
			Objects.requireNonNull(groupId, "groupId");
			Objects.requireNonNull(title, "title");
		}
	}

	/**
	 * Discover every garganttua-* module on the current thread's context
	 * classloader (falling back to {@link GarganttuaModules}'s own loader).
	 * Results are deduplicated by {@code artifactId} and returned sorted
	 * alphabetically.
	 */
	public static List<ModuleInfo> discover() {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		if (cl == null) {
			cl = GarganttuaModules.class.getClassLoader();
		}
		return discover(cl);
	}

	/**
	 * Discover every garganttua-* module on the given classloader. Results are
	 * deduplicated by {@code artifactId} and returned sorted alphabetically.
	 */
	public static List<ModuleInfo> discover(ClassLoader classLoader) {
		Objects.requireNonNull(classLoader, "classLoader");
		List<ModuleInfo> found = new ArrayList<>();
		java.util.Set<String> seen = new java.util.HashSet<>();
		Enumeration<URL> urls;
		try {
			urls = classLoader.getResources(MANIFEST_PATH);
		} catch (IOException e) {
			return List.of();
		}
		while (urls.hasMoreElements()) {
			URL url = urls.nextElement();
			ModuleInfo info = readManifest(url);
			if (info != null && seen.add(info.artifactId())) {
				found.add(info);
			}
		}
		Collections.sort(found, (a, b) -> a.artifactId().compareTo(b.artifactId()));
		return Collections.unmodifiableList(found);
	}

	private static ModuleInfo readManifest(URL url) {
		try (InputStream in = url.openStream()) {
			Manifest manifest = new Manifest(in);
			Attributes attrs = manifest.getMainAttributes();
			String vendor = attrs.getValue("Implementation-Vendor");
			if (vendor == null || !vendor.startsWith(GARGANTTUA_VENDOR_PREFIX)) {
				return null;
			}
			String artifactId = attrs.getValue("Garganttua-Module");
			String version = attrs.getValue("Implementation-Version");
			String title = attrs.getValue("Implementation-Title");
			if (artifactId == null || version == null) {
				return null;
			}
			return new ModuleInfo(artifactId, version, vendor,
					title != null ? title : artifactId);
		} catch (IOException ignored) {
			return null;
		}
	}
}
