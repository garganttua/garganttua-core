package com.garganttua.core.nativve.image.config.reflection;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.JdkClass;

/**
 * Behaviour tests for {@link ReflectConfigEntryBuilder} class initialization.
 *
 * <p><strong>Environment fact:</strong> {@code ReflectConfigEntryBuilder} eagerly
 * resolves {@code @Reflected} through {@code defaultProvider()} in a {@code static}
 * initializer ({@code REFLECTED_CLASS = wrapClass(Reflected.class)}), which calls
 * {@code Class.forName("...reflection.runtime.RuntimeReflectionProvider")}. This
 * module's compile classpath has no such provider, but the test classpath supplies
 * a test-scoped {@code RuntimeReflectionProvider} (at exactly that FQN) backed by a
 * JDK {@code IClass}. With it present the class initializes cleanly — these tests
 * pin that initialization succeeds and that the static factory yields a usable
 * builder. The companion {@code ReflectConfigEntryBuilderBuildBehaviourTest}
 * exercises the produced entry structure.</p>
 */
public class ReflectConfigEntryBuilderBehaviourTest {

    @Test
    public void classInitializesWhenAReflectionProviderIsOnClasspath() {
        assertDoesNotThrow(() ->
                Class.forName(
                        "com.garganttua.core.nativve.image.config.reflection.ReflectConfigEntryBuilder",
                        true,
                        ReflectConfigEntryBuilderBehaviourTest.class.getClassLoader()));
    }

    @Test
    public void staticFactoryReturnsAUsableBuilder() {
        var builder = ReflectConfigEntryBuilder.builder(JdkClass.of(String.class));
        assertNotNull(builder);
        assertEquals("java.lang.String", builder.build().getName());
    }
}
