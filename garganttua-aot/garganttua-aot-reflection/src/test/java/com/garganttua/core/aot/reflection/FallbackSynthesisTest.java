package com.garganttua.core.aot.reflection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import com.garganttua.core.aot.commons.AOTRegistry;
import com.garganttua.core.reflection.IClass;

/**
 * Verifies that {@link AOTReflectionProvider} never throws on a missing
 * registry entry — it synthesizes a type-identity descriptor from the
 * {@code Class<?>} on the fly. This is the contract that ended the
 * "il manque X" hand-curated seed loop.
 */
class FallbackSynthesisTest {

    // Random user-land types that nobody seeded, nobody @Reflected'd.
    interface ISomeUnknownIface {}
    public static class SomeUnknownClass {
        public String greet(String who) { return "hi " + who; }
        public static int staticThing() { return 42; }
        public String publicField = "x";
    }
    enum SomeUnknownEnum { A, B }

    @Test
    void getClass_returns_synthesized_descriptor_for_unregistered_class() {
        AOTReflectionProvider provider = new AOTReflectionProvider();
        IClass<SomeUnknownClass> desc = provider.getClass(SomeUnknownClass.class);
        assertNotNull(desc);
        assertEquals(SomeUnknownClass.class.getName(), desc.getName());
        // Same lookup twice → cached, same instance.
        assertSame(desc, provider.getClass(SomeUnknownClass.class));
    }

    @Test
    void getClass_preserves_interface_flag() {
        AOTReflectionProvider provider = new AOTReflectionProvider();
        IClass<ISomeUnknownIface> desc = provider.getClass(ISomeUnknownIface.class);
        assertTrue(desc.isInterface(),
                "Synthesized descriptor must honour Class.isInterface()");
    }

    @Test
    void getClass_preserves_enum_flag() {
        AOTReflectionProvider provider = new AOTReflectionProvider();
        IClass<SomeUnknownEnum> desc = provider.getClass(SomeUnknownEnum.class);
        assertTrue(desc.isEnum());
    }

    @Test
    void forName_synthesizes_for_reachable_unregistered_class() throws Exception {
        AOTReflectionProvider provider = new AOTReflectionProvider();
        IClass<?> desc = provider.forName(AtomicLong.class.getName());
        assertNotNull(desc);
        assertEquals(AtomicLong.class.getName(), desc.getName());
    }

    @Test
    void forName_still_throws_for_truly_unreachable_class() {
        AOTReflectionProvider provider = new AOTReflectionProvider();
        assertThrows(ClassNotFoundException.class,
                () -> provider.forName("com.example.DefinitelyDoesNotExist"));
    }

    @Test
    void supports_remains_strict_for_hybrid_mode() {
        AOTReflectionProvider provider = new AOTReflectionProvider();
        // Pre-condition: clear the registry's view of this random type, in
        // case a previous test cached it via fallback.
        // Note: AOTRegistry is a singleton; we use a class never touched by
        // the seed nor by other tests in this module.
        Class<?> never = NeverQueried.class;
        // The synthesis ran for other types may have cached them, so we use
        // this fresh marker.
        boolean alreadyRegistered = AOTRegistry.getInstance().contains(never.getName());
        if (!alreadyRegistered) {
            assertEquals(false, provider.supports(never),
                    "supports() must NOT claim ownership of unregistered types — "
                    + "hybrid mode relies on this to let runtime-reflection win");
        }
    }

    /** Marker used solely by the hybrid-mode test — never queried elsewhere. */
    static class NeverQueried {}

    // --- Member-level lazy fallback ---

    @Test
    void getMethod_falls_back_to_live_class_when_descriptor_misses() throws Exception {
        // Shallow descriptor (no methods array) — getMethod must still find
        // the method by synthesising from the live Class<?>.
        AOTReflectionProvider provider = new AOTReflectionProvider();
        IClass<SomeUnknownClass> desc = provider.getClass(SomeUnknownClass.class);
        com.garganttua.core.reflection.IMethod m = desc.getMethod("greet",
                provider.getClass(String.class));
        assertNotNull(m);
        assertEquals("greet", m.getName());
    }

    @Test
    void getDeclaredMethod_falls_back_to_live_class() throws Exception {
        AOTReflectionProvider provider = new AOTReflectionProvider();
        IClass<SomeUnknownClass> desc = provider.getClass(SomeUnknownClass.class);
        com.garganttua.core.reflection.IMethod m = desc.getDeclaredMethod("staticThing");
        assertNotNull(m);
        assertEquals("staticThing", m.getName());
    }

    @Test
    void getField_falls_back_to_live_class() throws Exception {
        AOTReflectionProvider provider = new AOTReflectionProvider();
        IClass<SomeUnknownClass> desc = provider.getClass(SomeUnknownClass.class);
        com.garganttua.core.reflection.IField f = desc.getField("publicField");
        assertNotNull(f);
        assertEquals("publicField", f.getName());
    }

}
