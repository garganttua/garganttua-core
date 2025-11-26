package com.garganttua.core.injection.context.dsl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
import com.garganttua.core.supply.dsl.NullObjectSupplierBuilder;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
        log.atTrace().log("Entered BeanFactoryBuilder constructor with beanClass: {}", beanClass);
        log.atTrace().log("Exiting BeanFactoryBuilder constructor");
    }

    public BeanFactoryBuilder(Class<Bean> beanClass, Optional<IInjectableElementResolver> resolver) {
        super();
        log.atTrace().log("Entered BeanFactoryBuilder constructor with optional resolver: {}", resolver);
        this.beanClass = Objects.requireNonNull(beanClass, "Bean class cannot be null");
        Objects.requireNonNull(resolver, "Registry cannot be null");
        this.resolver = resolver.orElse(null);
        log.atInfo().log("BeanFactoryBuilder initialized for beanClass: {} with resolver: {}", beanClass,
                this.resolver);
        log.atTrace().log("Exiting BeanFactoryBuilder constructor");
    }

    public BeanFactoryBuilder(Class<Bean> beanClass, IInjectableElementResolver resolver) {
        super();
        log.atTrace().log("Entered BeanFactoryBuilder constructor with resolver: {}", resolver);
        this.beanClass = Objects.requireNonNull(beanClass, "Bean class cannot be null");
        this.resolver = Objects.requireNonNull(resolver, "Registry cannot be null");
        log.atInfo().log("BeanFactoryBuilder initialized for beanClass: {} with resolver: {}", beanClass, resolver);
        log.atTrace().log("Exiting BeanFactoryBuilder constructor");
    }

    @Override
    protected IBeanFactory<Bean> doBuild() throws DslException {
        log.atTrace().log("Entering doBuild for beanClass: {}", this.beanClass);
        BeanFactoryBuilder.removeDuplicatesByHashCode(this.injectableFields);
        BeanDefinition<Bean> definition = new BeanDefinition<>(
                this.beanClass,
                Optional.ofNullable(this.strategy),
                Optional.ofNullable(this.name),
                this.qualifiers,
                this.constructorBinderBuilder != null ? Optional.of(this.constructorBinderBuilder.build())
                        : Optional.empty(),
                this.postConstructMethodBinderBuilders,
                new HashSet<>(this.injectableFields));
        log.atInfo().log("Building BeanFactory for beanClass: {} with definition: {}", this.beanClass, definition);
        BeanFactory<Bean> factory = new BeanFactory<>(definition);
        log.atTrace().log("Exiting doBuild");
        return factory;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        log.atTrace().log("Entering doAutoDetection for beanClass: {}", this.beanClass);
        if (this.resolver == null) {
            log.atError().log("Cannot do auto detection without registry");
            throw new DslException("Cannot do auto detection without registry");
        }
        if (this.constructorBinderBuilder == null) {
            this.lookForConstructor();
        }
        this.lookForPostConstructMethods();
        this.lookForInjectableFields();
        log.atTrace().log("Exiting doAutoDetection");
    }

    private void lookForInjectableFields() {
        log.atTrace().log("Looking for injectable fields in beanClass: {}", this.beanClass);
        Arrays.stream(this.beanClass.getDeclaredFields()).forEach(this::registerInjectableField);
        log.atTrace().log("Completed looking for injectable fields");
    }

    private void registerInjectableField(Field field) {
        try {
            log.atTrace().log("Registering injectable field: {}", field.getName());
            this.resolver.resolve(field.getType(), field).ifResolved((b, n) -> {
                IBeanInjectableFieldBuilder<?, Bean> injectable = new BeanInjectableFieldBuilder<>(this, this,
                        field.getType()).field(field).withValue(b).allowNull(n).autoDetect(true);
                this.injectableFields.add(injectable);
                log.atInfo().log("Registered injectable field: {} with builder: {}", field.getName(), b);
            });
        } catch (DiException e) {
            log.atWarn().log("Failed to register injectable field: {} due to {}", field.getName(), e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void lookForPostConstructMethods() {
        log.atTrace().log("Looking for post construct methods in beanClass: {}", this.beanClass);
        Arrays.stream(this.beanClass.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Inject.class))
                .filter(this::isPostConstructMethodNotAlreadyBound)
                .forEach(this::registerPostConstructMethodBinder);
        Arrays.stream(this.beanClass.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(PostConstruct.class))
                .filter(this::isPostConstructMethodNotAlreadyBound)
                .forEach(this::registerPostConstructMethodBinder);
        log.atTrace().log("Completed looking for post construct methods");
    }

    private boolean isPostConstructMethodNotAlreadyBound(Method method) {
        return this.postConstructMethodBinderBuilders.stream().noneMatch(builder -> {
            Method existing = ((BeanPostConstructMethodBinderBuilder<Bean>) builder).findMethod();
            return method.equals(existing);
        });
    }

    private void registerPostConstructMethodBinder(Method method) {
        try {
            log.atTrace().log("Registering post construct method: {}", method.getName());
            IBeanPostConstructMethodBinderBuilder<Bean> methodBinderBuilder = new BeanPostConstructMethodBinderBuilder<>(
                    this, this, Optional.ofNullable(this.resolver))
                    .autoDetect(true)
                    .method(method)
                    .withReturn(Void.class);

            Arrays.stream(method.getParameters()).forEach(parameter -> {
                try {
                    Class<?> paramType = parameter.getType();
                    methodBinderBuilder.withParam(new NullObjectSupplierBuilder<>(paramType));
                    log.atDebug().log("Added parameter {} to post construct method {}", paramType, method.getName());
                } catch (DslException e) {
                    log.atWarn().log("Failed to add parameter to post construct method {}: {}", method.getName(),
                            e.getMessage());
                }
            });

            this.postConstructMethodBinderBuilders.add(methodBinderBuilder);
            log.atInfo().log("Registered post construct method: {}", method.getName());
        } catch (DslException e) {
            log.atWarn().log("Failed to register post construct method {}: {}", method.getName(), e.getMessage());
        }
    }

    private void lookForConstructor() {
        log.atTrace().log("Looking for @Inject constructor in beanClass: {}", this.beanClass);
        Arrays.stream(this.beanClass.getDeclaredConstructors())
                .filter(constructor -> constructor.isAnnotationPresent(Inject.class))
                .findFirst()
                .ifPresent(constructor -> {
                    log.atInfo().log("Found @Inject constructor: {}", constructor);
                    this.constructorBinderBuilder = new BeanConstructorBinderBuilder<>(this, this.beanClass,
                            Optional.ofNullable(this.resolver))
                            .autoDetect(true);

                    Arrays.stream(constructor.getParameters())
                            .forEach(parameter -> {
                                Class<?> paramType = parameter.getType();
                                try {
                                    this.constructorBinderBuilder
                                            .withParam(new NullObjectSupplierBuilder<>(paramType), true);
                                    log.atDebug().log("Added constructor parameter: {}", paramType);
                                } catch (DslException e) {
                                    log.atWarn().log("Failed to add constructor parameter {}: {}", paramType,
                                            e.getMessage());
                                }
                            });
                });
        log.atTrace().log("Completed looking for @Inject constructor");
    }

    @Override
    public IBeanFactoryBuilder<Bean> strategy(BeanStrategy strategy) {
        this.strategy = Objects.requireNonNull(strategy, "Bean strategy cannot be null");
        log.atInfo().log("BeanFactoryBuilder strategy set to {}", strategy);
        return this;
    }

    @Override
    public IBeanConstructorBinderBuilder<Bean> constructor() {
        if (this.constructorBinderBuilder == null) {
            this.constructorBinderBuilder = new BeanConstructorBinderBuilder<>(this, this.beanClass,
                    Optional.ofNullable(this.resolver));
            log.atInfo().log("Initialized constructorBinderBuilder for beanClass: {}", this.beanClass);
        }
        return this.constructorBinderBuilder;
    }

    @Override
    public IBeanFactoryBuilder<Bean> name(String name) {
        this.name = Objects.requireNonNull(name, "Bean name cannot be null");
        log.atInfo().log("BeanFactoryBuilder name set to {}", name);
        return this;
    }

    @Override
    public IBeanFactoryBuilder<Bean> qualifier(Class<? extends Annotation> qualifier) throws DslException {
        if (qualifier.getAnnotation(Qualifier.class) == null) {
            log.atError().log("Provided qualifier {} is not annotated with @Qualifier", qualifier.getName());
            throw new DslException("Provided qualifier " + qualifier.getName() + " is not annotated with @Qualifier");
        }
        this.qualifiers.add(qualifier);
        log.atInfo().log("Added qualifier {}", qualifier.getName());
        return this;
    }

    @Override
    public IBeanPostConstructMethodBinderBuilder<Bean> postConstruction() throws DslException {
        IBeanPostConstructMethodBinderBuilder<Bean> builder = new BeanPostConstructMethodBinderBuilder<>(this,
                this, Optional.ofNullable(this.resolver));
        this.postConstructMethodBinderBuilders.add(builder);
        log.atInfo().log("Added post construct method builder: {}", builder);
        return builder;
    }

    @Override
    public Class<Bean> getSuppliedType() {
        return this.beanClass;
    }

    @Override
    public IBeanFactoryBuilder<Bean> qualifiers(Set<Class<? extends Annotation>> qualifiers) throws DslException {
        Set<Class<? extends Annotation>> verifiedQualifiers = qualifiers.stream()
                .filter(q -> q.getAnnotation(Qualifier.class) != null)
                .collect(Collectors.toSet());
        this.qualifiers.addAll(verifiedQualifiers);
        log.atInfo().log("Added multiple qualifiers: {}", verifiedQualifiers);
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
        log.atInfo().log("Added injectable field of type: {}", fieldType);
        return injectable;
    }

    @Override
    public Set<Class<?>> getDependencies() {
        log.atTrace().log("Calculating dependencies for beanClass: {}", this.beanClass);
        Set<Class<?>> dependencies = new HashSet<>();
        this.injectableFields.forEach(f -> dependencies.addAll(f.getDependencies()));
        Optional.ofNullable(this.constructorBinderBuilder).ifPresent(c -> dependencies.addAll(c.getDependencies()));
        this.postConstructMethodBinderBuilders.forEach(m -> dependencies.addAll(m.getDependencies()));
        log.atInfo().log("Dependencies for beanClass {}: {}", this.beanClass, dependencies);
        return dependencies;
    }

    @Override
    public boolean isContextual() {
        return false;
    }
}