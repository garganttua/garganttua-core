package com.garganttua.core.nativve.image.config.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.garganttua.core.reflection.JdkClass;

/**
 * Behaviour tests for {@link ResourceConfig}: the add/remove of literal-quoted
 * ({@code \Q...\E}) include patterns into a {@code resource-config.json} file,
 * de-duplication, the class-overload path-derivation, and the various no-op
 * short-circuits ({@code removeResource} on missing/empty/structure-less files).
 */
public class ResourceConfigBehaviourTest {

    @Test
    public void addResourceCreatesFileWithLiteralQuotedPattern(@TempDir Path dir) throws IOException {
        File f = dir.resolve("resource-config.json").toFile();

        ResourceConfig.addResource(f, "config/app.properties");

        String json = Files.readString(f.toPath());
        assertTrue(json.contains("\"resources\""), json);
        assertTrue(json.contains("\"includes\""), json);
        // \Q...\E literal markers, JSON-escaped as \\Q...\\E
        assertTrue(json.contains("\\\\Qconfig/app.properties\\\\E"), json);
    }

    @Test
    public void addResourceTwiceSamePatternIsDeDuplicated(@TempDir Path dir) throws IOException {
        File f = dir.resolve("resource-config.json").toFile();

        ResourceConfig.addResource(f, "a/b.txt");
        ResourceConfig.addResource(f, "a/b.txt");

        String json = Files.readString(f.toPath());
        int first = json.indexOf("a/b.txt");
        int last = json.lastIndexOf("a/b.txt");
        assertTrue(first >= 0, json);
        assertEquals(first, last, "duplicate pattern must appear exactly once: " + json);
    }

    @Test
    public void addingTwoDistinctPatternsKeepsBoth(@TempDir Path dir) throws IOException {
        File f = dir.resolve("resource-config.json").toFile();

        ResourceConfig.addResource(f, "one.txt");
        ResourceConfig.addResource(f, "two.txt");

        String json = Files.readString(f.toPath());
        assertTrue(json.contains("one.txt"), json);
        assertTrue(json.contains("two.txt"), json);
    }

    @Test
    public void addResourceByClassDerivesSlashSeparatedClassFilePath(@TempDir Path dir) throws IOException {
        File f = dir.resolve("resource-config.json").toFile();

        ResourceConfig.addResource(f, JdkClass.of(String.class));

        String json = Files.readString(f.toPath());
        // java.lang.String -> java/lang/String.class, wrapped in \Q..\E
        assertTrue(json.contains("\\\\Qjava/lang/String.class\\\\E"), json);
    }

    @Test
    public void removeResourceDropsAPreviouslyAddedPattern(@TempDir Path dir) throws IOException {
        File f = dir.resolve("resource-config.json").toFile();
        ResourceConfig.addResource(f, "keep.txt");
        ResourceConfig.addResource(f, "drop.txt");

        ResourceConfig.removeResource(f, "drop.txt");

        String json = Files.readString(f.toPath());
        assertTrue(json.contains("keep.txt"), json);
        assertFalse(json.contains("drop.txt"), json);
    }

    @Test
    public void removeResourceByClassRemovesTheClassFilePattern(@TempDir Path dir) throws IOException {
        File f = dir.resolve("resource-config.json").toFile();
        ResourceConfig.addResource(f, JdkClass.of(Integer.class));
        // sanity: present before removal
        assertTrue(Files.readString(f.toPath()).contains("java/lang/Integer.class"));

        ResourceConfig.removeResource(f, JdkClass.of(Integer.class));

        assertFalse(Files.readString(f.toPath()).contains("java/lang/Integer.class"));
    }

    @Test
    public void removeResourceOnMissingFileIsANoOpAndDoesNotCreateIt(@TempDir Path dir) throws IOException {
        File f = dir.resolve("does-not-exist.json").toFile();

        ResourceConfig.removeResource(f, "whatever.txt");

        assertFalse(f.exists(), "missing file must not be created by a remove no-op");
    }

    @Test
    public void removeResourceOnEmptyFileIsANoOp(@TempDir Path dir) throws IOException {
        File f = dir.resolve("empty.json").toFile();
        Files.writeString(f.toPath(), "");

        ResourceConfig.removeResource(f, "whatever.txt");

        // still empty, unchanged
        assertEquals(0, f.length());
    }

    @Test
    public void removeResourceWhenNoIncludesSectionIsANoOpLeavingContentUntouched(@TempDir Path dir) throws IOException {
        File f = dir.resolve("no-includes.json").toFile();
        // valid JSON object but with no "resources"/"includes" structure
        Files.writeString(f.toPath(), "{\"other\":{\"x\":1}}");

        ResourceConfig.removeResource(f, "whatever.txt");

        String json = Files.readString(f.toPath());
        assertTrue(json.contains("\"other\""), "untouched content expected: " + json);
        assertFalse(json.contains("includes"), json);
    }

    @Test
    public void removeNonMatchingPatternLeavesExistingPatternsIntact(@TempDir Path dir) throws IOException {
        File f = dir.resolve("resource-config.json").toFile();
        ResourceConfig.addResource(f, "present.txt");

        ResourceConfig.removeResource(f, "absent.txt"); // no such pattern

        assertTrue(Files.readString(f.toPath()).contains("present.txt"));
    }

    @Test
    public void addResourceAppendsToAnExistingPopulatedFile(@TempDir Path dir) throws IOException {
        File f = dir.resolve("resource-config.json").toFile();
        ResourceConfig.addResource(f, "first.txt");

        // second add re-reads the existing file (exists && length>0 branch) and appends
        ResourceConfig.addResource(f, "second.txt");

        String json = Files.readString(f.toPath());
        assertTrue(json.contains("first.txt"), json);
        assertTrue(json.contains("second.txt"), json);
    }
}
