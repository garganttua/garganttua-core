package com.garganttua.core.injection.context.dsl;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.binders.dsl.AbstractFieldBinderBuilder;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;
import com.garganttua.core.reflection.annotations.Reflected;

@Reflected
public class BeanInjectableFieldBuilder<FieldType, BeanType>
                extends
                AbstractFieldBinderBuilder<FieldType, BeanType, IBeanInjectableFieldBuilder<FieldType, BeanType>, IBeanFactoryBuilder<BeanType>>
                implements IBeanInjectableFieldBuilder<FieldType, BeanType> {
    private static final IDiagnostic log = Diagnostics.of(BeanInjectableFieldBuilder.class);

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

        @Override
        public Set<IClass<?>> dependencies() {
                log.trace("Entering getDependencies() for injectable field of type: {}", this.fieldType);
                Set<IClass<?>> dependencies = Set.of(this.fieldType);
                log.debug("Dependencies for injectable field: {}", dependencies);
                log.trace("Exiting getDependencies()");
                return dependencies;
        }

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

        @SuppressWarnings("unchecked")
        @Override
        public IBeanInjectableFieldBuilder<FieldType, BeanType> provide(IObservableBuilder<?, ?> dependency) throws DslException {
                super.provide(dependency);
                return this;
        }

        @Override
        public IField field() {
                log.trace("Entering field() for fieldType: {}", this.fieldType);
                IField iField = this.findField();
                log.debug("Retrieved field: {}", iField != null ? iField.getName() : "null");
                log.trace("Exiting field()");
                return iField;
        }
}
