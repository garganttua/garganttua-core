package com.garganttua.core.reflection.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.runtime.RuntimeClass;

/**
 * Behaviour tests for the package-private {@link MemberLookup} hierarchy walker.
 */
public class MemberLookupBehaviourTest {

    interface Greeter {
        String greet();
    }

    public static class Base {
        protected String baseField;

        public String shared() {
            return "base";
        }

        public String baseOnly() {
            return "x";
        }
    }

    public static class Child extends Base implements Greeter {
        public String childField;

        @Override
        public String shared() {
            return "child";
        }

        public String overload(int a) {
            return "i";
        }

        public String overload(String a) {
            return "s";
        }

        @Override
        public String greet() {
            return "hi";
        }
    }

    // ===== getField =====

    @Test
    public void getField_findsOwnField() {
        IField f = MemberLookup.getField(RuntimeClass.of(Child.class), "childField");
        assertNotNull(f);
        assertEquals("childField", f.getName());
    }

    @Test
    public void getField_findsInheritedField() {
        IField f = MemberLookup.getField(RuntimeClass.of(Child.class), "baseField");
        assertNotNull(f);
        assertEquals("baseField", f.getName());
    }

    @Test
    public void getField_returnsNullForUnknown() {
        assertNull(MemberLookup.getField(RuntimeClass.of(Child.class), "nope"));
    }

    // ===== getMethod =====

    @Test
    public void getMethod_findsOverriddenInChildFirst() {
        IMethod m = MemberLookup.getMethod(RuntimeClass.of(Child.class), "shared");
        assertNotNull(m);
        assertEquals("shared", m.getName());
        // declared on Child, not Base
        assertEquals("Child", m.getDeclaringClass().getSimpleName());
    }

    @Test
    public void getMethod_findsInheritedMethod() {
        IMethod m = MemberLookup.getMethod(RuntimeClass.of(Child.class), "baseOnly");
        assertNotNull(m);
        assertEquals("baseOnly", m.getName());
    }

    @Test
    public void getMethod_findsInterfaceMethod() {
        IMethod m = MemberLookup.getMethod(RuntimeClass.of(Child.class), "greet");
        assertNotNull(m);
        assertEquals("greet", m.getName());
    }

    @Test
    public void getMethod_returnsNullForUnknown() {
        assertNull(MemberLookup.getMethod(RuntimeClass.of(Child.class), "missing"));
    }

    // ===== getMethods (overloads) =====

    @Test
    public void getMethods_returnsAllOverloads() {
        List<IMethod> overloads = MemberLookup.getMethods(RuntimeClass.of(Child.class), "overload");
        assertEquals(2, overloads.size());
        assertTrue(overloads.stream().allMatch(m -> m.getName().equals("overload")));
    }

    @Test
    public void getMethods_deduplicatesOverriddenSignature() {
        // shared() is declared on both Base and Child with the same signature;
        // the seen-signature set must collapse them to one entry.
        List<IMethod> shared = MemberLookup.getMethods(RuntimeClass.of(Child.class), "shared");
        assertEquals(1, shared.size());
    }

    @Test
    public void getMethods_emptyForUnknownName() {
        assertTrue(MemberLookup.getMethods(RuntimeClass.of(Child.class), "ghost").isEmpty());
    }

    // ===== buildMethodSignature =====

    @Test
    public void buildMethodSignature_includesNameAndParamTypes() {
        IMethod m = MemberLookup.getMethods(RuntimeClass.of(Child.class), "overload").stream()
                .filter(x -> x.getParameterTypes().length == 1
                        && x.getParameterTypes()[0].getName().equals("int"))
                .findFirst().orElseThrow();
        assertEquals("overload(int)", MemberLookup.buildMethodSignature(m));
    }

    @Test
    public void buildMethodSignature_noArgs() {
        IMethod m = MemberLookup.getMethod(RuntimeClass.of(Child.class), "greet");
        assertEquals("greet()", MemberLookup.buildMethodSignature(m));
    }
}
