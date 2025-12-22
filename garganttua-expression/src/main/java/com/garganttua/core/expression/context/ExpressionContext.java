package com.garganttua.core.expression.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import com.garganttua.core.expression.Expression;
import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.IExpression;
import com.garganttua.core.expression.IExpressionNode;
import com.garganttua.core.expression.antlr4.ExpressionLexer;
import com.garganttua.core.expression.antlr4.ExpressionParser;
import com.garganttua.core.supply.ISupplier;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor
public class ExpressionContext implements IExpressionContext {

    private Map<String, IExpressionNodeFactory<?, ? extends ISupplier<?>>> nodeFactories;

    public ExpressionContext(Set<IExpressionNodeFactory<?, ? extends ISupplier<?>>> nodeFactories) {
        log.atTrace().log("Entering ExpressionContext constructor");
        Objects.requireNonNull(nodeFactories, "Node Factories set cannot be null");

        // Convert to map with merge function to handle duplicates
        // Duplicates can occur when the same method is registered multiple times
        // (e.g., through auto-detection and manual registration)
        this.nodeFactories = nodeFactories.stream()
                .collect(Collectors.toMap(
                        IExpressionNodeFactory::key,
                        ef -> ef ,
                        (existing, duplicate) -> {
                            log.atWarn().log("Duplicate factory key detected: {}. Keeping first factory, ignoring duplicate.",
                                    existing.key());
                            return existing;  // Keep the first factory, ignore duplicates
                        }
                ));

        log.atInfo().log("ExpressionContext initialized with {} unique node factories (from {} total provided)",
                this.nodeFactories.size(), nodeFactories.size());
        log.atTrace().log("Exiting ExpressionContext constructor");
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

            log.atInfo().log("Expression parsed successfully: {}", expressionString);
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
        final int[] index = {1};
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
        List<Map.Entry<String, IExpressionNodeFactory<?, ? extends ISupplier<?>>>> sortedFactories =
                this.nodeFactories.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .toList();

        // Check if index is in bounds (1-based index)
        if (index > sortedFactories.size()) {
            log.atWarn().log("Index {} out of bounds. Total factories: {}", index, sortedFactories.size());
            return null;
        }

        // Get factory at index (convert from 1-based to 0-based)
        Map.Entry<String, IExpressionNodeFactory<?, ? extends ISupplier<?>>> entry =
                sortedFactories.get(index - 1);

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
        public IExpressionNode<?, ? extends ISupplier<?>> visitFunctionCall(ExpressionParser.FunctionCallContext ctx) {
            String functionName = ctx.IDENTIFIER().getText();
            log.atTrace().log("Visiting function call: {}", functionName);
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
                log.atError().log("Unknown function: {}", functionKey);
                throw new ExpressionException("Unknown function: " + functionKey);
            }

            log.atDebug().log("Creating node for function: {}", functionKey);
            // Create expression node context with IExpressionNode instances
            ExpressionNodeContext context = new ExpressionNodeContext(arguments);
            Optional<? extends IExpressionNode<?, ? extends ISupplier<?>>> node = factory.supply(context);

            return node
                    .orElseThrow(() -> new ExpressionException("Failed to create node for function: " + functionKey));
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
            Optional<? extends IExpressionNode<?, ? extends ISupplier<?>>> node = factory.supply(context);

            return node.orElseThrow(() -> new ExpressionException("Failed to create array/list node"));
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
         */
        private String getFullClassName(ExpressionParser.ClassTypeContext ctx) {
            if (ctx.className() != null) {
                // Build the full class name from identifiers
                StringBuilder className = new StringBuilder();
                for (int i = 0; i < ctx.className().IDENTIFIER().size(); i++) {
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

            if (factory == null) {
                throw new ExpressionException("Function not found: " + functionKey);
            }

            // Create context with actual parameter values
            List<Object> paramList = List.of(params);
            ExpressionNodeContext context = new ExpressionNodeContext(paramList);
            Optional<? extends IExpressionNode<?, ? extends ISupplier<?>>> node = factory.supply(context);

            return node.orElseThrow(() -> new ExpressionException("Failed to create node for: " + functionKey));
        }

        /**
         * Builds a function key for direct parameters in the format "functionName(Type1,Type2,...)".
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
    }
}
