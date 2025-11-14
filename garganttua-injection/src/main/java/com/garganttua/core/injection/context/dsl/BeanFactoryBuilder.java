package com.garganttua.core.injection.context.dsl;

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

import com.garganttua.core.dsl.AbstractAutomaticBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.BeanDefinition;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IBeanFactory;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.injection.context.beans.BeanFactory;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;
import com.garganttua.core.supplying.dsl.NullObjectSupplierBuilder;

import jakarta.annotation.PostConstruct;

public class BeanFactoryBuilder<Bean> extends AbstractAutomaticBuilder<IBeanFactoryBuilder<Bean>, IBeanFactory<Bean>>
        implements IBeanFactoryBuilder<Bean> {

    private Class<Bean> beanClass;
    private BeanStrategy strategy;
    private String name;
    private IBeanConstructorBinderBuilder<Bean> constructorBinderBuilder;
    private Set<IBeanPostConstructMethodBinderBuilder<Bean>> postConstructMethodBinderBuilders = new HashSet<>();
    private List<IBeanInjectableFieldBuilder<?, Bean>> injectableFields = new ArrayList<>();

    private Set<Class<? extends Annotation>> qualifiers = new HashSet<>();
    private IInjectableElementResolver resolver;

    public BeanFactoryBuilder(Class<Bean> beanClass) {
        this(beanClass, Optional.empty());
    }

    public BeanFactoryBuilder(Class<Bean> beanClass, Optional<IInjectableElementResolver> resolver) {
        super();
        this.beanClass = Objects.requireNonNull(beanClass, "Bean class cannot be null");
        Objects.requireNonNull(resolver, "Registry cannot be null");
        this.resolver = resolver.orElse(null);
    }

    public BeanFactoryBuilder(Class<Bean> beanClass, IInjectableElementResolver resolver) {
        super();
        this.beanClass = Objects.requireNonNull(beanClass, "Bean class cannot be null");
        this.resolver = Objects.requireNonNull(resolver, "Registry cannot be null");
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
        if (this.resolver == null)
            throw new DslException("Cannot do auto detection without registry");
        if (this.constructorBinderBuilder == null) {
            this.lookForConstructor();
        }
        this.lookForPostConstructMethods();
        this.lookForInjectableFields();
    }

    private void lookForInjectableFields() {
        Arrays.stream(this.beanClass.getDeclaredFields()).forEach(t -> registerInjectableField(t));
    }

    private void registerInjectableField(Field field) throws DslException {
        Optional<IObjectSupplierBuilder<?, IObjectSupplier<?>>> builder;
        try {
            builder = this.resolver.resolve(field.getType(), field);
            builder.ifPresent(supplierBuilder -> {
                IBeanInjectableFieldBuilder<?, Bean> injectable = new BeanInjectableFieldBuilder<>(this, this,
                        field.getType()).field(field).withValue(supplierBuilder).autoDetect(true);
                this.injectableFields.add(injectable);
            });
        } catch (DiException e) {
            throw new DslException(e);
        }

    }

    private void lookForPostConstructMethods() {
        Arrays.stream(this.beanClass.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Inject.class))
                .filter(this::isPostConstructMethodNotAlreadyBound)
                .forEach(this::registerPostConstructMethodBinder);
        Arrays.stream(this.beanClass.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(PostConstruct.class))
                .filter(this::isPostConstructMethodNotAlreadyBound)
                .forEach(this::registerPostConstructMethodBinder);
    }

    private boolean isPostConstructMethodNotAlreadyBound(Method method) {
        return this.postConstructMethodBinderBuilders.stream().noneMatch(builder -> {
            Method existing = ((BeanPostConstructMethodBinderBuilder<Bean>) builder).findMethod();
            return method.equals(existing);
        });
    }

    private void registerPostConstructMethodBinder(Method method) {
        try {
            IBeanPostConstructMethodBinderBuilder<Bean> methodBinderBuilder = new BeanPostConstructMethodBinderBuilder<Bean>(
                    this, this, Optional.ofNullable(this.resolver))
                    .autoDetect(true)
                    .method(method)
                    .withReturn(Void.class);

            for (Parameter parameter : method.getParameters()) {
                try {
                    Class<?> paramType = parameter.getType();
                    methodBinderBuilder.withParam(new NullObjectSupplierBuilder<>(paramType));
                } catch (DslException e) {
                    e.printStackTrace();
                }
            }

            this.postConstructMethodBinderBuilders.add(methodBinderBuilder);

        } catch (DslException e) {
            e.printStackTrace();
        }
    }

    private void lookForConstructor() {
        Arrays.stream(this.beanClass.getDeclaredConstructors())
                .filter(constructor -> constructor.isAnnotationPresent(Inject.class))
                .findFirst()
                .ifPresent(constructor -> {
                    this.constructorBinderBuilder = new BeanConstructorBinderBuilder<Bean>(this, this.beanClass,
                            Optional.ofNullable(this.resolver))
                            .autoDetect(true);

                    Arrays.stream(constructor.getParameters())
                            .forEach(parameter -> {
                                Class<?> paramType = parameter.getType();
                                this.constructorBinderBuilder
                                        .withParam(new NullObjectSupplierBuilder<>(paramType), true);
                            });
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
                    Optional.ofNullable(this.resolver));
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
                this, Optional.ofNullable(this.resolver));
        this.postConstructMethodBinderBuilders.add(builder);
        return builder;
    }

    @Override
    public Class<Bean> getSuppliedType() {
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
    public <FieldType> IBeanInjectableFieldBuilder<FieldType, Bean> field(Class<FieldType> fieldType)
            throws DslException {
        Objects.requireNonNull(fieldType, "Field type cannot be null");
        IBeanInjectableFieldBuilder<FieldType, Bean> injectable = new BeanInjectableFieldBuilder<>(this, this,
                fieldType);
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

    @Override
    public boolean isContextual() {
        return false;
    }

}
