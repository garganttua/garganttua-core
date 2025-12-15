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

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExpressionContext implements IExpressionContext {

    private Map<String, IExpressionNodeFactory<?, ? extends ISupplier<?>>> nodeFactories;

    public ExpressionContext(Set<IExpressionNodeFactory<?, ? extends ISupplier<?>>> nodeFactories) {
        Objects.requireNonNull(nodeFactories, "Node Factories set cannot be null");
        this.nodeFactories = nodeFactories.stream().collect(Collectors.toMap(IExpressionNodeFactory::key, ef -> ef));
    }

    @Override
    public IExpression<?, ? extends ISupplier<?>> expression(String expressionString) {
        log.atDebug().log("Parsing expression: {}", expressionString);

        Objects.requireNonNull(expressionString, "Expression string cannot be null");

        try {
            // Create ANTLR4 lexer and parser
            ExpressionLexer lexer = new ExpressionLexer(CharStreams.fromString(expressionString));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            ExpressionParser parser = new ExpressionParser(tokens);

            // Parse the expression starting from root rule
            ExpressionParser.RootContext rootContext = parser.root();

            // Visit and build the expression tree
            ExpressionVisitor visitor = new ExpressionVisitor(this.nodeFactories);
            IExpressionNode<?, ? extends ISupplier<?>> rootNode = visitor.visit(rootContext);

            if (rootNode == null) {
                throw new ExpressionException("Failed to parse expression: " + expressionString);
            }

            log.atDebug().log("Expression parsed successfully");
            return new Expression<>(rootNode);

        } catch (Exception e) {
            String errorMsg = "Error parsing expression '" + expressionString + "': " + e.getMessage();
            log.atError().log(errorMsg, e);
            throw new ExpressionException(e);
        }
    }

    /**
     * ANTLR4 Visitor for building expression trees from parsed Expression.
     */
    private static class ExpressionVisitor
            extends com.garganttua.core.expression.antlr4.ExpressionBaseVisitor<IExpressionNode<?, ? extends ISupplier<?>>> {

        private final Map<String, IExpressionNodeFactory<?, ? extends ISupplier<?>>> nodeFactories;

        public ExpressionVisitor(Map<String, IExpressionNodeFactory<?, ? extends ISupplier<?>>> nodeFactories) {
            this.nodeFactories = nodeFactories;
        }

        @Override
        public IExpressionNode<?, ? extends ISupplier<?>> visitRoot(ExpressionParser.RootContext ctx) {
            return visit(ctx.expression());
        }

        @Override
        public IExpressionNode<?, ? extends ISupplier<?>> visitExpression(ExpressionParser.ExpressionContext ctx) {
            if (ctx.functionCall() != null) {
                return visit(ctx.functionCall());
            } else if (ctx.literal() != null) {
                return visit(ctx.literal());
            } else if (ctx.type() != null) {
                return visit(ctx.type());
            } else if (ctx.IDENTIFIER() != null) {
                // Handle standalone identifier as a string literal
                return createLeafNode("string", ctx.IDENTIFIER().getText());
            }
            throw new ExpressionException("Unknown expression type");
        }

        @Override
        public IExpressionNode<?, ? extends ISupplier<?>> visitFunctionCall(ExpressionParser.FunctionCallContext ctx) {
            String functionName = ctx.IDENTIFIER().getText();
            List<IExpressionNode<?, ? extends ISupplier<?>>> arguments = new ArrayList<>();

            if (ctx.arguments() != null) {
                for (ExpressionParser.ExpressionContext argCtx : ctx.arguments().expression()) {
                    IExpressionNode<?, ? extends ISupplier<?>> argNode = visit(argCtx);
                    arguments.add(argNode);
                }
            }

            // Build function key with parameter types
            String functionKey = buildNodeKey(functionName, arguments);

            IExpressionNodeFactory<?, ? extends ISupplier<?>> factory = nodeFactories.get(functionKey);

            if (factory == null) {
                throw new ExpressionException("Unknown function: " + functionKey);
            }

            // Create expression node context with child nodes
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
                return createLeafNode("string", value);
            } else if (ctx.CHAR() != null) {
                String value = ctx.CHAR().getText();
                // Extract character between single quotes
                value = value.substring(1, value.length() - 1);
                return createLeafNode("char", value);
            } else if (ctx.INT_LITERAL() != null) {
                String value = ctx.INT_LITERAL().getText();
                return createLeafNode("int", value);
            } else if (ctx.FLOAT_LIT() != null) {
                String value = ctx.FLOAT_LIT().getText();
                return createLeafNode("double", value);
            } else if (ctx.BOOLEAN() != null) {
                String value = ctx.BOOLEAN().getText();
                return createLeafNode("boolean", value);
            } else if (ctx.NULL() != null) {
                return createLeafNode("null");
            } else if (ctx.arrayLiteral() != null) {
                return visit(ctx.arrayLiteral());
            } else if (ctx.objectLiteral() != null) {
                return visit(ctx.objectLiteral());
            }
            throw new ExpressionException("Unknown literal type");
        }

        @Override
        public IExpressionNode<?, ? extends ISupplier<?>> visitArrayLiteral(ExpressionParser.ArrayLiteralContext ctx) {
            List<IExpressionNode<?, ? extends ISupplier<?>>> elements = new ArrayList<>();

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
                    return createLeafNode("class", primitiveTypeName);
                }

                // Handle Class<Type> or Class<?>
                if (simpleType.classOfType() != null) {
                    // For Class<Type> expressions, return Class.class
                    return createLeafNode("class", "java.lang.Class");
                }

                // Handle regular class types (e.g., java.lang.String, List<T>)
                if (simpleType.classType() != null) {
                    String className = getFullClassName(simpleType.classType());
                    return createLeafNode("class", className);
                }
            }

            // For array types or other complex types, convert to string representation
            String typeString = ctx.getText();
            return createLeafNode("string", typeString);
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
         * Creates a leaf node by finding the appropriate factory and supplying
         * parameters.
         */
        private IExpressionNode<?, ? extends ISupplier<?>> createLeafNode(String functionName, Object... params) {
            // Build parameter type list
            Class<?>[] paramTypes = new Class<?>[params.length];
            for (int i = 0; i < params.length; i++) {
                paramTypes[i] = params[i].getClass();
            }

            String functionKey = buildLeafKey(functionName, paramTypes);

            IExpressionNodeFactory<?, ? extends ISupplier<?>> factory = nodeFactories.get(functionKey);

            if (factory == null) {
                throw new ExpressionException("Function not found: " + functionKey);
            }

            // Create leaf context with actual parameter values
            List<Object> paramList = List.of(params);
            ExpressionNodeContext context = new ExpressionNodeContext(paramList, true);
            Optional<? extends IExpressionNode<?, ? extends ISupplier<?>>> node = factory.supply(context);

            return node.orElseThrow(() -> new ExpressionException("Failed to create leaf node for: " + functionKey));
        }

        private String buildLeafKey(String functionName, Class<?>[] paramTypes) {
            // Build function key
            StringBuilder keyBuilder = new StringBuilder(functionName);
            keyBuilder.append("(");
            for (int i = 0; i < paramTypes.length; i++) {
                if (i > 0)
                    keyBuilder.append(",");
                keyBuilder.append(paramTypes[i].getSimpleName());
            }
            keyBuilder.append(")");
            System.out.println("=> " + keyBuilder);
            return keyBuilder.toString();
        }

        /**
         * Builds a function key in the format "functionName(Type1,Type2,...)".
         */
        private String buildNodeKey(String functionName, List<IExpressionNode<?, ? extends ISupplier<?>>> arguments) {
            StringBuilder keyBuilder = new StringBuilder(functionName);
            keyBuilder.append("(");

            for (int i = 0; i < arguments.size(); i++) {
                if (i > 0)
                    keyBuilder.append(",");
                // Use the supplied class from the node
                keyBuilder.append(arguments.get(i).getFinalSuppliedClass().getSimpleName());
            }

            keyBuilder.append(")");
            System.out.println("=> " + keyBuilder);
            return keyBuilder.toString();
        }
    }
}
