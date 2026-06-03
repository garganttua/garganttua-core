package com.garganttua.core.nativve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.garganttua.core.nativve.image.config.reflection.ReflectConfigEntry;

/**
 * Further behaviour tests for {@link NativeConfiguration} targeting branches the
 * first-pass {@code NativeConfigurationBehaviourTest} leaves uncovered: end-to-end
 * reflection-file content (flat layout), the resources-writer {@link NativeException}
 * failure path, independent reflection vs resources base paths, and the dedup of
 * name-equal reflection entries through the serialized {@code Set}.
 */
public class NativeConfigurationMoreBehaviourTest {

    private static final String NATIVE_IMAGE_DIR = "META-INF" + File.separator + "native-image";

    private NativeConfiguration cfg(Set<IReflectionConfigurationEntry> entries, Set<String> resources,
            String resPath, String reflPath) {
        return new NativeConfiguration(NativeConfigurationMode.override, entries, resources, resPath, reflPath, "");
    }

    @Test
    public void writeReflectionFlatFileContainsTheEntryName(@TempDir Path dir) throws IOException {
        NativeConfiguration c = cfg(Set.of(new ReflectConfigEntry("com.flat.Entry")),
                Set.of(), dir.toString(), dir.toString());

        c.writeReflectionConfiguration();

        Path expected = dir.resolve(NATIVE_IMAGE_DIR).resolve("reflect-config.json");
        assertTrue(Files.exists(expected), "flat reflect-config.json expected at " + expected);
        String content = Files.readString(expected);
        assertTrue(content.trim().startsWith("["), content);
        assertTrue(content.contains("com.flat.Entry"), content);
    }

    @Test
    public void writeResourcesConfigurationFailsWithNativeExceptionWhenBaseIsAFile(@TempDir Path dir)
            throws IOException {
        // resourcesPath points at a regular file -> mkdirs() under it fails ->
        // IOException is wrapped into a NativeException.
        Path file = dir.resolve("not-a-dir");
        Files.writeString(file, "x");
        NativeConfiguration c = cfg(Set.of(), Set.of("r.txt"), file.toString(), file.toString());

        NativeException ex = assertThrows(NativeException.class, c::writeResourcesConfiguration);
        assertEquals(com.garganttua.core.CoreException.NATIVE_ERROR, ex.getCode());
    }

    @Test
    public void reflectionAndResourcesUseTheirOwnIndependentBasePaths(@TempDir Path reflDir,
            @TempDir Path resDir) {
        NativeConfiguration c = new NativeConfiguration(NativeConfigurationMode.override,
                Set.of(new ReflectConfigEntry("x.Y")), Set.of("res.txt"),
                resDir.toString(), reflDir.toString(), "");

        c.writeReflectionConfiguration();
        c.writeResourcesConfiguration();

        assertTrue(Files.exists(reflDir.resolve(NATIVE_IMAGE_DIR).resolve("reflect-config.json")),
                "reflect file must land under the reflection path");
        assertTrue(Files.exists(resDir.resolve(NATIVE_IMAGE_DIR).resolve("resource-config.json")),
                "resource file must land under the resources path");
        // and the reflection base must NOT have a resource file (paths are independent)
        assertFalse(Files.exists(reflDir.resolve(NATIVE_IMAGE_DIR).resolve("resource-config.json")));
    }

    @Test
    public void nameEqualEntriesCollapseToOneInSerializedReflectionOutput() throws IOException {
        // A Set built from two name-equal entries collapses (equals is name-only).
        Set<IReflectionConfigurationEntry> entries = new java.util.HashSet<>();
        entries.add(new ReflectConfigEntry("dup.Same"));
        entries.add(new ReflectConfigEntry("dup.Same"));
        assertEquals(1, entries.size(), "Set must dedup name-equal entries before construction");

        NativeConfiguration c = cfg(entries, Set.of(), "", "");
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        c.writeReflectionConfiguration(new java.io.ByteArrayInputStream(new byte[0]), out);

        String json = out.toString();
        assertEquals(json.indexOf("dup.Same"), json.lastIndexOf("dup.Same"),
                "name must appear exactly once: " + json);
    }

    @Test
    public void resourcesStreamWrapsEachPatternInItsOwnPatternMap() throws IOException {
        NativeConfiguration c = cfg(Set.of(), Set.of("only/one.res"), "", "");
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();

        c.writeResourcesConfiguration(new java.io.ByteArrayInputStream(new byte[0]), out);
        String json = out.toString();

        assertTrue(json.contains("\"pattern\" : \"only/one.res\""), json);
    }

    @Test
    public void writeReflectionConfigurationIsRepeatableOverwritingTheFile(@TempDir Path dir) throws IOException {
        NativeConfiguration first = cfg(Set.of(new ReflectConfigEntry("a.First")),
                Set.of(), dir.toString(), dir.toString());
        first.writeReflectionConfiguration();

        NativeConfiguration second = cfg(Set.of(new ReflectConfigEntry("b.Second")),
                Set.of(), dir.toString(), dir.toString());
        second.writeReflectionConfiguration();

        Path file = dir.resolve(NATIVE_IMAGE_DIR).resolve("reflect-config.json");
        String content = Files.readString(file);
        // override mode: second write replaces first content
        assertTrue(content.contains("b.Second"), content);
        assertFalse(content.contains("a.First"), content);
    }
}
