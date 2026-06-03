package com.garganttua.core.nativve.image.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Behaviour tests for {@link NativeImageConfig}: file-location resolution under
 * {@code META-INF/native-image}, on-demand directory creation, and the IOException
 * raised when the directory tree cannot be created.
 */
public class NativeImageConfigBehaviourTest {

    @Test
    public void getReflectConfigFileResolvesUnderNativeImageDirAndCreatesIt(@TempDir Path base) throws IOException {
        File f = NativeImageConfig.getReflectConfigFile(base.toString());

        Path nativeImageDir = base.resolve("META-INF").resolve("native-image");
        assertTrue(Files.isDirectory(nativeImageDir), "META-INF/native-image must be created");
        assertEquals(nativeImageDir.resolve("reflect-config.json").toFile(), f);
        // the file itself is only a location — it is not created
        assertFalse(f.exists(), "config file location must not be pre-created");
    }

    @Test
    public void getResourceConfigFileResolvesUnderNativeImageDirAndCreatesIt(@TempDir Path base) throws IOException {
        File f = NativeImageConfig.getResourceConfigFile(base.toString());

        Path nativeImageDir = base.resolve("META-INF").resolve("native-image");
        assertTrue(Files.isDirectory(nativeImageDir));
        assertEquals(nativeImageDir.resolve("resource-config.json").toFile(), f);
        assertFalse(f.exists());
    }

    @Test
    public void directoryIsReusedWhenItAlreadyExists(@TempDir Path base) throws IOException {
        // First call creates it; second call must succeed without error and
        // return the same location.
        File first = NativeImageConfig.getReflectConfigFile(base.toString());
        File second = NativeImageConfig.getReflectConfigFile(base.toString());
        assertEquals(first, second);
    }

    @Test
    public void reflectAndResourceFilesShareTheSameDirButDifferentNames(@TempDir Path base) throws IOException {
        File reflect = NativeImageConfig.getReflectConfigFile(base.toString());
        File resource = NativeImageConfig.getResourceConfigFile(base.toString());

        assertEquals(reflect.getParentFile(), resource.getParentFile());
        assertEquals("reflect-config.json", reflect.getName());
        assertEquals("resource-config.json", resource.getName());
    }

    @Test
    public void getReflectConfigFileThrowsIOExceptionWhenBaseIsAFile(@TempDir Path base) throws IOException {
        // baseDir is a regular file, so creating META-INF/native-image beneath it
        // is impossible and mkdirs() returns false -> IOException.
        Path asFile = base.resolve("plain-file");
        Files.writeString(asFile, "content");

        IOException ex = assertThrows(IOException.class,
                () -> NativeImageConfig.getReflectConfigFile(asFile.toString()));
        assertTrue(ex.getMessage().contains("Failed to create directory"), ex.getMessage());
    }

    @Test
    public void getResourceConfigFileThrowsIOExceptionWhenBaseIsAFile(@TempDir Path base) throws IOException {
        Path asFile = base.resolve("plain-file2");
        Files.writeString(asFile, "content");

        IOException ex = assertThrows(IOException.class,
                () -> NativeImageConfig.getResourceConfigFile(asFile.toString()));
        assertTrue(ex.getMessage().contains("Failed to create directory"), ex.getMessage());
    }
}
