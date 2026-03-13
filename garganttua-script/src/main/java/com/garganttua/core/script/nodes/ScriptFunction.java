package com.garganttua.core.script.nodes;

import java.util.List;

import com.garganttua.core.expression.context.IScriptFunction;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.runtime.IRuntimeContext;
import com.garganttua.core.runtime.RuntimeExpressionContext;
import com.garganttua.core.script.ScriptException;

/**
 * Runtime representation of a user-defined function.
 *
 * <p>Created from a {@link FunctionDefNode} during group execution, this object
 * is stored as a variable in the runtime context and invoked when the
 * expression engine resolves a dynamic function call matching this name.
 *
 * <p>Parameter scope isolation: before invocation, current values of parameter
 * variables are saved. Parameters are bound to the argument values, the body
 * block is executed, and the saved values are restored afterwards. This ensures
 * that function parameters do not overwrite caller variables with the same name.
 *
 * @see FunctionDefNode
 * @see com.garganttua.core.expression.context.IScriptFunction
 */
public class ScriptFunction implements IScriptFunction {

    private final String name;
    private final List<String> parameterNames;
    private final StatementBlock body;

    public ScriptFunction(String name, List<String> parameterNames, StatementBlock body) {
        this.name = name;
        this.parameterNames = List.copyOf(parameterNames);
        this.body = body;
    }

    @Override
    public List<String> parameters() {
        return this.parameterNames;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object invoke(Object... args) {
        if (args.length != parameterNames.size()) {
            throw new ScriptException("Function '" + name + "' expects " + parameterNames.size()
                    + " arguments but got " + args.length);
        }

        IRuntimeContext<Object[], Object> context = RuntimeExpressionContext.get();
        if (context == null) {
            throw new ScriptException("No runtime context available for function '" + name + "'");
        }

        // Save current parameter values to restore after (scope isolation)
        IClass<Object> objectClass = IClass.getClass(Object.class);
        Object[] savedValues = new Object[parameterNames.size()];
        boolean[] hadValue = new boolean[parameterNames.size()];
        for (int i = 0; i < parameterNames.size(); i++) {
            var existing = context.getVariable(parameterNames.get(i), objectClass);
            hadValue[i] = existing.isPresent();
            savedValues[i] = existing.orElse(null);
        }

        try {
            // Bind parameters as variables
            for (int i = 0; i < parameterNames.size(); i++) {
                context.setVariable(parameterNames.get(i), args[i]);
            }

            // Execute body
            return body.execute();
        } finally {
            // Restore previous values (scope isolation)
            for (int i = 0; i < parameterNames.size(); i++) {
                if (hadValue[i]) {
                    context.setVariable(parameterNames.get(i), savedValues[i]);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "function " + name + "(" + String.join(", ", parameterNames) + ")";
    }
}
