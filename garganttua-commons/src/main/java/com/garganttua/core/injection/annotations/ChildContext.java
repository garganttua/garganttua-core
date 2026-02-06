package com.garganttua.core.injection.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.nativve.annotations.Native;
import com.garganttua.core.reflection.annotations.Indexed;

/**
 * Annotation to mark a class as a child context factory for automatic registration.
 *
 * <p>
 * Classes annotated with {@code @ChildContext} are automatically detected during the
 * auto-detection phase and registered as child context factories in the injection context.
 * The annotated class must implement {@link com.garganttua.core.injection.IInjectionChildContextFactory}.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @ChildContext
 * public class RuntimeContextFactory
 *     implements IInjectionChildContextFactory<RuntimeContext> {
 *
 *     @Override
 *     public RuntimeContext createChildContext(
 *             IInjectionContext clonedParent,
 *             Object... args) throws DiException {
 *         return new RuntimeContext(clonedParent);
 *     }
 * }
 * }</pre>
 *
 * <h2>Auto-Detection</h2>
 * <p>
 * During the auto-detection phase, builders scan for classes annotated with
 * {@code @ChildContext} and automatically register them as child context factories.
 * This eliminates the need for manual registration via
 * {@code InjectionContextBuilder.childContextFactory()}.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.injection.IInjectionChildContextFactory
 * @see com.garganttua.core.injection.IInjectionContext#registerChildContextFactory
 */
@Indexed
@Native
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ChildContext {
}
