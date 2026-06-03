package com.garganttua.core.expression.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

/**
 * Behaviour tests for {@link FrameworkBuiltinRegistrar}: the dedup method-signature format and the
 * always-present framework function class list (the "hard guarantee" of the registrar).
 */
class FrameworkBuiltinRegistrarBehaviourTest {

    static class Sample {
        public static String none() {
            return "";
        }

        public static int one(String a) {
            return 0;
        }

        public static void two(String a, int b) {
            // no-op
        }
    }

    @BeforeEach
    void setUp() {
        IReflection reflection = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider(), 1)
                .build();
        IClass.setReflection(reflection);
    }

    @Test
    void buildMethodSignature_noParams_emptyParens() throws Exception {
        IMethod m = IClass.getClass(Sample.class).getMethod("none");
        String sig = FrameworkBuiltinRegistrar.buildMethodSignature(m);
        assertEquals(Sample.class.getName() + ".none()", sig);
    }

    @Test
    void buildMethodSignature_singleParam_usesFullyQualifiedName() throws Exception {
        IMethod m = IClass.getClass(Sample.class).getMethod("one", IClass.getClass(String.class));
        String sig = FrameworkBuiltinRegistrar.buildMethodSignature(m);
        assertEquals(Sample.class.getName() + ".one(java.lang.String)", sig);
    }

    @Test
    void buildMethodSignature_multipleParams_commaSeparated() throws Exception {
        IMethod m = IClass.getClass(Sample.class).getMethod("two",
                IClass.getClass(String.class), IClass.getClass(int.class));
        String sig = FrameworkBuiltinRegistrar.buildMethodSignature(m);
        // primitive int is rendered by its own name
        assertEquals(Sample.class.getName() + ".two(java.lang.String,int)", sig);
    }

    @Test
    void buildMethodSignature_distinguishesOverloadsByParamTypes() throws Exception {
        IMethod one = IClass.getClass(Sample.class).getMethod("one", IClass.getClass(String.class));
        IMethod two = IClass.getClass(Sample.class).getMethod("two",
                IClass.getClass(String.class), IClass.getClass(int.class));
        assertTrue(!FrameworkBuiltinRegistrar.buildMethodSignature(one)
                .equals(FrameworkBuiltinRegistrar.buildMethodSignature(two)));
    }

    @Test
    void frameworkFunctionClasses_includesCoreExpressionAndConditionClasses() {
        var list = java.util.Arrays.asList(FrameworkBuiltinRegistrar.FRAMEWORK_FUNCTION_CLASSES);
        assertNotNull(list);
        // the expression built-ins must always be in the hard-guarantee list
        assertTrue(list.contains("com.garganttua.core.expression.functions.Expressions"));
        // a sampling of the condition classes that the registrar promises to keep registering
        assertTrue(list.contains("com.garganttua.core.condition.AndCondition"));
        assertTrue(list.contains("com.garganttua.core.condition.NotNullCondition"));
        assertTrue(list.contains("com.garganttua.core.condition.EqualsCondition"));
        // no accidental duplicates in the list
        assertEquals(list.size(), list.stream().distinct().count(), "framework class list must be duplicate-free");
    }
}
