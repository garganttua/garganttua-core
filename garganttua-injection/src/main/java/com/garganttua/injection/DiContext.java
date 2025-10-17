package com.garganttua.injection;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Qualifier;

import com.garganttua.injection.spec.IBeanScope;
import com.garganttua.injection.spec.IDiChildContextFactory;
import com.garganttua.injection.spec.IDiContext;
import com.garganttua.injection.spec.ILifecycle;
import com.garganttua.injection.spec.IPropertyScope;
import com.garganttua.reflection.GGReflectionException;
import com.garganttua.reflection.utils.GGFieldAccessManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DiContext implements IDiContext {

    // --- Singleton public ---
    public static IDiContext context;

    private final List<IBeanScope> beanScopes;
    private final List<IPropertyScope> propertyScopes;
    private final List<IDiChildContextFactory<? extends IDiContext>> childContextFactories;

    // --- États internes du cycle de vie ---
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean started = new AtomicBoolean(false);

    // --- Constructeur ---
    public DiContext(List<IBeanScope> beanScopes, List<IPropertyScope> propertyScopes,
            List<IDiChildContextFactory<? extends IDiContext>> childContextFactories) {
        this.beanScopes = Collections.unmodifiableList(Objects.requireNonNull(beanScopes));
        this.propertyScopes = Collections.unmodifiableList(Objects.requireNonNull(propertyScopes));
        this.childContextFactories = Collections.unmodifiableList(Objects.requireNonNull(childContextFactories));
        context = this;
    }

    // --- Vérifications d'état ---
    private void ensureInitializedAndStarted() throws DiException {
        if (!initialized.get()) {
            throw new DiException("Context not initialized");
        }
        if (!started.get()) {
            throw new DiException("Context not started");
        }
    }

    // --- Getters ---
    @Override
    public Set<IBeanScope> getBeanScopes() throws DiException {
        ensureInitializedAndStarted();
        return Collections.unmodifiableSet(new HashSet<>(beanScopes));
    }

    @Override
    public Set<IPropertyScope> getPropertyScopes() throws DiException {
        ensureInitializedAndStarted();
        return Collections.unmodifiableSet(new HashSet<>(propertyScopes));
    }

    @Override
    public Set<IDiChildContextFactory<? extends IDiContext>> getChildContextFactories() throws DiException {
        ensureInitializedAndStarted();
        return Collections.unmodifiableSet(new HashSet<IDiChildContextFactory<? extends IDiContext>>(childContextFactories));
    }

    // --- Gestion des beans ---
    @Override
    public <T> Optional<T> getBean(Class<T> type) throws DiException {
        ensureInitializedAndStarted();
        return beanScopes.stream()
                .map(scope -> scope.getBean(type))
                .flatMap(Optional::stream)
                .findFirst();
    }

    @Override
    public <T> Optional<T> getBean(String name, Class<T> type) throws DiException {
        ensureInitializedAndStarted();
        return beanScopes.stream()
                .map(scope -> scope.getBean(name, type))
                .flatMap(Optional::stream)
                .findFirst();
    }

    @Override
    public <T> Optional<T> getBeanFromScope(String scopeName, Class<T> type) throws DiException {
        ensureInitializedAndStarted();
        return beanScopes.stream()
                .filter(scope -> scope.getName().equals(scopeName))
                .findFirst()
                .flatMap(scope -> scope.getBean(type));
    }

    @Override
    public <T> Optional<T> getBeanFromScope(String scopeName, String name, Class<T> type) throws DiException {
        ensureInitializedAndStarted();
        return beanScopes.stream()
                .filter(scope -> scope.getName().equals(scopeName))
                .findFirst()
                .flatMap(scope -> scope.getBean(name, type));
    }

    @Override
    public <T> List<T> getBeansImplementingInterface(String scopeName, Class<T> interfasse) throws DiException {
        ensureInitializedAndStarted();
        return beanScopes.stream()
                .filter(scope -> scope.getName().equals(scopeName))
                .findFirst()
                .map(scope -> scope.getBeansImplementingInterface(interfasse))
                .orElse(Collections.emptyList());
    }

    @Override
    public void setBeanInScope(String scopeName, String name, Object bean) throws DiException {
        ensureInitializedAndStarted();
        beanScopes.stream()
                .filter(scope -> scope.getName().equals(scopeName))
                .findFirst()
                .filter(IBeanScope::isMutable)
                .orElseThrow(() -> new DiException("Scope " + scopeName + " not found or immutable"))
                .registerBean(name, bean);
    }

    @Override
    public void setBeanInScope(String scopeName, Object bean) throws DiException {
        ensureInitializedAndStarted();
        setBeanInScope(scopeName, bean.getClass().getName(), bean);
    }

    // --- Gestion des propriétés ---
    @Override
    public <T> Optional<T> getProperty(String key, Class<T> type) throws DiException {
        ensureInitializedAndStarted();
        return propertyScopes.stream()
                .map(scope -> scope.getProperty(key, type))
                .flatMap(Optional::stream)
                .findFirst();
    }

    @Override
    public <T> Optional<T> getPropertyFromScope(String scopeName, String key, Class<T> type) throws DiException {
        ensureInitializedAndStarted();
        return propertyScopes.stream()
                .filter(scope -> scope.getName().equals(scopeName))
                .findFirst()
                .flatMap(scope -> scope.getProperty(key, type));
    }

    @Override
    public void setPropertyInScope(String scopeName, String key, Object value) throws DiException {
        ensureInitializedAndStarted();
        propertyScopes.stream()
                .filter(scope -> scope.getName().equals(scopeName))
                .findFirst()
                .filter(IPropertyScope::isMutable)
                .orElseThrow(() -> new DiException("PropertyScope " + scopeName + " not found or immutable"))
                .setProperty(key, value);
    }

    @Override
    public void doInjection(Object instance) throws DiException {
        ensureInitializedAndStarted();
        // future: injection logic
    }

    public void injectBeans(Object entity) throws DiException {
        Class<?> clazz = entity.getClass();
        this.injectBeans(entity, clazz);
    }

    private void injectBeans(Object entity, Class<?> clazz) throws DiException {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Inject.class)) {
                Object bean = this.getBean(entity, field);
                this.doInjection(entity, field, bean);
            }
        }

        if (clazz.getSuperclass() != null) {
            this.injectBeans(entity, clazz.getSuperclass());
        }
    }

    private void doInjection(Object entity, Field field, Object bean) throws DiException {
        try (GGFieldAccessManager accessManager = new GGFieldAccessManager(field)) {
            field.set(entity, bean);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            if (log.isDebugEnabled()) {
                log.warn("Field  " + field.getName() + " of entity of type " + entity.getClass().getName()
                        + " cannot be set", e);
            }
            throw new DiException("Field  " + field.getName() + " of entity of type "
                    + entity.getClass().getName() + " cannot be set", e);
        }
    }

    private Object getBean(Object entity, Field field) throws DiException {
        Object bean = null;

        if (Optional.class.isAssignableFrom(field.getType())) {
            Class<?> optionalClass = this.getOptionalFieldType(field.getGenericType());
            try {
                Object optionalBean = this.researchBean(field, optionalClass);
                return Optional.ofNullable(optionalBean);
            } catch (Exception e) {
                return Optional.empty();
            }

        } else {
            bean = this.researchBean(field, field.getType());
        }

        if (bean == null) {
            throw new DiException("Bean not found for field: " + field.getName());
        }

        return bean;
    }

    private Class<?> getOptionalFieldType(Type type) throws DiException {
        Type genericType = type;
        if (genericType instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) genericType;

            Type[] typeArguments = paramType.getActualTypeArguments();
            return (Class<?>) typeArguments[0];
        }

        throw new DiException("Invalid clazz " + type.getTypeName() + " should be Optional<?>");
    }

    private Object researchBean(Field field, Class<?> fieldType) throws DiException {
        Object bean = null;
        /* if (field.isAnnotationPresent(Qualifier.class)) {
            String qualifierName = field.getAnnotation(Qualifier.class).toString();
            bean = this.getBeanNamed(qualifierName);
        } else {
            bean = this.getBeanOfType(fieldType);
        } */
        /* if (bean == null) {
            throw new DiException("Bean not found for field: " + field.getName());
        } */
        return bean;
    }

    public void injectProperties(Object entity) throws DiException {
        // TODO Auto-generated method stub

    }

    @Override
    public <ChildContext extends IDiContext> ChildContext newChildContext(Class<ChildContext> contextClass,
            Object... args)
            throws DiException {
        ensureInitializedAndStarted();
        return childContextFactories.stream()
                .filter(factory -> {
                    Class<? extends IDiContext> childType = getChildContextType(factory);
                    return childType != null && contextClass.isAssignableFrom(childType);
                })
                .findFirst()
                .map(factory -> {
                    try {
                        return contextClass.cast(factory.createChildContext(this, args));
                    } catch (DiException e) {
                        throw new RuntimeException(e);
                    }
                })
                .orElseThrow(() -> new DiException(
                        "No child context factory registered for context class " + contextClass.getName()));
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends IDiContext> getChildContextType(
            IDiChildContextFactory<? extends IDiContext> factory) {
        Type[] genericInterfaces = factory.getClass().getGenericInterfaces();
        for (Type type : genericInterfaces) {
            if (type instanceof ParameterizedType pt
                    && pt.getRawType() instanceof Class<?> raw
                    && IDiChildContextFactory.class.isAssignableFrom(raw)) {
                Type actual = pt.getActualTypeArguments()[0];
                if (actual instanceof Class<?> clazz) {
                    return (Class<? extends IDiContext>) clazz;
                }
            }
        }
        return null;
    }

    // --- Cycle de vie ---
    @Override
    public void onInit() throws DiException {
        for (IBeanScope beanScope : beanScopes) {
            if (beanScope instanceof ILifecycle lifecycle) {
                lifecycle.onInit();
            }
        }
        for (IPropertyScope propertyScope : propertyScopes) {
            if (propertyScope instanceof ILifecycle lifecycle) {
                lifecycle.onInit();
            }
        }
        initialized.set(true);
    }

    @Override
    public void onStart() throws DiException {
        if (!initialized.get()) {
            throw new DiException("Cannot start context before initialization");
        }
        for (IBeanScope beanScope : beanScopes) {
            if (beanScope instanceof ILifecycle lifecycle) {
                lifecycle.onStart();
            }
        }
        for (IPropertyScope propertyScope : propertyScopes) {
            if (propertyScope instanceof ILifecycle lifecycle) {
                lifecycle.onStart();
            }
        }
        started.set(true);
    }

    @Override
    public void onStop() throws DiException {
        if (!started.get()) {
            throw new DiException("Cannot stop context that is not started");
        }
        for (IBeanScope beanScope : beanScopes) {
            if (beanScope instanceof ILifecycle lifecycle) {
                lifecycle.onStop();
            }
        }
        for (IPropertyScope propertyScope : propertyScopes) {
            if (propertyScope instanceof ILifecycle lifecycle) {
                lifecycle.onStop();
            }
        }
        started.set(false);
    }

    @Override
    public void onFlush() throws DiException {
        ensureInitializedAndStarted();
        for (IBeanScope beanScope : beanScopes) {
            if (beanScope instanceof ILifecycle lifecycle) {
                lifecycle.onFlush();
            }
        }
        for (IPropertyScope propertyScope : propertyScopes) {
            if (propertyScope instanceof ILifecycle lifecycle) {
                lifecycle.onFlush();
            }
        }
    }

    @Override
    public void onReload() throws DiException {
        ensureInitializedAndStarted();
        for (IBeanScope beanScope : beanScopes) {
            if (beanScope instanceof ILifecycle lifecycle) {
                lifecycle.onReload();
            }
        }
        for (IPropertyScope propertyScope : propertyScopes) {
            if (propertyScope instanceof ILifecycle lifecycle) {
                lifecycle.onReload();
            }
        }
    }
}