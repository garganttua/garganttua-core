package com.garganttua.core.reflection.binders;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.binders.dsl.AbstractFieldBinderBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeClass;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.FixedSupplierBuilder;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

/**
 * Behaviour tests for {@link AbstractFieldBinderBuilder} via a concrete subclass:
 * resolving the target field by name / {@link IField} / {@link ObjectAddress},
 * supplying the value as a raw object or supplier, get/set round trips, the
 * allowNull flag, metadata accessors and the error paths.
 */
public class FieldBinderBuilderTest {

    @BeforeAll
    static void setUpReflection() throws DslException {
        IClass.setReflection(ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider())
                .build());
    }

    @AfterAll
    static void tearDown() {
        IClass.setReflection(null);
    }

    public static class Bean {
        public String name = "initial";
        private int count = 5;

        public Bean() {
        }
    }

    static class ConcreteFieldBinderBuilder
            extends AbstractFieldBinderBuilder<String, Bean, ConcreteFieldBinderBuilder, Object> {

        protected ConcreteFieldBinderBuilder(ISupplierBuilder<Bean, ? extends ISupplier<Bean>> ownerSupplier)
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
    }

    private ConcreteFieldBinderBuilder builderFor(Bean bean) throws DslException {
        ConcreteFieldBinderBuilder b = new ConcreteFieldBinderBuilder(
                FixedSupplierBuilder.of(bean, RuntimeClass.of(Bean.class)));
        b.provide(ReflectionBuilder.builder().withProvider(new RuntimeReflectionProvider()));
        return b;
    }

    // ========================================================================
    // field(String) + withValue(raw)
    // ========================================================================

    @Test
    public void buildByNameThenGetReadsCurrentFieldValue() throws DslException, ReflectionException {
        Bean bean = new Bean();
        ConcreteFieldBinderBuilder b = builderFor(bean);
        b.field("name").withValue("ignored-for-get");

        IFieldBinder<Bean, String> binder = b.build();

        assertEquals("initial", binder.getValue());
    }

    @Test
    public void buildByNameThenSetWritesSuppliedValue() throws DslException, ReflectionException {
        Bean bean = new Bean();
        ConcreteFieldBinderBuilder b = builderFor(bean);
        b.field("name").withValue("written");

        IFieldBinder<Bean, String> binder = b.build();
        binder.setValue();

        assertEquals("written", bean.name);
        assertEquals("written", binder.getValue());
    }

    @Test
    public void buildResolvesPrivateFieldByName() throws DslException, ReflectionException {
        Bean bean = new Bean();
        ConcreteFieldBinderBuilder b = builderFor(bean);
        // count is int; field value type of the builder is String, but resolution is by name only
        b.field("count").withValue("x");

        IFieldBinder<Bean, String> binder = b.build();

        // getValue returns the raw int boxed
        assertEquals(5, ((Object) binder.getValue()));
    }

    // ========================================================================
    // withValue(supplier)
    // ========================================================================

    @Test
    public void buildWithValueSupplierWritesSuppliedValue() throws DslException, ReflectionException {
        Bean bean = new Bean();
        ConcreteFieldBinderBuilder b = builderFor(bean);
        b.field("name").withValue(FixedSupplierBuilder.of("fromSupplier", RuntimeClass.of(String.class)));

        IFieldBinder<Bean, String> binder = b.build();
        binder.setValue();

        assertEquals("fromSupplier", bean.name);
    }

    // ========================================================================
    // field(IField) and field(ObjectAddress)
    // ========================================================================

    @Test
    public void buildByIFieldResolvesSameField() throws DslException, ReflectionException {
        Bean bean = new Bean();
        IField nameField = null;
        for (IField f : RuntimeClass.of(Bean.class).getDeclaredFields()) {
            if ("name".equals(f.getName())) {
                nameField = f;
            }
        }
        assertNotNull(nameField);

        ConcreteFieldBinderBuilder b = builderFor(bean);
        b.field(nameField).withValue("viaField");

        IFieldBinder<Bean, String> binder = b.build();
        binder.setValue();

        assertEquals("viaField", bean.name);
    }

    @Test
    public void buildByObjectAddressResolvesField() throws DslException, ReflectionException {
        Bean bean = new Bean();
        ConcreteFieldBinderBuilder b = builderFor(bean);
        b.field(new ObjectAddress("name", true)).withValue("viaAddress");

        IFieldBinder<Bean, String> binder = b.build();
        binder.setValue();

        assertEquals("viaAddress", bean.name);
    }

    // ========================================================================
    // allowNull
    // ========================================================================

    @Test
    public void allowNullIsFluentAndDoesNotBreakNonNullWrite() throws DslException, ReflectionException {
        Bean bean = new Bean();
        ConcreteFieldBinderBuilder b = builderFor(bean);
        b.field("name").withValue("set");

        assertSame(b, b.allowNull(true));

        IFieldBinder<Bean, String> binder = b.build();
        binder.setValue();
        assertEquals("set", bean.name);
    }

    // ========================================================================
    // Metadata accessors
    // ========================================================================

    @Test
    public void getFieldReferenceMentionsBeanAndField() throws DslException, ReflectionException {
        ConcreteFieldBinderBuilder b = builderFor(new Bean());
        b.field("name").withValue("v");

        IFieldBinder<Bean, String> binder = b.build();
        String ref = binder.getFieldReference();

        assertTrue(ref.contains("Bean"));
        assertTrue(ref.contains("name"));
    }

    @Test
    public void getSuppliedClassReflectsValueType() throws DslException, ReflectionException {
        ConcreteFieldBinderBuilder b = builderFor(new Bean());
        b.field("name").withValue("v");

        IFieldBinder<Bean, String> binder = b.build();

        assertEquals(String.class, binder.getSuppliedClass().getType());
    }

    // ========================================================================
    // Error paths
    // ========================================================================

    @Test
    public void buildUnknownFieldThrowsDslException() throws DslException {
        ConcreteFieldBinderBuilder b = builderFor(new Bean());
        b.field("doesNotExist").withValue("v");

        assertThrows(DslException.class, b::build);
    }

    @Test
    public void buildWithoutAddressFailsBecauseAddressIsUnset() throws DslException {
        ConcreteFieldBinderBuilder b = builderFor(new Bean());
        b.withValue("v"); // no field() call

        // doBuild() guards the unresolved address with Objects.requireNonNull -> NPE
        assertThrows(NullPointerException.class, b::build);
    }

    @Test
    public void buildWithoutValueFailsBecauseSupplierIsUnset() throws DslException {
        ConcreteFieldBinderBuilder b = builderFor(new Bean());
        b.field("name"); // no withValue() call

        // doBuild() guards the unset value supplier with Objects.requireNonNull -> NPE
        assertThrows(NullPointerException.class, b::build);
    }

    @Test
    public void fieldRejectsNullName() throws DslException {
        ConcreteFieldBinderBuilder b = builderFor(new Bean());
        assertThrows(NullPointerException.class, () -> b.field((String) null));
    }

    @Test
    public void withValueRejectsNullRawValue() throws DslException {
        ConcreteFieldBinderBuilder b = builderFor(new Bean());
        assertThrows(NullPointerException.class, () -> b.withValue((Object) null));
    }

    @Test
    public void constructorRejectsNullOwnerSupplier() {
        assertThrows(NullPointerException.class, () -> new ConcreteFieldBinderBuilder(null));
    }
}
