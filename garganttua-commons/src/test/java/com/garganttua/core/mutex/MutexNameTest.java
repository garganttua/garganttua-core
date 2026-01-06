package com.garganttua.core.mutex;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MutexNameTest {

    @Test
    void testConstructorWithValidArguments() {
        MutexName mutexName = new MutexName("database", "user-table");

        assertEquals("database", mutexName.type());
        assertEquals("user-table", mutexName.name());
    }

    @Test
    void testConstructorTrimsWhitespace() {
        MutexName mutexName = new MutexName("  database  ", "  user-table  ");

        assertEquals("database", mutexName.type());
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
            new MutexName("type", null);
        });
    }

    @Test
    void testConstructorRejectsEmptyType() {
        assertThrows(IllegalArgumentException.class, () -> {
            new MutexName("", "name");
        });
    }

    @Test
    void testConstructorRejectsEmptyName() {
        assertThrows(IllegalArgumentException.class, () -> {
            new MutexName("type", "");
        });
    }

    @Test
    void testConstructorRejectsWhitespaceOnlyType() {
        assertThrows(IllegalArgumentException.class, () -> {
            new MutexName("   ", "name");
        });
    }

    @Test
    void testConstructorRejectsWhitespaceOnlyName() {
        assertThrows(IllegalArgumentException.class, () -> {
            new MutexName("type", "   ");
        });
    }

    @Test
    void testFromStringWithValidFormat() {
        MutexName mutexName = MutexName.fromString("database::user-table");

        assertEquals("database", mutexName.type());
        assertEquals("user-table", mutexName.name());
    }

    @Test
    void testFromStringTrimsWhitespace() {
        MutexName mutexName = MutexName.fromString("  database  ::  user-table  ");

        assertEquals("database", mutexName.type());
        assertEquals("user-table", mutexName.name());
    }

    @Test
    void testFromStringWithComplexNames() {
        MutexName mutexName = MutexName.fromString("cache::session_store-v2");

        assertEquals("cache", mutexName.type());
        assertEquals("session_store-v2", mutexName.name());
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
            MutexName.fromString("database-user-table");
        });

        assertTrue(exception.getMessage().contains("missing"));
        assertTrue(exception.getMessage().contains("::"));
    }

    @Test
    void testFromStringRejectsSingleColon() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            MutexName.fromString("database:user-table");
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
            MutexName.fromString("database::");
        });

        assertTrue(exception.getMessage().contains("name") ||
                exception.getMessage().contains("empty"));
    }

    @Test
    void testFromStringRejectsMultipleSeparators() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            MutexName.fromString("database::cache::user-table");
        });

        assertTrue(exception.getMessage().contains("exactly one"));
    }

    @Test
    void testToStringFormat() {
        MutexName mutexName = new MutexName("database", "user-table");

        assertEquals("database::user-table", mutexName.toString());
    }

    @Test
    void testRoundTripConversion() {
        String original = "cache::session-store";
        MutexName mutexName = MutexName.fromString(original);
        String result = mutexName.toString();

        assertEquals(original, result);
    }

    @Test
    void testEquality() {
        MutexName name1 = new MutexName("database", "user-table");
        MutexName name2 = new MutexName("database", "user-table");
        MutexName name3 = new MutexName("cache", "user-table");

        assertEquals(name1, name2);
        assertNotEquals(name1, name3);
    }

    @Test
    void testHashCode() {
        MutexName name1 = new MutexName("database", "user-table");
        MutexName name2 = new MutexName("database", "user-table");

        assertEquals(name1.hashCode(), name2.hashCode());
    }

    @Test
    void testSeparatorConstant() {
        assertEquals("::", MutexName.SEPARATOR);
    }

}
