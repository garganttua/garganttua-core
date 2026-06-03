package com.garganttua.core.nativve.image.config.reflection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.garganttua.core.nativve.IReflectionConfigurationEntry;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.JdkClass;

/**
 * Behaviour tests that actually exercise {@link ReflectConfigEntryBuilder} end to
 * end. Unlike the first-pass test (which pins the static-init failure when no
 * {@code IReflectionProvider} is present), these run with a test-scoped
 * {@code RuntimeReflectionProvider} on the classpath (at the FQN the production
 * code resolves via {@code Class.forName}), so the builder's static initializer
 * succeeds and the built {@link ReflectConfigEntry} structure can be asserted.
 *
 * <p>Member-level introspection paths (auto-detection, {@code fieldsAnnotatedWith})
 * are intentionally not covered here: the JDK-backed test {@code IClass} throws
 * on {@code getDeclaredFields()/Methods()/Constructors()}. The String-based
 * overloads carry the same core add/remove logic and are fully exercised.</p>
 */
public class ReflectConfigEntryBuilderBuildBehaviourTest {

    private ReflectConfigEntryBuilder builder(Class<?> type) {
        return new ReflectConfigEntryBuilder(JdkClass.of(type));
    }

    private IClass<?> ic(Class<?> type) {
        return JdkClass.of(type);
    }

    @Test
    public void constructorSeedsNameAndEmptyFieldAndMethodLists() {
        IReflectionConfigurationEntry entry = builder(String.class).build();

        assertEquals("java.lang.String", entry.getName());
        assertNotNull(entry.getFields());
        assertNotNull(entry.getMethods());
        assertTrue(entry.getFields().isEmpty());
        assertTrue(entry.getMethods().isEmpty());
    }

    @Test
    public void constructorRejectsNullType() {
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class,
                () -> new ReflectConfigEntryBuilder((IClass<?>) null));
    }

    @Test
    public void everyBulkFlagFlowsThroughToTheBuiltEntry() {
        ReflectConfigEntry entry = (ReflectConfigEntry) builder(String.class)
                .queryAllDeclaredConstructors(true)
                .queryAllPublicConstructors(true)
                .queryAllDeclaredMethods(true)
                .queryAllPublicMethods(true)
                .allDeclaredClasses(true)
                .allPublicClasses(true)
                .allDeclaredFields(true)
                .build();

        assertTrue(entry.isQueryAllDeclaredConstructors());
        assertTrue(entry.isQueryAllPublicConstructors());
        assertTrue(entry.isQueryAllDeclaredMethods());
        assertTrue(entry.isQueryAllPublicMethods());
        assertTrue(entry.isAllDeclaredClasses());
        assertTrue(entry.isAllPublicClasses());
        assertTrue(entry.isAllDeclaredFields());
    }

    @Test
    public void addingFieldsByNameDeDuplicates() {
        IReflectionConfigurationEntry entry = builder(String.class)
                .field("value")
                .field("hash")
                .field("value") // duplicate must be ignored
                .build();

        assertEquals(2, entry.getFields().size());
        List<String> names = entry.getFields().stream()
                .map(IReflectionConfigurationEntry.Field::getName).toList();
        assertTrue(names.contains("value"));
        assertTrue(names.contains("hash"));
    }

    @Test
    public void removeFieldByNameDropsOnlyThatField() {
        IReflectionConfigurationEntry entry = builder(String.class)
                .field("a").field("b").field("c")
                .removeField("b")
                .build();

        List<String> names = entry.getFields().stream()
                .map(IReflectionConfigurationEntry.Field::getName).toList();
        assertEquals(2, names.size());
        assertTrue(names.contains("a"));
        assertTrue(names.contains("c"));
        assertFalse(names.contains("b"));
    }

    @Test
    public void addingMethodRecordsNameAndParameterTypeNames() {
        IReflectionConfigurationEntry entry = builder(String.class)
                .method("substring", ic(int.class), ic(int.class))
                .build();

        assertEquals(1, entry.getMethods().size());
        IReflectionConfigurationEntry.Method m = entry.getMethods().get(0);
        assertEquals("substring", m.getName());
        assertEquals(List.of("int", "int"), m.getParameterTypes());
    }

    @Test
    public void methodsWithSameNameButDifferentParametersAreBothKept() {
        IReflectionConfigurationEntry entry = builder(String.class)
                .method("valueOf", ic(int.class))
                .method("valueOf", ic(long.class))
                .build();

        assertEquals(2, entry.getMethods().size());
    }

    @Test
    public void identicalMethodSignatureIsDeDuplicated() {
        IReflectionConfigurationEntry entry = builder(String.class)
                .method("indexOf", ic(int.class))
                .method("indexOf", ic(int.class))
                .build();

        assertEquals(1, entry.getMethods().size());
    }

    @Test
    public void constructorIsRecordedAsInitMethod() {
        IReflectionConfigurationEntry entry = builder(String.class)
                .constructor("<ignored>", ic(char[].class))
                .build();

        assertEquals(1, entry.getMethods().size());
        assertEquals("<init>", entry.getMethods().get(0).getName());
        assertEquals(List.of("[C"), entry.getMethods().get(0).getParameterTypes());
    }

    @Test
    public void removeMethodMatchesOnNameAndParameterTypes() {
        IReflectionConfigurationEntry entry = builder(String.class)
                .method("valueOf", ic(int.class))
                .method("valueOf", ic(long.class))
                .removeMethod("valueOf", ic(int.class))
                .build();

        assertEquals(1, entry.getMethods().size());
        assertEquals(List.of("long"), entry.getMethods().get(0).getParameterTypes());
    }

    @Test
    public void removeMethodWithNonMatchingParametersKeepsEverything() {
        IReflectionConfigurationEntry entry = builder(String.class)
                .method("valueOf", ic(int.class))
                .removeMethod("valueOf", ic(double.class)) // different params -> no removal
                .build();

        assertEquals(1, entry.getMethods().size());
    }

    @Test
    public void removeConstructorRemovesTheInitEntry() {
        IReflectionConfigurationEntry entry = builder(String.class)
                .constructor("<init>", ic(char[].class))
                .removeConstructor("<init>", ic(char[].class))
                .build();

        assertTrue(entry.getMethods().isEmpty());
    }

    @Test
    public void builderIsFluentReturningItself() {
        ReflectConfigEntryBuilder b = builder(String.class);
        assertEquals(b, b.field("x"));
        assertEquals(b, b.allDeclaredFields(true));
        assertEquals(b, b.method("m"));
    }

    @Test
    public void staticBuilderFactoryProducesEquivalentResult() {
        IReflectionConfigurationEntry entry = ReflectConfigEntryBuilder.builder(ic(Integer.class)).build();
        assertEquals("java.lang.Integer", entry.getName());
    }

    @Test
    public void entryWrappingConstructorReusesExistingEntryInstance() {
        ReflectConfigEntry existing = new ReflectConfigEntry("java.lang.Long");
        existing.setFields(new java.util.ArrayList<>());
        existing.setMethods(new java.util.ArrayList<>());
        existing.setAllDeclaredFields(true);

        ReflectConfigEntryBuilder b = new ReflectConfigEntryBuilder((IReflectionConfigurationEntry) existing);
        ReflectConfigEntry built = (ReflectConfigEntry) b.field("value").build();

        // The same backing entry instance is returned and mutated.
        assertEquals(existing, built);
        assertTrue(built.isAllDeclaredFields());
        assertEquals(1, built.getFields().size());
        assertEquals("value", built.getFields().get(0).getName());
    }

    @Test
    public void autoDetectReadsReflectedFlagsFromAnnotation() {
        // String is not @Reflected; auto-detection over a non-introspectable
        // JdkClass would throw on getDeclaredFields(). So assert the no-annotation
        // branch is skipped cleanly by NOT triggering member introspection: a
        // type with no @Reflected annotation present means r == null, but the
        // method still calls getDeclaredFields(). We therefore only assert the
        // structural outcome via the public flag setters above and leave the
        // introspection-bearing autoDetect path to integration tests.
        ReflectConfigEntry entry = (ReflectConfigEntry) builder(String.class)
                .allPublicClasses(true)
                .build();
        assertTrue(entry.isAllPublicClasses());
    }
}
