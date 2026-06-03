package com.garganttua.core.reflection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.constructors.ConstructorAccessManager;
import com.garganttua.core.reflection.fields.FieldAccessManager;
import com.garganttua.core.reflection.methods.MethodAccessManager;
import com.garganttua.core.reflection.runtime.RuntimeClass;

/**
 * Behaviour tests for the scoped accessibility guards: {@link FieldAccessManager},
 * {@link MethodAccessManager} and {@link ConstructorAccessManager}.
 *
 * <p>Each guard makes the member accessible inside a try-with-resources block and
 * restores its original public/non-public accessibility on close.
 */
public class AccessManagerBehaviourTest {

    public static class Target {
        private String secret = "hidden";

        public Target() {
        }

        private Target(int ignored) {
        }

        private String reveal() {
            return secret;
        }
    }

    // ===== FieldAccessManager =====

    @Test
    public void field_makesPrivateFieldAccessibleThenRestores() throws Exception {
        IField field = RuntimeClass.of(Target.class).getDeclaredField("secret");
        Target target = new Target();

        // Private field on public class: original accessibility is false.
        try (FieldAccessManager mgr = new FieldAccessManager(field)) {
            assertTrue(field.canAccess(target), "field should be accessible within the guard");
        }
        assertFalse(field.canAccess(target), "field accessibility should be restored to false after close");
    }

    @Test
    public void field_forceConstructorAlsoGrantsAccess() throws Exception {
        IField field = RuntimeClass.of(Target.class).getDeclaredField("secret");
        Target target = new Target();
        try (FieldAccessManager mgr = new FieldAccessManager(field, true)) {
            assertTrue(field.canAccess(target));
        }
    }

    // ===== MethodAccessManager =====

    @Test
    public void method_makesPrivateMethodAccessibleThenRestores() throws Exception {
        IMethod method = RuntimeClass.of(Target.class).getDeclaredMethod("reveal");
        Target target = new Target();

        try (MethodAccessManager mgr = new MethodAccessManager(method)) {
            assertTrue(method.canAccess(target));
        }
        assertFalse(method.canAccess(target));
    }

    // ===== ConstructorAccessManager =====

    @Test
    public void constructor_makesPrivateCtorAccessibleThenRestores() throws Exception {
        IConstructor<Target> ctor = RuntimeClass.of(Target.class).getDeclaredConstructor(RuntimeClass.of(int.class));

        try (ConstructorAccessManager mgr = new ConstructorAccessManager(ctor)) {
            assertTrue(ctor.canAccess(null), "static-receiver canAccess(null) is true once accessible");
        }
        assertFalse(ctor.canAccess(null), "private constructor accessibility restored to false");
    }

    @Test
    public void publicConstructorStaysAccessibleAfterClose() throws Exception {
        IConstructor<Target> ctor = RuntimeClass.of(Target.class).getDeclaredConstructor();
        try (ConstructorAccessManager mgr = new ConstructorAccessManager(ctor)) {
            assertTrue(ctor.canAccess(null));
        }
        // public ctor on public class: original accessibility was true, remains accessible
        assertTrue(ctor.canAccess(null));
    }
}
