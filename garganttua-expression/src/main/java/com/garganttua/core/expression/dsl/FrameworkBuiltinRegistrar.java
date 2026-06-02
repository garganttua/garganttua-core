package com.garganttua.core.expression.dsl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.expression.annotations.Expression;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.supply.dsl.NullSupplierBuilder;

/**
 * Hard-guarantee registration of every framework built-in {@code @Expression}
 * function by FQN, plus the signature key used to deduplicate scanned methods.
 *
 * <p>Extracted from {@link ExpressionContextBuilder}: the package-scan path is the
 * "soft" version of this; resolving these classes by name and registering every
 * static {@code @Expression} method guarantees the framework built-ins are always
 * present even when a consumer's shade configuration drops an annotation index.
 * Binding-module classes absent from the classpath are silently skipped.
 */
final class FrameworkBuiltinRegistrar {

    private FrameworkBuiltinRegistrar() {
    }

    /**
     * Framework function classes whose every {@code @Expression}-annotated static
     * method is registered directly by FQN. When splitting a {@code *Functions}
     * class, add the new class here so its functions keep registering.
     */
    static final String[] FRAMEWORK_FUNCTION_CLASSES = {
            // expression
            "com.garganttua.core.expression.functions.Expressions",
            // script
            "com.garganttua.core.script.functions.ScriptFunctions",
            "com.garganttua.core.script.functions.ScriptTimingFunctions",
            "com.garganttua.core.script.functions.ScriptResilienceFunctions",
            "com.garganttua.core.script.functions.ScriptConcurrencyFunctions",
            "com.garganttua.core.script.functions.LogFunctions",
            "com.garganttua.core.script.functions.ControlFlowFunctions",
            // runtime
            "com.garganttua.core.runtime.functions.RuntimeFunctions",
            // injection
            "com.garganttua.core.injection.functions.InjectionFunctions",
            "com.garganttua.core.injection.functions.BeanMutationFunctions",
            "com.garganttua.core.injection.context.beans.Beans",
            // mutex (+ binding-conditional redis)
            "com.garganttua.core.mutex.functions.MutexFunctions",
            "com.garganttua.core.mutex.redis.functions.RedisMutexFunctions",
            // console & observability
            "com.garganttua.core.console.ConsoleFunctions",
            "com.garganttua.core.observability.ObservabilityExpressions",
            // conditions — and / or / nor / nand / xor / not / null / notNull /
            // equals / notEquals / greater / greaterOrEquals / lower / lowerOrEquals
            "com.garganttua.core.condition.AndCondition",
            "com.garganttua.core.condition.OrCondition",
            "com.garganttua.core.condition.NorCondition",
            "com.garganttua.core.condition.NandCondition",
            "com.garganttua.core.condition.XorCondition",
            "com.garganttua.core.condition.NullCondition",
            "com.garganttua.core.condition.NotNullCondition",
            "com.garganttua.core.condition.EqualsCondition",
            "com.garganttua.core.condition.NotEqualsCondition",
            "com.garganttua.core.condition.GreaterCondition",
            "com.garganttua.core.condition.GreaterOrEqualsCondition",
            "com.garganttua.core.condition.LowerCondition",
            "com.garganttua.core.condition.LowerOrEqualsCondition"
    };

    @SuppressWarnings({ "unchecked", "rawtypes" })
    static void registerAll(ExpressionContextBuilder builder) {
        IClass<? extends Annotation> exprAnnoCls =
                (IClass<? extends Annotation>) (IClass<?>) IClass.getClass(Expression.class);
        for (String fqn : FRAMEWORK_FUNCTION_CLASSES) {
            Class<?> cls;
            try {
                cls = Class.forName(fqn, true, Thread.currentThread().getContextClassLoader());
            } catch (ClassNotFoundException | LinkageError missing) {
                // Binding module not on classpath — fine.
                continue;
            }
            IClass<?> ownerCls = IClass.getClass(cls);
            for (IMethod m : ownerCls.getDeclaredMethods()) {
                if (!Modifier.isStatic(m.getModifiers()) || m.getAnnotation(exprAnnoCls) == null) {
                    continue;
                }
                try {
                    builder.expression(new NullSupplierBuilder<>(ownerCls), (IClass) m.getReturnType())
                            .method(m)
                            .autoDetect(true);
                } catch (DslException ignored) {
                    // One bad function should not poison the whole context.
                }
            }
        }
    }

    /** Build a dedup signature (declaring class + method name + parameter types) for a scanned method. */
    static String buildMethodSignature(IMethod method) {
        StringBuilder signature = new StringBuilder();
        signature.append(method.getDeclaringClass().getName());
        signature.append(".");
        signature.append(method.getName());
        signature.append("(");
        IClass<?>[] paramTypes = method.getParameterTypes();
        for (int i = 0; i < paramTypes.length; i++) {
            if (i > 0) {
                signature.append(",");
            }
            signature.append(paramTypes[i].getName());
        }
        signature.append(")");
        return signature.toString();
    }
}
