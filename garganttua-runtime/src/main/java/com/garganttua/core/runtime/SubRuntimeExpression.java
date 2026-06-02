package com.garganttua.core.runtime;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.IExpression;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

/**
 * Expression that executes a {@link SubRuntime} within the current runtime context.
 *
 * <p>
 * This is used to compile statement groups {@code (...)} as nested runtime executions.
 * The sub-runtime shares the parent context (variables, output, exceptions) but provides
 * optional function scope isolation: variables listed in {@code scopedVariableNames} are
 * saved before execution and restored afterwards, preventing definitions from leaking.
 * </p>
 *
 * <p>
 * The expression returns the current output value after the sub-runtime completes,
 * or null if no output was set.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 */
public class SubRuntimeExpression implements IExpression<Object, ISupplier<Object>> {
    private static final Logger log = Logger.getLogger(SubRuntimeExpression.class);

    private final SubRuntime<Object[], Object> subRuntime;
    private final Set<String> scopedVariableNames;

    /**
     * Creates a sub-runtime expression.
     *
     * @param subRuntime           the sub-runtime to execute
     * @param scopedVariableNames  variable names to save/restore for scope isolation
     *                             (typically function names defined inside the group)
     */
    public SubRuntimeExpression(SubRuntime<Object[], Object> subRuntime, Set<String> scopedVariableNames) {
        this.subRuntime = Objects.requireNonNull(subRuntime, "SubRuntime cannot be null");
        this.scopedVariableNames = Set.copyOf(Objects.requireNonNull(scopedVariableNames,
                "Scoped variable names cannot be null"));
    }

    /**
     * Creates a sub-runtime expression with no scoped variables.
     *
     * @param subRuntime the sub-runtime to execute
     */
    public SubRuntimeExpression(SubRuntime<Object[], Object> subRuntime) {
        this(subRuntime, Set.of());
    }

    @Override
    public ISupplier<Object> evaluate() throws ExpressionException {
        return new SubRuntimeSupplier();
    }

    @Override
    public Type getSuppliedType() {
        return Object.class;
    }

    @Override
    public IClass<Object> getSuppliedClass() {
        return IClass.getClass(Object.class);
    }

    @Override
    public boolean isContextual() {
        return false;
    }

    private class SubRuntimeSupplier implements ISupplier<Object> {

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public Optional<Object> supply() throws SupplyException {
            IRuntimeContext context = RuntimeExpressionContext.get();
            if (context == null) {
                throw new SupplyException("No runtime context available for sub-runtime execution");
            }

            // Save scoped variables for isolation
            Map<String, Optional<Object>> savedValues = new HashMap<>();
            IClass<Object> objectClass = IClass.getClass(Object.class);
            for (String name : scopedVariableNames) {
                savedValues.put(name, context.getVariable(name, objectClass));
            }

            try {
                // No need to defensively save/restore RuntimeExpressionContext:
                // ScopedValue scoping inside subRuntime.execute is lambda-local and
                // automatically restored on return (normal or exceptional).
                subRuntime.execute(context);
                Object output = context.getOutput();
                return Optional.ofNullable(output);
            } catch (Exception e) {
                throw new SupplyException("Sub-runtime execution failed", e);
            } finally {
                // Restore scoped variables
                for (var entry : savedValues.entrySet()) {
                    if (entry.getValue().isPresent()) {
                        context.setVariable(entry.getKey(), entry.getValue().get());
                    }
                }
            }
        }

        @Override
        public Type getSuppliedType() {
            return Object.class;
        }

        @Override
        public IClass<Object> getSuppliedClass() {
            return IClass.getClass(Object.class);
        }
    }
}
