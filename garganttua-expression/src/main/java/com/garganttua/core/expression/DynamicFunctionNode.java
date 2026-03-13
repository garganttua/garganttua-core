package com.garganttua.core.expression;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

import com.garganttua.core.expression.context.ExpressionVariableContext;
import com.garganttua.core.expression.context.IExpressionVariableResolver;
import com.garganttua.core.expression.context.IScriptFunction;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DynamicFunctionNode implements IExpressionNode<Object, ISupplier<Object>> {

    private final String functionName;
    private final List<IExpressionNode<?, ? extends ISupplier<?>>> argumentNodes;

    public DynamicFunctionNode(String functionName,
                                List<IExpressionNode<?, ? extends ISupplier<?>>> argumentNodes) {
        this.functionName = functionName;
        this.argumentNodes = argumentNodes;
    }

    @Override
    public ISupplier<Object> evaluate() throws ExpressionException {
        return new ISupplier<Object>() {
            @Override
            public Optional<Object> supply() throws SupplyException {
                IExpressionVariableResolver resolver = ExpressionVariableContext.get();
                if (resolver == null) {
                    throw new SupplyException("No variable resolver available for function " + functionName);
                }

                Optional<Object> funcObj = resolver.resolve(functionName, IClass.getClass(Object.class));
                if (funcObj.isEmpty()) {
                    throw new SupplyException("Undefined function: " + functionName);
                }

                Object func = funcObj.get();

                // If it's an ISupplier wrapping an IScriptFunction, unwrap it
                if (func instanceof ISupplier<?> supplier) {
                    func = supplier.supply().orElse(null);
                }

                if (!(func instanceof IScriptFunction scriptFunc)) {
                    throw new SupplyException("Variable '" + functionName
                            + "' is not a function (type: " + (func != null ? func.getClass().getSimpleName() : "null") + ")");
                }

                // Evaluate arguments
                Object[] args = new Object[argumentNodes.size()];
                for (int i = 0; i < argumentNodes.size(); i++) {
                    ISupplier<?> argSupplier = Expression.evaluateNode(argumentNodes.get(i));
                    args[i] = argSupplier.supply().orElse(null);
                }

                log.atTrace().log("Invoking dynamic function '{}' with {} args", functionName, args.length);
                Object result = scriptFunc.invoke(args);
                return Optional.ofNullable(result);
            }

            @Override
            public Type getSuppliedType() {
                return Object.class;
            }

            @Override
            public IClass<Object> getSuppliedClass() {
                return IClass.getClass(Object.class);
            }
        };
    }

    @Override
    public IClass<Object> getFinalSuppliedClass() {
        return IClass.getClass(Object.class);
    }

    @Override
    public Type getSuppliedType() {
        return Object.class;
    }
}
