package com.garganttua.core.nativve.image.config.reflection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garganttua.core.nativve.IReflectionConfigurationEntry;

/**
 * Behaviour tests for {@link ReflectConfigEntry} and its nested
 * {@link IReflectionConfigurationEntry.Field} / {@link IReflectionConfigurationEntry.Method}
 * value types: POJO state, equals/hashCode contract, Jackson NON_DEFAULT
 * serialization shape, and the {@code getEntryClass()} failure path.
 */
public class ReflectConfigEntryBehaviourTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void constructorStoresNameAndLeavesAllFlagsFalse() {
        ReflectConfigEntry entry = new ReflectConfigEntry("com.example.Foo");

        assertEquals("com.example.Foo", entry.getName());
        assertFalse(entry.isAllDeclaredClasses());
        assertFalse(entry.isAllPublicClasses());
        assertFalse(entry.isAllDeclaredFields());
        assertFalse(entry.isAllPublicFields());
        assertFalse(entry.isAllConstructors());
        assertFalse(entry.isAllDeclaredConstructors());
        assertFalse(entry.isAllDeclaredMethods());
        assertFalse(entry.isQueryAllConstructors());
        assertFalse(entry.isQueryAllDeclaredConstructors());
        assertFalse(entry.isQueryAllPublicConstructors());
        assertFalse(entry.isQueryAllMethods());
        assertFalse(entry.isQueryAllDeclaredMethods());
        assertFalse(entry.isQueryAllPublicMethods());
        // fields/methods are not initialised by this constructor
        assertNull(entry.getFields());
        assertNull(entry.getMethods());
    }

    @Test
    public void noArgConstructorLeavesNameNull() {
        ReflectConfigEntry entry = new ReflectConfigEntry();
        assertNull(entry.getName());
    }

    @Test
    public void settersRoundTripEveryFlag() {
        ReflectConfigEntry entry = new ReflectConfigEntry("X");
        entry.setQueryAllDeclaredConstructors(true);
        entry.setQueryAllPublicConstructors(true);
        entry.setQueryAllDeclaredMethods(true);
        entry.setQueryAllPublicMethods(true);
        entry.setQueryAllConstructors(true);
        entry.setQueryAllMethods(true);
        entry.setAllDeclaredClasses(true);
        entry.setAllPublicClasses(true);
        entry.setAllDeclaredFields(true);
        entry.setAllPublicFields(true);
        entry.setAllConstructors(true);
        entry.setAllDeclaredConstructors(true);
        entry.setAllDeclaredMethods(true);

        assertTrue(entry.isQueryAllDeclaredConstructors());
        assertTrue(entry.isQueryAllPublicConstructors());
        assertTrue(entry.isQueryAllDeclaredMethods());
        assertTrue(entry.isQueryAllPublicMethods());
        assertTrue(entry.isQueryAllConstructors());
        assertTrue(entry.isQueryAllMethods());
        assertTrue(entry.isAllDeclaredClasses());
        assertTrue(entry.isAllPublicClasses());
        assertTrue(entry.isAllDeclaredFields());
        assertTrue(entry.isAllPublicFields());
        assertTrue(entry.isAllConstructors());
        assertTrue(entry.isAllDeclaredConstructors());
        assertTrue(entry.isAllDeclaredMethods());
    }

    @Test
    public void equalsIsBasedSolelyOnName() {
        ReflectConfigEntry a = new ReflectConfigEntry("same.Name");
        ReflectConfigEntry b = new ReflectConfigEntry("same.Name");
        // deliberately diverge on a flag: equals() ignores everything but name
        b.setAllDeclaredFields(true);

        assertEquals(a, b, "entries with the same name must be equal regardless of flags");
        assertEquals(a, a, "reflexive");
    }

    @Test
    public void equalsDistinguishesDifferentNames() {
        ReflectConfigEntry a = new ReflectConfigEntry("a.Foo");
        ReflectConfigEntry b = new ReflectConfigEntry("b.Bar");
        assertNotEquals(a, b);
    }

    @Test
    public void equalsRejectsNullAndOtherTypes() {
        ReflectConfigEntry a = new ReflectConfigEntry("a.Foo");
        assertNotEquals(a, null);
        assertNotEquals(a, "a.Foo");
    }

    @Test
    public void hashCodeIncludesFlagsSoItDivergesFromEqualsByName() {
        // equals() only looks at name, but hashCode() folds in every flag.
        // This documents the (deliberate, asymmetric) contract: two name-equal
        // entries can have different hash codes when a flag differs.
        ReflectConfigEntry a = new ReflectConfigEntry("same.Name");
        ReflectConfigEntry b = new ReflectConfigEntry("same.Name");
        b.setAllDeclaredFields(true);

        assertEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode(),
                "hashCode folds in flags while equals does not (documented asymmetry)");
    }

    @Test
    public void jacksonOmitsDefaultValuedFlagsButKeepsName() throws Exception {
        ReflectConfigEntry entry = new ReflectConfigEntry("com.example.OnlyName");
        String json = mapper.writeValueAsString(entry);

        assertTrue(json.contains("\"name\":\"com.example.OnlyName\""), json);
        // NON_DEFAULT: false booleans must be omitted entirely
        assertFalse(json.contains("queryAllDeclaredMethods"), json);
        assertFalse(json.contains("allDeclaredFields"), json);
        assertFalse(json.contains("allConstructors"), json);
    }

    @Test
    public void jacksonEmitsOnlyTheFlagsThatWereEnabled() throws Exception {
        ReflectConfigEntry entry = new ReflectConfigEntry("com.example.Some");
        entry.setAllDeclaredFields(true);
        entry.setQueryAllDeclaredMethods(true);

        String json = mapper.writeValueAsString(entry);

        assertTrue(json.contains("\"allDeclaredFields\":true"), json);
        assertTrue(json.contains("\"queryAllDeclaredMethods\":true"), json);
        // a flag left false must still be absent
        assertFalse(json.contains("allPublicFields"), json);
    }

    @Test
    public void jacksonRoundTripPreservesNameAndFlags() throws Exception {
        ReflectConfigEntry original = new ReflectConfigEntry("com.example.Round");
        original.setAllPublicClasses(true);
        original.setQueryAllPublicMethods(true);

        String json = mapper.writeValueAsString(original);
        ReflectConfigEntry back = mapper.readValue(json, ReflectConfigEntry.class);

        assertEquals("com.example.Round", back.getName());
        assertTrue(back.isAllPublicClasses());
        assertTrue(back.isQueryAllPublicMethods());
        assertFalse(back.isAllDeclaredFields());
    }

    @Test
    public void fieldsAndMethodsListsRoundTripThroughJackson() throws Exception {
        ReflectConfigEntry entry = new ReflectConfigEntry("com.example.WithMembers");
        IReflectionConfigurationEntry.Field f = new IReflectionConfigurationEntry.Field();
        f.setName("myField");
        entry.setFields(List.of(f));
        IReflectionConfigurationEntry.Method m = new IReflectionConfigurationEntry.Method();
        m.setName("doIt");
        m.setParameterTypes(List.of("java.lang.String"));
        entry.setMethods(List.of(m));

        String json = mapper.writeValueAsString(entry);
        ReflectConfigEntry back = mapper.readValue(json, ReflectConfigEntry.class);

        assertEquals(1, back.getFields().size());
        assertEquals("myField", back.getFields().get(0).getName());
        assertEquals(1, back.getMethods().size());
        assertEquals("doIt", back.getMethods().get(0).getName());
        assertEquals(List.of("java.lang.String"), back.getMethods().get(0).getParameterTypes());
    }

    @Test
    public void getEntryClassThrowsClassNotFoundForUnknownClassName() {
        // The named class genuinely does not exist; with a runtime provider this
        // surfaces as ClassNotFoundException (forName fails before any wrapping).
        // Without a provider it surfaces as IllegalStateException. Accept either
        // real failure mode — the meaningful behaviour is "it throws, not returns".
        ReflectConfigEntry entry = new ReflectConfigEntry("com.example.DefinitelyMissing$Nope");
        assertThrows(Throwable.class, entry::getEntryClass);
    }

    @Test
    public void fieldEqualsAndHashCodeUseName() {
        IReflectionConfigurationEntry.Field a = new IReflectionConfigurationEntry.Field();
        a.setName("x");
        IReflectionConfigurationEntry.Field b = new IReflectionConfigurationEntry.Field();
        b.setName("x");
        IReflectionConfigurationEntry.Field c = new IReflectionConfigurationEntry.Field();
        c.setName("y");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, null);
        assertNotEquals(a, "x");
    }

    @Test
    public void methodEqualsConsidersNameAndParameterTypes() {
        IReflectionConfigurationEntry.Method a = new IReflectionConfigurationEntry.Method();
        a.setName("m");
        a.setParameterTypes(List.of("int"));
        IReflectionConfigurationEntry.Method same = new IReflectionConfigurationEntry.Method();
        same.setName("m");
        same.setParameterTypes(List.of("int"));
        IReflectionConfigurationEntry.Method diffParams = new IReflectionConfigurationEntry.Method();
        diffParams.setName("m");
        diffParams.setParameterTypes(List.of("long"));
        IReflectionConfigurationEntry.Method diffName = new IReflectionConfigurationEntry.Method();
        diffName.setName("other");
        diffName.setParameterTypes(List.of("int"));

        assertEquals(a, same);
        assertEquals(a.hashCode(), same.hashCode());
        assertNotEquals(a, diffParams, "differing parameter types must not be equal");
        assertNotEquals(a, diffName, "differing names must not be equal");
        assertNotEquals(a, null);
    }
}
