package com.garganttua.injection.beans;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Qualifier;

import com.garganttua.dsl.AbstractAutomaticBuilder;
import com.garganttua.dsl.DslException;
import com.garganttua.injection.supplier.builder.supplier.NullObjectSupplierBuilder;

public class BeanFactoryBuilder<Bean> extends AbstractAutomaticBuilder<IBeanFactoryBuilder<Bean>, IBeanFactory<Bean>>
        implements IBeanFactoryBuilder<Bean> {

    private Class<Bean> beanClass;
    private BeanStrategy strategy;
    private String name;
    private IBeanConstructorBinderBuilder<Bean> constructorBinderBuilder;
    private Set<IBeanPostConstructMethodBinderBuilder<Bean>> postConstructMethodBinderBuilders = new HashSet<>();

    private Set<Class<? extends Annotation>> qualifiers = new HashSet<>();

    public BeanFactoryBuilder(Class<Bean> beanClass) {
        super();
        this.beanClass = Objects.requireNonNull(beanClass, "Bean class cannot be null");
    }

    @Override
    protected IBeanFactory<Bean> doBuild() throws DslException {
        BeanDefinition<Bean> definition = new BeanDefinition<Bean>(
                this.beanClass,
                Optional.ofNullable(this.strategy),
                Optional.ofNullable(this.name),
                this.qualifiers,
                this.constructorBinderBuilder != null ? Optional.of(this.constructorBinderBuilder.build())
                        : Optional.empty(),
                this.postConstructMethodBinderBuilders);
        return new BeanFactory<Bean>(definition);
    }

    @Override
    protected void doAutoDetection() throws DslException {
        if (this.constructorBinderBuilder == null) {
            this.lookForConstructor();
        }

    }

    private void lookForConstructor() throws DslException {
        Arrays.stream(this.beanClass.getDeclaredConstructors())
                .filter(constructor -> constructor.isAnnotationPresent(Inject.class))
                .findFirst()
                .ifPresent(constructor -> {
                    try {
                        this.constructorBinderBuilder = new BeanConstructorBinderBuilder<Bean>(this, this.beanClass)
                                .autoDetect(true);

                        Arrays.stream(constructor.getParameters())
                                .forEach(parameter -> {
                                    try {
                                        String paramName = parameter.getName();
                                        Class<?> paramType = parameter.getType();
                                        this.constructorBinderBuilder.withParam(new NullObjectSupplierBuilder<>(paramType), true);
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
            this.constructorBinderBuilder = new BeanConstructorBinderBuilder<Bean>(this, this.beanClass);
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
                this);
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

}
