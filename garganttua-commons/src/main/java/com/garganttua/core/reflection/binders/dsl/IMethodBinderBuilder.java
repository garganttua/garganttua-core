package com.garganttua.core.reflection.binders.dsl;

import java.lang.reflect.Method;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.mutex.IMutex;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.binders.IMethodBinder;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

/**
 * Builder interface for constructing method binders with various resolution
 * strategies.
 *
 * <p>
 * {@code IMethodBinderBuilder} specializes {@link IExecutableBinderBuilder} for
 * method-specific binder construction. It provides multiple ways to identify
 * and
 * bind to methods: by name, by {@link Method} reference, by
 * {@link ObjectAddress},
 * or with full signature specification. The builder supports both static and
 * instance methods.
 * </p>
 *
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * // Method identification by name
 * IMethodBinder<String> concat = MethodBinderBuilder
 *                 .forClass(String.class)
 *                 .method("concat")
 *                 .withParam("Hello")
 *                 .build();
 *
 * // Method with full signature
 * IMethodBinder<Integer> parse = MethodBinderBuilder
 *                 .forClass(Integer.class)
 *                 .method("parseInt", Integer.class, String.class)
 *                 .withParam("123")
 *                 .build();
 *
 * // Method using Method reference
 * Method toStringMethod = Object.class.getMethod("toString");
 * IMethodBinder<String> toString = MethodBinderBuilder
 *                 .forInstance(myObject)
 *                 .method(toStringMethod)
 *                 .build();
 *
 * // Method with explicit return type
 * IMethodBinder<Response> handler = MethodBinderBuilder
 *                 .forInstance(controller)
 *                 .method("handle")
 *                 .withReturn(Response.class)
 *                 .withParam(HttpRequest.class)
 *                 .build();
 *
 * // Auto-detect method (useful for single-method interfaces)
 * IMethodBinder<Void> runnable = MethodBinderBuilder
 *                 .forInstance(myRunnable)
 *                 .method() // Auto-detects the single method
 *                 .build();
 * }</pre>
 *
 * <h2>Method Resolution Strategies</h2>
 * <ul>
 * <li><b>By name</b>: {@link #method(String)} - Resolves by name and
 * parameters</li>
 * <li><b>By signature</b>: {@link #method(String, Class, Class[])} - Explicit
 * signature</li>
 * <li><b>By reference</b>: {@link #method(Method)} - Direct Method object</li>
 * <li><b>By address</b>: {@link #method(ObjectAddress)} - Symbolic address</li>
 * <li><b>Auto-detect</b>: {@link #method()} - For single-method scenarios</li>
 * </ul>
 *
 * <h2>Overloaded Methods</h2>
 * <p>
 * For overloaded methods, use the full signature methods that accept parameter
 * types to disambiguate. The builder will match the exact method based on the
 * provided parameter types.
 * </p>
 *
 * @param <ExecutionReturn> the return type of the bound method
 * @param <Builder>         the concrete builder type for method chaining
 * @param <Link>            the type of the parent builder for hierarchical
 *                          navigation
 * @param <Built>           the specific method binder type being constructed
 * @since 2.0.0-ALPHA01
 * @see IExecutableBinderBuilder
 * @see IMethodBinder
 */
public interface IMethodBinderBuilder<ExecutionReturn, Builder extends IMethodBinderBuilder<ExecutionReturn, ?, ?, ?>, Link, Built extends IMethodBinder<ExecutionReturn>>
                extends IExecutableBinderBuilder<ExecutionReturn, Builder, Link, Built> {

        /**
         * Returns the method to bind.
         *
         * <p>
         * Useful for single-method interfaces (like Runnable) or when there's only
         * one method matching the parameter configuration.
         * </p>
         *
         * @return this builder instance for method chaining
         * @throws DslException if auto-detection fails or finds multiple candidates
         */
        Method method() throws DslException;

        ObjectAddress methodAddress() throws DslException;

        /**
         * Specifies the method to bind with full signature.
         *
         * @param method         the Method object to bind
         * @param returnType     the expected return type
         * @param parameterTypes the parameter types (in order)
         * @return this builder instance for method chaining
         * @throws DslException if the signature doesn't match or is incompatible
         */
        Builder method(Method method) throws DslException;

        /**
         * Specifies the method to bind by address with full signature.
         *
         * @param methodAddress  the symbolic address of the method
         * @param returnType     the expected return type
         * @param parameterTypes the parameter types (in order)
         * @return this builder instance for method chaining
         * @throws DslException if the address is invalid or the signature doesn't match
         */
        Builder method(ObjectAddress methodAddress,
                        Class<ExecutionReturn> returnType, Class<?>... parameterTypes) throws DslException;

        /**
         * Specifies the method to bind by name with full signature.
         *
         * <p>
         * This is the most explicit method resolution strategy, providing the method
         * name, return type, and all parameter types. This is necessary for resolving
         * overloaded methods unambiguously.
         * </p>
         *
         * @param methodName     the name of the method
         * @param returnType     the expected return type
         * @param parameterTypes the parameter types (in order)
         * @return this builder instance for method chaining
         * @throws DslException if no method matches the signature or if multiple
         *                      matches
         *                      are found
         */
        Builder method(String methodName,
                        Class<ExecutionReturn> returnType, Class<?>... parameterTypes) throws DslException;
        
        
        Builder mutex(ISupplierBuilder<? extends IMutex, ? extends ISupplier<? extends IMutex>> mutex)
                        throws DslException;

}
