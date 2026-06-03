package com.garganttua.core.nativve.image.config.reflection;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Behaviour tests for {@link ReflectConfigEntryBuilder} as observable from this
 * module's test classpath.
 *
 * <p><strong>Important environment fact:</strong> {@code garganttua-native-commons}
 * does <em>not</em> declare a dependency on a concrete {@code IReflectionProvider}
 * (e.g. {@code garganttua-runtime-reflection}). {@code ReflectConfigEntryBuilder}
 * eagerly resolves {@code @Reflected} through {@code defaultProvider()} in a
 * {@code static} initializer ({@code REFLECTED_CLASS = wrapClass(Reflected.class)}).
 * With no provider on the classpath, the class therefore <em>cannot be
 * initialised at all</em> here. These tests pin that real failure mode so it is
 * documented rather than silently regressed. See the suspected-bug note in the
 * task summary.</p>
 */
public class ReflectConfigEntryBuilderBehaviourTest {

    @Test
    public void classInitializationFailsWithoutAReflectionProviderOnClasspath() {
        // Forcing initialization (without linking, so the failure surfaces as the
        // wrapped ExceptionInInitializerError rather than at link time).
        Throwable t = assertThrows(Throwable.class, () ->
                Class.forName(
                        "com.garganttua.core.nativve.image.config.reflection.ReflectConfigEntryBuilder",
                        true,
                        ReflectConfigEntryBuilderBehaviourTest.class.getClassLoader()));

        // Walk the cause chain and assert the precise, meaningful root cause:
        // the missing IReflectionProvider — not some unrelated error.
        boolean foundProviderMessage = false;
        for (Throwable c = t; c != null; c = c.getCause()) {
            String msg = c.getMessage();
            if (msg != null && msg.contains("No IReflectionProvider available")) {
                foundProviderMessage = true;
                break;
            }
        }
        assertTrue(foundProviderMessage,
                "expected root cause to mention the missing IReflectionProvider, got: " + t);
    }
}
