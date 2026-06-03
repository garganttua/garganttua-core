package com.garganttua.core.nativve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.garganttua.core.nativve.image.config.reflection.ReflectConfigEntry;

/**
 * Behaviour tests for {@link NativeConfiguration}: constructor null-validation,
 * the namespaced vs flat output layout, JSON shape written to streams, and the
 * end-to-end file writers.
 */
public class NativeConfigurationBehaviourTest {

    private static final String NATIVE_IMAGE_DIR = "META-INF" + File.separator + "native-image";

    private NativeConfiguration config(Set<IReflectionConfigurationEntry> entries, Set<String> resources,
            String resPath, String reflPath, String ns) {
        return new NativeConfiguration(NativeConfigurationMode.override, entries, resources, resPath, reflPath, ns);
    }

    @Test
    public void constructorRejectsNullMode() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> new NativeConfiguration(null, Set.of(), Set.of(), "", ""));
        assertEquals("Mode cannot be null", ex.getMessage());
    }

    @Test
    public void constructorRejectsNullCollect() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> new NativeConfiguration(NativeConfigurationMode.override, null, Set.of(), "", ""));
        assertEquals("Reflection entries cannot be null", ex.getMessage());
    }

    @Test
    public void constructorRejectsNullResources() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> new NativeConfiguration(NativeConfigurationMode.override, Set.of(), null, "", ""));
        assertEquals("Resources cannot be null", ex.getMessage());
    }

    @Test
    public void constructorRejectsNullResourcesPath() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> new NativeConfiguration(NativeConfigurationMode.override, Set.of(), Set.of(), null, ""));
        assertEquals("Resources path cannot be null", ex.getMessage());
    }

    @Test
    public void constructorRejectsNullReflectionPath() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> new NativeConfiguration(NativeConfigurationMode.override, Set.of(), Set.of(), "", null));
        assertEquals("Reflection path cannot be null", ex.getMessage());
    }

    @Test
    public void nullNamespaceIsTreatedAsFlatLayout(@TempDir Path dir) throws IOException {
        NativeConfiguration cfg = config(Set.of(new ReflectConfigEntry("a.B")), Set.of(),
                dir.toString(), dir.toString(), null);
        cfg.writeReflectionConfiguration();
        // flat layout: file lands directly under META-INF/native-image
        Path expected = dir.resolve(NATIVE_IMAGE_DIR).resolve("reflect-config.json");
        assertTrue(Files.exists(expected), "expected flat-layout file at " + expected);
    }

    @Test
    public void writeReflectionToStreamSerializesEntriesAsJsonArray() throws IOException {
        NativeConfiguration cfg = config(
                Set.of(new ReflectConfigEntry("com.example.Alpha")),
                Set.of(), "", "", "");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in = new ByteArrayInputStream(new byte[0]);

        cfg.writeReflectionConfiguration(in, out);
        String json = out.toString();

        assertTrue(json.trim().startsWith("["), "reflect config must be a JSON array: " + json);
        assertTrue(json.contains("\"name\" : \"com.example.Alpha\""), json);
    }

    @Test
    public void writeReflectionToStreamWithNoEntriesProducesEmptyArray() throws IOException {
        NativeConfiguration cfg = config(Set.of(), Set.of(), "", "", "");
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        cfg.writeReflectionConfiguration(new ByteArrayInputStream(new byte[0]), out);

        assertEquals("[ ]", out.toString().trim());
    }

    @Test
    public void writeResourcesToStreamWrapsPatternsUnderResourcesIncludes() throws IOException {
        NativeConfiguration cfg = config(Set.of(), Set.of("foo/Bar.class", "baz/Qux.class"), "", "", "");
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        cfg.writeResourcesConfiguration(new ByteArrayInputStream(new byte[0]), out);
        String json = out.toString();

        assertTrue(json.contains("\"resources\""), json);
        assertTrue(json.contains("\"includes\""), json);
        assertTrue(json.contains("\"pattern\""), json);
        assertTrue(json.contains("foo/Bar.class"), json);
        assertTrue(json.contains("baz/Qux.class"), json);
    }

    @Test
    public void writeResourcesToStreamWithNoPatternsStillHasEmptyIncludesArray() throws IOException {
        NativeConfiguration cfg = config(Set.of(), Set.of(), "", "", "");
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        cfg.writeResourcesConfiguration(new ByteArrayInputStream(new byte[0]), out);
        String json = out.toString();

        assertTrue(json.contains("\"resources\""), json);
        assertTrue(json.contains("\"includes\""), json);
        assertFalse(json.contains("\"pattern\""), json);
    }

    @Test
    public void writeReflectionConfigurationCreatesNamespacedFileWithContent(@TempDir Path dir) throws IOException {
        NativeConfiguration cfg = config(
                Set.of(new ReflectConfigEntry("com.example.Namespaced")),
                Set.of(), dir.toString(), dir.toString(), "com.acme/widget");

        cfg.writeReflectionConfiguration();

        Path expected = dir.resolve(NATIVE_IMAGE_DIR)
                .resolve("com.acme").resolve("widget").resolve("reflect-config.json");
        assertTrue(Files.exists(expected), "expected namespaced file at " + expected);
        String content = Files.readString(expected);
        assertTrue(content.contains("com.example.Namespaced"), content);
    }

    @Test
    public void writeResourcesConfigurationCreatesFileWithPatterns(@TempDir Path dir) throws IOException {
        NativeConfiguration cfg = config(Set.of(), Set.of("res/data.txt"),
                dir.toString(), dir.toString(), "");

        cfg.writeResourcesConfiguration();

        Path expected = dir.resolve(NATIVE_IMAGE_DIR).resolve("resource-config.json");
        assertTrue(Files.exists(expected));
        String content = Files.readString(expected);
        assertTrue(content.contains("res/data.txt"), content);
        assertTrue(content.contains("includes"), content);
    }

    @Test
    public void writeReflectionConfigurationFailsWhenBaseDirIsExistingFile(@TempDir Path dir) throws IOException {
        // Point the base path at a regular file, so mkdirs() under it cannot succeed.
        Path file = dir.resolve("not-a-dir");
        Files.writeString(file, "x");
        NativeConfiguration cfg = config(Set.of(), Set.of(), file.toString(), file.toString(), "");

        assertThrows(NativeException.class, cfg::writeReflectionConfiguration);
    }

    @Test
    public void namespaceIsTrimmedWhitespaceOnly(@TempDir Path dir) throws IOException {
        // A blank (whitespace) namespace trims to empty -> flat layout, not a
        // directory literally named " ".
        NativeConfiguration cfg = config(Set.of(new ReflectConfigEntry("a.B")), Set.of(),
                dir.toString(), dir.toString(), "   ");
        cfg.writeReflectionConfiguration();
        Path flat = dir.resolve(NATIVE_IMAGE_DIR).resolve("reflect-config.json");
        assertTrue(Files.exists(flat), "blank namespace must collapse to flat layout");
    }
}
