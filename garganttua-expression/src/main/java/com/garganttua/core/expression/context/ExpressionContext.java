package com.garganttua.core.expression.context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import com.garganttua.core.bootstrap.banner.IBootstrapSummaryContributor;
import com.garganttua.core.expression.Expression;
import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.ExpressionNode;
import com.garganttua.core.expression.ForLoopExpressionNode;
import com.garganttua.core.expression.IExpression;
import com.garganttua.core.expression.IExpressionNode;
import com.garganttua.core.expression.antlr4.ExpressionLexer;
import com.garganttua.core.expression.antlr4.ExpressionParser;
import com.garganttua.core.supply.ISupplier;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor
public class ExpressionContext implements IExpressionContext, IBootstrapSummaryContributor {

    private Map<String, IExpressionNodeFactory<?, ? extends ISupplier<?>>> nodeFactories = new ConcurrentHashMap<>();

    public ExpressionContext(Set<IExpressionNodeFactory<?, ? extends ISupplier<?>>> nodeFactories) {
        log.atTrace().log("Entering ExpressionContext constructor");
        Objects.requireNonNull(nodeFactories, "Node Factories set cannot be null");

        // Populate ConcurrentHashMap with merge function to handle duplicates
        // Duplicates can occur when the same method is registered multiple times
        // (e.g., through auto-detection and manual registration)
        for (IExpressionNodeFactory<?, ? extends ISupplier<?>> ef : nodeFactories) {
            this.nodeFactories.putIfAbsent(ef.key(), ef);
        }

        log.atDebug().log("ExpressionContext initialized with {} unique node factories (from {} total provided)",
                this.nodeFactories.size(), nodeFactories.size());
        log.atTrace().log("Exiting ExpressionContext constructor");
    }

    @Override
    public void register(String key, IExpressionNodeFactory<?, ? extends ISupplier<?>> factory) {
        log.atDebug().log("Registering expression factory with key: {}", key);
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(factory, "Factory cannot be null");
        this.nodeFactories.put(key, factory);
        log.atDebug().log("Expression factory registered: {}", key);
    }

    @Override
    public IExpression<?, ? extends ISupplier<?>> expression(String expressionString) {
        log.atTrace().log("Entering expression(expressionString={})", expressionString);
        log.atDebug().log("Parsing expression: {}", expressionString);

        Objects.requireNonNull(expressionString, "Expression string cannot be null");

        try {
            // Create ANTLR4 lexer and parser
            ExpressionLexer lexer = new ExpressionLexer(CharStreams.fromString(expressionString));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            ExpressionParser parser = new ExpressionParser(tokens);
            log.atDebug().log("ANTLR4 lexer and parser created");

            // Parse the expression starting from root rule
            ExpressionParser.RootContext rootContext = parser.root();
            log.atDebug().log("Expression parsed by ANTLR4");

            // Visit and build the expression tree
            ExpressionVisitor visitor = new ExpressionVisitor(this.nodeFactories);
            IExpressionNode<?, ? extends ISupplier<?>> rootNode = visitor.visit(rootContext);

            if (rootNode == null) {
                log.atError().log("Failed to parse expression: {}", expressionString);
                throw new ExpressionException("Failed to parse expression: " + expressionString);
            }

            log.atDebug().log("Expression parsed successfully: {}", expressionString);
            log.atTrace().log("Exiting expression");
            return new Expression<>(rootNode);

        } catch (Exception e) {
            String errorMsg = "Error parsing expression '" + expressionString + "': " + e.getMessage();
            log.atError().log(errorMsg, e);
            throw new ExpressionException(e);
        }
    }

    @Override
    public String man(String key) {
        log.atTrace().log("Entering man(key={})", key);
        log.atDebug().log("Looking up manual for expression node: {}", key);

        Objects.requireNonNull(key, "Key cannot be null");

        IExpressionNodeFactory<?, ? extends ISupplier<?>> factory = this.nodeFactories.get(key);

        if (factory == null) {
            log.atWarn().log("No expression node factory found for key: {}", key);
            return null;
        }

        String manual = factory.man();
        log.atDebug().log("Manual retrieved for key: {}", key);
        log.atTrace().log("Exiting man");

        return manual;
    }

    @Override
    public String man() {
        log.atTrace().log("Entering listFactories()");
        log.atDebug().log("Generating list of {} expression node factories", this.nodeFactories.size());

        StringBuilder list = new StringBuilder();

        // Header
        list.append("AVAILABLE EXPRESSION FUNCTIONS\n");
        list.append("==============================\n\n");
        list.append("Total functions: ").append(this.nodeFactories.size()).append("\n\n");

        // Sort factories by key for consistent output and track index
        final int[] index = { 1 };
        this.nodeFactories.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String key = entry.getKey();
                    IExpressionNodeFactory<?, ? extends ISupplier<?>> factory = entry.getValue();

                    // Format: [index] key - description
                    String indexStr = String.format("[%d]", index[0]++);
                    list.append("  ").append(indexStr).append(" ").append(key);

                    // Align descriptions (pad to 45 characters to account for index)
                    int totalLength = indexStr.length() + 1 + key.length();
                    int padding = Math.max(1, 45 - totalLength);
                    list.append(" ".repeat(padding));

                    list.append("- ").append(factory.description()).append("\n");
                });

        list.append("\n");
        list.append("Use man(\"key\") or man(index) to get detailed documentation for a specific function.\n");

        log.atDebug().log("Factory list generated");
        log.atTrace().log("Exiting listFactories");

        return list.toString();
    }

    @Override
    public String man(int index) {
        log.atTrace().log("Entering man(index={})", index);
        log.atDebug().log("Looking up manual for expression node at index: {}", index);

        if (index < 1) {
            log.atWarn().log("Invalid index: {}. Index must be >= 1", index);
            return null;
        }

        // Get sorted list of factories
        List<Map.Entry<String, IExpressionNodeFactory<?, ? extends ISupplier<?>>>> sortedFactories = this.nodeFactories
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .toList();

        // Check if index is in bounds (1-based index)
        if (index > sortedFactories.size()) {
            log.atWarn().log("Index {} out of bounds. Total factories: {}", index, sortedFactories.size());
            return null;
        }

        // Get factory at index (convert from 1-based to 0-based)
        Map.Entry<String, IExpressionNodeFactory<?, ? extends ISupplier<?>>> entry = sortedFactories.get(index - 1);

        String manual = entry.getValue().man();
        log.atDebug().log("Manual retrieved for index {} (key: {})", index, entry.getKey());
        log.atTrace().log("Exiting man");

        return manual;
    }

    /**
     * ANTLR4 Visitor for building expression trees from parsed Expression.
     */
    private static class ExpressionVisitor
            extends
            com.garganttua.core.expression.antlr4.ExpressionBaseVisitor<IExpressionNode<?, ? extends ISupplier<?>>> {

        private final Map<String, IExpressionNodeFactory<?, ? extends ISupplier<?>>> nodeFactories;

        public ExpressionVisitor(Map<String, IExpressionNodeFactory<?, ? extends ISupplier<?>>> nodeFactories) {
            this.nodeFactories = nodeFactories;
        }

        @Override
        public IExpressionNode<?, ? extends ISupplier<?>> visitRoot(ExpressionParser.RootContext ctx) {
            log.atTrace().log("Visiting root node");
            return visit(ctx.expression());
        }

        @Override
        public IExpressionNode<?, ? extends ISupplier<?>> visitExpression(ExpressionParser.ExpressionContext ctx) {
            log.atTrace().log("Visiting expression node");
            if (ctx.functionCall() != null) {
                log.atDebug().log("Expression is a function call");
                return visit(ctx.functionCall());
            } else if (ctx.literal() != null) {
                log.atDebug().log("Expression is a literal");
                return visit(ctx.literal());
            } else if (ctx.variableReference() != null) {
                log.atDebug().log("Expression is a variable reference");
                return visit(ctx.variableReference());
            } else if (ctx.type() != null) {
                log.atDebug().log("Expression is a type");
                return visit(ctx.type());
            } else if (ctx.IDENTIFIER() != null) {
                // Handle standalone identifier as a string literal
                log.atDebug().log("Expression is an identifier: {}", ctx.IDENTIFIER().getText());
                return createNode("string", ctx.IDENTIFIER().getText());
            }
            log.atError().log("Unknown expression type in context: {}", ctx.getText());
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
                log.atDebug().log("Visiting eager variable reference: .{}", varName);
            } else if (ctx.IDENTIFIER() != null) {
                varName = ctx.IDENTIFIER().getText();
                eagerEval = false;
                log.atDebug().log("Visiting variable reference: @{}", varName);
            } else if (ctx.INT_LITERAL() != null) {
                // Argument index - prefix with "$" to distinguish from variables
                varName = "$" + ctx.INT_LITERAL().getText();
                eagerEval = false;
                log.atDebug().log("Visiting argument reference: @{}", ctx.INT_LITERAL().getText());
            } else {
                throw new ExpressionException("Invalid variable reference: " + ctx.getText());
            }

            String nodeName = eagerEval ? "." + varName : "@" + varName;
            return new ExpressionNode<>(nodeName, (params) -> {
                return new ISupplier<Object>() {
                    @Override
                    public java.util.Optional<Object> supply() throws com.garganttua.core.supply.SupplyException {
                        IExpressionVariableResolver resolver = ExpressionVariableContext.get();
                        if (resolver == null) {
                            throw new com.garganttua.core.supply.SupplyException(
                                    "No variable resolver available for " + nodeName);
                        }
                        java.util.Optional<Object> resolved = resolver.resolve(varName, Object.class);

                        if (eagerEval && resolved.isPresent()) {
                            Object value = resolved.get();
                            // If value is a supplier (stored expression), evaluate it
                            if (value instanceof ISupplier<?> supplier) {
                                log.atTrace().log("Eager evaluating supplier for .{}", varName);
                                return supplier.supply().map(r -> (Object) r);
                            }
                            // If value is an IExpression, evaluate it
                            if (value instanceof IExpression<?, ?> expr) {
                                log.atTrace().log("Eager evaluating expression for .{}", varName);
                                ISupplier<?> supplier = expr.evaluate();
                                return supplier.supply().map(r -> (Object) r);
                            }
                        }
                        return resolved;
                    }

                    @Override
                    public java.lang.reflect.Type getSuppliedType() {
                        return Object.class;
                    }
                };
            }, Object.class);
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
            log.atTrace().log("Visiting function call: {}", functionName);

            // Special handling for 'for' loop expression
            if ("for".equals(functionName)) {
                return visitForLoop(ctx);
            }

            List<Object> arguments = new ArrayList<>();

            if (ctx.arguments() != null) {
                log.atDebug().log("Processing {} arguments for function {}", ctx.arguments().expression().size(),
                        functionName);
                for (ExpressionParser.ExpressionContext argCtx : ctx.arguments().expression()) {
                    IExpressionNode<?, ? extends ISupplier<?>> argNode = visit(argCtx);
                    arguments.add(argNode);
                }
            } else {
                log.atDebug().log("No arguments for function {}", functionName);
            }

            // Build function key with parameter types (IExpressionNode instances)
            String functionKey = buildNodeKey(functionName, arguments);

            IExpressionNodeFactory<?, ? extends ISupplier<?>> factory = nodeFactories.get(functionKey);

            if (factory == null) {
                // Fallback: search for a factory with matching name and arity when Object types are involved
                factory = findCompatibleFactory(functionName, arguments);
            }

            if (factory == null) {
                log.atError().log("Unknown function: {}", functionKey);
                throw new ExpressionException("Unknown function: " + functionKey);
            }

            log.atDebug().log("Creating node for function: {}", functionKey);
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
            log.atTrace().log("Visiting method call: {}", methodName);

            List<Object> arguments = new ArrayList<>();
            if (ctx.arguments() != null) {
                for (ExpressionParser.ExpressionContext argCtx : ctx.arguments().expression()) {
                    IExpressionNode<?, ? extends ISupplier<?>> argNode = visit(argCtx);
                    arguments.add(argNode);
                }
            }

            String functionKey = buildNodeKey(":"+methodName, arguments);
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
            log.atTrace().log("Visiting constructor call");

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

            String functionKey = buildNodeKey(":", arguments);
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
            String functionKey = buildNodeKey("list", elements);
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

            String functionKey = buildKey(functionName, paramTypes);

            IExpressionNodeFactory<?, ? extends ISupplier<?>> factory = nodeFactories.get(functionKey);

            // If exact match not found, try type-compatible match
            if (factory == null) {
                factory = findCompatibleFactoryForDirectParams(functionName, paramTypes);
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
        private IExpressionNodeFactory<?, ? extends ISupplier<?>> findCompatibleFactoryForDirectParams(
                String functionName, Class<?>[] argTypes) {
            String prefix = functionName + "(";
            int arity = argTypes.length;

            for (Map.Entry<String, IExpressionNodeFactory<?, ? extends ISupplier<?>>> entry : nodeFactories.entrySet()) {
                String key = entry.getKey();
                if (!key.startsWith(prefix)) continue;

                String paramPart = key.substring(prefix.length(), key.length() - 1);
                String[] paramTypeNames = paramPart.isEmpty() ? new String[0] : paramPart.split(",");
                int keyArity = paramTypeNames.length;

                if (keyArity != arity) continue;

                // Check if all argument types are assignable to factory parameter types
                boolean compatible = true;
                for (int i = 0; i < keyArity && compatible; i++) {
                    Class<?> factoryParamType = resolveSimpleTypeName(paramTypeNames[i].trim());
                    if (factoryParamType == null) {
                        compatible = false;
                    } else if (!factoryParamType.isAssignableFrom(argTypes[i])) {
                        if (!isPrimitiveCompatible(factoryParamType, argTypes[i])) {
                            compatible = false;
                        }
                    }
                }

                if (compatible) {
                    log.atDebug().log("Found compatible factory for direct params: {} for {}({})",
                            key, functionName, java.util.Arrays.toString(argTypes));
                    return entry.getValue();
                }
            }
            return null;
        }

        /**
         * Builds a function key for direct parameters in the format
         * "functionName(Type1,Type2,...)".
         */
        private String buildKey(String functionName, Class<?>[] paramTypes) {
            StringBuilder keyBuilder = new StringBuilder(functionName);
            keyBuilder.append("(");
            for (int i = 0; i < paramTypes.length; i++) {
                if (i > 0)
                    keyBuilder.append(",");
                keyBuilder.append(paramTypes[i].getSimpleName());
            }
            keyBuilder.append(")");
            String key = keyBuilder.toString();
            log.atDebug().log("Built key: {}", key);
            return key;
        }

        /**
         * Builds a function key in the format "functionName(Type1,Type2,...)".
         */
        private String buildNodeKey(String functionName, List<Object> arguments) {
            StringBuilder keyBuilder = new StringBuilder(functionName);
            keyBuilder.append("(");

            for (int i = 0; i < arguments.size(); i++) {
                if (i > 0)
                    keyBuilder.append(",");

                if (arguments.get(i) instanceof IExpressionNode<?, ?> node) {
                    keyBuilder.append(node.getFinalSuppliedClass().getSimpleName());
                } else {
                    keyBuilder.append(arguments.get(i).getClass().getSimpleName());
                }

            }

            keyBuilder.append(")");
            String key = keyBuilder.toString();
            log.atDebug().log("Built node key: {}", key);
            return key;
        }

        private IExpressionNodeFactory<?, ? extends ISupplier<?>> findCompatibleFactory(String functionName, List<Object> arguments) {
            String prefix = functionName + "(";
            int arity = arguments.size();

            // Extract argument types
            Class<?>[] argTypes = new Class<?>[arguments.size()];
            for (int i = 0; i < arguments.size(); i++) {
                if (arguments.get(i) instanceof IExpressionNode<?, ?> node) {
                    argTypes[i] = node.getFinalSuppliedClass();
                } else {
                    argTypes[i] = arguments.get(i).getClass();
                }
            }

            // Search for compatible factory with type matching
            // First pass: look for exact matches or narrowing (argType extends factoryParamType)
            IExpressionNodeFactory<?, ? extends ISupplier<?>> bestMatch = null;
            int bestScore = -1;

            for (Map.Entry<String, IExpressionNodeFactory<?, ? extends ISupplier<?>>> entry : nodeFactories.entrySet()) {
                String key = entry.getKey();
                if (!key.startsWith(prefix)) continue;

                String paramPart = key.substring(prefix.length(), key.length() - 1);
                String[] paramTypeNames = paramPart.isEmpty() ? new String[0] : paramPart.split(",");
                int keyArity = paramTypeNames.length;

                if (keyArity != arity) continue;

                // Check if all argument types are compatible with factory parameter types
                boolean compatible = true;
                int score = 0; // Higher score = better match
                for (int i = 0; i < keyArity && compatible; i++) {
                    String paramTypeName = paramTypeNames[i].trim();

                    // Special handling for ISupplier (lazy) parameters - they accept any type
                    if ("ISupplier".equals(paramTypeName)) {
                        // ISupplier parameters accept any argument type (they'll be wrapped lazily)
                        // Score 3 for ISupplier match (high priority for lazy evaluation)
                        score += 3;
                        continue;
                    }

                    Class<?> factoryParamType = resolveSimpleTypeName(paramTypeName);
                    if (factoryParamType == null) {
                        compatible = false;
                    } else if (argTypes[i] == Object.class) {
                        // Object type from variable reference - compatible with any parameter type
                        // Score 0 for Object (lowest priority)
                        score += 0;
                    } else if (factoryParamType.isAssignableFrom(argTypes[i])) {
                        // Exact match or argType is subtype of factoryParamType
                        // Score 2 for exact match, 1 for subtype
                        score += (factoryParamType == argTypes[i]) ? 2 : 1;
                    } else if (isPrimitiveCompatible(factoryParamType, argTypes[i])) {
                        // Primitive/wrapper compatibility
                        score += 2;
                    } else {
                        compatible = false;
                    }
                }

                if (compatible && score > bestScore) {
                    bestScore = score;
                    bestMatch = entry.getValue();
                    log.atDebug().log("Found compatible factory via type matching: {} (score={}) for {}({})",
                            key, score, functionName, java.util.Arrays.toString(argTypes));
                }
            }
            return bestMatch;
        }

        /**
         * Resolves a simple type name to its Class.
         * Handles primitives and common types using simple names.
         */
        private Class<?> resolveSimpleTypeName(String simpleName) {
            return switch (simpleName) {
                case "Object" -> Object.class;
                case "String" -> String.class;
                case "Integer", "int" -> Integer.class;
                case "Long", "long" -> Long.class;
                case "Double", "double" -> Double.class;
                case "Float", "float" -> Float.class;
                case "Boolean", "boolean" -> Boolean.class;
                case "Byte", "byte" -> Byte.class;
                case "Short", "short" -> Short.class;
                case "Character", "char" -> Character.class;
                case "Class" -> Class.class;
                case "Set" -> java.util.Set.class;
                case "List" -> java.util.List.class;
                case "Map" -> java.util.Map.class;
                case "Optional" -> java.util.Optional.class;
                case "BeanReference" -> com.garganttua.core.injection.BeanReference.class;
                default -> {
                    // Try to load class by simple name in common packages
                    try {
                        yield Class.forName("java.lang." + simpleName);
                    } catch (ClassNotFoundException e1) {
                        try {
                            yield Class.forName("java.util." + simpleName);
                        } catch (ClassNotFoundException e2) {
                            log.atTrace().log("Could not resolve type name: {}", simpleName);
                            yield null;
                        }
                    }
                }
            };
        }

        /**
         * Checks if the argument type is compatible with the parameter type
         * considering primitive/wrapper conversions.
         */
        private boolean isPrimitiveCompatible(Class<?> paramType, Class<?> argType) {
            if (paramType == int.class || paramType == Integer.class) {
                return argType == int.class || argType == Integer.class;
            }
            if (paramType == long.class || paramType == Long.class) {
                return argType == long.class || argType == Long.class;
            }
            if (paramType == double.class || paramType == Double.class) {
                return argType == double.class || argType == Double.class;
            }
            if (paramType == float.class || paramType == Float.class) {
                return argType == float.class || argType == Float.class;
            }
            if (paramType == boolean.class || paramType == Boolean.class) {
                return argType == boolean.class || argType == Boolean.class;
            }
            if (paramType == byte.class || paramType == Byte.class) {
                return argType == byte.class || argType == Byte.class;
            }
            if (paramType == short.class || paramType == Short.class) {
                return argType == short.class || argType == Short.class;
            }
            if (paramType == char.class || paramType == Character.class) {
                return argType == char.class || argType == Character.class;
            }
            return false;
        }
    }

    // --- IBootstrapSummaryContributor implementation ---

    @Override
    public String getSummaryCategory() {
        return "Expression Engine";
    }

    @Override
    public Map<String, String> getSummaryItems() {
        Map<String, String> items = new LinkedHashMap<>();
        int factoryCount = nodeFactories != null ? nodeFactories.size() : 0;
        items.put("Expression functions", String.valueOf(factoryCount));
        return items;
    }
}
