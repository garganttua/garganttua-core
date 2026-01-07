package com.garganttua.core.mutex;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MutexNameTest {

    @Test
    void testConstructorWithValidArguments() {
        MutexName mutexName = new MutexName(TestMutexA.class, "user-table");

        assertEquals(TestMutexA.class, mutexName.type());
        assertEquals("user-table", mutexName.name());
    }

    @Test
    void testConstructorTrimsWhitespace() {
        MutexName mutexName = new MutexName(TestMutexA.class, "  user-table  ");

        assertEquals(TestMutexA.class, mutexName.type());
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
            new MutexName(TestMutexA.class, null);
        });
    }

    @Test
    void testConstructorRejectsEmptyName() {
        assertThrows(IllegalArgumentException.class, () -> {
            new MutexName(TestMutexA.class, "");
        });
    }

    @Test
    void testConstructorRejectsWhitespaceOnlyName() {
        assertThrows(IllegalArgumentException.class, () -> {
            new MutexName(TestMutexA.class, "   ");
        });
    }

    @Test
    void testFromStringWithValidFormat() {
        MutexName mutexName = MutexName.fromString("TestMutexA::user-table");

        assertEquals(TestMutexA.class, mutexName.type());
        assertEquals("user-table", mutexName.name());
    }

    @Test
    void testFromStringTrimsWhitespace() {
        MutexName mutexName = MutexName.fromString("  TestMutexA  ::  user-table  ");

        assertEquals(TestMutexA.class, mutexName.type());
        assertEquals("user-table", mutexName.name());
    }

    @Test
    void testFromStringWithComplexNames() {
        MutexName mutexName = MutexName.fromString("TestMutexB::session_store-v2");

        assertEquals(TestMutexB.class, mutexName.type());
        assertEquals("session_store-v2", mutexName.name());
    }

    @Test
    void testFromStringWithFullyQualifiedClassName() {
        MutexName mutexName = MutexName.fromString("com.garganttua.core.mutex.TestMutexA::user-table");

        assertEquals(TestMutexA.class, mutexName.type());
        assertEquals("user-table", mutexName.name());
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
            MutexName.fromString("TestMutexA::");
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
    void testFromStringRejectsUnknownClass() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            MutexName.fromString("UnknownMutexClass::user-table");
        });

        assertTrue(exception.getMessage().contains("not found") ||
                exception.getMessage().contains("class"));
    }

    @Test
    void testFromStringRejectsNonIMutexClass() {
        // String class doesn't implement IMutex
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            MutexName.fromString("java.lang.String::user-table");
        });

        assertTrue(exception.getMessage().contains("IMutex") ||
                exception.getMessage().contains("implement"));
    }

    @Test
    void testToStringFormat() {
        MutexName mutexName = new MutexName(TestMutexA.class, "user-table");

        assertEquals("TestMutexA::user-table", mutexName.toString());
    }

    @Test
    void testToStringUsesSimpleClassName() {
        // Even when created with fully qualified name, toString should use simple name
        MutexName mutexName = MutexName.fromString("com.garganttua.core.mutex.TestMutexA::user-table");

        assertEquals("TestMutexA::user-table", mutexName.toString());
    }

    @Test
    void testRoundTripConversionWithSimpleName() {
        String original = "TestMutexB::session-store";
        MutexName mutexName = MutexName.fromString(original);
        String result = mutexName.toString();

        assertEquals(original, result);
    }

    @Test
    void testEquality() {
        MutexName name1 = new MutexName(TestMutexA.class, "user-table");
        MutexName name2 = new MutexName(TestMutexA.class, "user-table");
        MutexName name3 = new MutexName(TestMutexB.class, "user-table");

        assertEquals(name1, name2);
        assertNotEquals(name1, name3);
    }

    @Test
    void testEqualityWithDifferentNames() {
        MutexName name1 = new MutexName(TestMutexA.class, "user-table");
        MutexName name2 = new MutexName(TestMutexA.class, "session-store");

        assertNotEquals(name1, name2);
    }

    @Test
    void testHashCode() {
        MutexName name1 = new MutexName(TestMutexA.class, "user-table");
        MutexName name2 = new MutexName(TestMutexA.class, "user-table");

        assertEquals(name1.hashCode(), name2.hashCode());
    }

    @Test
    void testSeparatorConstant() {
        assertEquals("::", MutexName.SEPARATOR);
    }

}
