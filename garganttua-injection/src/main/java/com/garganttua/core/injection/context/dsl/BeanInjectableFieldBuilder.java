package com.garganttua.core.injection.context.dsl;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.binders.dsl.AbstractFieldBinderBuilder;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Builder for a single injectable field of a bean, wiring the field's value supplier
 * to the owning bean instance.
 *
 * @param <FieldType> the declared type of the field being injected
 * @param <BeanType>  the type of the bean that owns the field
 */
@Reflected
public class BeanInjectableFieldBuilder<FieldType, BeanType>
                extends
                AbstractFieldBinderBuilder<FieldType, BeanType, IBeanInjectableFieldBuilder<FieldType, BeanType>, IBeanFactoryBuilder<BeanType>>
                implements IBeanInjectableFieldBuilder<FieldType, BeanType> {
    private static final Logger log = Logger.getLogger(BeanInjectableFieldBuilder.class);

        /**
         * Creates a builder for an injectable field of the given type on the owning bean.
         *
         * @param link               the parent bean factory builder
         * @param beanSupplierBuilder the bean factory builder supplying the owner instance
         * @param fieldType           the declared type of the injectable field
         * @throws DslException if the underlying field binder cannot be initialised
         */
        public BeanInjectableFieldBuilder(IBeanFactoryBuilder<BeanType> link,
                        IBeanFactoryBuilder<BeanType> beanSupplierBuilder, IClass<FieldType> fieldType)
                        throws DslException {
                super(link, beanSupplierBuilder, fieldType, Collections.emptySet());
                log.trace(
                                "Entering BeanInjectableFieldBuilder constructor with link: {}, beanSupplierBuilder: {}, fieldType: {}",
                                link, beanSupplierBuilder, fieldType);
                log.debug("BeanInjectableFieldBuilder initialized for fieldType: {} in beanClass: {}", fieldType,
                                link.getSuppliedClass());
                log.trace("Exiting BeanInjectableFieldBuilder constructor");
        }

        @Override
        protected void doPreBuildWithDependency_(Object dependency) {
                // No additional pre-build handling needed
        }

        @Override
        protected void doAutoDetectionWithDependency(Object dependency) throws DslException {
                // No auto-detection with dependency needed
        }

        @Override
        protected void doPostBuildWithDependency(Object dependency) {
                // No post-build handling needed
        }

        /** {@return the single dependency of this field, namely its declared field type} */
        @Override
        public Set<IClass<?>> dependencies() {
                log.trace("Entering getDependencies() for injectable field of type: {}", this.fieldType);
                Set<IClass<?>> dependencies = Set.of(this.fieldType);
                log.debug("Dependencies for injectable field: {}", dependencies);
                log.trace("Exiting getDependencies()");
                return dependencies;
        }

        /**
         * Sets the supplier builder that provides the owning bean instance into which the field is injected.
         *
         * @param ownerSupplierBuilder the owner supplier builder; must not be {@code null}
         * @return this builder for chaining
         */
        @Override
        public IBeanInjectableFieldBuilder<FieldType, BeanType> ownerSupplierBuilder(
                        ISupplierBuilder<BeanType, ? extends ISupplier<BeanType>> ownerSupplierBuilder) {
                log.trace("Entering ownerSupplierBuilder() with ownerSupplierBuilder: {}", ownerSupplierBuilder);
                this.ownerSupplierBuilder = Objects.requireNonNull(ownerSupplierBuilder, "ownerSupplierBuilder cannot be null");
                log.debug("Set ownerSupplierBuilder for fieldType: {} to supplier of type: {}", this.fieldType,
                                ownerSupplierBuilder.getSuppliedClass());
                log.debug("ownerSupplierBuilder set for fieldType: {} in beanClass: {}", this.fieldType,
                                ownerSupplierBuilder.getSuppliedClass());
                log.trace("Exiting ownerSupplierBuilder()");
                return this;
        }

        @Override
        protected void doAutoDetection() throws DslException {
                // Field address is resolved lazily in doBuild() after IReflection is available.
                // Nullable flag is already set during field registration in BeanFactoryBuilder.registerInjectableField().
                log.trace("doAutoDetection() for fieldType: {} - no-op (nullable already set)", this.fieldType);
        }

        @Override
        public Set<IClass<? extends IObservableBuilder<?, ?>>> use() {
                return Collections.emptySet();
        }

        @Override
        public Set<IClass<? extends IObservableBuilder<?, ?>>> require() {
                return Collections.emptySet();
        }

        /**
         * Provides a dependency builder to this field builder and returns it for chaining.
         *
         * @param dependency the dependency builder to register
         * @return this builder
         * @throws DslException if the dependency cannot be accepted
         */
        @SuppressWarnings("unchecked")
        @Override
        public IBeanInjectableFieldBuilder<FieldType, BeanType> provide(IObservableBuilder<?, ?> dependency) throws DslException {
                super.provide(dependency);
                return this;
        }

        /** {@return the resolved reflective {@link IField} for this injectable field} */
        @Override
        public IField field() {
                log.trace("Entering field() for fieldType: {}", this.fieldType);
                IField iField = this.findField();
                log.debug("Retrieved field: {}", iField != null ? iField.getName() : "null");
                log.trace("Exiting field()");
                return iField;
        }
}
