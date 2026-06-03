package com.garganttua.core.aot.reflection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IConstructor;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IMethod;

/**
 * Behavioural tests for {@link AOTClassBuilder} descriptor-assembly logic.
 *
 * <p>The builder is fed a shallow {@link IClass} synthesized by
 * {@link AOTReflectionProvider}; member queries fall back to the live class.
 * We assert exact member selection, removal semantics, the global query flags
 * and the class-metadata copied into the assembled {@link AOTClass}.</p>
 */
class AOTClassBuilderBehaviourTest {

    // --- Sample target hierarchy ---

    interface IGreeter {
    }

    interface IShouter {
    }

    static class Base {
        public int baseField;
    }

    @SuppressWarnings("unused")
    static class Sample extends Base implements IGreeter, IShouter {
        public String publicName;
        private int privateAge;
        protected long protectedCount;

        public Sample() {
        }

        public Sample(String name) {
            this.publicName = name;
        }

        Sample(String name, int age) {
            this.publicName = name;
            this.privateAge = age;
        }

        public String greet(String who) {
            return "hi " + who;
        }

        public int add(int a, int b) {
            return a + b;
        }

        private void secret() {
        }
    }

    private AOTReflectionProvider provider;

    @BeforeEach
    void setUp() {
        TestReflectionSupport.installReflection();
        provider = new AOTReflectionProvider();
    }

    private AOTClassBuilder<Sample> builderFor() {
        return new AOTClassBuilder<>(provider.getClass(Sample.class));
    }

    private IClass<?> ic(Class<?> c) {
        return provider.getClass(c);
    }

    private static boolean hasField(IClass<?> built, String name) {
        return Arrays.stream(built.getDeclaredFields()).anyMatch(f -> f.getName().equals(name));
    }

    private static boolean hasMethod(IClass<?> built, String name) {
        return Arrays.stream(built.getDeclaredMethods()).anyMatch(m -> m.getName().equals(name));
    }

    // --- explicit single-member addition ---

    @Test
    void field_byName_addsExactlyThatField() throws DslException {
        IClass<Sample> built = builderFor().field("publicName").build();
        IField[] fields = built.getDeclaredFields();
        assertEquals(1, fields.length, "Only the named field must be present");
        assertEquals("publicName", fields[0].getName());
        assertEquals("java.lang.String", fields[0].getType().getName());
    }

    @Test
    void field_unknownName_isSilentlyIgnored() throws DslException {
        // Pair with a real field so the descriptor's array stays non-empty and
        // the shallow-descriptor live-class fallback does NOT kick in; we then
        // observe that the unknown field was logged-and-skipped (only 1 field).
        IClass<Sample> built = builderFor()
                .field("publicName")
                .field("doesNotExist")
                .build();
        assertEquals(1, built.getDeclaredFields().length,
                "Unknown field must be logged-and-skipped, not added");
        assertTrue(hasField(built, "publicName"));
        assertFalse(hasField(built, "doesNotExist"));
    }

    @Test
    void method_byNameAndParams_addsMatchingOverload() throws DslException {
        IClass<Sample> built = builderFor()
                .method("greet", ic(String.class))
                .build();
        IMethod[] methods = built.getDeclaredMethods();
        assertEquals(1, methods.length);
        assertEquals("greet", methods[0].getName());
        assertEquals(1, methods[0].getParameterCount());
        assertEquals("java.lang.String", methods[0].getReturnType().getName());
    }

    @Test
    void method_wrongParamTypes_isIgnored() throws DslException {
        // greet(int) does not exist. Pair with a real method so the descriptor
        // array stays non-empty (no live-class fallback) and we can see the
        // non-matching overload was skipped.
        IClass<Sample> built = builderFor()
                .method("add", ic(int.class), ic(int.class))
                .method("greet", ic(int.class))   // no such overload → skipped
                .build();
        assertEquals(1, built.getDeclaredMethods().length);
        assertTrue(hasMethod(built, "add"));
        assertFalse(hasMethod(built, "greet"));
    }

    @Test
    void constructor_byParamTypes_addsMatchingConstructor() throws DslException {
        // Add two ctors so the descriptor's own array (length > 1) is trusted
        // and NOT replaced by the shallow-descriptor live-class fallback.
        IClass<Sample> built = builderFor()
                .constructor(ic(String.class))
                .constructor(ic(String.class), ic(int.class))
                .build();
        IConstructor<?>[] ctors = built.getDeclaredConstructors();
        assertEquals(2, ctors.length);
        assertEquals(1, Arrays.stream(ctors)
                .filter(c -> c.getParameterTypes().length == 1).count());
        assertEquals(1, Arrays.stream(ctors)
                .filter(c -> c.getParameterTypes().length == 2).count());
    }

    @Test
    void constructor_missingArity_isIgnored() throws DslException {
        // No 3-arg constructor exists; pair it with two real ones so the array
        // is trusted (length > 1) and we observe only the two that matched.
        IClass<Sample> built = builderFor()
                .constructor(ic(String.class))
                .constructor(ic(String.class), ic(int.class))
                .constructor(ic(String.class), ic(int.class), ic(long.class)) // no match → skipped
                .build();
        IConstructor<?>[] ctors = built.getDeclaredConstructors();
        assertEquals(2, ctors.length, "Only the two existing ctors must be added");
        assertFalse(Arrays.stream(ctors).anyMatch(c -> c.getParameterTypes().length == 3));
    }

    // --- removal semantics ---

    @Test
    void removeField_removesPreviouslyAddedField() throws DslException {
        IClass<Sample> built = builderFor()
                .field("publicName")
                .field("privateAge")
                .removeField("publicName")
                .build();
        assertFalse(hasField(built, "publicName"));
        assertTrue(hasField(built, "privateAge"));
        assertEquals(1, built.getDeclaredFields().length);
    }

    @Test
    void removeField_unknownName_isNoOp() throws DslException {
        IClass<Sample> built = builderFor()
                .field("publicName")
                .removeField("nope")
                .build();
        assertEquals(1, built.getDeclaredFields().length);
        assertTrue(hasField(built, "publicName"));
    }

    @Test
    void removeMethod_matchesNameAndParams() throws DslException {
        IClass<Sample> built = builderFor()
                .method("greet", ic(String.class))
                .method("add", ic(int.class), ic(int.class))
                .removeMethod("greet", ic(String.class))
                .build();
        assertFalse(hasMethod(built, "greet"));
        assertTrue(hasMethod(built, "add"));
        assertEquals(1, built.getDeclaredMethods().length);
    }

    @Test
    void removeMethod_wrongParamCount_isNoOp() throws DslException {
        IClass<Sample> built = builderFor()
                .method("add", ic(int.class), ic(int.class))
                .removeMethod("add", ic(int.class))   // wrong arity
                .build();
        assertTrue(hasMethod(built, "add"));
        assertEquals(1, built.getDeclaredMethods().length);
    }

    @Test
    void removeConstructor_matchesByParamTypes() throws DslException {
        // Keep at least two ctors after removal so the descriptor array
        // (length > 1) is trusted and not enriched by the live fallback.
        IClass<Sample> built = builderFor()
                .constructor()                                  // no-arg
                .constructor(ic(String.class))                  // arity 1 → removed
                .constructor(ic(String.class), ic(int.class))   // arity 2
                .removeConstructor(ic(String.class))
                .build();
        IConstructor<?>[] ctors = built.getDeclaredConstructors();
        assertEquals(2, ctors.length);
        assertEquals(0, Arrays.stream(ctors)
                .filter(c -> c.getParameterTypes().length == 1).count(),
                "1-arg ctor must have been removed");
        assertEquals(1, Arrays.stream(ctors)
                .filter(c -> c.getParameterTypes().length == 0).count());
        assertEquals(1, Arrays.stream(ctors)
                .filter(c -> c.getParameterTypes().length == 2).count());
    }

    // --- global query flags ---

    @Test
    void allDeclaredFields_collectsEveryDeclaredFieldOnce() throws DslException {
        IClass<Sample> built = builderFor().allDeclaredFields(true).build();
        // Sample declares exactly publicName, privateAge, protectedCount.
        assertEquals(3, built.getDeclaredFields().length);
        assertTrue(hasField(built, "publicName"));
        assertTrue(hasField(built, "privateAge"));
        assertTrue(hasField(built, "protectedCount"));
        // Inherited baseField must NOT appear (declared-only).
        assertFalse(hasField(built, "baseField"));
    }

    @Test
    void allDeclaredFields_doesNotDuplicateAnAlreadyAddedField() throws DslException {
        IClass<Sample> built = builderFor()
                .field("publicName")
                .allDeclaredFields(true)
                .build();
        long publicNameCount = Arrays.stream(built.getDeclaredFields())
                .filter(f -> f.getName().equals("publicName")).count();
        assertEquals(1, publicNameCount, "Pre-added field must not be duplicated by the flag");
        assertEquals(3, built.getDeclaredFields().length);
    }

    @Test
    void allPublicFields_collectsOnlyPublicDeclaredFields() throws DslException {
        IClass<Sample> built = builderFor().allPublicFields(true).build();
        assertEquals(1, built.getDeclaredFields().length);
        assertTrue(hasField(built, "publicName"));
        assertFalse(hasField(built, "privateAge"));
    }

    @Test
    void queryAllPublicMethods_excludesPrivateMethods() throws DslException {
        IClass<Sample> built = builderFor().queryAllPublicMethods(true).build();
        assertTrue(hasMethod(built, "greet"));
        assertTrue(hasMethod(built, "add"));
        assertFalse(hasMethod(built, "secret"),
                "private method must be excluded from allPublicMethods");
        assertTrue(Arrays.stream(built.getDeclaredMethods())
                .allMatch(m -> Modifier.isPublic(m.getModifiers())));
    }

    @Test
    void queryAllDeclaredMethods_includesPrivateMethods() throws DslException {
        IClass<Sample> built = builderFor().queryAllDeclaredMethods(true).build();
        assertTrue(hasMethod(built, "greet"));
        assertTrue(hasMethod(built, "secret"),
                "declared-methods flag must include private members");
    }

    @Test
    void queryAllPublicConstructors_excludesPackagePrivateConstructor() throws DslException {
        IClass<Sample> built = builderFor().queryAllPublicConstructors(true).build();
        // Sample has 2 public ctors (no-arg, String) + 1 package-private (String,int).
        assertEquals(2, built.getDeclaredConstructors().length);
        assertTrue(Arrays.stream(built.getDeclaredConstructors())
                .allMatch(c -> Modifier.isPublic(c.getModifiers())));
    }

    @Test
    void queryAllDeclaredConstructors_includesPackagePrivateConstructor() throws DslException {
        IClass<Sample> built = builderFor().queryAllDeclaredConstructors(true).build();
        assertEquals(3, built.getDeclaredConstructors().length);
        boolean hasPackagePrivate = Arrays.stream(built.getDeclaredConstructors())
                .anyMatch(c -> !Modifier.isPublic(c.getModifiers()));
        assertTrue(hasPackagePrivate);
    }

    // --- assembled class metadata ---

    @Test
    void assembledDescriptor_copiesIdentityAndHierarchy() throws DslException {
        IClass<Sample> built = builderFor().build();
        assertEquals(Sample.class.getName(), built.getName());
        assertEquals("Sample", built.getSimpleName());
        assertEquals(Sample.class.getPackageName(), built.getPackageName());
        // Superclass is Base.
        assertEquals(Base.class.getName(), built.getSuperclass().getName());
        // Interfaces preserved by binary name.
        String[] ifaceNames = Arrays.stream(built.getInterfaces())
                .map(IClass::getName).sorted().toArray(String[]::new);
        assertEquals(IGreeter.class.getName(), ifaceNames[0]);
        assertEquals(IShouter.class.getName(), ifaceNames[1]);
    }

    @Test
    void assembledDescriptor_copiesJvmFlags() throws DslException {
        IClass<Sample> built = builderFor().build();
        assertFalse(built.isInterface());
        assertFalse(built.isEnum());
        assertFalse(built.isRecord());
        assertFalse(built.isPrimitive());
        assertFalse(built.isArray());
        assertTrue(built.isMemberClass(), "Sample is a nested member class");
    }

    @Test
    void build_isCachedAndReturnsSameInstance() throws DslException {
        AOTClassBuilder<Sample> b = builderFor();
        IClass<Sample> first = b.build();
        IClass<Sample> second = b.build();
        assertSame(first, second, "AbstractAutomaticBuilder must cache the built instance");
    }

    @Test
    void emptyBuilder_collectsNoExplicitFields_butExposesIdentity() throws DslException {
        // With no flags and no explicit members the builder's own fieldList is
        // empty. The assembled AOTClass then transparently falls back to the
        // live class for getDeclaredFields() — documenting the shallow-
        // descriptor enrichment contract rather than fighting it.
        IClass<Sample> built = builderFor().build();
        assertEquals(Sample.class.getName(), built.getName());
        // Live fallback surfaces the 3 declared fields of Sample.
        assertEquals(3, built.getDeclaredFields().length);
    }
}
