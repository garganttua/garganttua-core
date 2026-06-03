package com.garganttua.core.reflection.binders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.binders.dsl.AbstractFieldBinderBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeClass;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ContextualSupplierBuilder;
import com.garganttua.core.supply.dsl.FixedSupplierBuilder;
import com.garganttua.core.supply.dsl.ISupplierBuilder;
import com.garganttua.core.supply.dsl.NullSupplierBuilder;

/**
 * Additional behaviour tests for {@link AbstractFieldBinderBuilder} covering the
 * branches not reached by {@code FieldBinderBuilderTest}: builder identity (equals /
 * hashCode based on the field address), the contextual-owner production path,
 * allowNull combined with a null-supplying value supplier, the null-supplier guard
 * on {@code withValue}, the unresolved-address {@code findField} guard, and
 * re-targeting a field after a first selection.
 */
public class FieldBinderBuilderMoreBehaviourTest {

    @BeforeAll
    static void setUp() throws DslException {
        IClass.setReflection(ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider()).build());
    }

    @AfterAll
    static void tearDown() {
        IClass.setReflection(null);
    }

    public static class Bean {
        public String name = "initial";
        public String other = "other";
    }

    static class ConcreteFieldBinderBuilder
            extends AbstractFieldBinderBuilder<String, Bean, ConcreteFieldBinderBuilder, Object> {

        ConcreteFieldBinderBuilder(ISupplierBuilder<Bean, ? extends ISupplier<Bean>> ownerSupplier)
                throws DslException {
            super(new Object(), ownerSupplier, RuntimeClass.of(String.class), Set.of());
        }

        @Override
        protected void doAutoDetection() throws DslException {
        }

        @Override
        protected void doAutoDetectionWithDependency(Object dependency) throws DslException {
        }

        @Override
        protected void doPreBuildWithDependency_(Object dependency) {
        }

        @Override
        protected void doPostBuildWithDependency(Object dependency) {
        }

        // expose protected findField for the unresolved-address guard test
        com.garganttua.core.reflection.IField exposeFindField() throws DslException {
            return this.findField();
        }
    }

    private ConcreteFieldBinderBuilder builderFor(Bean bean) throws DslException {
        ConcreteFieldBinderBuilder b = new ConcreteFieldBinderBuilder(
                FixedSupplierBuilder.of(bean, RuntimeClass.of(Bean.class)));
        b.provide(ReflectionBuilder.builder().withProvider(new RuntimeReflectionProvider()));
        return b;
    }

    // ===== identity equals / hashCode based on resolved address =====

    @Test
    public void equalsAndHashCodeUsePendingAddress() throws DslException {
        ConcreteFieldBinderBuilder b = builderFor(new Bean());
        ObjectAddress addr = new ObjectAddress("name", true);
        b.field(addr);

        // pendingAddress is delegated to in equals/hashCode
        assertTrue(b.equals(addr));
        assertEquals(addr.hashCode(), b.hashCode());
    }

    @Test
    public void equalsUsesResolvedAddressAfterBuild() throws DslException {
        Bean bean = new Bean();
        ConcreteFieldBinderBuilder b = builderFor(bean);
        b.field("name").withValue("v");
        b.build(); // resolves the address

        // after build, this.address is set and equals delegates to it
        assertTrue(b.equals(new ObjectAddress("name", true)));
    }

    // ===== contextual owner production path =====

    @Test
    public void contextualOwnerProducesContextualFieldBinder() throws DslException {
        ContextualSupplierBuilder<Bean, Bean> ownerBuilder = new ContextualSupplierBuilder<>(
                (ctx, others) -> Optional.of(ctx),
                RuntimeClass.of(Bean.class), RuntimeClass.of(Bean.class));
        ConcreteFieldBinderBuilder b = new ConcreteFieldBinderBuilder(ownerBuilder);
        b.provide(ReflectionBuilder.builder().withProvider(new RuntimeReflectionProvider()));
        b.field("name").withValue("ctxValue");

        IFieldBinder<Bean, String> binder = b.build();
        assertTrue(binder instanceof IContextualFieldBinder,
                "expected contextual field binder, got " + binder.getClass().getSimpleName());

        Bean bean = new Bean();
        IContextualFieldBinder<Bean, String, Bean, Object> ctxBinder =
                (IContextualFieldBinder<Bean, String, Bean, Object>) binder;
        ctxBinder.setValue(bean, null);
        assertEquals("ctxValue", bean.name);
    }

    // ===== allowNull with a null-supplying value supplier =====

    @Test
    public void allowNullTrueBuildsButSetValueThrowsOnEmptyOptional() throws DslException {
        Bean bean = new Bean();
        ConcreteFieldBinderBuilder b = builderFor(bean);
        b.field("name")
                .withValue(new NullSupplierBuilder<String>(RuntimeClass.of(String.class)))
                .allowNull(true);

        // allowNull(true) lets the build succeed (the NullableSupplier does not reject empties),
        // but FieldBinder.setValue() unconditionally calls valueSupplier.supply().get() on the
        // resulting empty Optional -> NoSuchElementException. Documenting the real behaviour;
        // see SUSPECTED BUG note in the agent report.
        IFieldBinder<Bean, String> binder = b.build();
        assertThrows(java.util.NoSuchElementException.class, binder::setValue);
    }

    // ===== withValue null-supplier guard =====

    @Test
    public void withValueRejectsNullSupplier() throws DslException {
        ConcreteFieldBinderBuilder b = builderFor(new Bean());
        assertThrows(NullPointerException.class,
                () -> b.withValue((ISupplierBuilder<?, ? extends ISupplier<?>>) null));
    }

    // ===== findField guard when address unresolved =====

    @Test
    public void findFieldBeforeResolutionThrows() throws DslException {
        ConcreteFieldBinderBuilder b = builderFor(new Bean());
        // no field() call -> address is null -> findField guards with DslException
        assertThrows(DslException.class, b::exposeFindField);
    }

    @Test
    public void findFieldAfterBuildResolvesLeafField() throws DslException, com.garganttua.core.reflection.ReflectionException {
        Bean bean = new Bean();
        ConcreteFieldBinderBuilder b = builderFor(bean);
        b.field("name").withValue("v");
        b.build();

        assertEquals("name", b.exposeFindField().getName());
    }

    // ===== re-targeting a field clears previous pending selection =====

    @Test
    public void reTargetingFieldOverridesPreviousSelection() throws DslException, com.garganttua.core.reflection.ReflectionException {
        Bean bean = new Bean();
        ConcreteFieldBinderBuilder b = builderFor(bean);
        b.field("name");   // first selection
        b.field("other");  // overrides
        b.withValue("changed");

        IFieldBinder<Bean, String> binder = b.build();
        binder.setValue();
        assertEquals("changed", bean.other);
        assertEquals("initial", bean.name); // untouched
    }

    // ===== allowNull rejects null primitive argument =====

    @Test
    public void allowNullFalseStillWritesNonNull() throws DslException, com.garganttua.core.reflection.ReflectionException {
        Bean bean = new Bean();
        ConcreteFieldBinderBuilder b = builderFor(bean);
        b.field("name").withValue("present").allowNull(false);

        IFieldBinder<Bean, String> binder = b.build();
        binder.setValue();
        assertEquals("present", bean.name);
        assertFalse(binder.getValue() == null);
    }
}
