package com.garganttua.core.reflection.binders;

import java.util.Optional;

import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.SupplyException;

/**
 * Context-aware executable binder for methods and constructors that require
 * runtime context.
 *
 * <p>
 * {@code IContextualExecutableBinder} extends {@link IExecutableBinder} to
 * support
 * context-dependent execution. This is essential for dependency injection
 * scenarios
 * where method or constructor parameters must be resolved from a runtime
 * context
 * (such as a DI container, request scope, or parent object). The binder
 * requires
 * an owner context and optionally accepts additional contexts for parameter
 * resolution.
 * </p>
 *
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * // Constructor binder requiring InjectionContext for parameter resolution
 * IContextualExecutableBinder<UserService, InjectionContext> constructor =
 *     ContextualConstructorBinder
 *         .forClass(UserService.class)
 *         .withParameter(UserRepository.class)  // Resolved from context
 *         .withParameter(EmailService.class)    // Resolved from context
 *         .build();
 *
 * // Execute with context
 * InjectionContext context = ...;
 * Optional<UserService> service = constructor.execute(context);
 *
 * // Method binder with context
 * IContextualExecutableBinder<Void, RequestScope> methodBinder =
 *     ContextualMethodBinder
 *         .forClass(Controller.class)
 *         .method("handleRequest")
 *         .withParameter(HttpRequest.class)  // From context
 *         .build();
 *
 * RequestScope scope = ...;
 * methodBinder.execute(scope);
 * }</pre>
 *
 * <h2>Context Resolution</h2>
 * <p>
 * The owner context is the primary context required for execution. Additional
 * contexts can be provided to resolve complex parameter hierarchies. The binder
 * implementation is responsible for matching parameter types to available
 * contexts
 * and extracting the required values.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Thread safety depends on the implementation and the provided contexts. Ensure
 * that context objects are thread-safe if the binder is used concurrently.
 * </p>
 *
 * @param <ExecutionReturn>  the return type of the executable element
 * @param <OwnerContextType> the type of the required owner context
 * @since 2.0.0-ALPHA01
 * @see IExecutableBinder
 * @see IContextualMethodBinder
 * @see IContextualConstructorBinder
 */
public interface IContextualExecutableBinder<ExecutionReturn, OwnerContextType>
                extends IExecutableBinder<ExecutionReturn>, IContextualSupplier<ExecutionReturn, OwnerContextType> {

        /**
         * Returns the required owner context type for this binder.
         *
         * <p>
         * This method declares the primary context type required for execution.
         * The framework uses this information to match and provide the appropriate
         * context at runtime.
         * </p>
         *
         * @return the {@link Class} object representing the required context type
         */
        Class<OwnerContextType> getOwnerContextType();

        /**
         * Returns the context types required for each parameter.
         *
         * <p>
         * This method provides an array of context types corresponding to each
         * parameter of the bound method or constructor. Each element indicates
         * the context type from which the corresponding parameter should be resolved.
         * A {@code null} value or {@code Object.class} typically indicates that
         * the parameter will be resolved from the owner context.
         * </p>
         *
         * @return an array of parameter context types (never {@code null})
         */
        Class<?>[] getParametersContextTypes();

        /**
         * Executes the bound method or constructor using the provided contexts.
         *
         * <p>
         * This method invokes the underlying executable element, resolving parameters
         * from the provided owner context and optional additional contexts. The
         * implementation matches parameter types to available contexts and extracts
         * the required values for execution.
         * </p>
         *
         * @param ownerContext the primary context required for execution (never
         *                     {@code null})
         * @param contexts     additional optional contexts for parameter resolution
         * @return an {@link Optional} containing the execution result, or empty for
         *         void
         *         methods or when no result is produced
         * @throws ReflectionException if the execution fails due to illegal access,
         *                             invocation target exceptions, parameter
         *                             resolution
         *                             failures, or instantiation errors
         */
        Optional<ExecutionReturn> execute(OwnerContextType ownerContext, Object... contexts)
                        throws ReflectionException;

        /**
         * Throws an exception indicating that context is required.
         *
         * <p>
         * This default implementation ensures that contextual executable binders
         * cannot be executed without providing the required context. Callers must
         * use {@link #execute(Object, Object...)} instead.
         * </p>
         *
         * @return never returns normally
         * @throws ReflectionException always, indicating that context is required
         */
        @Override
        default Optional<ExecutionReturn> execute() throws ReflectionException {
                if (getOwnerContextType() != Void.class)
                        throw new SupplyException("Owner context of type " + getOwnerContextType().getSimpleName()
                                        + " required for this supplier");

                return execute(null);
        }

        @Override
        default Optional<ExecutionReturn> supply() throws SupplyException {
                return this.execute();
        }

}
