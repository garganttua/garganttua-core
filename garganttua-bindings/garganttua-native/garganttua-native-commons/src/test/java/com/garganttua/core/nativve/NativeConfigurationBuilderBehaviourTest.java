package com.garganttua.core.nativve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.JdkClass;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

/**
 * Behaviour tests that genuinely exercise {@link NativeConfigurationBuilder}.
 *
 * <p>The builder's constructor wraps {@code IReflectionBuilder} via
 * {@code IClass.getClass(...)}, so a global {@code IReflection} must be installed
 * first — mirroring the framework contract that {@code IClass.setReflection(...)}
 * is wired before builders are used. Here we install the same test-scoped
 * {@code RuntimeReflectionProvider} used elsewhere in this module's tests. Auto-
 * detection defaults to off and no dependency is provided, so {@code build()}
 * runs no classpath scanning and the assembled {@link NativeConfiguration} is
 * deterministic.</p>
 */
public class NativeConfigurationBuilderBehaviourTest {

    private static final String NATIVE_IMAGE_DIR = "META-INF" + File.separator + "native-image";

    @BeforeAll
    public static void installReflection() {
        IClass.setReflection(new RuntimeReflectionProvider());
    }

    @AfterAll
    public static void clearReflection() {
        IClass.setReflection(null);
    }

    private IClass<?> ic(Class<?> type) {
        return JdkClass.of(type);
    }

    // --- doBuild() null guards ---------------------------------------------

    @Test
    public void buildFailsWhenReflectionPathMissing() {
        var b = NativeConfigurationBuilder.builder().resourcesPath("x");
        NullPointerException ex = assertThrows(NullPointerException.class, b::build);
        assertEquals("Reflection path cannot be null", ex.getMessage());
    }

    @Test
    public void buildFailsWhenResourcesPathMissing() {
        var b = NativeConfigurationBuilder.builder().reflectionPath("x");
        NullPointerException ex = assertThrows(NullPointerException.class, b::build);
        assertEquals("Resouces path cannot be null", ex.getMessage());
    }

    // --- path / namespace / mode setters reject null -----------------------

    @Test
    public void resourcesPathRejectsNull() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> NativeConfigurationBuilder.builder().resourcesPath(null));
        assertEquals("Path cannot be null", ex.getMessage());
    }

    @Test
    public void reflectionPathRejectsNull() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> NativeConfigurationBuilder.builder().reflectionPath(null));
        assertEquals("Path cannot be null", ex.getMessage());
    }

    @Test
    public void modeRejectsNull() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> NativeConfigurationBuilder.builder().mode(null));
        assertEquals("Mode cannot be null", ex.getMessage());
    }

    @Test
    public void resourceRejectsNullString() {
        assertThrows(NullPointerException.class,
                () -> NativeConfigurationBuilder.builder().resource((String) null));
    }

    @Test
    public void resourceRejectsNullClass() {
        assertThrows(NullPointerException.class,
                () -> NativeConfigurationBuilder.builder().resource((IClass<?>) null));
    }

    @Test
    public void configurationBuilderRejectsNull() {
        assertThrows(NullPointerException.class,
                () -> NativeConfigurationBuilder.builder().configurationBuilder(null));
    }

    // --- packages ----------------------------------------------------------

    @Test
    public void packagesAreAccumulatedAndDeDuplicated() {
        var b = NativeConfigurationBuilder.builder()
                .withPackage("com.a")
                .withPackages(new String[] {"com.b", "com.c"})
                .withPackage("com.a"); // duplicate
        String[] pkgs = b.getPackages();
        java.util.Set<String> set = new java.util.HashSet<>(java.util.Arrays.asList(pkgs));
        assertEquals(3, set.size());
        assertTrue(set.contains("com.a"));
        assertTrue(set.contains("com.b"));
        assertTrue(set.contains("com.c"));
    }

    @Test
    public void emptyPackagesByDefault() {
        assertEquals(0, NativeConfigurationBuilder.builder().getPackages().length);
    }

    // --- end-to-end build, asserting serialized structure ------------------

    @Test
    public void buildWithNoEntriesProducesEmptyReflectArray() throws IOException {
        INativeConfiguration cfg = NativeConfigurationBuilder.builder()
                .reflectionPath("r").resourcesPath("s")
                .build();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        cfg.writeReflectionConfiguration(new ByteArrayInputStream(new byte[0]), out);
        assertEquals("[ ]", out.toString().trim());
    }

    @Test
    public void reflectionEntryByClassIsIncludedInTheBuiltConfiguration() throws IOException {
        var b = NativeConfigurationBuilder.builder()
                .reflectionPath("r").resourcesPath("s");
        // reflectionEntry returns the entry builder; configuring it mutates the
        // entry held by reference inside the native builder.
        b.reflectionEntry(ic(String.class)).allDeclaredFields(true);
        INativeConfiguration cfg = b.build();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        cfg.writeReflectionConfiguration(new ByteArrayInputStream(new byte[0]), out);
        String json = out.toString();
        assertTrue(json.contains("\"name\" : \"java.lang.String\""), json);
        assertTrue(json.contains("\"allDeclaredFields\" : true"), json);
    }

    @Test
    public void twoEntriesWithSameNameCollapseSinceBuiltEntryEqualityIsNameOnly() throws IOException {
        // doBuild() maps each entry builder via build() into a Set<entry>;
        // ReflectConfigEntry.equals()/hashCode include the name, so two
        // separately-registered entries for the same class collapse to one
        // when the resulting set is serialized.
        var b = NativeConfigurationBuilder.builder()
                .reflectionPath("r").resourcesPath("s");
        b.reflectionEntry(ic(String.class));
        b.reflectionEntry(ic(String.class));
        INativeConfiguration cfg = b.build();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        cfg.writeReflectionConfiguration(new ByteArrayInputStream(new byte[0]), out);
        String json = out.toString();
        int first = json.indexOf("java.lang.String");
        int last = json.lastIndexOf("java.lang.String");
        assertTrue(first >= 0, "class name should appear: " + json);
        assertEquals(first, last, "class name should appear exactly once: " + json);
    }

    @Test
    public void resourceStringIsQuotedWithRegexLiteralMarkers() throws IOException {
        INativeConfiguration cfg = NativeConfigurationBuilder.builder()
                .reflectionPath("r").resourcesPath("s")
                .resource("config/app.properties")
                .build();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        cfg.writeResourcesConfiguration(new ByteArrayInputStream(new byte[0]), out);
        String json = out.toString();
        // resource(String) wraps the pattern in \Q...\E literal-quote markers
        assertTrue(json.contains("\\\\Qconfig/app.properties\\\\E"), json);
    }

    @Test
    public void resourceByClassDerivesSlashSeparatedClassFilePattern() throws IOException {
        INativeConfiguration cfg = NativeConfigurationBuilder.builder()
                .reflectionPath("r").resourcesPath("s")
                .resource(ic(String.class))
                .build();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        cfg.writeResourcesConfiguration(new ByteArrayInputStream(new byte[0]), out);
        String json = out.toString();
        assertTrue(json.contains("java/lang/String.class"), json);
    }

    @Test
    public void buildWritesNamespacedReflectFileWhenNamespaceSet(@TempDir Path dir) {
        var b = NativeConfigurationBuilder.builder()
                .reflectionPath(dir.toString()).resourcesPath(dir.toString())
                .configNamespace("com.acme/widget");
        b.reflectionEntry(ic(Integer.class));
        INativeConfiguration cfg = b.build();

        cfg.writeReflectionConfiguration();
        Path expected = dir.resolve(NATIVE_IMAGE_DIR)
                .resolve("com.acme").resolve("widget").resolve("reflect-config.json");
        assertTrue(Files.exists(expected), "expected namespaced file at " + expected);
    }

    @Test
    public void blankNamespaceCollapsesToFlatLayout(@TempDir Path dir) {
        INativeConfiguration cfg = NativeConfigurationBuilder.builder()
                .reflectionPath(dir.toString()).resourcesPath(dir.toString())
                .configNamespace("   ")
                .build();

        cfg.writeReflectionConfiguration();
        assertTrue(Files.exists(dir.resolve(NATIVE_IMAGE_DIR).resolve("reflect-config.json")));
    }

    @Test
    public void nullNamespaceIsTreatedAsEmptyAndDoesNotThrow(@TempDir Path dir) {
        INativeConfiguration cfg = NativeConfigurationBuilder.builder()
                .reflectionPath(dir.toString()).resourcesPath(dir.toString())
                .configNamespace(null)
                .build();
        cfg.writeReflectionConfiguration();
        assertTrue(Files.exists(dir.resolve(NATIVE_IMAGE_DIR).resolve("reflect-config.json")));
    }

    @Test
    public void mergeModeBuildsSuccessfully() throws IOException {
        // mode is accepted; it does not alter the JSON shape but must not break build.
        INativeConfiguration cfg = NativeConfigurationBuilder.builder()
                .reflectionPath("r").resourcesPath("s")
                .mode(NativeConfigurationMode.merge)
                .build();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        cfg.writeReflectionConfiguration(new ByteArrayInputStream(new byte[0]), out);
        assertEquals("[ ]", out.toString().trim());
    }

    @Test
    public void buildIsIdempotentReturningCachedInstance() {
        var b = NativeConfigurationBuilder.builder().reflectionPath("r").resourcesPath("s");
        INativeConfiguration first = b.build();
        INativeConfiguration second = b.build();
        assertEquals(first, second);
    }

    @Test
    public void entryFromExistingDefinitionIsRebuiltAndIncluded() throws IOException, DslException {
        com.garganttua.core.nativve.image.config.reflection.ReflectConfigEntry existing =
                new com.garganttua.core.nativve.image.config.reflection.ReflectConfigEntry("java.lang.Double");
        existing.setFields(new java.util.ArrayList<>());
        existing.setMethods(new java.util.ArrayList<>());

        var b = NativeConfigurationBuilder.builder()
                .reflectionPath("r").resourcesPath("s");
        b.reflectionEntry(existing).field("value");
        INativeConfiguration cfg = b.build();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        cfg.writeReflectionConfiguration(new ByteArrayInputStream(new byte[0]), out);
        String json = out.toString();
        assertTrue(json.contains("java.lang.Double"), json);
        assertTrue(json.contains("\"value\""), json);
    }

    @Test
    public void resourcesWithNoPatternsHaveNoPatternKey() throws IOException {
        INativeConfiguration cfg = NativeConfigurationBuilder.builder()
                .reflectionPath("r").resourcesPath("s")
                .build();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        cfg.writeResourcesConfiguration(new ByteArrayInputStream(new byte[0]), out);
        String json = out.toString();
        assertTrue(json.contains("includes"), json);
        assertFalse(json.contains("pattern"), json);
    }
}
