package com.garganttua.core.aot.reflection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.aot.commons.AOTRegistry;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IConstructor;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;

/**
 * Behavioural tests for {@link CoreInfrastructureSeed} — the static-init seed
 * that pre-populates {@link AOTRegistry} with framework infrastructure types
 * and synthesizes type-identity {@link AOTClass} descriptors.
 */
class CoreInfrastructureSeedBehaviourTest {

    @BeforeEach
    void setUp() {
        TestReflectionSupport.installReflection();
        CoreInfrastructureSeed.bootstrap();
    }

    // --- bootstrap() registration list ---

    @Test
    void bootstrap_registersFrameworkBuilderInterfaces() {
        assertTrue(AOTRegistry.getInstance().contains(IReflectionBuilder.class.getName()));
    }

    @Test
    void bootstrap_registeredBuilderInterfaceCarriesInterfaceFlag() {
        Optional<IClass<IReflectionBuilder>> hit =
                AOTRegistry.getInstance().get(IReflectionBuilder.class.getName());
        assertTrue(hit.isPresent());
        assertTrue(hit.get().isInterface(),
                "registerInterface must force the INTERFACE modifier bit");
        assertTrue(Modifier.isInterface(hit.get().getModifiers()));
    }

    @Test
    void bootstrap_registersJdkValueTypesAsClassesNotInterfaces() {
        Optional<IClass<String>> hit = AOTRegistry.getInstance().get(String.class.getName());
        assertTrue(hit.isPresent());
        assertFalse(hit.get().isInterface(), "java.lang.String must stay a class");
        assertEquals("java.lang.String", hit.get().getName());
    }

    @Test
    void bootstrap_registersJdkCollectionInterfaces() {
        assertTrue(AOTRegistry.getInstance().contains(java.util.Map.class.getName()));
        assertTrue(AOTRegistry.getInstance().contains(java.util.List.class.getName()));
        Optional<IClass<java.util.Map>> map = AOTRegistry.getInstance().get(java.util.Map.class.getName());
        assertTrue(map.isPresent());
        assertTrue(map.get().isInterface());
    }

    @Test
    void bootstrap_isIdempotent_repeatedCallsKeepSameDescriptorInstance() {
        Optional<IClass<String>> first = AOTRegistry.getInstance().get(String.class.getName());
        CoreInfrastructureSeed.bootstrap();
        CoreInfrastructureSeed.bootstrap();
        Optional<IClass<String>> second = AOTRegistry.getInstance().get(String.class.getName());
        assertTrue(first.isPresent() && second.isPresent());
        // Idempotency: registerType short-circuits on contains(), so the
        // instance must be reused, never overwritten.
        assertEquals(first.get(), second.get());
    }

    // --- synthesize(Class) ---

    @Test
    void synthesize_copiesIdentityAndHierarchy() {
        AOTClass<java.util.ArrayList> desc = CoreInfrastructureSeed.synthesize(java.util.ArrayList.class);
        assertEquals(java.util.ArrayList.class.getName(), desc.getName());
        assertEquals("ArrayList", desc.getSimpleName());
        assertEquals("java.util", desc.getPackageName());
        assertEquals(java.util.AbstractList.class.getName(), desc.getSuperclass().getName());
        assertFalse(desc.isInterface());
    }

    @Test
    void synthesize_doesNotForceInterfaceFlagForConcreteClass() {
        AOTClass<java.util.ArrayList> desc = CoreInfrastructureSeed.synthesize(java.util.ArrayList.class);
        assertFalse(Modifier.isInterface(desc.getModifiers()));
    }

    @Test
    void synthesize_emitsNoArgConstructorWhenPresent() {
        AOTClass<java.util.ArrayList> desc = CoreInfrastructureSeed.synthesize(java.util.ArrayList.class);
        IConstructor<?>[] ctors = desc.getDeclaredConstructors();
        // ArrayList has a public no-arg ctor → at least one descriptor ctor.
        boolean hasNoArg = false;
        for (IConstructor<?> c : ctors) {
            if (c.getParameterTypes().length == 0) {
                hasNoArg = true;
                break;
            }
        }
        assertTrue(hasNoArg, "synthesizeNoArgConstructor must emit the no-arg ctor descriptor");
    }

    @Test
    void synthesize_handlesPrimitiveType_noNullsForCanonicalOrPackage() {
        AOTClass<Integer> desc = CoreInfrastructureSeed.synthesize(int.class);
        assertTrue(desc.isPrimitive());
        assertEquals("int", desc.getName());
        // Defensive non-null contract: package may be empty string but never null.
        assertNotNull(desc.getPackageName());
        assertNotNull(desc.getCanonicalName());
        // Primitive has no superclass.
        assertNull(desc.getSuperclass());
        // No constructor synthesizable for a primitive.
        assertEquals(0, desc.getDeclaredConstructors().length);
    }

    @Test
    void synthesize_arrayType_hasArrayFlagAndNoNoArgCtor() {
        AOTClass<String[]> desc = CoreInfrastructureSeed.synthesize(String[].class);
        assertTrue(desc.isArray());
        assertEquals(0, desc.getDeclaredConstructors().length,
                "Arrays have no declared no-arg constructor to synthesize");
    }

    @Test
    void synthesize_interfaceType_reportsInterfaceFlagWithoutForcing() {
        AOTClass<Runnable> desc = CoreInfrastructureSeed.synthesize(Runnable.class);
        assertTrue(desc.isInterface(), "Runnable is genuinely an interface");
    }

    @Test
    void synthesize_enumType_reportsEnumFlag() {
        AOTClass<java.time.DayOfWeek> desc = CoreInfrastructureSeed.synthesize(java.time.DayOfWeek.class);
        assertTrue(desc.isEnum());
    }

    @Test
    void synthesize_capturesDirectlyImplementedInterfaces() {
        AOTClass<java.util.ArrayList> desc = CoreInfrastructureSeed.synthesize(java.util.ArrayList.class);
        boolean hasList = false;
        for (IClass<?> iface : desc.getInterfaces()) {
            if (iface.getName().equals(java.util.List.class.getName())) {
                hasList = true;
            }
        }
        assertTrue(hasList, "ArrayList directly implements java.util.List");
    }

    @Test
    void synthesize_exposesClassLevelAnnotations() {
        AOTClass<AnnotatedSample> desc = CoreInfrastructureSeed.synthesize(AnnotatedSample.class);
        assertEquals(1, desc.getDeclaredAnnotations().length);
        assertEquals(SampleMarker.class.getName(),
                desc.getDeclaredAnnotations()[0].annotationType().getName());
    }

    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @interface SampleMarker {
    }

    @SampleMarker
    static class AnnotatedSample {
    }
}
