package com.garganttua.core.mutex;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.JdkClass;

class MutexNameTest {

    private static final IClass<? extends IMutex> MUTEX_A = JdkClass.ofUnchecked(TestMutexA.class);
    private static final IClass<? extends IMutex> MUTEX_B = JdkClass.ofUnchecked(TestMutexB.class);

    @Test
    void testConstructorWithValidArguments() {
        MutexName mutexName = new MutexName(MUTEX_A, "user-table");

        assertEquals(MUTEX_A, mutexName.type());
        assertEquals("user-table", mutexName.name());
    }

    @Test
    void testConstructorTrimsWhitespace() {
        MutexName mutexName = new MutexName(MUTEX_A, "  user-table  ");

        assertEquals(MUTEX_A, mutexName.type());
        assertEquals("user-table", mutexName.name());
    }

    @Test
    void testConstructorRejectsNullType() {
        assertThrows(NullPointerException.class, () -> {
            new MutexName(null, "name");
        });
    }

    @Test
    void testConstructorRejectsNullName() {
        assertThrows(NullPointerException.class, () -> {
            new MutexName(MUTEX_A, null);
        });
    }

    @Test
    void testConstructorRejectsEmptyName() {
        assertThrows(IllegalArgumentException.class, () -> {
            new MutexName(MUTEX_A, "");
        });
    }

    @Test
    void testConstructorRejectsWhitespaceOnlyName() {
        assertThrows(IllegalArgumentException.class, () -> {
            new MutexName(MUTEX_A, "   ");
        });
    }

    @Test
    void testFromStringRejectsNullInput() {
        assertThrows(NullPointerException.class, () -> {
            MutexName.fromString(null);
        });
    }

    @Test
    void testFromStringRejectsEmptyString() {
        assertThrows(IllegalArgumentException.class, () -> {
            MutexName.fromString("");
        });
    }

    @Test
    void testFromStringRejectsWhitespaceOnly() {
        assertThrows(IllegalArgumentException.class, () -> {
            MutexName.fromString("   ");
        });
    }

    @Test
    void testFromStringRejectsMissingSeparator() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            MutexName.fromString("TestMutexA-user-table");
        });

        assertTrue(exception.getMessage().contains("missing"));
        assertTrue(exception.getMessage().contains("::"));
    }

    @Test
    void testFromStringRejectsSingleColon() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            MutexName.fromString("TestMutexA:user-table");
        });

        assertTrue(exception.getMessage().contains("missing"));
    }

    @Test
    void testFromStringRejectsEmptyType() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            MutexName.fromString("::user-table");
        });

        assertTrue(exception.getMessage().contains("type") ||
                exception.getMessage().contains("empty"));
    }

    @Test
    void testFromStringRejectsEmptyName() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            MutexName.fromString("com.garganttua.core.mutex.TestMutexA::");
        });

        assertTrue(exception.getMessage().contains("name") ||
                exception.getMessage().contains("empty"));
    }

    @Test
    void testFromStringRejectsMultipleSeparators() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            MutexName.fromString("TestMutexA::TestMutexB::user-table");
        });

        assertTrue(exception.getMessage().contains("exactly one"));
    }

    @Test
    void testFromStringCurrentlyUnsupported() {
        // fromString requires IReflectionProvider to resolve class names into IClass
        // This is currently deferred (throws UnsupportedOperationException)
        assertThrows(UnsupportedOperationException.class, () -> {
            MutexName.fromString("com.garganttua.core.mutex.TestMutexA::user-table");
        });
    }

    @Test
    void testToStringFormat() {
        MutexName mutexName = new MutexName(MUTEX_A, "user-table");

        assertEquals("com.garganttua.core.mutex.TestMutexA::user-table", mutexName.toString());
    }

    @Test
    void testEquality() {
        MutexName name1 = new MutexName(MUTEX_A, "user-table");
        MutexName name2 = new MutexName(MUTEX_A, "user-table");
        MutexName name3 = new MutexName(MUTEX_B, "user-table");

        assertEquals(name1, name2);
        assertNotEquals(name1, name3);
    }

    @Test
    void testEqualityWithDifferentNames() {
        MutexName name1 = new MutexName(MUTEX_A, "user-table");
        MutexName name2 = new MutexName(MUTEX_A, "session-store");

        assertNotEquals(name1, name2);
    }

    @Test
    void testHashCode() {
        MutexName name1 = new MutexName(MUTEX_A, "user-table");
        MutexName name2 = new MutexName(MUTEX_A, "user-table");

        assertEquals(name1.hashCode(), name2.hashCode());
    }

    @Test
    void testSeparatorConstant() {
        assertEquals("::", MutexName.SEPARATOR);
    }

}
