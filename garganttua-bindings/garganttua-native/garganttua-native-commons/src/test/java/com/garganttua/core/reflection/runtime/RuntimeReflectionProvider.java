package com.garganttua.core.reflection.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IConstructor;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.JdkClass;
import com.garganttua.core.reflection.ObjectAddress;

/**
 * Test-only reflection provider published at the exact fully-qualified name
 * ({@code com.garganttua.core.reflection.runtime.RuntimeReflectionProvider})
 * that {@code ReflectConfigEntryBuilder} and {@code ReflectConfigEntry} hard-code
 * via {@code Class.forName(...)} in their {@code defaultProvider()} helpers.
 *
 * <p>This module's test classpath does <em>not</em> include
 * {@code garganttua-runtime-reflection}; without this stub the builders' static
 * initializers fail (as the first-pass behaviour test documents). Providing a
 * real {@link IReflection} backed by {@link JdkClass} lets the builders be
 * genuinely exercised. Only the methods actually exercised by the native-config
 * builders are implemented; the rest throw {@link UnsupportedOperationException}.</p>
 */
public final class RuntimeReflectionProvider implements IReflection {

    // --- IReflectionProvider (the methods the native-config code relies on) ---

    @Override
    public <T> IClass<T> getClass(Class<T> clazz) {
        return JdkClass.of(clazz);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> IClass<T> forName(String className) throws ClassNotFoundException {
        return (IClass<T>) JdkClass.ofUnchecked(Class.forName(className));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> IClass<T> forName(String className, boolean initialize, ClassLoader loader)
            throws ClassNotFoundException {
        return (IClass<T>) JdkClass.ofUnchecked(Class.forName(className, initialize, loader));
    }

    @Override
    public boolean supports(Class<?> type) {
        return true;
    }

    // --- IAnnotationScanner (no scanning support in the stub) ---

    @Override
    public List<IClass<?>> getClassesWithAnnotation(IClass<? extends Annotation> annotation) {
        return List.of();
    }

    @Override
    public List<IClass<?>> getClassesWithAnnotation(String packageName, IClass<? extends Annotation> annotation) {
        return List.of();
    }

    @Override
    public List<IMethod> getMethodsWithAnnotation(IClass<? extends Annotation> annotation) {
        return List.of();
    }

    @Override
    public List<IMethod> getMethodsWithAnnotation(String packageName, IClass<? extends Annotation> annotation) {
        return List.of();
    }

    // --- Remaining IReflection facade methods: unused by these tests ---

    private static UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("Not implemented in the native-commons test stub");
    }

    @Override
    public Optional<IConstructor<?>> findConstructor(IClass<?> clazz) {
        throw unsupported();
    }

    @Override
    public Optional<IConstructor<?>> findConstructor(IClass<?> clazz, IClass<?>... parameterTypes) {
        throw unsupported();
    }

    @Override
    public Optional<IField> findField(IClass<?> clazz, String fieldName) {
        throw unsupported();
    }

    @Override
    public Optional<IField> findFieldAnnotatedWith(IClass<?> clazz, IClass<? extends Annotation> annotation) {
        throw unsupported();
    }

    @Override
    public List<String> findFieldAddressesWithAnnotation(IClass<?> clazz, IClass<? extends Annotation> annotation,
            boolean linked) {
        throw unsupported();
    }

    @Override
    public Optional<ObjectAddress> resolveFieldAddress(String fieldName, IClass<?> entityClass) {
        throw unsupported();
    }

    @Override
    public Optional<ObjectAddress> resolveFieldAddress(String fieldName, IClass<?> entityClass, IClass<?> fieldType) {
        throw unsupported();
    }

    @Override
    public Optional<ObjectAddress> resolveFieldAddress(ObjectAddress address, IClass<?> entityClass) {
        throw unsupported();
    }

    @Override
    public Optional<IMethod> findMethod(IClass<?> clazz, String methodName) {
        throw unsupported();
    }

    @Override
    public List<IMethod> findMethods(IClass<?> clazz, String methodName) {
        throw unsupported();
    }

    @Override
    public Optional<IMethod> findMethodAnnotatedWith(IClass<?> clazz, IClass<? extends Annotation> annotation) {
        throw unsupported();
    }

    @Override
    public Optional<IMethod> resolveMethod(IClass<?> ownerType, String methodName) {
        throw unsupported();
    }

    @Override
    public Optional<IMethod> resolveMethod(IClass<?> ownerType, String methodName, IClass<?> returnType,
            IClass<?>... parameterTypes) {
        throw unsupported();
    }

    @Override
    public Optional<IMethod> resolveMethod(IClass<?> ownerType, ObjectAddress methodAddress) {
        throw unsupported();
    }

    @Override
    public Optional<IMethod> resolveMethod(IClass<?> ownerType, ObjectAddress methodAddress, IClass<?> returnType,
            IClass<?>... parameterTypes) {
        throw unsupported();
    }

    @Override
    public <R> com.garganttua.core.reflection.IMethodReturn<R> invokeDeep(Object object, ObjectAddress address,
            IClass<R> returnType, Object... args) {
        throw unsupported();
    }

    @Override
    public <R> com.garganttua.core.reflection.IMethodReturn<R> invokeDeep(Object object, ObjectAddress address,
            IClass<R> returnType, boolean force, Object... args) {
        throw unsupported();
    }

    @Override
    public IClass<?> extractClass(Type type) {
        throw unsupported();
    }

    @Override
    public boolean typeEquals(Type type1, Type type2) {
        throw unsupported();
    }

    @Override
    public boolean isImplementingInterface(IClass<?> interfaceType, IClass<?> objectType) {
        throw unsupported();
    }

    @Override
    public IClass<?>[] parameterTypes(Object[] args) {
        throw unsupported();
    }

    @Override
    public boolean isComplexType(IClass<?> clazz) {
        throw unsupported();
    }

    @Override
    public IClass<?> getGenericTypeArgument(IClass<?> type, int index) {
        throw unsupported();
    }

    @Override
    public boolean isCollectionOrMapOrArray(IField field) {
        throw unsupported();
    }

    @Override
    public <T> com.garganttua.core.reflection.IObjectQuery<T> query(IClass<T> objectClass)
            throws com.garganttua.core.reflection.ReflectionException {
        throw unsupported();
    }

    @Override
    public <T> com.garganttua.core.reflection.IObjectQuery<T> query(T object)
            throws com.garganttua.core.reflection.ReflectionException {
        throw unsupported();
    }

    @Override
    public <T> com.garganttua.core.reflection.IObjectQuery<T> query(IClass<T> objectClass, T object)
            throws com.garganttua.core.reflection.ReflectionException {
        throw unsupported();
    }

    @Override
    public <T> T newInstance(IClass<T> clazz) throws com.garganttua.core.reflection.ReflectionException {
        throw unsupported();
    }

    @Override
    public <T> T newInstance(IClass<T> clazz, Object... args) throws com.garganttua.core.reflection.ReflectionException {
        throw unsupported();
    }

    @Override
    public <T> T newInstance(IClass<T> clazz, boolean force) throws com.garganttua.core.reflection.ReflectionException {
        throw unsupported();
    }

    @Override
    public <T> T newInstance(IClass<T> clazz, boolean force, Object... args)
            throws com.garganttua.core.reflection.ReflectionException {
        throw unsupported();
    }

    @Override
    public Object getFieldValue(Object object, String fieldName)
            throws com.garganttua.core.reflection.ReflectionException {
        throw unsupported();
    }

    @Override
    public Object getFieldValue(Object object, IField field)
            throws com.garganttua.core.reflection.ReflectionException {
        throw unsupported();
    }

    @Override
    public Object getFieldValue(Object object, ObjectAddress address)
            throws com.garganttua.core.reflection.ReflectionException {
        throw unsupported();
    }

    @Override
    public Object getFieldValue(Object object, String fieldName, boolean force)
            throws com.garganttua.core.reflection.ReflectionException {
        throw unsupported();
    }

    @Override
    public Object getFieldValue(Object object, IField field, boolean force)
            throws com.garganttua.core.reflection.ReflectionException {
        throw unsupported();
    }

    @Override
    public Object getFieldValue(Object object, ObjectAddress address, boolean force)
            throws com.garganttua.core.reflection.ReflectionException {
        throw unsupported();
    }

    @Override
    public void setFieldValue(Object object, String fieldName, Object value)
            throws com.garganttua.core.reflection.ReflectionException {
        throw unsupported();
    }

    @Override
    public void setFieldValue(Object object, IField field, Object value)
            throws com.garganttua.core.reflection.ReflectionException {
        throw unsupported();
    }

    @Override
    public void setFieldValue(Object object, ObjectAddress address, Object value)
            throws com.garganttua.core.reflection.ReflectionException {
        throw unsupported();
    }

    @Override
    public void setFieldValue(Object object, String fieldName, Object value, boolean force)
            throws com.garganttua.core.reflection.ReflectionException {
        throw unsupported();
    }

    @Override
    public void setFieldValue(Object object, IField field, Object value, boolean force)
            throws com.garganttua.core.reflection.ReflectionException {
        throw unsupported();
    }

    @Override
    public void setFieldValue(Object object, ObjectAddress address, Object value, boolean force)
            throws com.garganttua.core.reflection.ReflectionException {
        throw unsupported();
    }

    @Override
    public <R> R invokeMethod(Object object, IMethod method, IClass<R> returnType, Object... args)
            throws com.garganttua.core.reflection.ReflectionException {
        throw unsupported();
    }

    @Override
    public <R> R invokeMethod(Object object, String methodName, IClass<R> returnType, Object... args)
            throws com.garganttua.core.reflection.ReflectionException {
        throw unsupported();
    }

    @Override
    public <R> R invokeMethod(Object object, IMethod method, IClass<R> returnType, boolean force, Object... args)
            throws com.garganttua.core.reflection.ReflectionException {
        throw unsupported();
    }

    @Override
    public <R> R invokeMethod(Object object, String methodName, IClass<R> returnType, boolean force, Object... args)
            throws com.garganttua.core.reflection.ReflectionException {
        throw unsupported();
    }
}
