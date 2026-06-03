package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.injection.Resolved;
import com.garganttua.core.injection.annotations.Provider;
import com.garganttua.core.injection.context.beans.resolver.PrototypeElementResolver;
import com.garganttua.core.injection.context.beans.resolver.SingletonElementResolver;
import com.garganttua.core.injection.dummies.DummyBean;
import com.garganttua.core.reflection.IAnnotatedElement;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeField;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

import jakarta.annotation.Nullable;

/**
 * Behaviour tests for the shared {@link com.garganttua.core.injection.context.beans.resolver.BeanElementResolver}
 * annotation-parsing logic exercised through the concrete Singleton/Prototype resolvers:
 * {@code @Named}, {@code @Provider}, qualifier filtering, {@code @Nullable} propagation and
 * the always-present supplier builder carrying the element type.
 */
public class BeanElementResolverBehaviourTest {

    @SuppressWarnings("unchecked")
    private final IClass<? extends Annotation> dummyQualifierClass =
            (IClass<? extends Annotation>) IClass.getClass(QualifierA.class);

    private SingletonElementResolver singletonWithQualifier;
    private PrototypeElementResolver prototypeNoQualifier;

    @BeforeEach
    void setUp() {
        ReflectionBuilder.builder().withProvider(new RuntimeReflectionProvider()).build();
        Set<IClass<? extends Annotation>> qualifiers = new HashSet<>();
        qualifiers.add(dummyQualifierClass);
        singletonWithQualifier = new SingletonElementResolver(qualifiers);
        prototypeNoQualifier = new PrototypeElementResolver(new HashSet<>());
    }

    private static IAnnotatedElement field(String name) throws NoSuchFieldException {
        Field f = Fixture.class.getDeclaredField(name);
        return RuntimeField.of(f);
    }

    @Test
    void resolvesPlainFieldWithPresentSupplierAndElementType() throws NoSuchFieldException {
        Resolved resolved = prototypeNoQualifier.resolve(IClass.getClass(DummyBean.class), field("plain"));
        assertTrue(resolved.resolved(), "BeanElementResolver always returns a present supplier builder");
        assertEquals(IClass.getClass(DummyBean.class), resolved.elementType());
        assertNotNull(resolved.elementSupplier());
        assertSame(IClass.getClass(DummyBean.class), resolved.elementSupplier().getSuppliedClass());
    }

    @Test
    void nullableAnnotationIsPropagated() throws NoSuchFieldException {
        Resolved nullableResolved =
                singletonWithQualifier.resolve(IClass.getClass(DummyBean.class), field("nullableField"));
        assertTrue(nullableResolved.nullable(), "@Nullable field must be reported nullable");

        Resolved plainResolved =
                singletonWithQualifier.resolve(IClass.getClass(DummyBean.class), field("plain"));
        assertFalse(plainResolved.nullable(), "non-annotated field defaults to not nullable");
    }

    @Test
    void namedFieldStillResolvesToSameElementType() throws NoSuchFieldException {
        // @Named parsing is internal; behaviourally the resolved type and supplier remain consistent.
        Resolved resolved = singletonWithQualifier.resolve(IClass.getClass(DummyBean.class), field("namedField"));
        assertTrue(resolved.resolved());
        assertSame(IClass.getClass(DummyBean.class), resolved.elementSupplier().getSuppliedClass());
    }

    @Test
    void blankNamedValueDoesNotBreakResolution() throws NoSuchFieldException {
        Resolved resolved = singletonWithQualifier.resolve(IClass.getClass(DummyBean.class), field("blankNamedField"));
        assertTrue(resolved.resolved());
        assertNotNull(resolved.elementSupplier());
    }

    @Test
    void providerAnnotatedFieldResolves() throws NoSuchFieldException {
        Resolved resolved = singletonWithQualifier.resolve(IClass.getClass(DummyBean.class), field("providerField"));
        assertTrue(resolved.resolved());
        assertSame(IClass.getClass(DummyBean.class), resolved.elementSupplier().getSuppliedClass());
    }

    @Test
    void qualifierKnownToResolverIsAccepted() throws NoSuchFieldException {
        // QualifierA is registered in singletonWithQualifier's qualifier set.
        Resolved resolved = singletonWithQualifier.resolve(IClass.getClass(DummyBean.class), field("qualifiedField"));
        assertTrue(resolved.resolved());
        assertNotNull(resolved.elementSupplier());
    }

    @Test
    void unknownQualifierIsIgnoredButFieldStillResolves() throws NoSuchFieldException {
        // prototypeNoQualifier has an empty qualifier set, so QualifierA is treated as a non-qualifier.
        Resolved resolved = prototypeNoQualifier.resolve(IClass.getClass(DummyBean.class), field("qualifiedField"));
        assertTrue(resolved.resolved(), "unknown qualifier is simply ignored, resolution still succeeds");
    }

    @Test
    void singletonAndPrototypeReturnIndependentResults() throws NoSuchFieldException {
        Resolved s = singletonWithQualifier.resolve(IClass.getClass(DummyBean.class), field("plain"));
        Resolved p = prototypeNoQualifier.resolve(IClass.getClass(DummyBean.class), field("plain"));
        assertNotNull(s.elementSupplier());
        assertNotNull(p.elementSupplier());
        // Different resolver invocations build distinct supplier builders.
        assertFalse(s.elementSupplier() == p.elementSupplier());
    }

    @SuppressWarnings("unused")
    private static class Fixture {
        private DummyBean plain;

        @Nullable
        private DummyBean nullableField;

        @Named("specific")
        private DummyBean namedField;

        @Named("")
        private DummyBean blankNamedField;

        @Provider("app")
        private DummyBean providerField;

        @QualifierA
        private DummyBean qualifiedField;
    }
}
