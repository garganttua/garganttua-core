package com.garganttua.core.reflection.constructors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IConstructor;
import com.garganttua.core.reflection.runtime.RuntimeClass;

/**
 * Behaviour tests for the {@link Constructors} static helper.
 */
public class ConstructorsBehaviourTest {

    public static class Sample {
        public Sample() {
        }

        public Sample(String a, int b) {
        }

        private Sample(boolean flag) {
        }
    }

    @SuppressWarnings("unchecked")
    private static IConstructor<Sample> ctor(IClass<?>... params) {
        try {
            return (IConstructor<Sample>) (IConstructor<?>) RuntimeClass.of(Sample.class).getDeclaredConstructor(params);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new AssertionError("constructor not found", e);
        }
    }

    @Test
    public void isPublic_trueForPublicCtor() {
        assertTrue(Constructors.isPublic(ctor()));
        assertFalse(Constructors.isPrivate(ctor()));
    }

    @Test
    public void isPrivate_trueForPrivateCtor() {
        IConstructor<Sample> c = ctor(RuntimeClass.of(boolean.class));
        assertTrue(Constructors.isPrivate(c));
        assertFalse(Constructors.isPublic(c));
    }

    @Test
    public void pretty_noArgsRendersEmptyParens() {
        assertEquals("Sample()", Constructors.pretty(ctor()));
    }

    @Test
    public void pretty_rendersParamTypes() {
        IConstructor<Sample> c = ctor(RuntimeClass.of(String.class), RuntimeClass.of(int.class));
        assertEquals("Sample(String, int)", Constructors.pretty(c));
    }

    @Test
    public void prettyColored_containsAnsiAndClassName() {
        String s = Constructors.prettyColored(ctor());
        assertTrue(s.contains("["));
        assertTrue(s.contains("Sample"));
    }

    @Test
    public void parameterTypesMatch_noArgMatchesEmpty() {
        assertTrue(Constructors.parameterTypesMatch(ctor()));
        assertTrue(Constructors.parameterTypesMatch(ctor(), (IClass<?>[]) null));
    }

    @Test
    public void parameterTypesMatch_noArgRejectsArgs() {
        assertFalse(Constructors.parameterTypesMatch(ctor(), RuntimeClass.of(String.class)));
    }

    @Test
    public void parameterTypesMatch_exactSignature() {
        IConstructor<Sample> c = ctor(RuntimeClass.of(String.class), RuntimeClass.of(int.class));
        assertTrue(Constructors.parameterTypesMatch(c, RuntimeClass.of(String.class), RuntimeClass.of(int.class)));
    }

    @Test
    public void parameterTypesMatch_wrongCount() {
        IConstructor<Sample> c = ctor(RuntimeClass.of(String.class), RuntimeClass.of(int.class));
        assertFalse(Constructors.parameterTypesMatch(c, RuntimeClass.of(String.class)));
    }

    @Test
    public void parameterTypesMatch_incompatibleType() {
        IConstructor<Sample> c = ctor(RuntimeClass.of(String.class), RuntimeClass.of(int.class));
        assertFalse(Constructors.parameterTypesMatch(c, RuntimeClass.of(Integer.class), RuntimeClass.of(int.class)));
    }
}
