package com.garganttua.core.script.context;

import java.util.Optional;

import com.garganttua.core.expression.ForLoopExpressionNode;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.runtime.IRuntimeContext;
import com.garganttua.core.runtime.RuntimeExpressionContext;

/**
 * Bridges script variable references to the runtime context.
 *
 * <p>
 * Resolves special script variables:
 * <ul>
 *   <li>{@code $0, $1, ...} — positional arguments from the runtime input</li>
 *   <li>{@code code} — the current exit code</li>
 *   <li>{@code output} — the current output value</li>
 *   <li>All other names — runtime context variables</li>
 * </ul>
 *
 * <p>
 * This resolver reads the current {@link IRuntimeContext} from
 * {@link RuntimeExpressionContext} at resolution time, so it does not need
 * a context reference at construction time.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 */
public class ScriptVariableResolver implements ForLoopExpressionNode.VariableSettableResolver {

    private static final IClass<Object> OBJECT_CLASS = IClass.getClass(Object.class);
    private static final IClass<Integer> INTEGER_CLASS = IClass.getClass(Integer.class);

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> resolve(String name, IClass<T> type) {
        IRuntimeContext<?, ?> context = RuntimeExpressionContext.get();
        if (context == null) {
            return Optional.empty();
        }

        // Positional arguments: $0, $1, $2, ...
        if (name.startsWith("$")) {
            try {
                int index = Integer.parseInt(name.substring(1));
                Object input = context.getInput().orElse(null);
                if (input instanceof Object[] args && index >= 0 && index < args.length) {
                    Object value = args[index];
                    if (value == null) {
                        return Optional.empty();
                    }
                    if (type.isInstance(value)) {
                        return Optional.of(type.cast(value));
                    }
                    if (type.equals(OBJECT_CLASS)) {
                        return Optional.of((T) value);
                    }
                }
                return Optional.empty();
            } catch (NumberFormatException e) {
                // Not a positional arg, fall through
            }
        }

        // Special: @code
        if ("code".equals(name)) {
            Optional<Integer> code = context.getCode();
            if (code.isPresent() && type.isAssignableFrom(INTEGER_CLASS)) {
                return Optional.of((T) code.get());
            }
            return Optional.empty();
        }

        // Special: @output
        if ("output".equals(name)) {
            Object output = context.getOutput();
            if (output != null && type.isInstance(output)) {
                return Optional.of(type.cast(output));
            }
            if (output != null && type.equals(OBJECT_CLASS)) {
                return Optional.of((T) output);
            }
            return Optional.empty();
        }

        // All other variables
        return context.getVariable(name, type);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void setVariable(String name, Object value) {
        IRuntimeContext context = RuntimeExpressionContext.get();
        if (context == null) {
            return;
        }

        if ("output".equals(name)) {
            if (value != null) {
                context.setOutput(value);
            }
            return;
        }
        context.setVariable(name, value);
    }
}
