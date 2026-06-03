package com.garganttua.core.bootstrap.dsl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;

/**
 * Behaviour tests for {@link BootstrapSpiLoader}.
 *
 * <p>The bootstrap test classpath ships {@code RuntimeReflectionProvider}
 * (via garganttua-runtime-reflection) and {@code ReflectionsAnnotationScanner}
 * (via garganttua-reflections) as ServiceLoader-discoverable SPI services, so the
 * loader must find at least one provider and produce a usable {@link IReflection}.
 *
 * <p>These tests mutate the global {@code IClass} reflection facade, so each one
 * saves and restores it around the call.
 */
@DisplayName("BootstrapSpiLoader behaviour")
class BootstrapSpiLoaderBehaviourTest {

    private IReflection previous;

    @BeforeEach
    void capturePrevious() {
        try {
            previous = IClass.getReflection();
        } catch (IllegalStateException e) {
            previous = null;
        }
    }

    @AfterEach
    void restorePrevious() {
        IClass.setReflection(previous);
    }

    @Test
    @DisplayName("buildReflectionBuilderFromSpi finds SPI providers and returns a non-null builder")
    void buildReflectionBuilderFromSpiReturnsBuilder() {
        IReflectionBuilder rb = BootstrapSpiLoader.buildReflectionBuilderFromSpi();
        assertNotNull(rb, "SPI providers are on the classpath, so a builder must be returned");
    }

    @Test
    @DisplayName("the SPI-built builder builds an IReflection that can resolve a class")
    void spiBuiltReflectionResolvesClasses() throws DslException {
        IReflectionBuilder rb = BootstrapSpiLoader.buildReflectionBuilderFromSpi();
        assertNotNull(rb);

        IReflection reflection = rb.build();
        assertNotNull(reflection);
        // A working provider must be able to mirror java.lang.String.
        assertNotNull(reflection.getClass(String.class));
        assertSame(String.class, reflection.getClass(String.class).getType());
    }

    @Test
    @DisplayName("ensureReflectionAvailable is a no-op when reflection is already installed")
    void ensureReflectionNoOpWhenAlreadyPresent() throws DslException {
        IReflection installed = BootstrapSpiLoader.buildReflectionBuilderFromSpi().build();
        IClass.setReflection(installed);

        BootstrapSpiLoader.ensureReflectionAvailable();

        // The already-installed facade is preserved untouched (same instance).
        assertSame(installed, IClass.getReflection(),
                "ensureReflectionAvailable must not replace an existing facade");
    }

    @Test
    @DisplayName("ensureReflectionAvailable installs a facade via SPI when none is present")
    void ensureReflectionInstallsViaSpiWhenAbsent() {
        // Start from a cleared facade.
        IClass.setReflection(null);

        BootstrapSpiLoader.ensureReflectionAvailable();

        // SPI providers exist, so a facade should now be resolvable without throwing.
        IReflection installed = IClass.getReflection();
        assertNotNull(installed,
                "ensureReflectionAvailable should have installed an SPI-built facade");
        assertSame(String.class, installed.getClass(String.class).getType());
    }

    @Test
    @DisplayName("buildReflectionBuilderFromSpi is idempotent across repeated calls")
    void buildReflectionBuilderFromSpiIsRepeatable() throws DslException {
        IReflection first = BootstrapSpiLoader.buildReflectionBuilderFromSpi().build();
        IReflection second = BootstrapSpiLoader.buildReflectionBuilderFromSpi().build();

        // Distinct builder instances each time, both functional.
        assertNotNull(first);
        assertNotNull(second);
        assertSame(String.class, first.getClass(String.class).getType());
        assertSame(String.class, second.getClass(String.class).getType());
    }
}
