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

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.fields.Fields;
import com.garganttua.core.reflection.utils.ParameterizedTypeImpl;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MethodInvoker<T, R> implements ISupplier<IMethodReturn<R>> {

	private IClass<T> ownerType;
	private List<Object> methodPath;
	private ObjectAddress address;
	private IClass<R> returnType;
	private boolean statix;


	public MethodInvoker(ResolvedMethod method) throws ReflectionException {
		Objects.requireNonNull(method, "Resolved method cannot be null");
		log.atTrace().log("Creating ObjectMethodInvoker for resolved method ={}", method);

		this.returnType = (IClass<R>) Objects.requireNonNull(method.getReturnType(), "Return type cannot be null");
		this.ownerType = (IClass<T>) Objects.requireNonNull(method.getDeclaringClass(), "Class cannot be null");
		this.methodPath = Objects.requireNonNull(method.methodPath(), "Method path cannot be null");
		this.address = Objects.requireNonNull(method.address(), "Address cannot be null");
		this.statix = Methods.isStatic(method);
		log.atDebug().log("ObjectMethodInvoker initialized for ownerType={}, address={}", ownerType.getName(), address);
	}

	public IMethodReturn<R> invoke(Object object, Object... args) throws ReflectionException {
		log.atTrace().log("invoke entry: object={}, ownerType={}, address={}, args count={}", object, this.ownerType,
				this.address, args != null ? args.length : 0);
		log.atDebug().log("Invoking method: ownerType={}, address={}, parameters count={}", this.ownerType.getName(),
				this.address, args != null ? args.length : 0);

		if (object == null && !statix) {
			log.atError().log("object parameter is null");
			throw new ReflectionException("object is null");
		}
		if (!statix && !this.ownerType.isInstance(object)) {
			log.atError().log("object type {} is not assignable from {}", object.getClass(), this.ownerType);
			throw new ReflectionException("object is not of type " + this.ownerType);
		}

		if (this.methodPath.size() == 1) {
			IMethod method = (IMethod) methodPath.get(0);
			String methodName = this.address.getElement(0);
			log.atDebug().log("Direct method invocation: methodName={}", methodName);

			if (!method.getName().equals(methodName)) {
				log.atError().log("method names mismatch: address={}, method={}", methodName, method.getName());
				throw new ReflectionException("method names of address " + methodName + " and fields list "
						+ method.getName() + " do not match");
			}

			SingleMethodReturn<R> result = invokeMethodSafely(object, methodName, method, args);
			log.atDebug().log("Successfully invoked method {} on ownerType {}", methodName, this.ownerType.getName());
			return result;

		} else {
			log.atDebug().log("Recursive method invocation with {} fields", this.methodPath.size());
			IMethodReturn<R> result = this.invokeMethodRecursively(object, 0, 0, args);
			log.atDebug().log("Successfully invoked method recursively for address={}", this.address);
			return result;
		}
	}

	private IMethodReturn<R> invokeMethodRecursively(Object object, int fieldIndex, int fieldNameIndex, Object... args)
			throws ReflectionException {
		log.atTrace().log("invokeMethodRecursively: fieldIndex={}, fieldNameIndex={}", fieldIndex, fieldNameIndex);
		log.atDebug().log("Invoking method recursively: class={}, address={}, fieldIndex={}, fieldNameIndex={}",
				this.ownerType.getName(), this.address, fieldIndex, fieldNameIndex);
		boolean isLastIteration = (fieldIndex + 2 == this.methodPath.size());
		IField field = (IField) this.methodPath.get(fieldIndex);
		String fieldName = this.address.getElement(fieldNameIndex);

		if (!field.getName().equals(fieldName)) {
			log.atError().log("field names mismatch: address={}, field={}", fieldName, field.getName());
			throw new ReflectionException(
					"field names of address " + fieldName + " and fields list " + field.getName() + " do not match");
		}

		Object temp = getFieldValue(object, field);
		if (temp == null) {
			log.atError().log("Field {} is null, cannot invoke method with address {}", fieldName, this.address);
			throw new ReflectionException("cannot invoke method with address " + this.address + ". The field "
					+ fieldName + " of object " + object + " is null");
		}

		if (Fields.isArrayOrMapOrCollectionField(field)) {
			log.atDebug().log("Field {} is array/map/collection type", fieldName);
			List<SingleMethodReturn<R>> returned = new ArrayList<>();
			this.doIfIsCollection(fieldIndex, fieldNameIndex, isLastIteration, field, temp, returned, args);
			this.doIfIsMap(fieldIndex, fieldNameIndex, isLastIteration, field, temp, returned, args);
			this.doIfIsArray(fieldIndex, fieldNameIndex, isLastIteration, field, temp, returned, args);
			return new MultipleMethodReturn<>(returnType, returned);
		} else {
			if (isLastIteration) {
				log.atDebug().log("Last iteration, invoking final method");
				IMethod leafMethod = (IMethod) this.methodPath.get(fieldIndex + 1);
				String methodName = this.address.getElement(fieldNameIndex + 1);
				return invokeMethodSafely(temp, methodName, leafMethod, args);
			} else {
				log.atTrace().log("Continuing recursive method invocation");
				return this.invokeMethodRecursively(temp, fieldIndex + 1, fieldNameIndex + 1, args);
			}
		}
	}

	private void doIfIsArray(int fieldIndex, int fieldNameIndex, boolean isLastIteration, IField field, Object temp,
			List<SingleMethodReturn<R>> returned, Object[] args) throws ReflectionException {
		if (field.getType().isArray()) {
			log.atDebug().log("Processing array field: {}", field.getName());
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
			log.atDebug().log("Processing map field: {}", field.getName());
			Map<Object, Object> sub = (Map<Object, Object>) temp;
			String mapElement = this.address.getElement(fieldNameIndex + 1);
			Iterator<?> it = null;
			if (mapElement.equals(ObjectAddress.MAP_KEY_INDICATOR)) {
				log.atDebug().log("Processing map keys");
				it = sub.keySet().iterator();
			}
			if (mapElement.equals(ObjectAddress.MAP_VALUE_INDICATOR)) {
				log.atDebug().log("Processing map values");
				it = sub.values().iterator();
			}
			if (it == null) {
				log.atError().log("Invalid map address element: {}, expected #key or #value", mapElement);
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
			log.atDebug().log("Processing collection field: {}", field.getName());
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
		log.atTrace().log("Invoking method safely {} on object of type {}", methodName,
				object != null ? object.getClass().getName() : "null");

		checkMethodAndParams(method, returnType, args);

		try {
			method.setAccessible(true);
			R result = (R) method.invoke(object, args);
			log.atDebug().log("Successfully invoked method {} on object of type {}", methodName,
					object != null ? object.getClass().getName() : "null");
			return new SingleMethodReturn<>(result, returnType);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause() != null ? e.getCause() : e;
			log.atDebug().log("Method {} threw exception: {}", methodName, cause.getClass().getName());
			return new SingleMethodReturn<>(cause, returnType);
		} catch (IllegalAccessException e) {
			log.atError().log("Cannot access method {} of object {}", methodName,
					object != null ? object.getClass().getName() : "null", e);
			return new SingleMethodReturn<>(e, returnType);
		}
	}

	private static void checkMethodAndParams(IMethod method, IClass<?> returnType, Object... args) {
		if (method.getParameterCount() != args.length) {
			log.atWarn().log("Method {} needs {} params but {} provided", method.getName(),
					method.getParameterCount(), args.length);
		}

		if (returnType != null && !returnType.isAssignableFrom(method.getReturnType())
				&& !method.getReturnType().isAssignableFrom(returnType)) {
			log.atWarn().log("Method {} return type {} does not match expected {}", method.getName(),
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

	private static Object getFieldValue(Object object, IField field) throws ReflectionException {
		try {
			field.setAccessible(true);
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
