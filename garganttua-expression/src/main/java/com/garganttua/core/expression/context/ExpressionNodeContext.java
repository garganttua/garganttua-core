package com.garganttua.core.expression.context;

import java.util.List;
import java.util.Objects;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.expression.IExpressionNode;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.IContextualSupplier;

/**
 * Default {@link IExpressionNodeContext} carrying the argument list supplied to a node factory.
 *
 * <p>Exposes the parameters and their inferred {@link IClass} types, reports whether the node must
 * be built as contextual (any parameter is an {@link IContextualSupplier}), and verifies that the
 * arguments are assignable to a factory's declared parameter types — treating {@code Object}-typed
 * and {@link com.garganttua.core.supply.ISupplier} (lazy) parameters as wildcards.
 */
public class ExpressionNodeContext implements IExpressionNodeContext {
    private static final Logger log = Logger.getLogger(ExpressionNodeContext.class);

    private List<Object> parameters;

    /**
     * Creates a node context wrapping the given argument values.
     *
     * @param parameters the argument values or {@link IExpressionNode} arguments (must not be {@code null})
     */
    public ExpressionNodeContext(List<Object> parameters) {
        this.parameters = Objects.requireNonNull(parameters, "Parameters list cannot be null");
    }

    @Override
    public boolean buildContextual() {
        return this.parameters.stream().anyMatch(s -> IContextualSupplier.class.isAssignableFrom(s.getClass()));
    }

    @Override
    public boolean matches(IClass<?>[] parameterTypes) {
        if (parameterTypes.length != parameters().size()) {
            log.debug("Expression leaf is expecting {} parameters, but context contains {}",
                    parameterTypes.length, parameters().size());
            return false;
        }

        for (int i = 0; i < parameterTypes.length; i++) {
            // If the factory expects an ISupplier (lazy parameter), accept any argument type
            if (parameterTypes[i].isAssignableFrom(com.garganttua.core.supply.ISupplier.class)) {
                // Lazy parameters accept any type - they'll be wrapped in a supplier
                continue;
            }

            if (parameters().get(i) instanceof IExpressionNode<?, ?> node) {
                // Object.class means the type is dynamic (e.g. variable references) - accept any target type
                IClass<?> nodeType = node.getFinalSuppliedClass();
                if (nodeType.getType() != Object.class
                        && !parameterTypes[i].isAssignableFrom(nodeType)) {
                    log.debug("Expression node is expecting parameter {} of type {} but context provided {}",
                            i, parameterTypes[i].getSimpleName(), nodeType.getSimpleName());
                    return false;
                }
            } else {
                if (!parameterTypes[i].isAssignableFrom(parameters().get(i).getClass())) {
                    log.debug("Expression node is expecting parameter {} of type {} but context provided {}",
                            i, parameterTypes[i].getSimpleName(), parameters().get(i).getClass().getSimpleName());
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public List<Object> parameters() {
        return this.parameters;
    }

    @Override
    public IClass<?>[] parameterTypes() {
        return this.parameters.stream().map(p -> {
            if (p instanceof IExpressionNode<?, ?> node) {
                return (IClass<?>) node.getFinalSuppliedClass();
            } else {
                return (IClass<?>) IClass.getClass(p.getClass());
            }
        }).toArray(IClass[]::new);
    }
}
