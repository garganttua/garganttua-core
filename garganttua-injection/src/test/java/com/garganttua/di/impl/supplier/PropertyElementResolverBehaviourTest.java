package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.injection.Resolved;
import com.garganttua.core.injection.annotations.Property;
import com.garganttua.core.injection.annotations.Provider;
import com.garganttua.core.injection.context.properties.resolver.PropertyElementResolver;
import com.garganttua.core.reflection.IAnnotatedElement;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeField;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

import jakarta.annotation.Nullable;

/**
 * Behaviour tests for {@link PropertyElementResolver}: it resolves {@code @Property}-annotated
 * elements to a property supplier builder keyed by the annotation value, optionally scoped to a
 * {@code @Provider}, and propagates the element type and {@code @Nullable} flag. It rejects null
 * arguments and fails fast when no {@code @Property} annotation is present.
 */
public class PropertyElementResolverBehaviourTest {

    private PropertyElementResolver resolver;

    @BeforeEach
    void setUp() {
        ReflectionBuilder.builder().withProvider(new RuntimeReflectionProvider()).build();
        resolver = new PropertyElementResolver();
    }

    private static IAnnotatedElement field(String name) throws NoSuchFieldException {
        Field f = Fixture.class.getDeclaredField(name);
        return RuntimeField.of(f);
    }

    @Test
    void resolvesPropertyFieldToPresentSupplierWithElementType() throws NoSuchFieldException {
        Resolved resolved = resolver.resolve(IClass.getClass(String.class), field("url"));
        assertTrue(resolved.resolved(), "a @Property element always resolves");
        assertSame(IClass.getClass(String.class), resolved.elementType());
        assertNotNull(resolved.elementSupplier());
        assertSame(IClass.getClass(String.class), resolved.elementSupplier().getSuppliedClass());
    }

    @Test
    void supplierTypeMatchesTargetType() throws NoSuchFieldException {
        Resolved resolved = resolver.resolve(IClass.getClass(Integer.class), field("port"));
        assertSame(IClass.getClass(Integer.class), resolved.elementSupplier().getSuppliedClass());
    }

    @Test
    void nullableFlagIsPropagated() throws NoSuchFieldException {
        Resolved nullableResolved = resolver.resolve(IClass.getClass(String.class), field("nullableProp"));
        assertTrue(nullableResolved.nullable());

        Resolved plain = resolver.resolve(IClass.getClass(String.class), field("url"));
        assertFalse(plain.nullable());
    }

    @Test
    void providerScopedPropertyResolves() throws NoSuchFieldException {
        Resolved resolved = resolver.resolve(IClass.getClass(String.class), field("scopedProp"));
        assertTrue(resolved.resolved());
        assertNotNull(resolved.elementSupplier());
    }

    @Test
    void blankProviderValueResolvesWithoutError() throws NoSuchFieldException {
        Resolved resolved = resolver.resolve(IClass.getClass(String.class), field("blankProviderProp"));
        assertTrue(resolved.resolved());
    }

    @Test
    void nullElementThrows() {
        assertThrows(NullPointerException.class, () -> resolver.resolve(IClass.getClass(String.class), null));
    }

    @Test
    void nullElementTypeThrows() throws NoSuchFieldException {
        assertThrows(NullPointerException.class, () -> resolver.resolve(null, field("url")));
    }

    @Test
    void missingPropertyAnnotationThrows() throws NoSuchFieldException {
        // No @Property present -> getAnnotation returns null -> property.value() NPEs inside resolve.
        assertThrows(NullPointerException.class,
                () -> resolver.resolve(IClass.getClass(String.class), field("notAProperty")));
    }

    @SuppressWarnings("unused")
    private static class Fixture {
        @Property("database.url")
        private String url;

        @Property("server.port")
        private Integer port;

        @Property("optional.key")
        @Nullable
        private String nullableProp;

        @Property("scoped.key")
        @Provider("config")
        private String scopedProp;

        @Property("blank.provider.key")
        @Provider("")
        private String blankProviderProp;

        private String notAProperty;
    }
}
