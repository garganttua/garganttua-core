package com.garganttua.core.expression.context;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.bootstrap.banner.IBootstrapSummaryContributor;
import com.garganttua.core.expression.Expression;
import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.IExpression;
import com.garganttua.core.expression.IExpressionNode;
import com.garganttua.core.expression.antlr4.ExpressionLexer;
import com.garganttua.core.expression.antlr4.ExpressionParser;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.annotations.Reflected;
import com.garganttua.core.supply.ISupplier;

// @Reflected so the AOT processor emits a full IClass descriptor (with methods)
// rather than a shallow synthesized one. ExpressionContextBuilder.doBuild()
// resolves the man() built-in via getMethod("man", …); under native image the
// shallow-descriptor live-reflection fallback fails (closed world), so the real
// descriptor — registered for reflection by GarganttuaAotFeature — is required.
/**
 * Default {@link IExpressionContext}: a registry of {@link IExpressionNodeFactory} keyed by
 * function signature, used to parse expression strings (via ANTLR4) into {@link IExpressionNode}
 * trees and to expose {@code man()} documentation.
 *
 * <p>Also acts as an {@link IBootstrapSummaryContributor}, reporting the number of registered
 * expression functions. Factory storage and variable-type registration are thread-safe.
 */
@Reflected(queryAllDeclaredMethods = true)
public class ExpressionContext implements IExpressionContext, IBootstrapSummaryContributor {
    /** Creates an empty expression context with no registered node factories. */
    public ExpressionContext() {
    }

    private static final Logger log = Logger.getLogger(ExpressionContext.class);

    private Map<String, IExpressionNodeFactory<?, ? extends ISupplier<?>>> nodeFactories = new ConcurrentHashMap<>();
    private final Map<String, IClass<?>> variableTypes = new ConcurrentHashMap<>();
    private volatile boolean dynamicFunctionsEnabled = false;

    /**
     * Creates a context populated from the given factories, keyed by {@link IExpressionNodeFactory#key()}.
     * Duplicate keys are ignored (first registration wins).
     *
     * @param nodeFactories the node factories to register (must not be {@code null})
     */
    public ExpressionContext(Set<IExpressionNodeFactory<?, ? extends ISupplier<?>>> nodeFactories) {
        log.trace("Entering ExpressionContext constructor");
        Objects.requireNonNull(nodeFactories, "Node Factories set cannot be null");

        // Populate ConcurrentHashMap with merge function to handle duplicates
        // Duplicates can occur when the same method is registered multiple times
        // (e.g., through auto-detection and manual registration)
        for (IExpressionNodeFactory<?, ? extends ISupplier<?>> ef : nodeFactories) {
            this.nodeFactories.putIfAbsent(ef.key(), ef);
        }

        log.debug("ExpressionContext initialized with {} unique node factories (from {} total provided)",
                this.nodeFactories.size(), nodeFactories.size());
        log.trace("Exiting ExpressionContext constructor");
    }

    @Override
    public void register(String key, IExpressionNodeFactory<?, ? extends ISupplier<?>> factory) {
        log.debug("Registering expression factory with key: {}", key);
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(factory, "Factory cannot be null");
        this.nodeFactories.put(key, factory);
        log.debug("Expression factory registered: {}", key);
    }

    @Override
    public void enableDynamicFunctions() {
        this.dynamicFunctionsEnabled = true;
        log.debug("Dynamic function resolution enabled");
    }

    @Override
    public void registerVariableType(String name, IClass<?> type) {
        Objects.requireNonNull(name, "Variable name cannot be null");
        Objects.requireNonNull(type, "Variable type cannot be null");
        this.variableTypes.put(name, type);
        log.debug("Registered variable type: @{} -> {}", name, type.getName());
    }

    @Override
    public IExpression<?, ? extends ISupplier<?>> expression(String expressionString) {
        log.trace("Entering expression(expressionString={})", expressionString);
        log.debug("Parsing expression: {}", expressionString);

        Objects.requireNonNull(expressionString, "Expression string cannot be null");

        try {
            // Create ANTLR4 lexer and parser
            ExpressionLexer lexer = new ExpressionLexer(CharStreams.fromString(expressionString));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            ExpressionParser parser = new ExpressionParser(tokens);
            log.debug("ANTLR4 lexer and parser created");

            // Parse the expression starting from root rule
            ExpressionParser.RootContext rootContext = parser.root();
            log.debug("Expression parsed by ANTLR4");

            // Visit and build the expression tree
            ExpressionVisitor visitor = new ExpressionVisitor(this.nodeFactories, this.variableTypes, this.dynamicFunctionsEnabled);
            IExpressionNode<?, ? extends ISupplier<?>> rootNode = visitor.visit(rootContext);

            if (rootNode == null) {
                log.error("Failed to parse expression: {}", expressionString);
                throw new ExpressionException("Failed to parse expression: " + expressionString);
            }

            log.debug("Expression parsed successfully: {}", expressionString);
            log.trace("Exiting expression");
            return new Expression<>(rootNode);

        } catch (Exception e) {
            String errorMsg = "Error parsing expression '" + expressionString + "': " + e.getMessage();
            log.error(errorMsg, e);
            throw new ExpressionException(e);
        }
    }

    @Override
    public String man(String key) {
        log.trace("Entering man(key={})", key);
        log.debug("Looking up manual for expression node: {}", key);

        Objects.requireNonNull(key, "Key cannot be null");

        IExpressionNodeFactory<?, ? extends ISupplier<?>> factory = this.nodeFactories.get(key);

        if (factory == null) {
            log.warn("No expression node factory found for key: {}", key);
            return null;
        }

        String manual = factory.man();
        log.debug("Manual retrieved for key: {}", key);
        log.trace("Exiting man");

        return manual;
    }

    @Override
    public String man() {
        log.trace("Entering listFactories()");
        log.debug("Generating list of {} expression node factories", this.nodeFactories.size());

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

        log.debug("Factory list generated");
        log.trace("Exiting listFactories");

        return list.toString();
    }

    @Override
    public String man(int index) {
        log.trace("Entering man(index={})", index);
        log.debug("Looking up manual for expression node at index: {}", index);

        if (index < 1) {
            log.warn("Invalid index: {}. Index must be >= 1", index);
            return null;
        }

        // Get sorted list of factories
        List<Map.Entry<String, IExpressionNodeFactory<?, ? extends ISupplier<?>>>> sortedFactories = this.nodeFactories
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .toList();

        // Check if index is in bounds (1-based index)
        if (index > sortedFactories.size()) {
            log.warn("Index {} out of bounds. Total factories: {}", index, sortedFactories.size());
            return null;
        }

        // Get factory at index (convert from 1-based to 0-based)
        Map.Entry<String, IExpressionNodeFactory<?, ? extends ISupplier<?>>> entry = sortedFactories.get(index - 1);

        String manual = entry.getValue().man();
        log.debug("Manual retrieved for index {} (key: {})", index, entry.getKey());
        log.trace("Exiting man");

        return manual;
    }

    @Override
    public Set<String> getFactoryKeys() {
        return Collections.unmodifiableSet(nodeFactories.keySet());
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
