package com.garganttua.core.reflection.methods;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.fields.FieldAccessManager;
import com.garganttua.core.reflection.fields.Fields;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

/**
 * Invokes a resolved method against a target object, traversing any intermediate
 * field path (including collections, arrays and maps) encoded in the method's
 * {@link ObjectAddress}.
 *
 * <p>As an {@link ISupplier}, it can also be evaluated lazily as a no-argument
 * static invocation producing an {@link IMethodReturn}.
 *
 * @param <T> the owner (declaring) type the method is invoked on
 * @param <R> the method's return type
 */
public class MethodInvoker<T, R> implements ISupplier<IMethodReturn<R>> {
    private static final Logger log = Logger.getLogger(MethodInvoker.class);

	private IClass<T> ownerType;
	private List<Object> methodPath;
	private ObjectAddress address;
	private IClass<R> returnType;
	private boolean statix;
	private boolean force;

	/**
	 * Creates an invoker for {@code method} without forcing access on inaccessible members.
	 *
	 * @param method the resolved method to invoke
	 * @throws ReflectionException if the method metadata cannot be initialized
	 */
	public MethodInvoker(ResolvedMethod method) throws ReflectionException {
		this(method, false);
	}

	/**
	 * Creates an invoker for {@code method}.
	 *
	 * @param method the resolved method to invoke
	 * @param force  whether to force access even for non-public members
	 * @throws ReflectionException if the method metadata cannot be initialized
	 */
	public MethodInvoker(ResolvedMethod method, boolean force) throws ReflectionException {
		Objects.requireNonNull(method, "Resolved method cannot be null");
		log.trace("Creating ObjectMethodInvoker for resolved method={}, force={}", method, force);

		this.returnType = (IClass<R>) Objects.requireNonNull(method.getReturnType(), "Return type cannot be null");
		this.ownerType = (IClass<T>) Objects.requireNonNull(method.getDeclaringClass(), "Class cannot be null");
		this.methodPath = Objects.requireNonNull(method.methodPath(), "Method path cannot be null");
		this.address = Objects.requireNonNull(method.address(), "Address cannot be null");
		this.statix = Methods.isStatic(method);
		this.force = force;
		log.debug("ObjectMethodInvoker initialized for ownerType={}, address={}, force={}", ownerType.getName(), address, force);
	}

	/**
	 * Invokes the resolved method on {@code object}, walking the field path when the
	 * method lives on a nested member.
	 *
	 * @param object the target instance, or {@code null} for a static method
	 * @param args   the arguments passed to the leaf method
	 * @return the invocation result, possibly wrapping multiple values when the path
	 *         traverses a collection, array or map
	 * @throws ReflectionException if {@code object} is null for an instance method, is
	 *         of the wrong type, the path is inconsistent, or an intermediate field is null
	 */
	public IMethodReturn<R> invoke(Object object, Object... args) throws ReflectionException {
		log.trace("invoke entry: object={}, ownerType={}, address={}, args count={}", object, this.ownerType,
				this.address, args != null ? args.length : 0);
		log.debug("Invoking method: ownerType={}, address={}, parameters count={}", this.ownerType.getName(),
				this.address, args != null ? args.length : 0);

		if (object == null && !statix) {
			log.error("object parameter is null");
			throw new ReflectionException("object is null");
		}
		if (!statix && !this.ownerType.isInstance(object)) {
			log.error("object type {} is not assignable from {}", object.getClass(), this.ownerType);
			throw new ReflectionException("object is not of type " + this.ownerType);
		}

		if (this.methodPath.size() == 1) {
			IMethod method = (IMethod) methodPath.get(0);
			String methodName = this.address.getElement(0);
			log.debug("Direct method invocation: methodName={}", methodName);

			if (!method.getName().equals(methodName)) {
				log.error("method names mismatch: address={}, method={}", methodName, method.getName());
				throw new ReflectionException("method names of address " + methodName + " and fields list "
						+ method.getName() + " do not match");
			}

			SingleMethodReturn<R> result = invokeMethodSafely(object, methodName, method, args);
			log.debug("Successfully invoked method {} on ownerType {}", methodName, this.ownerType.getName());
			return result;

		} else {
			log.debug("Recursive method invocation with {} fields", this.methodPath.size());
			IMethodReturn<R> result = this.invokeMethodRecursively(object, 0, 0, args);
			log.debug("Successfully invoked method recursively for address={}", this.address);
			return result;
		}
	}

	private IMethodReturn<R> invokeMethodRecursively(Object object, int fieldIndex, int fieldNameIndex, Object... args)
			throws ReflectionException {
		log.trace("invokeMethodRecursively: fieldIndex={}, fieldNameIndex={}", fieldIndex, fieldNameIndex);
		log.debug("Invoking method recursively: class={}, address={}, fieldIndex={}, fieldNameIndex={}",
				this.ownerType.getName(), this.address, fieldIndex, fieldNameIndex);
		boolean isLastIteration = (fieldIndex + 2 == this.methodPath.size());
		IField field = (IField) this.methodPath.get(fieldIndex);
		String fieldName = this.address.getElement(fieldNameIndex);

		Object temp = resolveStepFieldValue(object, field, fieldName);

		if (Fields.isArrayOrMapOrCollectionField(field)) {
			log.debug("Field {} is array/map/collection type", fieldName);
			List<SingleMethodReturn<R>> returned = new ArrayList<>();
			this.doIfIsCollection(fieldIndex, fieldNameIndex, isLastIteration, field, temp, returned, args);
			this.doIfIsMap(fieldIndex, fieldNameIndex, isLastIteration, field, temp, returned, args);
			this.doIfIsArray(fieldIndex, fieldNameIndex, isLastIteration, field, temp, returned, args);
			return new MultipleMethodReturn<>(returnType, returned);
		} else {
			if (isLastIteration) {
				log.debug("Last iteration, invoking final method");
				IMethod leafMethod = (IMethod) this.methodPath.get(fieldIndex + 1);
				String methodName = this.address.getElement(fieldNameIndex + 1);
				return invokeMethodSafely(temp, methodName, leafMethod, args);
			} else {
				log.trace("Continuing recursive method invocation");
				return this.invokeMethodRecursively(temp, fieldIndex + 1, fieldNameIndex + 1, args);
			}
		}
	}

	/** Validate the path field name matches and return its non-null value, or throw. */
	private Object resolveStepFieldValue(Object object, IField field, String fieldName) throws ReflectionException {
		if (!field.getName().equals(fieldName)) {
			log.error("field names mismatch: address={}, field={}", fieldName, field.getName());
			throw new ReflectionException(
					"field names of address " + fieldName + " and fields list " + field.getName() + " do not match");
		}
		Object temp = getFieldValue(object, field);
		if (temp == null) {
			log.error("Field {} is null, cannot invoke method with address {}", fieldName, this.address);
			throw new ReflectionException("cannot invoke method with address " + this.address + ". The field "
					+ fieldName + " of object " + object + " is null");
		}
		return temp;
	}

	private void doIfIsArray(int fieldIndex, int fieldNameIndex, boolean isLastIteration, IField field, Object temp,
			List<SingleMethodReturn<R>> returned, Object[] args) throws ReflectionException {
		if (field.getType().isArray()) {
			log.debug("Processing array field: {}", field.getName());
			Object[] array = (Object[]) temp;

			for (Object obj : array) {
				if (isLastIteration) {
					IMethod leafMethod = (IMethod) this.methodPath.get(fieldIndex + 1);
					String methodName = this.address.getElement(fieldNameIndex + 1);
					returned.add(invokeMethodSafely(obj, methodName, leafMethod, args));
				} else {
					IMethodReturn<R> result = this.invokeMethodRecursively(obj, fieldIndex + 1, fieldNameIndex + 1, args);
					collectResults(result, returned);
				}
			}
		}
	}

	private void doIfIsMap(int fieldIndex, int fieldNameIndex, boolean isLastIteration, IField field, Object temp,
			List<SingleMethodReturn<R>> returned, Object[] args) throws ReflectionException {
		Class<?> rawType = (Class<?>) field.getType().getType();
		if (Map.class.isAssignableFrom(rawType)) {
			log.debug("Processing map field: {}", field.getName());
			Map<Object, Object> sub = (Map<Object, Object>) temp;
			String mapElement = this.address.getElement(fieldNameIndex + 1);
			Iterator<?> it = null;
			if (mapElement.equals(ObjectAddress.MAP_KEY_INDICATOR)) {
				log.debug("Processing map keys");
				it = sub.keySet().iterator();
			}
			if (mapElement.equals(ObjectAddress.MAP_VALUE_INDICATOR)) {
				log.debug("Processing map values");
				it = sub.values().iterator();
			}
			if (it == null) {
				log.error("Invalid map address element: {}, expected #key or #value", mapElement);
				throw new ReflectionException("Invalid address, " + mapElement + " should be either #key or #value");
			}
			for (int i = 0; i < sub.size(); i++) {
				Object tempObject = it.next();
				if (isLastIteration) {
					IMethod leafMethod = (IMethod) this.methodPath.get(fieldIndex + 2);
					String methodName = this.address.getElement(fieldNameIndex + 2);
					returned.add(invokeMethodSafely(tempObject, methodName, leafMethod, args));
				} else {
					IMethodReturn<R> result = this.invokeMethodRecursively(tempObject, fieldIndex + 2, fieldNameIndex + 2, args);
					collectResults(result, returned);
				}
			}
		}
	}

	private void doIfIsCollection(int fieldIndex, int fieldNameIndex, boolean isLastIteration, IField field, Object temp,
			List<SingleMethodReturn<R>> returned, Object... args)
			throws ReflectionException {
		Class<?> rawType = (Class<?>) field.getType().getType();
		if (Collection.class.isAssignableFrom(rawType)) {
			log.debug("Processing collection field: {}", field.getName());
			Collection<Object> sub = (Collection<Object>) temp;
			for (Object obj : sub) {
				if (isLastIteration) {
					IMethod leafMethod = (IMethod) this.methodPath.get(fieldIndex + 1);
					String methodName = this.address.getElement(fieldNameIndex + 1);
					returned.add(invokeMethodSafely(obj, methodName, leafMethod, args));
				} else {
					IMethodReturn<R> result = this.invokeMethodRecursively(obj, fieldIndex + 1, fieldNameIndex + 1, args);
					collectResults(result, returned);
				}
			}
		}
	}

	private SingleMethodReturn<R> invokeMethodSafely(Object object, String methodName, IMethod method, Object... args) {
		log.trace("Invoking method safely {} on object of type {}", methodName,
				object != null ? object.getClass().getName() : "null");

		checkMethodAndParams(method, returnType, args);

		try (var mgr = new MethodAccessManager(method, this.force)) {
			R result = (R) method.invoke(object, args);
			log.debug("Successfully invoked method {} on object of type {}", methodName,
					object != null ? object.getClass().getName() : "null");
			return new SingleMethodReturn<>(result, returnType);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause() != null ? e.getCause() : e;
			log.debug("Method {} threw exception: {}", methodName, cause.getClass().getName());
			return new SingleMethodReturn<>(cause, returnType);
		} catch (IllegalAccessException e) {
			log.error("Cannot access method {} of object {}", methodName,
					object != null ? object.getClass().getName() : "null", e);
			return new SingleMethodReturn<>(e, returnType);
		}
	}

	private static void checkMethodAndParams(IMethod method, IClass<?> returnType, Object... args) {
		if (method.getParameterCount() != args.length) {
			log.warn("Method {} needs {} params but {} provided", method.getName(),
					method.getParameterCount(), args.length);
		}

		if (returnType != null && !returnType.isAssignableFrom(method.getReturnType())
				&& !method.getReturnType().isAssignableFrom(returnType)) {
			log.warn("Method {} return type {} does not match expected {}", method.getName(),
					method.getReturnType(), returnType);
		}
	}

	private void collectResults(IMethodReturn<R> result, List<SingleMethodReturn<R>> returned) {
		if (result.isSingle()) {
			if (result.hasException()) {
				returned.add(new SingleMethodReturn<>(result.getException(), returnType));
			} else {
				returned.add(new SingleMethodReturn<>(result.single(), returnType));
			}
		} else if (result instanceof MultipleMethodReturn<R> multiple) {
			returned.addAll(multiple.getReturns());
		} else {
			for (R value : result.multiple()) {
				returned.add(new SingleMethodReturn<>(value, returnType));
			}
		}
	}

	private Object getFieldValue(Object object, IField field) throws ReflectionException {
		try (var mgr = new FieldAccessManager(field, this.force)) {
			return field.get(object);
		} catch (IllegalAccessException | IllegalArgumentException e) {
			throw new ReflectionException("Cannot get field " + field.getName() + " of object " + object.getClass().getName(), e);
		}
	}

	// --- ISupplier<R> ---

	@Override
	public Optional<IMethodReturn<R>> supply() throws SupplyException {
		try {
			IMethodReturn<R> result = invoke(null);
			if (result.hasException()) {
				throw new SupplyException("Method invocation failed", result.getException());
			}
			return Optional.of(result);
		} catch (ReflectionException e) {
			throw new SupplyException(e);
		}
	}

	@Override
	public Type getSuppliedType() {
		return getSuppliedClass().getType();
	}

	@Override
	public IClass<IMethodReturn<R>> getSuppliedClass() {
		return (IClass<IMethodReturn<R>>) (IClass<?>) IClass.getClass(IMethodReturn.class);
	}

}
