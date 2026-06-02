package com.garganttua.core.reflection.runtime;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflectionProvider;

import jakarta.annotation.Priority;

/**
 * {@link IReflectionProvider} backed by standard JVM {@code java.lang.reflect}.
 *
 * <p>This is the default provider for ordinary (non-native) execution. It is
 * discovered at cold start via {@code ServiceLoader} and ranked by its
 * {@link Priority} of {@code 10} (AOT providers, at {@code 20}, take precedence
 * when present).</p>
 */
@Priority(10)
public class RuntimeReflectionProvider implements IReflectionProvider {

	/**
	 * {@inheritDoc}
	 *
	 * <p>Returns a cached {@link RuntimeClass} mirror of the given raw class.</p>
	 */
	@Override
	public <T> IClass<T> getClass(Class<T> clazz) {
		return RuntimeClass.of(clazz);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws ClassNotFoundException if no class with the given name is found
	 */
	@Override
	public IClass<?> forName(String className) throws ClassNotFoundException {
		return RuntimeClass.ofUnchecked(Class.forName(className));
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws ClassNotFoundException if no class with the given name is found
	 */
	@Override
	public IClass<?> forName(String className, boolean initialize, ClassLoader loader) throws ClassNotFoundException {
		return RuntimeClass.ofUnchecked(Class.forName(className, initialize, loader));
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>The runtime provider supports every type, so this always returns
	 * {@code true}.</p>
	 */
	@Override
	public boolean supports(Class<?> type) {
		return true;
	}
}
