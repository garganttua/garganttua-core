package com.garganttua.core.bootstrap.dsl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for Bootstrap package management capabilities.
 *
 * <p>
 * Validates that Bootstrap correctly implements the IPackageableBuilder interface
 * and properly manages package configuration for reflection and auto-detection.
 * </p>
 */
@DisplayName("Bootstrap Package Management Tests")
class BootstrapPackageTest {

    @Test
    @DisplayName("Should implement IPackageableBuilder interface")
    void testImplementsIPackageableBuilder() {
        Bootstrap bootstrap = new Bootstrap();
        assertInstanceOf(com.garganttua.core.dsl.IPackageableBuilder.class, bootstrap);
    }

    @Test
    @DisplayName("Should implement IBoostrap interface")
    void testImplementsIBoostrap() {
        Bootstrap bootstrap = new Bootstrap();
        assertInstanceOf(IBoostrap.class, bootstrap);
    }

    @Test
    @DisplayName("Should support fluent API pattern")
    void testFluentAPI() {
        IBoostrap result = Bootstrap.builder()
                .withPackage("com.example")
                .withPackage("com.test")
                .withBuilder(new BootstrapTest.MockBuilder("test1"))
                .withBuilder(new BootstrapTest.MockBuilder("test2"))
                .autoDetect(true);

        assertNotNull(result);
        assertInstanceOf(IBoostrap.class, result);
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCaseTests {

        private Bootstrap bootstrap;

        @BeforeEach
        void setUp() {
            bootstrap = new Bootstrap();
        }

        @Test
        @DisplayName("Should handle empty string package name")
        void testEmptyPackageName() {
            assertDoesNotThrow(() -> bootstrap.withPackage(""),
                "Empty package name should be accepted");
            assertEquals(1, bootstrap.getPackages().length);
        }

        @Test
        @DisplayName("Should handle package names with special characters")
        void testSpecialCharactersInPackage() {
            bootstrap.withPackage("com.example.my_app");

            assertEquals(1, bootstrap.getPackages().length);
            assertEquals("com.example.my_app", bootstrap.getPackages()[0]);
        }

        @Test
        @DisplayName("Should store all packages in Set")
        void testPackageStorage() {
            bootstrap.withPackage("com.a")
                    .withPackage("com.b")
                    .withPackage("com.c");

            String[] packages = bootstrap.getPackages();
            assertEquals(3, packages.length);
            // Note: Set doesn't guarantee order, just checking all are present
            Set<String> packageSet = bootstrap.getConfiguredPackages();
            assertTrue(packageSet.contains("com.a"));
            assertTrue(packageSet.contains("com.b"));
            assertTrue(packageSet.contains("com.c"));
        }
    }
}
