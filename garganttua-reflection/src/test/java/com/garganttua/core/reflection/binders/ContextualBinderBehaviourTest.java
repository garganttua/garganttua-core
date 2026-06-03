package com.garganttua.core.reflection.binders;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.constructors.ConstructorResolver;
import com.garganttua.core.reflection.constructors.ResolvedConstructor;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.methods.MethodResolver;
import com.garganttua.core.reflection.methods.ResolvedMethod;
import com.garganttua.core.reflection.runtime.RuntimeClass;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.supply.ContextualSupplier;
import com.garganttua.core.supply.FixedSupplier;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

/**
 * Behaviour tests for the contextual binder implementations
 * ({@link ContextualConstructorBinder}, {@link ContextualMethodBinder},
 * {@link ContextualFieldBinder}) and the shared {@link ContextualExecutableBinder}
 * base — exercising context-driven argument resolution, metadata accessors and the
 * exception paths that the index-based builder tests never reach.
 */
public class ContextualBinderBehaviourTest {

    private static IReflection reflection;
    private static final RuntimeReflectionProvider PROVIDER = new RuntimeReflectionProvider();

    @BeforeAll
    static void setUp() throws DslException {
        reflection = ReflectionBuilder.builder().withProvider(new RuntimeReflectionProvider()).build();
        IClass.setReflection(reflection);
    }

    @AfterAll
    static void tearDown() {
        IClass.setReflection(null);
    }

    public static class Target {
        public final String name;
        public final int value;

        public Target(String name, int value) {
            this.name = name;
            this.value = value;
        }
    }

    public static class Greeter {
        public String greet(String who) {
            return "hi " + who;
        }
    }

    public static class Holder {
        public String field = "start";
    }

    // A contextual supplier that pulls a String out of a String context (identity).
    private static IContextualSupplier<String, String> stringFromContext() {
        return new ContextualSupplier<>(
                (ctx, others) -> Optional.of(ctx),
                RuntimeClass.of(String.class),
                RuntimeClass.of(String.class));
    }

    // =====================================================================
    // ContextualConstructorBinder
    // =====================================================================

    @Test
    public void contextualConstructorBuildsArgumentsFromContext() throws ReflectionException {
        ResolvedConstructor<Target> resolved = ConstructorResolver.constructorByParameterTypes(
                RuntimeClass.of(Target.class), reflection,
                RuntimeClass.of(String.class), RuntimeClass.of(int.class));

        List<ISupplier<?>> params = List.of(
                stringFromContext(),
                new FixedSupplier<>(42, RuntimeClass.of(Integer.class)));

        ContextualConstructorBinder<Target> binder = new ContextualConstructorBinder<>(
                RuntimeClass.of(Target.class), resolved.constructor(), params);

        Optional<IMethodReturn<Target>> result = binder.execute(null, "Bob");
        assertTrue(result.isPresent());
        Target t = result.get().single();
        assertEquals("Bob", t.name);
        assertEquals(42, t.value);
    }

    @Test
    public void contextualConstructorSupplyDelegatesToExecute() throws ReflectionException, SupplyException {
        ResolvedConstructor<Target> resolved = ConstructorResolver.constructorByParameterTypes(
                RuntimeClass.of(Target.class), reflection,
                RuntimeClass.of(String.class), RuntimeClass.of(int.class));
        List<ISupplier<?>> params = List.of(
                stringFromContext(),
                new FixedSupplier<>(7, RuntimeClass.of(Integer.class)));
        ContextualConstructorBinder<Target> binder = new ContextualConstructorBinder<>(
                RuntimeClass.of(Target.class), resolved.constructor(), params);

        Target t = binder.supply(null, "Eve").get().single();
        assertEquals("Eve", t.name);
        assertEquals(7, t.value);
    }

    @Test
    public void contextualConstructorMetadataAccessors() throws ReflectionException {
        ResolvedConstructor<Target> resolved = ConstructorResolver.constructorByParameterTypes(
                RuntimeClass.of(Target.class), reflection,
                RuntimeClass.of(String.class), RuntimeClass.of(int.class));
        List<ISupplier<?>> params = List.of(
                stringFromContext(),
                new FixedSupplier<>(1, RuntimeClass.of(Integer.class)));
        ContextualConstructorBinder<Target> binder = new ContextualConstructorBinder<>(
                RuntimeClass.of(Target.class), resolved.constructor(), params);

        assertEquals(Target.class.getName(), binder.getConstructedType().getName());
        assertEquals(Target.class, binder.getSuppliedType());
        assertEquals(Target.class.getName(), binder.getSuppliedClass().getName());
        assertNotNull(binder.constructor());
        assertTrue(binder.getExecutableReference().contains("Target"));
    }

    @Test
    public void contextualConstructorRejectsNullClassAndConstructor() throws ReflectionException {
        ResolvedConstructor<Target> resolved = ConstructorResolver.constructorByParameterTypes(
                RuntimeClass.of(Target.class), reflection,
                RuntimeClass.of(String.class), RuntimeClass.of(int.class));
        assertThrows(NullPointerException.class,
                () -> new ContextualConstructorBinder<>(null, resolved.constructor(), List.of()));
        assertThrows(NullPointerException.class,
                () -> new ContextualConstructorBinder<>(RuntimeClass.of(Target.class), null, List.of()));
    }

    // =====================================================================
    // ContextualExecutableBinder shared behaviour (via constructor binder)
    // =====================================================================

    @Test
    public void getParametersContextTypesMarksContextualSlotsOnly() throws ReflectionException {
        ResolvedConstructor<Target> resolved = ConstructorResolver.constructorByParameterTypes(
                RuntimeClass.of(Target.class), reflection,
                RuntimeClass.of(String.class), RuntimeClass.of(int.class));
        List<ISupplier<?>> params = List.of(
                stringFromContext(),
                new FixedSupplier<>(9, RuntimeClass.of(Integer.class)));
        ContextualConstructorBinder<Target> binder = new ContextualConstructorBinder<>(
                RuntimeClass.of(Target.class), resolved.constructor(), params);

        IClass<?>[] ctxTypes = binder.getParametersContextTypes();
        assertEquals(2, ctxTypes.length);
        assertEquals(String.class.getName(), ctxTypes[0].getName()); // contextual -> owner context type
        assertNull(ctxTypes[1]); // fixed supplier -> null
    }

    @Test
    public void getParametersContextTypesEmptyForNoParams() throws ReflectionException {
        ResolvedConstructor<Target> resolved = ConstructorResolver.constructorByParameterTypes(
                RuntimeClass.of(Target.class), reflection,
                RuntimeClass.of(String.class), RuntimeClass.of(int.class));
        // build a no-arg contextual binder to test the empty short-circuit
        ContextualConstructorBinder<Target> binder = new ContextualConstructorBinder<>(
                RuntimeClass.of(Target.class), resolved.constructor(), List.of());
        assertArrayEquals(new IClass<?>[0], binder.getParametersContextTypes());
    }

    @Test
    public void dependenciesContainSuppliedClasses() throws ReflectionException {
        ResolvedConstructor<Target> resolved = ConstructorResolver.constructorByParameterTypes(
                RuntimeClass.of(Target.class), reflection,
                RuntimeClass.of(String.class), RuntimeClass.of(int.class));
        List<ISupplier<?>> params = List.of(
                stringFromContext(),
                new FixedSupplier<>(3, RuntimeClass.of(Integer.class)));
        ContextualConstructorBinder<Target> binder = new ContextualConstructorBinder<>(
                RuntimeClass.of(Target.class), resolved.constructor(), params);

        var deps = binder.dependencies();
        assertTrue(deps.stream().anyMatch(c -> c.getName().equals(String.class.getName())));
        assertTrue(deps.stream().anyMatch(c -> c.getName().equals(Integer.class.getName())));
    }

    @Test
    public void buildArgumentsPropagatesSupplyFailureAsReflectionException() throws ReflectionException {
        ResolvedConstructor<Target> resolved = ConstructorResolver.constructorByParameterTypes(
                RuntimeClass.of(Target.class), reflection,
                RuntimeClass.of(String.class), RuntimeClass.of(int.class));
        // contextual supplier declaring an Integer context; we pass a String context at execute
        // time -> ContextualSupplier.supply raises a SupplyException, which buildArguments wraps
        // into a ReflectionException.
        IContextualSupplier<String, Integer> mismatched = new ContextualSupplier<>(
                (ctx, others) -> Optional.of("never"),
                RuntimeClass.of(String.class), RuntimeClass.of(Integer.class));
        List<ISupplier<?>> params = List.of(mismatched,
                new FixedSupplier<>(1, RuntimeClass.of(Integer.class)));
        ContextualConstructorBinder<Target> binder = new ContextualConstructorBinder<>(
                RuntimeClass.of(Target.class), resolved.constructor(), params);

        // ownerContext is Void for constructors; the bad context is forwarded to the
        // contextual parameter supplier via the varargs.
        assertThrows(ReflectionException.class, () -> binder.execute(null, "not-an-integer"));
    }

    @Test
    public void contextualExecutableRejectsNullSuppliersList() throws ReflectionException {
        ResolvedConstructor<Target> resolved = ConstructorResolver.constructorByParameterTypes(
                RuntimeClass.of(Target.class), reflection,
                RuntimeClass.of(String.class), RuntimeClass.of(int.class));
        assertThrows(NullPointerException.class,
                () -> new ContextualConstructorBinder<>(RuntimeClass.of(Target.class), resolved.constructor(), null));
    }

    // =====================================================================
    // ContextualMethodBinder
    // =====================================================================

    @Test
    public void contextualMethodInvokesWithContextualParameter() throws ReflectionException {
        ResolvedMethod method = MethodResolver.methodByName(
                RuntimeClass.of(Greeter.class), reflection, "greet",
                RuntimeClass.of(String.class), RuntimeClass.of(String.class));

        // owner is fixed, parameter is contextual
        ISupplier<Greeter> owner = new FixedSupplier<>(new Greeter(), RuntimeClass.of(Greeter.class));
        List<ISupplier<?>> params = List.of(stringFromContext());

        ContextualMethodBinder<String, Object> binder = new ContextualMethodBinder<>(owner, method, params, false);

        Optional<IMethodReturn<String>> result = binder.execute(null, "World");
        assertTrue(result.isPresent());
        assertEquals("hi World", result.get().single());
    }

    @Test
    public void contextualMethodOwnerContextTypeNullWhenOwnerNonContextual() throws ReflectionException {
        ResolvedMethod method = MethodResolver.methodByName(
                RuntimeClass.of(Greeter.class), reflection, "greet",
                RuntimeClass.of(String.class), RuntimeClass.of(String.class));
        ISupplier<Greeter> owner = new FixedSupplier<>(new Greeter(), RuntimeClass.of(Greeter.class));
        ContextualMethodBinder<String, Object> binder = new ContextualMethodBinder<>(
                owner, method, List.of(stringFromContext()));
        assertNull(binder.getOwnerContextType());
    }

    @Test
    public void contextualMethodOwnerContextTypeFromContextualOwner() throws ReflectionException {
        ResolvedMethod method = MethodResolver.methodByName(
                RuntimeClass.of(Greeter.class), reflection, "greet",
                RuntimeClass.of(String.class), RuntimeClass.of(String.class));
        // contextual owner: supplies a Greeter out of a Greeter context
        IContextualSupplier<Greeter, Greeter> owner = new ContextualSupplier<>(
                (ctx, others) -> Optional.of(ctx),
                RuntimeClass.of(Greeter.class), RuntimeClass.of(Greeter.class));
        List<ISupplier<?>> params = List.of(new FixedSupplier<>("Sam", RuntimeClass.of(String.class)));
        ContextualMethodBinder<String, Greeter> binder = new ContextualMethodBinder<>(owner, method, params);

        assertEquals(Greeter.class.getName(), binder.getOwnerContextType().getName());
        Optional<IMethodReturn<String>> result = binder.execute(new Greeter());
        assertEquals("hi Sam", result.get().single());
    }

    @Test
    public void contextualMethodMetadataAndSupply() throws ReflectionException, SupplyException {
        ResolvedMethod method = MethodResolver.methodByName(
                RuntimeClass.of(Greeter.class), reflection, "greet",
                RuntimeClass.of(String.class), RuntimeClass.of(String.class));
        ISupplier<Greeter> owner = new FixedSupplier<>(new Greeter(), RuntimeClass.of(Greeter.class));
        ContextualMethodBinder<String, Object> binder = new ContextualMethodBinder<>(
                owner, method, List.of(stringFromContext()));

        assertEquals(String.class, binder.getSuppliedType());
        assertEquals(IMethodReturn.class.getName(), binder.getSuppliedClass().getName());
        assertTrue(binder.getExecutableReference().contains("greet"));
        assertEquals("hi Joe", binder.supply(null, "Joe").get().single());
    }

    @Test
    public void contextualMethodRejectsNullMethodAndOwner() throws ReflectionException {
        ResolvedMethod method = MethodResolver.methodByName(
                RuntimeClass.of(Greeter.class), reflection, "greet",
                RuntimeClass.of(String.class), RuntimeClass.of(String.class));
        ISupplier<Greeter> owner = new FixedSupplier<>(new Greeter(), RuntimeClass.of(Greeter.class));
        assertThrows(NullPointerException.class, () -> new ContextualMethodBinder<>(owner, null, List.of()));
        assertThrows(NullPointerException.class, () -> new ContextualMethodBinder<>(null, method, List.of()));
    }

    // =====================================================================
    // ContextualFieldBinder
    // =====================================================================

    @Test
    public void contextualFieldGetReadsCurrentValue() throws ReflectionException {
        Holder holder = new Holder();
        ISupplier<Holder> owner = new FixedSupplier<>(holder, RuntimeClass.of(Holder.class));
        ISupplier<String> value = new FixedSupplier<>("ignored", RuntimeClass.of(String.class));

        ContextualFieldBinder<Holder, String, Object, Object> binder = new ContextualFieldBinder<>(
                owner, new ObjectAddress("field", true), value, PROVIDER);

        assertEquals("start", binder.getValue(null));
    }

    @Test
    public void contextualFieldSetWritesValueFromContext() throws ReflectionException {
        Holder holder = new Holder();
        ISupplier<Holder> owner = new FixedSupplier<>(holder, RuntimeClass.of(Holder.class));
        // value supplier is contextual: pulls the String to write out of the value context
        IContextualSupplier<String, String> value = stringFromContext();

        ContextualFieldBinder<Holder, String, Object, String> binder = new ContextualFieldBinder<>(
                owner, new ObjectAddress("field", true), value, PROVIDER);

        binder.setValue(null, "written");
        assertEquals("written", holder.field);
    }

    @Test
    public void contextualFieldContextTypesDefaultToVoidWhenNonContextual() throws ReflectionException {
        Holder holder = new Holder();
        ISupplier<Holder> owner = new FixedSupplier<>(holder, RuntimeClass.of(Holder.class));
        ISupplier<String> value = new FixedSupplier<>("v", RuntimeClass.of(String.class));
        ContextualFieldBinder<Holder, String, Object, Object> binder = new ContextualFieldBinder<>(
                owner, new ObjectAddress("field", true), value, PROVIDER);

        assertEquals(Void.class.getName(), binder.getOwnerContextType().getName());
        assertEquals(Void.class.getName(), binder.getValueContextType().getName());
    }

    @Test
    public void contextualFieldContextTypesReflectContextualSuppliers() throws ReflectionException {
        Holder holder = new Holder();
        IContextualSupplier<Holder, Holder> owner = new ContextualSupplier<>(
                (ctx, others) -> Optional.of(ctx),
                RuntimeClass.of(Holder.class), RuntimeClass.of(Holder.class));
        IContextualSupplier<String, String> value = stringFromContext();
        ContextualFieldBinder<Holder, String, Holder, String> binder = new ContextualFieldBinder<>(
                owner, new ObjectAddress("field", true), value, PROVIDER);

        assertEquals(Holder.class.getName(), binder.getOwnerContextType().getName());
        assertEquals(String.class.getName(), binder.getValueContextType().getName());
    }

    @Test
    public void contextualFieldMetadataAccessors() throws ReflectionException {
        Holder holder = new Holder();
        ISupplier<Holder> owner = new FixedSupplier<>(holder, RuntimeClass.of(Holder.class));
        ISupplier<String> value = new FixedSupplier<>("v", RuntimeClass.of(String.class));
        ContextualFieldBinder<Holder, String, Object, Object> binder = new ContextualFieldBinder<>(
                owner, new ObjectAddress("field", true), value, PROVIDER);

        assertEquals(String.class, binder.getSuppliedType());
        assertEquals(String.class.getName(), binder.getSuppliedClass().getName());
        assertTrue(binder.getFieldReference().contains("field"));
    }

    @Test
    public void contextualFieldRejectsNullArguments() throws ReflectionException {
        Holder holder = new Holder();
        ISupplier<Holder> owner = new FixedSupplier<>(holder, RuntimeClass.of(Holder.class));
        ISupplier<String> value = new FixedSupplier<>("v", RuntimeClass.of(String.class));
        ObjectAddress addr = new ObjectAddress("field", true);

        assertThrows(NullPointerException.class, () -> new ContextualFieldBinder<>(owner, null, value, PROVIDER));
        assertThrows(NullPointerException.class, () -> new ContextualFieldBinder<>(owner, addr, null, PROVIDER));
        assertThrows(NullPointerException.class, () -> new ContextualFieldBinder<>(null, addr, value, PROVIDER));
        assertThrows(NullPointerException.class, () -> new ContextualFieldBinder<>(owner, addr, value, null));
    }

    @Test
    public void contextualFieldSetThrowsWhenOwnerSuppliesNull() throws ReflectionException {
        // owner supplier returns empty/null -> setValue must raise ReflectionException
        ISupplier<Holder> owner = new ContextualSupplier<Holder, Object>(
                (ctx, others) -> Optional.empty(),
                RuntimeClass.of(Holder.class), RuntimeClass.of(Object.class));
        ISupplier<String> value = new FixedSupplier<>("v", RuntimeClass.of(String.class));
        ContextualFieldBinder<Holder, String, Object, Object> binder = new ContextualFieldBinder<>(
                owner, new ObjectAddress("field", true), value, PROVIDER);

        assertThrows(ReflectionException.class, () -> binder.setValue(new Object(), null));
    }
}
