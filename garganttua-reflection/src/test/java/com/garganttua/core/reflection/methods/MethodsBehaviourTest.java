package com.garganttua.core.reflection.methods;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.runtime.RuntimeClass;

/**
 * Behaviour tests for the {@link Methods} static helper.
 */
public class MethodsBehaviourTest {

    public static class Sample {
        public String instanceMethod(int a, String b) {
            return b + a;
        }

        public static void staticMethod() {
        }

        public void noArgs() {
        }
    }

    private static IMethod method(String name, IClass<?>... params) {
        try {
            return RuntimeClass.of(Sample.class).getMethod(name, params);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new AssertionError("test method not found: " + name, e);
        }
    }

    @Test
    public void isStatic_trueForStaticMethod() {
        assertTrue(Methods.isStatic(method("staticMethod")));
    }

    @Test
    public void isStatic_falseForInstanceMethod() {
        assertFalse(Methods.isStatic(method("noArgs")));
    }

    @Test
    public void pretty_rendersClassNameAndParamTypes() {
        IMethod m = method("instanceMethod", RuntimeClass.of(int.class), RuntimeClass.of(String.class));
        String pretty = Methods.pretty(m);
        assertEquals("Sample.instanceMethod(int, String)", pretty);
    }

    @Test
    public void pretty_noArgsHasEmptyParens() {
        assertEquals("Sample.noArgs()", Methods.pretty(method("noArgs")));
    }

    @Test
    public void prettyColored_containsAnsiAndMethodName() {
        IMethod m = method("noArgs");
        String colored = Methods.prettyColored(m);
        assertTrue(colored.contains("["), "expected ANSI escape sequences");
        assertTrue(colored.contains("noArgs"));
        assertTrue(colored.contains("Sample"));
    }

    @Test
    public void prettyColored_paramTypesIncluded() {
        IMethod m = method("instanceMethod", RuntimeClass.of(int.class), RuntimeClass.of(String.class));
        String colored = Methods.prettyColored(m);
        assertTrue(colored.contains("int"));
        assertTrue(colored.contains("String"));
        assertTrue(colored.contains("instanceMethod"));
    }
}
