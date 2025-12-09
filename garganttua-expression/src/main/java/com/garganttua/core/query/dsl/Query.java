package com.garganttua.core.query.dsl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import com.garganttua.core.expression.IQuery;
import com.garganttua.core.expression.dsl.IQueryMethodBinder;
import com.garganttua.core.query.antlr4.QueryBaseVisitor;
import com.garganttua.core.query.antlr4.QueryLexer;
import com.garganttua.core.query.antlr4.QueryParser;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.NullSupplierBuilder;

public class Query extends QueryBaseVisitor<ExprNode> implements IQuery {
    
    private final Map<String, IQueryMethodBinder<?>> queries;

    public Query(Set<IQueryMethodBinder<?>> queries) {
        Objects.requireNonNull(queries, "Queries set cannot be null");
        this.queries = queries.stream().collect(Collectors.toMap(IQueryMethodBinder::queryName, q -> q));
    }

    @Override
    public Optional<ISupplier<?>> query(String query) {

        System.out.print("Query : " + query);

        QueryLexer lexer = new QueryLexer(CharStreams.fromString(query));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        QueryParser parser = new QueryParser(tokens);

        ParseTree tree = parser.query();
        ExprNode ast = this.visit(tree);

        System.out.println(" AST : " + ast);

        if (ast instanceof FunctionNode) {
            
        }

        return Optional.ofNullable(new NullSupplierBuilder<Object>(Object.class).build());
    }

    /**
     * Convertit un nœud AST en ISupplier
     */
    private ISupplier<?> convertToSupplier(ExprNode node) {
        Object value = convertToValue(node);
        if (value == null) {
            return new com.garganttua.core.supply.dsl.NullSupplierBuilder<>(Object.class).build();
        }
        return new com.garganttua.core.supply.dsl.FixedSupplierBuilder<>(value).build();
    }

    /**
     * Convertit un nœud AST en valeur Java réelle
     */
    private Object convertToValue(ExprNode node) {
        if (node instanceof LiteralNode) {
            return ((LiteralNode) node).value;
        }
        if (node instanceof FunctionNode) {
            // Nested function call - execute it and return the supplied value
            FunctionNode funcNode = (FunctionNode) node;
            IQueryMethodBinder<?> binder = this.queries.get(funcNode.name);
            if (binder != null) {
                // Recursively convert arguments to ISupplier
                List<ISupplier<?>> arguments = funcNode.arguments.stream()
                        .map(this::convertToSupplier)
                        .collect(Collectors.toList());

                try {
                    Optional<? extends ISupplier<?>> supplierResult = binder.execute(arguments);
                    if (supplierResult.isPresent()) {
                        ISupplier<?> supplier = supplierResult.get();
                        // Extract the actual value from the supplier
                        Optional<?> value = supplier.supply();
                        return value.orElse(null);
                    }
                    return null;
                } catch (Exception e) {
                    throw new RuntimeException("Error executing nested function " + funcNode.name, e);
                }
            } else {
                // Function not registered - return null for nested calls
                System.out.println(" Nested function not registered: " + funcNode.name);
                return null;
            }
        }
        if (node instanceof IdentifierNode) {
            // Pour true/false/null, retourner les valeurs correspondantes
            String name = ((IdentifierNode) node).name;
            switch (name) {
                case "true": return true;
                case "false": return false;
                case "null": return null;
                default: return name; // ou lever une exception
            }
        }
        if (node instanceof ArrayLiteralNode) {
            List<ExprNode> elements = ((ArrayLiteralNode) node).elements;
            return elements.stream()
                    .map(this::convertToValue)
                    .toArray();
        }
        if (node instanceof TypedArrayNode) {
            TypedArrayNode typedArray = (TypedArrayNode) node;
            return convertTypedArray(typedArray);
        }
        if (node instanceof ObjectLiteralNode) {
            Map<String, ExprNode> props = ((ObjectLiteralNode) node).properties;
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<String, ExprNode> entry : props.entrySet()) {
                result.put(entry.getKey(), convertToValue(entry.getValue()));
            }
            return result;
        }
        // Types (utilisés comme Class<?>)
        if (node instanceof PrimitiveTypeNode) {
            return getPrimitiveClass(((PrimitiveTypeNode) node).name);
        }
        if (node instanceof ClassTypeNode) {
            ClassTypeNode classType = (ClassTypeNode) node;
            // Handle special cases for keywords that might be parsed as identifiers
            switch (classType.className) {
                case "true": return true;
                case "false": return false;
                case "null": return null;
            }
            try {
                return Class.forName(classType.className);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Class not found: " + classType.className, e);
            }
        }
        if (node instanceof ClassOfTypeNode) {
            // Class<String> ou Class<?>
            return Class.class; // ou retourner le type spécifique
        }

        return null;
    }

    private Object convertTypedArray(TypedArrayNode typedArray) {
        String typeName = null;
        if (typedArray.elementType instanceof PrimitiveTypeNode) {
            typeName = ((PrimitiveTypeNode) typedArray.elementType).name;
        }

        List<Object> values = typedArray.values.stream()
                .map(this::convertToValue)
                .collect(Collectors.toList());

        // Check if any values are arrays (nested arrays)
        boolean hasNestedArrays = values.stream().anyMatch(v -> v != null && v.getClass().isArray());
        if (hasNestedArrays) {
            // For nested arrays, return Object[]
            return values.toArray();
        }

        // Convertir en tableau typé Java
        if ("int".equals(typeName)) {
            return values.stream().mapToInt(v -> ((Number) v).intValue()).toArray();
        } else if ("double".equals(typeName)) {
            return values.stream().mapToDouble(v -> ((Number) v).doubleValue()).toArray();
        } else if ("float".equals(typeName)) {
            float[] result = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                result[i] = ((Number) values.get(i)).floatValue();
            }
            return result;
        } else if ("long".equals(typeName)) {
            return values.stream().mapToLong(v -> ((Number) v).longValue()).toArray();
        } else if ("boolean".equals(typeName)) {
            boolean[] result = new boolean[values.size()];
            for (int i = 0; i < values.size(); i++) {
                result[i] = (Boolean) values.get(i);
            }
            return result;
        } else if ("char".equals(typeName)) {
            char[] result = new char[values.size()];
            for (int i = 0; i < values.size(); i++) {
                String str = values.get(i).toString();
                result[i] = str.isEmpty() ? '\0' : str.charAt(0);
            }
            return result;
        }

        // Par défaut, retourner un tableau d'objets
        return values.toArray();
    }

    private Class<?> getPrimitiveClass(String name) {
        switch (name) {
            case "int": return int.class;
            case "long": return long.class;
            case "double": return double.class;
            case "float": return float.class;
            case "boolean": return boolean.class;
            case "char": return char.class;
            case "byte": return byte.class;
            case "short": return short.class;
            default: return Object.class;
        }
    }

    @Override
    public ExprNode visitQuery(QueryParser.QueryContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public ExprNode visitFunctionCall(QueryParser.FunctionCallContext ctx) {
        String name = ctx.IDENTIFIER().getText();
        List<ExprNode> args = new ArrayList<>();
        if (ctx.arguments() != null) {
            for (QueryParser.ExpressionContext e : ctx.arguments().expression()) {
                args.add(visit(e));
            }
        }
        return new FunctionNode(name, this.queries.get(name), args);
    }

    @Override
    public ExprNode visitExpression(QueryParser.ExpressionContext ctx) {
        if (ctx.functionCall() != null)
            return visit(ctx.functionCall());
        if (ctx.literal() != null)
            return visit(ctx.literal());
        if (ctx.type() != null)
            return visit(ctx.type());
        if (ctx.IDENTIFIER() != null)
            return new IdentifierNode(ctx.IDENTIFIER().getText());
        return null;
    }

    @Override
    public ExprNode visitLiteral(QueryParser.LiteralContext ctx) {
        if (ctx.STRING() != null) {
            String s = ctx.STRING().getText();
            s = s.substring(1, s.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
            return new LiteralNode(s);
        }
        if (ctx.CHAR() != null) {
            String s = ctx.CHAR().getText();
            s = s.substring(1, s.length() - 1);
            return new LiteralNode(s);
        }
        if (ctx.INT() != null)
            return new LiteralNode(Long.parseLong(ctx.INT().getText()));
        if (ctx.FLOAT() != null)
            return new LiteralNode(Double.parseDouble(ctx.FLOAT().getText()));
        if (ctx.BOOLEAN() != null)
            return new LiteralNode(Boolean.parseBoolean(ctx.BOOLEAN().getText()));
        if (ctx.NULL() != null)
            return new LiteralNode(null);
        if (ctx.arrayLiteral() != null)
            return visitArrayLiteral(ctx.arrayLiteral());
        if (ctx.objectLiteral() != null)
            return visitObjectLiteral(ctx.objectLiteral());
        return null;
    }

    public ExprNode visitArrayLiteral(QueryParser.ArrayLiteralContext ctx) {
        List<ExprNode> elems = new ArrayList<>();
        if (ctx.expression() != null) {
            for (QueryParser.ExpressionContext e : ctx.expression()) {
                elems.add(visit(e));
            }
        }
        return new ArrayLiteralNode(elems);
    }

    public ExprNode visitObjectLiteral(QueryParser.ObjectLiteralContext ctx) {
        Map<String, ExprNode> map = new LinkedHashMap<>();
        if (ctx.pair() != null) {
            for (QueryParser.PairContext p : ctx.pair()) {
                String key = p.STRING().getText();
                key = key.substring(1, key.length() - 1);

                ExprNode val;
                if (p.literal() != null)
                    val = visit(p.literal());
                else if (p.type() != null)
                    val = visit(p.type());
                else if (p.objectLiteral() != null)
                    val = visitObjectLiteral(p.objectLiteral());
                else
                    val = null;

                map.put(key, val);
            }
        }
        return new ObjectLiteralNode(map);
    }

    @Override
    public ExprNode visitType(QueryParser.TypeContext ctx) {
        TypeNode base;

        QueryParser.SimpleTypeContext simple = ctx.simpleType();
        if (simple.primitiveType() != null) {
            base = new PrimitiveTypeNode(simple.primitiveType().getText());
        } else if (simple.classType() != null) {
            base = visitClassType(simple.classType());
        } else if (simple.classOfType() != null) {
            base = visitClassOfType(simple.classOfType());
        } else {
            base = null;
        }

        if (ctx.arrayDims() != null) {
            return visitArrayDims(ctx.arrayDims(), base);
        }

        return base;
    }

    private ExprNode visitArrayDims(QueryParser.ArrayDimsContext dims, TypeNode baseType) {
        List<ExprNode> values = new ArrayList<>();
        boolean hasValues = false;

        for (int i = 0; i < dims.getChildCount(); i++) {
            if (dims.getChild(i) instanceof QueryParser.ExpressionContext) {
                hasValues = true;
                values.add(visit(dims.getChild(i)));
            }
        }

        if (hasValues && !values.isEmpty()) {
            return new TypedArrayNode(baseType, values);
        } else {
            int dimCount = dims.getChildCount() / 2;
            TypeNode result = baseType;
            for (int i = 0; i < dimCount; i++) {
                result = new ArrayTypeNode(result);
            }
            return result;
        }
    }

    public TypeNode visitClassType(QueryParser.ClassTypeContext ctx) {
        StringBuilder name = new StringBuilder();
        List<TypeNode> generics = Collections.emptyList();
        QueryParser.ClassNameContext cn = ctx.className();
        name.append(cn.IDENTIFIER(0).getText());
        for (int i = 1; i < cn.IDENTIFIER().size(); i++) {
            name.append('.').append(cn.IDENTIFIER(i).getText());
        }
        if (ctx.genericArguments() != null) {
            generics = new ArrayList<>();
            for (QueryParser.TypeContext t : ctx.genericArguments().type()) {
                generics.add((TypeNode) visit(t));
            }
        }
        return new ClassTypeNode(name.toString(), generics);
    }

    public TypeNode visitClassOfType(QueryParser.ClassOfTypeContext ctx) {
        if (ctx.getText().startsWith("Class<?>")
                || (ctx.getChildCount() >= 3 && ctx.getChild(2).getText().equals("?"))) {
            return new ClassOfTypeNode(null, true);
        } else {
            QueryParser.TypeContext t = (QueryParser.TypeContext) ctx.getChild(2);
            TypeNode inner = (TypeNode) visit(t);
            return new ClassOfTypeNode(inner, false);
        }
    }

}

// =================================
// AST Nodes
// =================================
class ExprNode {
    public String dump() {
        return toString();
    }
}

class FunctionNode extends ExprNode {
    public final String name;
    public final List<ExprNode> arguments;
    private IQueryMethodBinder<?> query;

    public FunctionNode(String name, IQueryMethodBinder<?> query, List<ExprNode> arguments) {
        this.name = name;
        this.query = query;
        this.arguments = arguments;
    }

    @Override
    public String toString() {
        return name + "(" + arguments + ")";
    }
}

class LiteralNode extends ExprNode {
    public final Object value;

    public LiteralNode(Object value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf(value) + "#literal";
    }
}

class IdentifierNode extends ExprNode {
    public final String name;

    public IdentifierNode(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name + "#identifier";
    }
}

class ArrayLiteralNode extends ExprNode {
    public final List<ExprNode> elements;

    public ArrayLiteralNode(List<ExprNode> elements) {
        this.elements = elements;
    }

    @Override
    public String toString() {
        return elements.toString() + "#arrayliteral";
    }
}

class ObjectLiteralNode extends ExprNode {
    public final Map<String, ExprNode> properties;

    public ObjectLiteralNode(Map<String, ExprNode> properties) {
        this.properties = properties;
    }

    @Override
    public String toString() {
        return properties.toString() + "#objectliteral";
    }
}

abstract class TypeNode extends ExprNode {
}

class PrimitiveTypeNode extends TypeNode {
    public final String name;

    public PrimitiveTypeNode(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name + "#primitivetype";
    }
}

class ClassTypeNode extends TypeNode {
    public final String className;
    public final List<TypeNode> genericArgs;

    public ClassTypeNode(String className, List<TypeNode> genericArgs) {
        this.className = className;
        this.genericArgs = genericArgs;
    }

    @Override
    public String toString() {
        if (genericArgs == null || genericArgs.isEmpty())
            return className;
        return className + "<" + genericArgs + ">";
    }
}

class ArrayTypeNode extends TypeNode {
    public final TypeNode elementType;

    public ArrayTypeNode(TypeNode elementType) {
        this.elementType = elementType;
    }

    @Override
    public String toString() {
        return elementType.toString() + "[]" + "#arraytype";
    }
}

class ClassOfTypeNode extends TypeNode {
    public final TypeNode inner;
    public final boolean wildcard;

    public ClassOfTypeNode(TypeNode inner, boolean wildcard) {
        this.inner = inner;
        this.wildcard = wildcard;
    }

    @Override
    public String toString() {
        return "Class<" + (wildcard ? "?" : inner) + ">";
    }
}

class TypedArrayNode extends ExprNode {
    public final TypeNode elementType;
    public final List<ExprNode> values;

    public TypedArrayNode(TypeNode elementType, List<ExprNode> values) {
        this.elementType = elementType;
        this.values = values;
    }

    @Override
    public String toString() {
        return elementType.toString() + values.toString();
    }
}