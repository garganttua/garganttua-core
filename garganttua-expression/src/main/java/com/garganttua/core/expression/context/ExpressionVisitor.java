package com.garganttua.core.expression.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


import com.garganttua.core.observability.Logger;
import com.garganttua.core.expression.Expression;
import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.ExpressionNode;
import com.garganttua.core.expression.ForLoopExpressionNode;
import com.garganttua.core.expression.IEvaluateNode;
import com.garganttua.core.expression.IExpression;
import com.garganttua.core.expression.IExpressionNode;
import com.garganttua.core.expression.antlr4.ExpressionParser;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.ISupplier;

/**
 * ANTLR visitor that builds {@link IExpressionNode} trees from a parsed expression.
 * Promoted from a nested class of {@link ExpressionContext} so that type stays focused
 * on registration/lookup; this visitor owns the parse-tree-to-node translation.
 */
final class ExpressionVisitor extends com.garganttua.core.expression.antlr4.ExpressionBaseVisitor<IExpressionNode<?, ? extends ISupplier<?>>> {

    private static final Logger log = Logger.getLogger(ExpressionVisitor.class);


    private final Map<String, IExpressionNodeFactory<?, ? extends ISupplier<?>>> nodeFactories;
    private final Map<String, IClass<?>> variableTypes;
    private final boolean dynamicFunctionsEnabled;
    private final FactoryKeyResolver keys;

    public ExpressionVisitor(Map<String, IExpressionNodeFactory<?, ? extends ISupplier<?>>> nodeFactories,
            Map<String, IClass<?>> variableTypes, boolean dynamicFunctionsEnabled) {
        this.nodeFactories = nodeFactories;
        this.variableTypes = variableTypes;
        this.dynamicFunctionsEnabled = dynamicFunctionsEnabled;
        this.keys = new FactoryKeyResolver(nodeFactories);
    }

    @Override
    public IExpressionNode<?, ? extends ISupplier<?>> visitRoot(ExpressionParser.RootContext ctx) {
        log.trace("Visiting root node");
        return visit(ctx.expression());
    }

    @Override
    public IExpressionNode<?, ? extends ISupplier<?>> visitExpression(ExpressionParser.ExpressionContext ctx) {
        log.trace("Visiting expression node");
        if (ctx.functionCall() != null) {
            log.debug("Expression is a function call");
            return visit(ctx.functionCall());
        } else if (ctx.literal() != null) {
            log.debug("Expression is a literal");
            return visit(ctx.literal());
        } else if (ctx.variableReference() != null) {
            log.debug("Expression is a variable reference");
            return visit(ctx.variableReference());
        } else if (ctx.type() != null) {
            log.debug("Expression is a type");
            return visit(ctx.type());
        } else if (ctx.IDENTIFIER() != null) {
            // Handle standalone identifier as a string literal
            log.debug("Expression is an identifier: {}", ctx.IDENTIFIER().getText());
            return createNode("string", ctx.IDENTIFIER().getText());
        }
        log.error("Unknown expression type in context: {}", ctx.getText());
        throw new ExpressionException("Unknown expression type");
    }

    @Override
    public IExpressionNode<?, ? extends ISupplier<?>> visitVariableReference(
            ExpressionParser.VariableReferenceContext ctx) {
        // Handle @IDENTIFIER (variable), @INT_LITERAL (argument index), and .IDENTIFIER (eager evaluation)
        final String varName;
        final boolean eagerEval;

        String text = ctx.getText();
        if (text.startsWith(".") && ctx.IDENTIFIER() != null) {
            // Eager evaluation: .varName - evaluate stored expression immediately
            varName = ctx.IDENTIFIER().getText();
            eagerEval = true;
            log.debug("Visiting eager variable reference: .{}", varName);
        } else if (ctx.IDENTIFIER() != null) {
            varName = ctx.IDENTIFIER().getText();
            eagerEval = false;
            log.debug("Visiting variable reference: @{}", varName);
        } else if (ctx.INT_LITERAL() != null) {
            // Argument index - prefix with "$" to distinguish from variables
            varName = "$" + ctx.INT_LITERAL().getText();
            eagerEval = false;
            log.debug("Visiting argument reference: @{}", ctx.INT_LITERAL().getText());
        } else {
            throw new ExpressionException("Invalid variable reference: " + ctx.getText());
        }

        String nodeName = eagerEval ? "." + varName : "@" + varName;

        // Look up registered type for this variable, default to Object.class
        final IClass<?> resolvedIClass = variableTypes.getOrDefault(varName, IClass.getClass(Object.class));
        if (resolvedIClass.getType() != Object.class) {
            log.debug("Using registered type {} for variable {}", resolvedIClass.getName(), nodeName);
        }

        @SuppressWarnings({ "rawtypes" })
        ExpressionNode node = new ExpressionNode(nodeName, (IEvaluateNode) (params) -> {
            return new ISupplier<Object>() {
                @Override
                public java.util.Optional<Object> supply() throws com.garganttua.core.supply.SupplyException {
                    IExpressionVariableResolver resolver = ExpressionVariableContext.get();
                    if (resolver == null) {
                        throw new com.garganttua.core.supply.SupplyException(
                                "No variable resolver available for " + nodeName);
                    }
                    java.util.Optional<Object> resolved = resolver.resolve(varName, IClass.getClass(Object.class));

                    if (eagerEval && resolved.isPresent()) {
                        Object value = resolved.get();
                        // If value is a supplier (stored expression), evaluate it
                        if (value instanceof ISupplier<?> supplier) {
                            log.trace("Eager evaluating supplier for .{}", varName);
                            return supplier.supply().map(r -> (Object) r);
                        }
                        // If value is an IExpression, evaluate it
                        if (value instanceof IExpression<?, ?> expr) {
                            log.trace("Eager evaluating expression for .{}", varName);
                            ISupplier<?> supplier = expr.evaluate();
                            return supplier.supply().map(r -> (Object) r);
                        }
                    }
                    return resolved;
                }

                @Override
                public java.lang.reflect.Type getSuppliedType() {
                    return resolvedIClass.getType();
                }

                @SuppressWarnings("unchecked")
                @Override
                public IClass<Object> getSuppliedClass() {
                    return (IClass<Object>) (IClass<?>) resolvedIClass;
                }
            };
        }, (IClass) resolvedIClass);
        return node;
    }

    @Override
    public IExpressionNode<?, ? extends ISupplier<?>> visitFunctionCall(ExpressionParser.FunctionCallContext ctx) {
        String text = ctx.getText();

        // Check if it starts with ':' (method call or constructor)
        if (text.startsWith(":")) {
            if (ctx.IDENTIFIER() != null) {
                // Case: :methodName(args) - instance or static method call
                return visitMethodCall(ctx);
            } else {
                // Case: :(args) - constructor call
                return visitConstructorCall(ctx);
            }
        }

        // Case: functionName(args) - classic function call
        String functionName = ctx.IDENTIFIER().getText();
        log.trace("Visiting function call: {}", functionName);

        // Special handling for 'for' loop expression
        if ("for".equals(functionName)) {
            return visitForLoop(ctx);
        }

        List<Object> arguments = new ArrayList<>();

        if (ctx.arguments() != null) {
            log.debug("Processing {} arguments for function {}", ctx.arguments().expression().size(),
                    functionName);
            for (ExpressionParser.ExpressionContext argCtx : ctx.arguments().expression()) {
                IExpressionNode<?, ? extends ISupplier<?>> argNode = visit(argCtx);
                arguments.add(argNode);
            }
        } else {
            log.debug("No arguments for function {}", functionName);
        }

        // Build function key with parameter types (IExpressionNode instances)
        String functionKey = keys.buildNodeKey(functionName, arguments);

        IExpressionNodeFactory<?, ? extends ISupplier<?>> factory = nodeFactories.get(functionKey);

        if (factory == null) {
            // Fallback: search for a factory with matching name and arity when Object types are involved
            factory = keys.findCompatibleFactory(functionName, arguments);
        }

        if (factory == null) {
            if (dynamicFunctionsEnabled && !keys.hasRegisteredFunction(functionName)) {
                // Fallback: create a DynamicFunctionNode that resolves the function
                // from runtime variables (supports user-defined script functions).
                // Only if no registered factory exists with this name (avoids masking type mismatches).
                log.debug("No registered factory for '{}', creating dynamic function node", functionName);
                List<IExpressionNode<?, ? extends ISupplier<?>>> argNodes = new ArrayList<>();
                for (Object arg : arguments) {
                    argNodes.add((IExpressionNode<?, ? extends ISupplier<?>>) arg);
                }
                return new com.garganttua.core.expression.DynamicFunctionNode(functionName, argNodes);
            }
            throw new ExpressionException("Unknown function: " + functionKey);
        }

        log.debug("Creating node for function: {}", functionKey);
        // Create expression node context with IExpressionNode instances
        ExpressionNodeContext context = new ExpressionNodeContext(arguments);
        return factory.supply(context)
                .flatMap(methodReturn -> methodReturn.firstOptional())
                .orElseThrow(() -> new ExpressionException("Failed to create node for function: " + functionKey));
    }

    /**
     * Handles method calls: :methodName(target, args...)
     * - If first argument is a Class<?>, it's a static method call
     * - Otherwise, it's an instance method call on the first argument
     */
    private IExpressionNode<?, ? extends ISupplier<?>> visitForLoop(ExpressionParser.FunctionCallContext ctx) {
        if (ctx.arguments() == null || ctx.arguments().expression().size() != 4) {
            throw new ExpressionException("for() requires 4 arguments: for(\"varName\", updateExpr, conditionExpr, bodyExpr)");
        }
        List<ExpressionParser.ExpressionContext> args = ctx.arguments().expression();
        // First arg: variable name (must be a string literal)
        IExpressionNode<?, ? extends ISupplier<?>> varNameNode = visit(args.get(0));
        ISupplier<?> varNameSupplier = Expression.evaluateNode(varNameNode);
        Object varNameObj = varNameSupplier.supply().orElse(null);
        if (!(varNameObj instanceof String varName)) {
            throw new ExpressionException("for() first argument must be a string (variable name)");
        }
        // Remaining args: update, condition, body - kept as expression nodes for re-evaluation
        IExpressionNode<?, ? extends ISupplier<?>> updateNode = visit(args.get(1));
        IExpressionNode<?, ? extends ISupplier<?>> conditionNode = visit(args.get(2));
        IExpressionNode<?, ? extends ISupplier<?>> bodyNode = visit(args.get(3));
        return new ForLoopExpressionNode(varName, updateNode, conditionNode, bodyNode);
    }

    private IExpressionNode<?, ? extends ISupplier<?>> visitMethodCall(ExpressionParser.FunctionCallContext ctx) {
        String methodName = ctx.IDENTIFIER().getText();
        log.trace("Visiting method call: {}", methodName);

        List<Object> arguments = new ArrayList<>();
        if (ctx.arguments() != null) {
            for (ExpressionParser.ExpressionContext argCtx : ctx.arguments().expression()) {
                IExpressionNode<?, ? extends ISupplier<?>> argNode = visit(argCtx);
                arguments.add(argNode);
            }
        }

        String functionKey = keys.buildNodeKey(":"+methodName, arguments);
        IExpressionNodeContext context = new ExpressionNodeContext(arguments.subList(1, arguments.size()));

        IExpressionNodeFactory<?, ?> factory = new MethodCallExpressionNodeFactory<>((IExpressionNode<?, ?>) arguments.get(0), methodName, context.parameterTypes());
        return factory.supply(context)
                .flatMap(methodReturn -> methodReturn.firstOptional())
                .orElseThrow(() -> new ExpressionException("Failed to create node for function: " + functionKey));
    }

    /**
     * Handles constructor calls: :(ClassName, args...)
     * The first argument must be a Class<?> representing the class to instantiate
     */
    private IExpressionNode<?, ? extends ISupplier<?>> visitConstructorCall(
            ExpressionParser.FunctionCallContext ctx) {
        log.trace("Visiting constructor call");

        List<Object> arguments = new ArrayList<>();
        if (ctx.arguments() != null) {
            for (ExpressionParser.ExpressionContext argCtx : ctx.arguments().expression()) {
                IExpressionNode<?, ? extends ISupplier<?>> argNode = visit(argCtx);
                arguments.add(argNode);
            }
        }

        if (arguments.isEmpty()) {
            throw new ExpressionException("Constructor call requires at least a class argument");
        }

        // First argument is the class to instantiate
        IExpressionNode<?, ?> classNode = (IExpressionNode<?, ?>) arguments.get(0);

        // Remaining arguments are constructor parameters
        IExpressionNodeContext context = new ExpressionNodeContext(arguments.subList(1, arguments.size()));

        IExpressionNodeFactory<?, ?> factory = new ConstructorCallExpressionNodeFactory<>(
                classNode, context.parameterTypes());

        String functionKey = keys.buildNodeKey(":", arguments);
        return factory.supply(context)
                .flatMap(methodReturn -> methodReturn.firstOptional())
                .orElseThrow(() -> new ExpressionException("Failed to create node for constructor: " + functionKey));
    }

    @Override
    public IExpressionNode<?, ? extends ISupplier<?>> visitLiteral(ExpressionParser.LiteralContext ctx) {
        if (ctx.STRING() != null) {
            String value = ctx.STRING().getText();
            // Remove surrounding quotes
            value = value.substring(1, value.length() - 1);
            return createNode("string", value);
        } else if (ctx.CHAR() != null) {
            String value = ctx.CHAR().getText();
            // Extract character between single quotes
            value = value.substring(1, value.length() - 1);
            return createNode("char", value);
        } else if (ctx.INT_LITERAL() != null) {
            String value = ctx.INT_LITERAL().getText();
            return createNode("int", value);
        } else if (ctx.FLOAT_LIT() != null) {
            String value = ctx.FLOAT_LIT().getText();
            return createNode("double", value);
        } else if (ctx.BOOLEAN() != null) {
            String value = ctx.BOOLEAN().getText();
            return createNode("boolean", value);
        } else if (ctx.NULL() != null) {
            return createNode("null");
        } else if (ctx.arrayLiteral() != null) {
            return visit(ctx.arrayLiteral());
        } else if (ctx.objectLiteral() != null) {
            return visit(ctx.objectLiteral());
        }
        throw new ExpressionException("Unknown literal type");
    }

    @Override
    public IExpressionNode<?, ? extends ISupplier<?>> visitArrayLiteral(ExpressionParser.ArrayLiteralContext ctx) {
        List<Object> elements = new ArrayList<>();

        if (ctx.expression() != null) {
            for (ExpressionParser.ExpressionContext exprCtx : ctx.expression()) {
                elements.add(visit(exprCtx));
            }
        }

        // Use the "list" function to create array/list
        String functionKey = keys.buildNodeKey("list", elements);
        IExpressionNodeFactory<?, ? extends ISupplier<?>> factory = nodeFactories.get(functionKey);

        if (factory == null) {
            throw new ExpressionException("Array/List factory not found: " + functionKey);
        }

        ExpressionNodeContext context = new ExpressionNodeContext(elements);
        return factory.supply(context)
                .flatMap(methodReturn -> methodReturn.firstOptional())
                .orElseThrow(() -> new ExpressionException("Failed to create array/list node"));
    }

    @Override
    public IExpressionNode<?, ? extends ISupplier<?>> visitType(ExpressionParser.TypeContext ctx) {
        // Handle type expressions like Class<String>, int[], etc.

        // Check if it's a simple type (no array dimensions)
        if (ctx.arrayDims() == null && ctx.simpleType() != null) {
            ExpressionParser.SimpleTypeContext simpleType = ctx.simpleType();

            // Handle primitive types
            if (simpleType.primitiveType() != null) {
                String primitiveTypeName = simpleType.primitiveType().getText();
                return createNode("class", primitiveTypeName);
            }

            // Handle Class<Type> or Class<?>
            if (simpleType.classOfType() != null) {
                // For Class<Type> expressions, return Class.class
                return createNode("class", "java.lang.Class");
            }

            // Handle regular class types (e.g., java.lang.String, List<T>)
            if (simpleType.classType() != null) {
                String className = getFullClassName(simpleType.classType());
                return createNode("class", className);
            }
        }

        // For array types or other complex types, convert to string representation
        String typeString = ctx.getText();
        return createNode("string", typeString);
    }

    /**
     * Extracts the full class name from a classType context.
     * Handles the ".class" suffix convention (e.g., "String.class" -> "java.lang.String").
     */
    private String getFullClassName(ExpressionParser.ClassTypeContext ctx) {
        if (ctx.className() != null) {
            // Build the full class name from identifiers
            StringBuilder className = new StringBuilder();
            int size = ctx.className().IDENTIFIER().size();
            // If last identifier is "class", strip it (e.g., String.class -> String)
            int end = (size > 1 && "class".equals(ctx.className().IDENTIFIER(size - 1).getText()))
                    ? size - 1
                    : size;
            for (int i = 0; i < end; i++) {
                if (i > 0)
                    className.append(".");
                className.append(ctx.className().IDENTIFIER(i).getText());
            }
            return className.toString();
        }
        return ctx.getText();
    }

    /**
     * Creates a node by finding the appropriate factory and supplying parameters.
     * Parameters can be direct values (String, Integer, etc.) used by the node.
     */
    private IExpressionNode<?, ? extends ISupplier<?>> createNode(String functionName, Object... params) {
        // Build parameter type list
        Class<?>[] paramTypes = new Class<?>[params.length];
        for (int i = 0; i < params.length; i++) {
            paramTypes[i] = params[i].getClass();
        }

        String functionKey = keys.buildKey(functionName, paramTypes);

        IExpressionNodeFactory<?, ? extends ISupplier<?>> factory = nodeFactories.get(functionKey);

        // If exact match not found, try type-compatible match
        if (factory == null) {
            factory = keys.findCompatibleFactoryForDirectParams(functionName, paramTypes);
        }

        if (factory == null) {
            throw new ExpressionException("Function not found: " + functionKey);
        }

        // Create context with actual parameter values
        List<Object> paramList = List.of(params);
        ExpressionNodeContext context = new ExpressionNodeContext(paramList);
        return factory.supply(context)
                .flatMap(methodReturn -> methodReturn.firstOptional())
                .orElseThrow(() -> new ExpressionException("Failed to create node for: " + functionKey));
    }

    /**
     * Finds a compatible factory for direct parameters (not IExpressionNode arguments).
     */
}
