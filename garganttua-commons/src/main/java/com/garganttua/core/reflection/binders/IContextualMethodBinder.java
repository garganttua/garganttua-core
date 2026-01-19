package com.garganttua.core.reflection.binders;

import java.util.Optional;

import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.supply.SupplyException;

/**
 * Context-aware method binder for reflective method invocation with runtime
 * context resolution.
 *
 * <p>
 * {@code IContextualMethodBinder} combines {@link IMethodBinder} and
 * {@link IContextualExecutableBinder} to enable method invocation where
 * parameters
 * are resolved from a runtime context. This is particularly useful in
 * dependency
 * injection scenarios where method parameters represent dependencies that
 * should
 * be supplied by the container at invocation time.
 * </p>
 *
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * // Lifecycle method requiring dependencies from InjectionContext
 * IContextualMethodBinder<Void, InjectionContext> initMethod =
 *     ContextualMethodBinder
 *         .forClass(UserService.class)
 *         .method("initialize")
 *         .withParameter(Logger.class)      // Resolved from InjectionContext
 *         .withParameter(Config.class)      // Resolved from InjectionContext
 *         .build();
 *
 * // Execute with context
 * InjectionContext context = ...;
 * initMethod.execute(context);
 *
 * // Request handler method with multiple contexts
 * IContextualMethodBinder<Response, RequestScope> handler =
 *     ContextualMethodBinder
 *         .forInstance(controller)
 *         .method("handlePost")
 *         .withParameter(HttpRequest.class) // From RequestScope
 *         .withParameter(Session.class)     // From RequestScope
 *         .build();
 *
 * RequestScope scope = ...;
 * Optional<Response> response = handler.execute(scope);
 * }</pre>
 *
 * <h2>Method Resolution with Context</h2>
 * <p>
 * Unlike simple method binders that require explicit parameter values,
 * contextual
 * method binders resolve parameters dynamically at invocation time from the
 * provided
 * context(s). This enables late binding and allows the same binder to be reused
 * across different context instances.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * The binder itself is typically thread-safe if configured with immutable
 * settings.
 * However, thread safety of the actual invocation depends on the target method,
 * instance (if applicable), and context objects.
 * </p>
 *
 * @param <ExecutionReturned> the return type of the bound method
 * @param <OwnerContextType>  the type of the required owner context
 * @since 2.0.0-ALPHA01
 * @see IMethodBinder
 * @see IContextualExecutableBinder
 */
public interface IContextualMethodBinder<ExecutionReturned, OwnerContextType>
        extends IMethodBinder<ExecutionReturned>, IContextualExecutableBinder<ExecutionReturned, OwnerContextType> {
            
    @Override
    default Optional<IMethodReturn<ExecutionReturned>> supply() throws SupplyException {
        return this.execute();
    }

}
