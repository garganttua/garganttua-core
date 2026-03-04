package com.garganttua.core.nativve.image.config.reflection;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import com.garganttua.core.nativve.IReflectionConfigurationEntry;
import com.garganttua.core.nativve.NativeConfiguration;
import com.garganttua.core.nativve.NativeConfigurationMode;

public class NativeConfigurationTest {

    //@Test
    public void testWriteReflectionConfiguration() throws IOException {

        Set<IReflectionConfigurationEntry> entries = Set.of(new ReflectConfigEntry("testEntry"));
        String reflectionPath = "";

        NativeConfiguration config = new NativeConfiguration(
                NativeConfigurationMode.override, entries, Set.of(), "", reflectionPath);

        config.writeReflectionConfiguration();

        String content = Files.readString(Path.of(reflectionPath));
        assertTrue(content.contains("testEntry"));
    }

    //@Test
    public void testWriteResourcesConfiguration() throws IOException {
        Set<String> resources = Set.of("resource1", "resource2");
        String resourcesPath = "";

        NativeConfiguration config = new NativeConfiguration(
                NativeConfigurationMode.override, Set.of(), resources, resourcesPath, "");

        config.writeResourcesConfiguration();

        String content = Files.readString(Path.of(resourcesPath));
        assertTrue(content.contains("resource1"));
        assertTrue(content.contains("resource2"));
    }
}
