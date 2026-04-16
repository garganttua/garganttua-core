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
    void testPlaceholderWithDefaultValue(@TempDir Path tempDir) throws IOException {
        File propsFile = tempDir.resolve("placeholder.properties").toFile();
        try (FileWriter writer = new FileWriter(propsFile)) {
            writer.write("mail.host=${MAIL_HOST:smtp.gmail.com}\n");
            writer.write("mail.port=${MAIL_PORT:587}\n");
        }

        PropertiesFileProviderBuilder builder = PropertiesFileProviderBuilder.create(injectionContextBuilder);
        builder.file(propsFile);

        IPropertyProvider provider = builder.build();

        // MAIL_HOST is not set in env, should use default
        Optional<String> host = provider.getProperty("mail.host", IClass.getClass(String.class));
        assertTrue(host.isPresent());
        assertEquals("smtp.gmail.com", host.get());

        Optional<String> port = provider.getProperty("mail.port", IClass.getClass(String.class));
        assertTrue(port.isPresent());
        assertEquals("587", port.get());
    }

    @Test
    void testPlaceholderWithSystemProperty(@TempDir Path tempDir) throws IOException {
        File propsFile = tempDir.resolve("sysprop.properties").toFile();
        try (FileWriter writer = new FileWriter(propsFile)) {
            writer.write("test.value=${garganttua.test.sysprop:fallback}\n");
        }

        System.setProperty("garganttua.test.sysprop", "from-system");
        try {
            PropertiesFileProviderBuilder builder = PropertiesFileProviderBuilder.create(injectionContextBuilder);
            builder.file(propsFile);

            IPropertyProvider provider = builder.build();

            Optional<String> val = provider.getProperty("test.value", IClass.getClass(String.class));
            assertTrue(val.isPresent());
            assertEquals("from-system", val.get());
        } finally {
            System.clearProperty("garganttua.test.sysprop");
        }
    }

    @Test
    void testPlaceholderWithEnvVariable(@TempDir Path tempDir) throws IOException {
        File propsFile = tempDir.resolve("env.properties").toFile();
        try (FileWriter writer = new FileWriter(propsFile)) {
            // HOME env var is always set on Linux
            writer.write("user.home.dir=${HOME:/tmp}\n");
        }

        PropertiesFileProviderBuilder builder = PropertiesFileProviderBuilder.create(injectionContextBuilder);
        builder.file(propsFile);

        IPropertyProvider provider = builder.build();

        Optional<String> val = provider.getProperty("user.home.dir", IClass.getClass(String.class));
        assertTrue(val.isPresent());
        assertNotEquals("/tmp", val.get()); // should resolve to actual HOME, not fallback
        assertFalse(val.get().contains("${"));
    }

    @Test
    void testPlaceholderWithoutDefault(@TempDir Path tempDir) throws IOException {
        File propsFile = tempDir.resolve("nodefault.properties").toFile();
        try (FileWriter writer = new FileWriter(propsFile)) {
            writer.write("test.unresolved=${GARGANTTUA_NONEXISTENT_VAR_12345}\n");
        }

        PropertiesFileProviderBuilder builder = PropertiesFileProviderBuilder.create(injectionContextBuilder);
        builder.file(propsFile);

        IPropertyProvider provider = builder.build();

        // No default, no env var → keeps original placeholder
        Optional<String> val = provider.getProperty("test.unresolved", IClass.getClass(String.class));
        assertTrue(val.isPresent());
        assertEquals("${GARGANTTUA_NONEXISTENT_VAR_12345}", val.get());
    }

    @Test
    void testMultiplePlaceholdersInOneValue(@TempDir Path tempDir) throws IOException {
        File propsFile = tempDir.resolve("multi.properties").toFile();
        try (FileWriter writer = new FileWriter(propsFile)) {
            writer.write("jdbc.url=jdbc:${DB_TYPE:postgresql}://${DB_HOST:localhost}:${DB_PORT:5432}/mydb\n");
        }

        PropertiesFileProviderBuilder builder = PropertiesFileProviderBuilder.create(injectionContextBuilder);
        builder.file(propsFile);

        IPropertyProvider provider = builder.build();

        Optional<String> url = provider.getProperty("jdbc.url", IClass.getClass(String.class));
        assertTrue(url.isPresent());
        assertEquals("jdbc:postgresql://localhost:5432/mydb", url.get());
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
