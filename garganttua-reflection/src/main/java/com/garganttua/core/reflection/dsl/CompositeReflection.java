package com.garganttua.core.reflection.dsl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

import com.garganttua.core.reflection.IAnnotationScanner;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IConstructor;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.IObjectQuery;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.query.ObjectQueryFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * Composite implementation of {@link IReflection} that delegates to prioritized
 * providers and scanners via specialized delegate classes.
 *
 * <p>
 * Provider-level methods ({@code getClass}, {@code forName}) delegate to
 * {@link ProviderSelector} which selects the first provider that
 * {@link IReflectionProvider#supports(Class) supports} the requested type.
 * Scanner-level methods ({@code getClassesWithAnnotation},
 * {@code getMethodsWithAnnotation}) delegate to {@link ScannerAggregator}
 * which merges results from ALL scanners (deduplicated).
 * </p>
 *
 * <p>
 * Facade-level methods (field/method/constructor lookup, invocation, type
 * utilities)
 * delegate to {@link ConstructorDelegate}, {@link FieldDelegate},
 * {@link MethodDelegate}, and {@link TypeDelegate}.
 * </p>
 */
@Slf4j
class CompositeReflection implements IReflection {

    private final ProviderSelector providerSelector;
    private final ScannerAggregator scannerAggregator;
    private final ConstructorDelegate constructorDelegate;
    private final FieldDelegate fieldDelegate;
    private final MethodDelegate methodDelegate;
    private final TypeDelegate typeDelegate;

    CompositeReflection(List<IReflectionProvider> providers, List<IAnnotationScanner> scanners) {
        this.providerSelector = new ProviderSelector(providers);
        this.scannerAggregator = new ScannerAggregator(scanners);
        this.constructorDelegate = new ConstructorDelegate(providerSelector);
        this.fieldDelegate = new FieldDelegate(providerSelector);
        this.methodDelegate = new MethodDelegate(providerSelector);
        this.typeDelegate = new TypeDelegate(providerSelector);
    }

    // ========================================================================
    // IReflectionProvider methods — via ProviderSelector
    // ========================================================================

    @Override
    public <T> IClass<T> getClass(Class<T> clazz) {
        return providerSelector.getClass(clazz);
    }

    @Override
    public <T> IClass<T> forName(String className) throws ClassNotFoundException {
        return providerSelector.forName(className);
    }

    @Override
    public <T> IClass<T> forName(String className, boolean initialize, ClassLoader loader)
            throws ClassNotFoundException {
        return providerSelector.forName(className, initialize, loader);
    }

    @Override
    public boolean supports(Class<?> type) {
        return providerSelector.supports(type);
    }

    // ========================================================================
    // IAnnotationScanner methods — via ScannerAggregator
    // ========================================================================

    @Override
    public List<IClass<?>> getClassesWithAnnotation(IClass<? extends Annotation> annotation) {
        return scannerAggregator.getClassesWithAnnotation(annotation);
    }

    @Override
    public List<IClass<?>> getClassesWithAnnotation(String packageName, IClass<? extends Annotation> annotation) {
        return scannerAggregator.getClassesWithAnnotation(packageName, annotation);
    }

    @Override
    public List<IMethod> getMethodsWithAnnotation(IClass<? extends Annotation> annotation) {
        return scannerAggregator.getMethodsWithAnnotation(annotation);
    }

    @Override
    public List<IMethod> getMethodsWithAnnotation(String packageName, IClass<? extends Annotation> annotation) {
        return scannerAggregator.getMethodsWithAnnotation(packageName, annotation);
    }

    // ========================================================================
    // Object Query — inlined (formerly QueryDelegate)
    // ========================================================================

    @Override
    public <T> IObjectQuery<T> query(IClass<T> objectClass) throws ReflectionException {
        log.atTrace().log("Creating query for class: {}", objectClass);
        return ObjectQueryFactory.objectQuery(objectClass, providerSelector);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> IObjectQuery<T> query(T object) throws ReflectionException {
        log.atTrace().log("Creating query for object: {}", object);
        if (object == null) {
            throw new ReflectionException("object is null");
        }
        IClass<T> objectClass = (IClass<T>) providerSelector.getClass(object.getClass());
        return ObjectQueryFactory.objectQuery(objectClass, providerSelector);
    }

    @Override
    public <T> IObjectQuery<T> query(IClass<T> objectClass, T object) throws ReflectionException {
        log.atTrace().log("Creating query for class: {} with object: {}", objectClass, object);
        return ObjectQueryFactory.objectQuery(objectClass, providerSelector);
    }

    // ========================================================================
    // Constructor Lookup — via ConstructorDelegate
    // ========================================================================

    @Override
    public Optional<IConstructor<?>> findConstructor(IClass<?> clazz) {
        return constructorDelegate.findConstructor(clazz);
    }

    @Override
    public Optional<IConstructor<?>> findConstructor(IClass<?> clazz, IClass<?>... parameterTypes) {
        return constructorDelegate.findConstructor(clazz, parameterTypes);
    }

    @Override
    public <T> T newInstance(IClass<T> clazz) throws ReflectionException {
        return constructorDelegate.newInstance(clazz);
    }

    @Override
    public <T> T newInstance(IClass<T> clazz, Object... args) throws ReflectionException {
        return constructorDelegate.newInstance(clazz, args);
    }

    @Override
    public <T> T newInstance(IClass<T> clazz, boolean force) throws ReflectionException {
        return constructorDelegate.newInstance(clazz, force);
    }

    @Override
    public <T> T newInstance(IClass<T> clazz, boolean force, Object... args) throws ReflectionException {
        return constructorDelegate.newInstance(clazz, force, args);
    }

    // ========================================================================
    // Field Lookup & Access — via FieldDelegate
    // ========================================================================

    @Override
    public Optional<IField> findField(IClass<?> clazz, String fieldName) {
        return fieldDelegate.findField(clazz, fieldName);
    }

    @Override
    public Optional<IField> findFieldAnnotatedWith(IClass<?> clazz, IClass<? extends Annotation> annotation) {
        return fieldDelegate.findFieldAnnotatedWith(clazz, annotation);
    }

    @Override
    public List<String> findFieldAddressesWithAnnotation(IClass<?> clazz, IClass<? extends Annotation> annotation,
            boolean linked) {
        return fieldDelegate.findFieldAddressesWithAnnotation(clazz, annotation, linked);
    }

    @Override
    public Object getFieldValue(Object object, String fieldName) throws ReflectionException {
        return fieldDelegate.getFieldValue(object, fieldName);
    }

    @Override
    public Object getFieldValue(Object object, IField field) throws ReflectionException {
        return fieldDelegate.getFieldValue(object, field);
    }

    @Override
    public void setFieldValue(Object object, String fieldName, Object value) throws ReflectionException {
        fieldDelegate.setFieldValue(object, fieldName, value);
    }

    @Override
    public void setFieldValue(Object object, IField field, Object value) throws ReflectionException {
        fieldDelegate.setFieldValue(object, field, value);
    }

    @Override
    public Object getFieldValue(Object object, ObjectAddress address) throws ReflectionException {
        return fieldDelegate.getFieldValue(object, address);
    }

    @Override
    public Object getFieldValue(Object object, String fieldName, boolean force) throws ReflectionException {
        return fieldDelegate.getFieldValue(object, fieldName, force);
    }

    @Override
    public Object getFieldValue(Object object, IField field, boolean force) throws ReflectionException {
        return fieldDelegate.getFieldValue(object, field, force);
    }

    @Override
    public Object getFieldValue(Object object, ObjectAddress address, boolean force) throws ReflectionException {
        return fieldDelegate.getFieldValue(object, address, force);
    }

    @Override
    public void setFieldValue(Object object, ObjectAddress address, Object value) throws ReflectionException {
        fieldDelegate.setFieldValue(object, address, value);
    }

    @Override
    public void setFieldValue(Object object, String fieldName, Object value, boolean force) throws ReflectionException {
        fieldDelegate.setFieldValue(object, fieldName, value, force);
    }

    @Override
    public void setFieldValue(Object object, IField field, Object value, boolean force) throws ReflectionException {
        fieldDelegate.setFieldValue(object, field, value, force);
    }

    @Override
    public void setFieldValue(Object object, ObjectAddress address, Object value, boolean force) throws ReflectionException {
        fieldDelegate.setFieldValue(object, address, value, force);
    }

    // ========================================================================
    // Field Resolution — via FieldDelegate
    // ========================================================================

    @Override
    public Optional<ObjectAddress> resolveFieldAddress(String fieldName, IClass<?> entityClass) {
        try {
            return fieldDelegate.resolveFieldAddress(fieldName, entityClass);
        } catch (ReflectionException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<ObjectAddress> resolveFieldAddress(String fieldName, IClass<?> entityClass, IClass<?> fieldType) {
        try {
            return fieldDelegate.resolveFieldAddress(fieldName, entityClass, fieldType);
        } catch (ReflectionException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<ObjectAddress> resolveFieldAddress(ObjectAddress address, IClass<?> entityClass) {
        try {
            return fieldDelegate.resolveFieldAddress(address, entityClass);
        } catch (ReflectionException e) {
            return Optional.empty();
        }
    }

    // ========================================================================
    // Method Lookup — via MethodDelegate
    // ========================================================================

    @Override
    public Optional<IMethod> findMethod(IClass<?> clazz, String methodName) {
        return methodDelegate.findMethod(clazz, methodName);
    }

    @Override
    public List<IMethod> findMethods(IClass<?> clazz, String methodName) {
        return methodDelegate.findMethods(clazz, methodName);
    }

    @Override
    public Optional<IMethod> findMethodAnnotatedWith(IClass<?> clazz, IClass<? extends Annotation> annotation) {
        return methodDelegate.findMethodAnnotatedWith(clazz, annotation);
    }

    // ========================================================================
    // Method Resolution — via MethodDelegate
    // ========================================================================

    @Override
    public Optional<IMethod> resolveMethod(IClass<?> ownerType, String methodName) {
        try {
            return methodDelegate.resolveMethod(ownerType, methodName);
        } catch (ReflectionException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<IMethod> resolveMethod(IClass<?> ownerType, String methodName, IClass<?> returnType,
            IClass<?>... parameterTypes) {
        try {
            return methodDelegate.resolveMethod(ownerType, methodName, returnType, parameterTypes);
        } catch (ReflectionException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<IMethod> resolveMethod(IClass<?> ownerType, ObjectAddress methodAddress) {
        try {
            return methodDelegate.resolveMethod(ownerType, methodAddress);
        } catch (ReflectionException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<IMethod> resolveMethod(IClass<?> ownerType, ObjectAddress methodAddress, IClass<?> returnType,
            IClass<?>... parameterTypes) {
        try {
            return methodDelegate.resolveMethod(ownerType, methodAddress, returnType, parameterTypes);
        } catch (ReflectionException e) {
            return Optional.empty();
        }
    }

    // ========================================================================
    // Method Invocation — via MethodDelegate
    // ========================================================================

    @Override
    public <R> R invokeMethod(Object object, IMethod method, IClass<R> returnType, Object... args)
            throws ReflectionException {
        return methodDelegate.invokeMethod(object, method, returnType, args);
    }

    @Override
    public <R> R invokeMethod(Object object, String methodName, IClass<R> returnType, Object... args)
            throws ReflectionException {
        return methodDelegate.invokeMethod(object, methodName, returnType, args);
    }

    @Override
    public <R> IMethodReturn<R> invokeDeep(Object object, ObjectAddress address, IClass<R> returnType, Object... args)
            throws ReflectionException {
        IClass<?>[] paramTypes = typeDelegate.parameterTypes(args);
        return methodDelegate.invokeDeep(object, address, returnType, paramTypes, args);
    }

    @Override
    public <R> R invokeMethod(Object object, IMethod method, IClass<R> returnType, boolean force, Object... args)
            throws ReflectionException {
        return methodDelegate.invokeMethod(object, method, returnType, force, args);
    }

    @Override
    public <R> R invokeMethod(Object object, String methodName, IClass<R> returnType, boolean force, Object... args)
            throws ReflectionException {
        return methodDelegate.invokeMethod(object, methodName, returnType, force, args);
    }

    @Override
    public <R> IMethodReturn<R> invokeDeep(Object object, ObjectAddress address, IClass<R> returnType, boolean force,
            Object... args) throws ReflectionException {
        IClass<?>[] paramTypes = typeDelegate.parameterTypes(args);
        return methodDelegate.invokeDeep(object, address, returnType, force, paramTypes, args);
    }

    // ========================================================================
    // Type Utilities — via TypeDelegate
    // ========================================================================

    @Override
    public IClass<?> extractClass(Type type) {
        return typeDelegate.extractClass(type);
    }

    @Override
    public boolean typeEquals(Type type1, Type type2) {
        return typeDelegate.typeEquals(type1, type2);
    }

    @Override
    public boolean isImplementingInterface(IClass<?> interfaceType, IClass<?> objectType) {
        return typeDelegate.isImplementingInterface(interfaceType, objectType);
    }

    @Override
    public IClass<?>[] parameterTypes(Object[] args) {
        return typeDelegate.parameterTypes(args);
    }

    @Override
    public boolean isComplexType(IClass<?> clazz) {
        return typeDelegate.isComplexType(clazz);
    }

    @Override
    public IClass<?> getGenericTypeArgument(IClass<?> type, int index) {
        return typeDelegate.getGenericTypeArgument(type, index);
    }

    @Override
    public boolean isCollectionOrMapOrArray(IField field) {
        return typeDelegate.isCollectionOrMapOrArray(field);
    }
}
