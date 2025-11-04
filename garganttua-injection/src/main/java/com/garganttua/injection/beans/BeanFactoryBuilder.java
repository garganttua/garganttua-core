package com.garganttua.injection.beans;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Qualifier;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.BeanDefinition;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IBeanFactory;
import com.garganttua.core.injection.IInjectableBuilderRegistry;
import com.garganttua.core.injection.context.dsl.IBeanConstructorBinderBuilder;
import com.garganttua.core.injection.context.dsl.IBeanFactoryBuilder;
import com.garganttua.core.injection.context.dsl.IBeanInjectableFieldBuilder;
import com.garganttua.core.injection.context.dsl.IBeanPostConstructMethodBinderBuilder;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;
import com.garganttua.core.supplying.dsl.NullObjectSupplierBuilder;
import com.garganttua.dsl.AbstractAutomaticBuilder;

public class BeanFactoryBuilder<Bean> extends AbstractAutomaticBuilder<IBeanFactoryBuilder<Bean>, IBeanFactory<Bean>>
        implements IBeanFactoryBuilder<Bean> {

    private Class<Bean> beanClass;
    private BeanStrategy strategy;
    private String name;
    private IBeanConstructorBinderBuilder<Bean> constructorBinderBuilder;
    private Set<IBeanPostConstructMethodBinderBuilder<Bean>> postConstructMethodBinderBuilders = new HashSet<>();
    private List<IBeanInjectableFieldBuilder<?, Bean>> injectableFields = new ArrayList<>();

    private Set<Class<? extends Annotation>> qualifiers = new HashSet<>();
    private IInjectableBuilderRegistry registry;

    public BeanFactoryBuilder(Class<Bean> beanClass) {
        this(beanClass, Optional.empty());
    }

    public BeanFactoryBuilder(Class<Bean> beanClass, Optional<IInjectableBuilderRegistry> registry) {
        super();
        this.beanClass = Objects.requireNonNull(beanClass, "Bean class cannot be null");
        Objects.requireNonNull(registry, "Registry cannot be null");
        this.registry = registry.orElse(null);
    }

    public BeanFactoryBuilder(Class<Bean> beanClass, IInjectableBuilderRegistry registry) {
        super();
        this.beanClass = Objects.requireNonNull(beanClass, "Bean class cannot be null");
        this.registry = Objects.requireNonNull(registry, "Registry cannot be null");
    }

    @Override
    protected IBeanFactory<Bean> doBuild() throws DslException {
        BeanFactoryBuilder.removeDuplicatesByHashCode(this.injectableFields);
        BeanDefinition<Bean> definition = new BeanDefinition<Bean>(
                this.beanClass,
                Optional.ofNullable(this.strategy),
                Optional.ofNullable(this.name),
                this.qualifiers,
                this.constructorBinderBuilder != null ? Optional.of(this.constructorBinderBuilder.build())
                        : Optional.empty(),
                this.postConstructMethodBinderBuilders,
                new HashSet<>(this.injectableFields));
        return new BeanFactory<Bean>(definition);
    }

    @Override
    protected void doAutoDetection() throws DslException {
        if (this.registry == null)
            throw new DslException("Cannot do auto detection without registry");
        if (this.constructorBinderBuilder == null) {
            this.lookForConstructor();
        }
        this.lookForMethods();
        this.lookForInjectableFields();
    }

    private void lookForInjectableFields() {
        Arrays.stream(this.beanClass.getDeclaredFields()).forEach(this::registerInjectableField);
    }

    private void registerInjectableField(Field field){
        Optional<IObjectSupplierBuilder<?, IObjectSupplier<?>>> builder = this.registry.createBuilder(field.getType(), field);

        builder.ifPresent(supplierBuilder -> {
            try {
                IBeanInjectableFieldBuilder<?, Bean> injectable = (IBeanInjectableFieldBuilder<?, Bean>) new BeanInjectableFieldBuilder<>(this, this.beanClass, field.getType()).field(field).withValue(supplierBuilder);
                this.injectableFields.add(injectable);
            } catch (DslException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });
    }

    private void lookForMethods() {
        Arrays.stream(this.beanClass.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Inject.class))
                .filter(this::isMethodNotAlreadyBound)
                .forEach(this::registerMethodBinder);
    }

    private boolean isMethodNotAlreadyBound(Method method) {
        return this.postConstructMethodBinderBuilders.stream().noneMatch(builder -> {
            try {
                Method existing = ((BeanPostConstructMethodBinderBuilder<Bean>) builder).findMethod();
                return method.equals(existing);
            } catch (DiException e) {
                return false;
            }
        });
    }

    private void registerMethodBinder(Method method) {
        try {
            IBeanPostConstructMethodBinderBuilder<Bean> methodBinderBuilder = new BeanPostConstructMethodBinderBuilder<Bean>(
                    this, this, Optional.ofNullable(this.registry))
                    .autoDetect(true)
                    .method(method)
                    .withReturn(Void.class);

            for (Parameter parameter : method.getParameters()) {
                try {
                    Class<?> paramType = parameter.getType();
                    methodBinderBuilder.withParam(new NullObjectSupplierBuilder<>(paramType), true);
                } catch (DslException e) {
                    e.printStackTrace();
                }
            }

            this.postConstructMethodBinderBuilders.add(methodBinderBuilder);

        } catch (DslException e) {
            e.printStackTrace();
        }
    }

    private void lookForConstructor() throws DslException {
        Arrays.stream(this.beanClass.getDeclaredConstructors())
                .filter(constructor -> constructor.isAnnotationPresent(Inject.class))
                .findFirst()
                .ifPresent(constructor -> {
                    try {
                        this.constructorBinderBuilder = new BeanConstructorBinderBuilder<Bean>(this, this.beanClass,
                                Optional.ofNullable(this.registry))
                                .autoDetect(true);

                        Arrays.stream(constructor.getParameters())
                                .forEach(parameter -> {
                                    try {
                                        Class<?> paramType = parameter.getType();
                                        this.constructorBinderBuilder
                                                .withParam(new NullObjectSupplierBuilder<>(paramType), true);
                                    } catch (DslException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }
                                });
                    } catch (DslException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                });
    }

    @Override
    public IBeanFactoryBuilder<Bean> strategy(BeanStrategy strategy) {
        this.strategy = Objects.requireNonNull(strategy, "Bean strategy cannot be null");
        return this;
    }

    @Override
    public IBeanConstructorBinderBuilder<Bean> constructor() {
        if (this.constructorBinderBuilder == null) {
            this.constructorBinderBuilder = new BeanConstructorBinderBuilder<Bean>(this, this.beanClass,
                    Optional.ofNullable(this.registry));
        }
        return this.constructorBinderBuilder;
    }

    @Override
    public IBeanFactoryBuilder<Bean> name(String name) {
        this.name = Objects.requireNonNull(name, "Bean name cannot be null");
        return this;
    }

    @Override
    public IBeanFactoryBuilder<Bean> qualifier(Class<? extends Annotation> qualifier) throws DslException {
        if (qualifier.getAnnotation(Qualifier.class) == null) {
            throw new DslException("Provided qualifier " + qualifier.getName() + " is not annotated with @Qualifier");
        }
        this.qualifiers.add(qualifier);
        return this;
    }

    @Override
    public IBeanPostConstructMethodBinderBuilder<Bean> postConstruction() throws DslException {
        IBeanPostConstructMethodBinderBuilder<Bean> builder = new BeanPostConstructMethodBinderBuilder<Bean>(this,
                this, Optional.ofNullable(this.registry));
        this.postConstructMethodBinderBuilders.add(builder);
        return builder;
    }

    @Override
    public Class<Bean> getObjectClass() {
        return this.beanClass;
    }

    @Override
    public IBeanFactoryBuilder<Bean> qualifiers(Set<Class<? extends Annotation>> qualifiers) throws DslException {
        Set<Class<? extends Annotation>> verifiedQualifiers = qualifiers.stream()
                .filter(qualifierAnnotation -> qualifierAnnotation.getAnnotation(Qualifier.class) != null)
                .collect(Collectors.toSet());
        this.qualifiers.addAll(verifiedQualifiers);
        return this;
    }

    public static <T> void removeDuplicatesByHashCode(List<T> list) {
        Set<Integer> seen = new HashSet<>();
        list.removeIf(item -> !seen.add(item != null ? item.hashCode() : 0));
    }

    @Override
    public <FieldType> IBeanInjectableFieldBuilder<FieldType, Bean> field(Class<FieldType> fieldType) throws DslException {
        Objects.requireNonNull(fieldType, "Field type cannot be null");
        IBeanInjectableFieldBuilder<FieldType, Bean> injectable = new BeanInjectableFieldBuilder<>(this, this.beanClass, fieldType);
        this.injectableFields.add(injectable);
        return injectable;
    }

    @Override
    public Set<Class<?>> getDependencies() {
        Set<Class<?>> dependencies = new HashSet<>();
        this.injectableFields.stream().forEach(f -> {
            dependencies.addAll(f.getDependencies());
        });
        Optional.ofNullable(this.constructorBinderBuilder).ifPresent(c -> {
            dependencies.addAll(c.getDependencies());
        });
        this.postConstructMethodBinderBuilders.stream().forEach(m -> {
            dependencies.addAll(m.getDependencies());
        });
        return dependencies;
    }

}
