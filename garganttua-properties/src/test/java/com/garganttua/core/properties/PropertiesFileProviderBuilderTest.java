package com.garganttua.core.properties;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.garganttua.core.injection.IPropertyProvider;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;

import java.nio.file.Path;

class PropertiesFileProviderBuilderTest {

    private static IInjectionContextBuilder injectionContextBuilder;

    @BeforeAll
    static void setup() throws Exception {
        @SuppressWarnings("unchecked")
        Class<? extends IReflectionProvider> providerClass =
                (Class<? extends IReflectionProvider>) Class.forName(
                        "com.garganttua.core.reflection.runtime.RuntimeReflectionProvider");
        var reflectionBuilder = ReflectionBuilder.builder()
                .withProvider(providerClass.getDeclaredConstructor().newInstance())
                .withScanner(new ReflectionsAnnotationScanner());
        reflectionBuilder.build();

        injectionContextBuilder = InjectionContext.builder()
                .provide(reflectionBuilder)
                .autoDetect(true)
                .withPackage("com.garganttua.core.runtime");
    }

    @Test
    void testAutoDetectApplicationProperties() {
        PropertiesFileProviderBuilder builder = PropertiesFileProviderBuilder.create(injectionContextBuilder);
        builder.autoDetect(true);

        IPropertyProvider provider = builder.build();
        assertNotNull(provider);

        Optional<String> appName = provider.getProperty("app.name", IClass.getClass(String.class));
        assertTrue(appName.isPresent());
        assertEquals("Garganttua Test", appName.get());

        Optional<String> dbUrl = provider.getProperty("database.url", IClass.getClass(String.class));
        assertTrue(dbUrl.isPresent());
        assertEquals("jdbc:h2:mem:testdb", dbUrl.get());

        Optional<Integer> port = provider.getProperty("server.port", IClass.getClass(Integer.class));
        assertTrue(port.isPresent());
        assertEquals(8080, port.get());
    }

    @Test
    void testClasspathResource() {
        PropertiesFileProviderBuilder builder = PropertiesFileProviderBuilder.create(injectionContextBuilder);
        builder.classpathResource("custom.properties");

        IPropertyProvider provider = builder.build();
        assertNotNull(provider);

        Optional<String> val = provider.getProperty("custom.key1", IClass.getClass(String.class));
        assertTrue(val.isPresent());
        assertEquals("value1", val.get());

        Optional<Integer> intVal = provider.getProperty("custom.key2", IClass.getClass(Integer.class));
        assertTrue(intVal.isPresent());
        assertEquals(42, intVal.get());

        Optional<Boolean> boolVal = provider.getProperty("custom.key3", IClass.getClass(Boolean.class));
        assertTrue(boolVal.isPresent());
        assertTrue(boolVal.get());
    }

    @Test
    void testFilesystemFile(@TempDir Path tempDir) throws IOException {
        File propsFile = tempDir.resolve("external.properties").toFile();
        try (FileWriter writer = new FileWriter(propsFile)) {
            writer.write("ext.key=external-value\n");
            writer.write("ext.number=999\n");
        }

        PropertiesFileProviderBuilder builder = PropertiesFileProviderBuilder.create(injectionContextBuilder);
        builder.file(propsFile);

        IPropertyProvider provider = builder.build();
        assertNotNull(provider);

        Optional<String> val = provider.getProperty("ext.key", IClass.getClass(String.class));
        assertTrue(val.isPresent());
        assertEquals("external-value", val.get());

        Optional<Integer> num = provider.getProperty("ext.number", IClass.getClass(Integer.class));
        assertTrue(num.isPresent());
        assertEquals(999, num.get());
    }

    @Test
    void testMultipleSourcesMerge(@TempDir Path tempDir) throws IOException {
        // Classpath file has app.name=Garganttua Test
        // Filesystem file overrides it
        File overrideFile = tempDir.resolve("override.properties").toFile();
        try (FileWriter writer = new FileWriter(overrideFile)) {
            writer.write("app.name=Overridden Name\n");
            writer.write("extra.key=bonus\n");
        }

        PropertiesFileProviderBuilder builder = PropertiesFileProviderBuilder.create(injectionContextBuilder);
        builder.classpathResource("application.properties")
               .file(overrideFile);

        IPropertyProvider provider = builder.build();

        // Overridden by filesystem file
        Optional<String> appName = provider.getProperty("app.name", IClass.getClass(String.class));
        assertTrue(appName.isPresent());
        assertEquals("Overridden Name", appName.get());

        // Still available from classpath
        Optional<String> dbUrl = provider.getProperty("database.url", IClass.getClass(String.class));
        assertTrue(dbUrl.isPresent());

        // Added by filesystem file
        Optional<String> extra = provider.getProperty("extra.key", IClass.getClass(String.class));
        assertTrue(extra.isPresent());
        assertEquals("bonus", extra.get());
    }

    @Test
    void testMissingFileIgnored() {
        PropertiesFileProviderBuilder builder = PropertiesFileProviderBuilder.create(injectionContextBuilder);
        builder.file("/nonexistent/path/nope.properties");

        IPropertyProvider provider = builder.build();
        assertNotNull(provider);
        assertTrue(provider.keys().isEmpty());
    }

    @Test
    void testEmptyBuilderProducesEmptyProvider() {
        PropertiesFileProviderBuilder builder = PropertiesFileProviderBuilder.create(injectionContextBuilder);

        IPropertyProvider provider = builder.build();
        assertNotNull(provider);
        assertTrue(provider.keys().isEmpty());
    }

    @Test
    void testPropertyKeys() {
        PropertiesFileProviderBuilder builder = PropertiesFileProviderBuilder.create(injectionContextBuilder);
        builder.classpathResource("application.properties");

        IPropertyProvider provider = builder.build();

        assertTrue(provider.keys().contains("app.name"));
        assertTrue(provider.keys().contains("database.url"));
        assertTrue(provider.keys().contains("server.port"));
        assertEquals(6, provider.keys().size());
    }
}
