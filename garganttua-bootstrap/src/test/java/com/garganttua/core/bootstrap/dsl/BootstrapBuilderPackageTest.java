package com.garganttua.core.bootstrap.dsl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for BootstrapBuilder package management capabilities.
 *
 * <p>
 * Validates that BootstrapBuilder correctly implements the IPackageableBuilder interface
 * and properly manages package configuration for reflection and auto-detection.
 * </p>
 */
@DisplayName("BootstrapBuilder Package Management Tests")
class BootstrapBuilderPackageTest {

    @Test
    @DisplayName("Should implement IPackageableBuilder interface")
    void testImplementsIPackageableBuilder() {
        BootstrapBuilder bootstrap = new BootstrapBuilder();
        assertInstanceOf(com.garganttua.core.dsl.IPackageableBuilder.class, bootstrap);
    }

    @Test
    @DisplayName("Should implement IBoostrap interface")
    void testImplementsIBoostrap() {
        BootstrapBuilder bootstrap = new BootstrapBuilder();
        assertInstanceOf(IBoostrap.class, bootstrap);
    }

    @Test
    @DisplayName("Should support fluent API pattern")
    void testFluentAPI() {
        IBoostrap result = BootstrapBuilder.builder()
                .withPackage("com.example")
                .withPackage("com.test")
                .withBuilder(new BootstrapBuilderTest.MockBuilder("test1"))
                .withBuilder(new BootstrapBuilderTest.MockBuilder("test2"))
                .autoDetect(true);

        assertNotNull(result);
        assertInstanceOf(IBoostrap.class, result);
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCaseTests {

        private BootstrapBuilder bootstrap;

        @BeforeEach
        void setUp() {
            bootstrap = new BootstrapBuilder();
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
