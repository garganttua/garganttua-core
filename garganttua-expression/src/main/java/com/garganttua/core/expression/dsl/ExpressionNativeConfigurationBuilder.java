package com.garganttua.core.expression.dsl;

import java.util.HashSet;
import java.util.Set;

import com.garganttua.core.dsl.AbstractAutomaticBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.expression.ContextualExpressionNode;
import com.garganttua.core.expression.Expression;
import com.garganttua.core.expression.ExpressionNode;
import com.garganttua.core.expression.ForLoopExpressionNode;
import com.garganttua.core.expression.antlr4.ExpressionBaseListener;
import com.garganttua.core.expression.antlr4.ExpressionBaseVisitor;
import com.garganttua.core.expression.antlr4.ExpressionLexer;
import com.garganttua.core.expression.antlr4.ExpressionListener;
import com.garganttua.core.expression.antlr4.ExpressionParser;
import com.garganttua.core.expression.antlr4.ExpressionVisitor;
import com.garganttua.core.expression.context.ConstructorCallExpressionNodeFactory;
import com.garganttua.core.expression.context.ExpressionContext;
import com.garganttua.core.expression.context.ExpressionNodeContext;
import com.garganttua.core.expression.context.ExpressionNodeFactory;
import com.garganttua.core.expression.context.ExpressionVariableContext;
import com.garganttua.core.expression.context.MethodCallExpressionNodeFactory;
import com.garganttua.core.expression.functions.Expressions;
import com.garganttua.core.nativve.INativeBuilder;
import com.garganttua.core.nativve.INativeReflectionConfiguration;
import com.garganttua.core.nativve.IReflectionConfigurationEntryBuilder;
import com.garganttua.core.nativve.annotations.NativeConfigurationBuilder;
import com.garganttua.core.nativve.image.config.reflection.ReflectConfigEntryBuilder;

import lombok.extern.slf4j.Slf4j;

/**
 * Native image configuration builder for the garganttua-expression module.
 *
 * <p>
 * This builder provides GraalVM native image reflection configuration for all
 * expression-related classes including:
 * </p>
 * <ul>
 *   <li>Core expression classes (Expression, ExpressionNode, etc.)</li>
 *   <li>Expression context and factory classes</li>
 *   <li>ANTLR4 generated parser and lexer classes</li>
 *   <li>Built-in expression functions</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 */
@Slf4j
@NativeConfigurationBuilder
public class ExpressionNativeConfigurationBuilder
        extends AbstractAutomaticBuilder<ExpressionNativeConfigurationBuilder, INativeReflectionConfiguration>
        implements INativeBuilder<ExpressionNativeConfigurationBuilder, INativeReflectionConfiguration> {

    private final Set<String> packages = new HashSet<>();

    @Override
    public ExpressionNativeConfigurationBuilder withPackages(String[] packageNames) {
        log.atTrace().log("Adding {} packages to expression native configuration", packageNames.length);
        this.packages.addAll(Set.of(packageNames));
        return this;
    }

    @Override
    public ExpressionNativeConfigurationBuilder withPackage(String packageName) {
        log.atTrace().log("Adding package to expression native configuration: {}", packageName);
        this.packages.add(packageName);
        return this;
    }

    @Override
    public String[] getPackages() {
        return this.packages.toArray(new String[0]);
    }

    @Override
    protected INativeReflectionConfiguration doBuild() throws DslException {
        log.atTrace().log("Building expression native configuration");

        Set<IReflectionConfigurationEntryBuilder> entries = new HashSet<>();

        // Core expression classes
        entries.add(new ReflectConfigEntryBuilder(Expression.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true));

        entries.add(new ReflectConfigEntryBuilder(ExpressionNode.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true)
                .allDeclaredClasses(true));

        entries.add(new ReflectConfigEntryBuilder(ContextualExpressionNode.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true)
                .allDeclaredClasses(true));

        entries.add(new ReflectConfigEntryBuilder(ForLoopExpressionNode.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true));

        // Context and factory classes
        entries.add(new ReflectConfigEntryBuilder(ExpressionContext.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true)
                .allDeclaredClasses(true));

        entries.add(new ReflectConfigEntryBuilder(ExpressionNodeContext.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true));

        entries.add(new ReflectConfigEntryBuilder(ExpressionNodeFactory.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true)
                .allDeclaredClasses(true));

        entries.add(new ReflectConfigEntryBuilder(ExpressionVariableContext.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true));

        entries.add(new ReflectConfigEntryBuilder(ConstructorCallExpressionNodeFactory.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true)
                .allDeclaredClasses(true));

        entries.add(new ReflectConfigEntryBuilder(MethodCallExpressionNodeFactory.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true)
                .allDeclaredClasses(true));

        // DSL builders
        entries.add(new ReflectConfigEntryBuilder(ExpressionContextBuilder.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true));

        entries.add(new ReflectConfigEntryBuilder(ExpressionNodeFactoryBuilder.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true));

        // Built-in functions class with @Expression annotated methods
        entries.add(new ReflectConfigEntryBuilder(Expressions.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true));

        // ANTLR4 generated classes - Parser
        entries.add(new ReflectConfigEntryBuilder(ExpressionParser.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true)
                .allDeclaredClasses(true)
                .allPublicClasses(true));

        // ANTLR4 generated classes - Lexer
        entries.add(new ReflectConfigEntryBuilder(ExpressionLexer.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true));

        // ANTLR4 generated classes - Visitor/Listener
        entries.add(new ReflectConfigEntryBuilder(ExpressionVisitor.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true));

        entries.add(new ReflectConfigEntryBuilder(ExpressionBaseVisitor.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true));

        entries.add(new ReflectConfigEntryBuilder(ExpressionListener.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true));

        entries.add(new ReflectConfigEntryBuilder(ExpressionBaseListener.class)
                .queryAllDeclaredConstructors(true)
                .queryAllDeclaredMethods(true)
                .allDeclaredFields(true));

        log.atDebug().log("Expression native configuration built with {} entries", entries.size());

        final Set<IReflectionConfigurationEntryBuilder> finalEntries = entries;
        return () -> finalEntries;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        log.atTrace().log("Auto-detection not required for expression native configuration");
    }
}
