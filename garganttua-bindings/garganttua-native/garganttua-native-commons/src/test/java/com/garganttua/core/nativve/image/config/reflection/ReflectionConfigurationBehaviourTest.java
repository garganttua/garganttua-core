package com.garganttua.core.nativve.image.config.reflection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.garganttua.core.nativve.IReflectionConfigurationEntry;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.JdkClass;

/**
 * Behaviour tests for {@link ReflectionConfiguration}: in-memory entry list
 * management (add with name de-dup, remove, update, find), JSON save/load
 * round-trips, and the graceful empty-list fallback when a file cannot be read.
 */
public class ReflectionConfigurationBehaviourTest {

    private ReflectionConfiguration emptyConfig() {
        ReflectionConfiguration c = new ReflectionConfiguration();
        c.setEntries(new ArrayList<>());
        return c;
    }

    @Test
    public void addEntryAppendsToTheList() {
        ReflectionConfiguration c = emptyConfig();
        c.addEntry(new ReflectConfigEntry("a.A"));
        c.addEntry(new ReflectConfigEntry("b.B"));

        List<String> names = c.getEntries().stream()
                .map(IReflectionConfigurationEntry::getName).toList();
        assertEquals(List.of("a.A", "b.B"), names);
    }

    @Test
    public void addEntryWithDuplicateNameReplacesTheExistingOne() {
        ReflectionConfiguration c = emptyConfig();
        ReflectConfigEntry first = new ReflectConfigEntry("dup.X");
        ReflectConfigEntry second = new ReflectConfigEntry("dup.X");
        second.setAllDeclaredFields(true);

        c.addEntry(first);
        c.addEntry(second); // addEntry removes by name first, then adds

        assertEquals(1, c.getEntries().size());
        assertTrue(((ReflectConfigEntry) c.getEntries().get(0)).isAllDeclaredFields(),
                "the replacing entry's flags must win");
    }

    @Test
    public void removeEntryDropsMatchingNameAndKeepsOthers() {
        ReflectionConfiguration c = emptyConfig();
        c.addEntry(new ReflectConfigEntry("keep.A"));
        c.addEntry(new ReflectConfigEntry("drop.B"));

        c.removeEntry(new ReflectConfigEntry("drop.B"));

        List<String> names = c.getEntries().stream()
                .map(IReflectionConfigurationEntry::getName).toList();
        assertEquals(List.of("keep.A"), names);
    }

    @Test
    public void removeEntryWithNoMatchLeavesListUnchanged() {
        ReflectionConfiguration c = emptyConfig();
        c.addEntry(new ReflectConfigEntry("only.One"));

        c.removeEntry(new ReflectConfigEntry("never.There"));

        assertEquals(1, c.getEntries().size());
        assertEquals("only.One", c.getEntries().get(0).getName());
    }

    @Test
    public void updateEntryReplacesInPlaceAtSameIndex() {
        ReflectionConfiguration c = emptyConfig();
        c.addEntry(new ReflectConfigEntry("u.First"));
        c.addEntry(new ReflectConfigEntry("u.Target"));
        c.addEntry(new ReflectConfigEntry("u.Last"));

        ReflectConfigEntry replacement = new ReflectConfigEntry("u.Target");
        replacement.setQueryAllDeclaredMethods(true);
        c.updateEntry(replacement);

        // order preserved, replacement sits where the old one was (index 1)
        assertEquals(3, c.getEntries().size());
        assertEquals("u.Target", c.getEntries().get(1).getName());
        assertTrue(((ReflectConfigEntry) c.getEntries().get(1)).isQueryAllDeclaredMethods());
    }

    @Test
    public void updateEntryWithUnknownNameIsANoOp() {
        ReflectionConfiguration c = emptyConfig();
        c.addEntry(new ReflectConfigEntry("present.A"));

        c.updateEntry(new ReflectConfigEntry("absent.Z"));

        assertEquals(1, c.getEntries().size());
        assertEquals("present.A", c.getEntries().get(0).getName());
    }

    @Test
    public void findEntryByTypeReturnsMatchAndEmptyForMiss() {
        ReflectionConfiguration c = emptyConfig();
        c.addEntry(new ReflectConfigEntry("java.lang.String"));

        IClass<?> hit = JdkClass.of(String.class);
        IClass<?> miss = JdkClass.of(Integer.class);

        Optional<IReflectionConfigurationEntry> found = c.findEntryByType(hit);
        assertTrue(found.isPresent());
        assertEquals("java.lang.String", found.get().getName());

        assertTrue(c.findEntryByType(miss).isEmpty());
    }

    @Test
    public void saveToFileThenLoadFromFileRoundTripsEntries(@TempDir Path dir) throws IOException {
        File f = dir.resolve("reflect-config.json").toFile();
        ReflectionConfiguration c = emptyConfig();
        ReflectConfigEntry e = new ReflectConfigEntry("round.Trip");
        e.setAllPublicClasses(true);
        c.addEntry(e);

        c.saveToFile(f);
        assertTrue(f.exists() && f.length() > 0);

        ReflectionConfiguration loaded = ReflectionConfiguration.loadFromFile(f);
        assertEquals(1, loaded.getEntries().size());
        assertEquals("round.Trip", loaded.getEntries().get(0).getName());
        assertTrue(((ReflectConfigEntry) loaded.getEntries().get(0)).isAllPublicClasses());
    }

    @Test
    public void loadFromMissingFileYieldsEmptyConfigurationRatherThanThrowing(@TempDir Path dir) {
        File missing = dir.resolve("nope.json").toFile();

        ReflectionConfiguration loaded = ReflectionConfiguration.loadFromFile(missing);

        assertTrue(loaded.getEntries().isEmpty(),
                "unreadable file must degrade to an empty entry list");
    }

    @Test
    public void loadFromMalformedJsonYieldsEmptyConfiguration(@TempDir Path dir) throws IOException {
        File f = dir.resolve("bad.json").toFile();
        Files.writeString(f.toPath(), "{ this is not valid json ]");

        ReflectionConfiguration loaded = ReflectionConfiguration.loadFromFile(f);

        assertTrue(loaded.getEntries().isEmpty());
    }

    @Test
    public void savedJsonOmitsDefaultFlagsButKeepsName(@TempDir Path dir) throws IOException {
        File f = dir.resolve("reflect-config.json").toFile();
        ReflectionConfiguration c = emptyConfig();
        c.addEntry(new ReflectConfigEntry("shape.Only"));

        c.saveToFile(f);

        String json = Files.readString(f.toPath());
        assertTrue(json.contains("shape.Only"), json);
        // NON_DEFAULT serialization: false flags are absent
        assertFalse(json.contains("allDeclaredFields"), json);
    }
}
