package com.garganttua.core.script;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.script.dsl.IScriptsBuilder;
import com.garganttua.core.script.dsl.ScriptsBuilder;

/**
 * Behaviour tests for the package-management surface of {@link ScriptsBuilder}.
 * These exercise the deduplicating package set and the null-argument guards
 * without requiring a full injection/expression/runtimes dependency chain.
 */
class ScriptsBuilderBehaviourTest {

    @BeforeAll
    @SuppressWarnings("unchecked")
    static void setupReflection() throws Exception {
        // ScriptsBuilder's static DEPENDENCIES field calls IClass.getClass(...),
        // so an IReflection must be installed before the class is initialised.
        Class<? extends IReflectionProvider> providerClass =
                (Class<? extends IReflectionProvider>) Class.forName(
                        "com.garganttua.core.reflection.runtime.RuntimeReflectionProvider");
        IReflectionBuilder reflectionBuilder = ReflectionBuilder.builder()
                .withProvider(providerClass.getDeclaredConstructor().newInstance())
                .withScanner(new ReflectionsAnnotationScanner());
        reflectionBuilder.build();
    }

    @Test
    void freshBuilderHasNoPackages() {
        IScriptsBuilder b = ScriptsBuilder.builder();
        assertEquals(0, b.getPackages().length);
    }

    @Test
    void withPackageAddsPackage() {
        IScriptsBuilder b = ScriptsBuilder.builder().withPackage("com.example");
        assertArrayEquals(new String[] { "com.example" }, b.getPackages());
    }

    @Test
    void withPackageDeduplicates() {
        IScriptsBuilder b = ScriptsBuilder.builder()
                .withPackage("com.a")
                .withPackage("com.a")
                .withPackage("com.b");
        Set<String> pkgs = new HashSet<>(Arrays.asList(b.getPackages()));
        assertEquals(Set.of("com.a", "com.b"), pkgs);
        assertEquals(2, b.getPackages().length);
    }

    @Test
    void withPackagesAddsAll() {
        IScriptsBuilder b = ScriptsBuilder.builder()
                .withPackages(new String[] { "com.x", "com.y", "com.x" });
        Set<String> pkgs = new HashSet<>(Arrays.asList(b.getPackages()));
        assertEquals(Set.of("com.x", "com.y"), pkgs);
    }

    @Test
    void withPackageNullThrowsNpeWithMessage() {
        IScriptsBuilder b = ScriptsBuilder.builder();
        NullPointerException ex = assertThrows(NullPointerException.class, () -> b.withPackage(null));
        assertTrue(ex.getMessage().contains("Package name cannot be null"));
    }

    @Test
    void withPackagesNullArrayThrowsNpeWithMessage() {
        IScriptsBuilder b = ScriptsBuilder.builder();
        NullPointerException ex = assertThrows(NullPointerException.class, () -> b.withPackages(null));
        assertTrue(ex.getMessage().contains("Package names cannot be null"));
    }

    @Test
    void withPackageReturnsSameBuilderForChaining() {
        IScriptsBuilder b = ScriptsBuilder.builder();
        assertSame(b, b.withPackage("p"));
    }

    @Test
    void getPackagesReturnsIndependentArrayCopy() {
        IScriptsBuilder b = ScriptsBuilder.builder().withPackage("com.z");
        String[] first = b.getPackages();
        first[0] = "mutated";
        // Mutating the returned array must not affect the builder's internal state.
        assertArrayEquals(new String[] { "com.z" }, b.getPackages());
    }
}
