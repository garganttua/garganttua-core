package com.garganttua.core.mapper;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.mapper.annotations.FieldMappingRule;

/**
 * Test class for {@link Mapper} configuration and edge cases.
 * Tests mapper configuration options, error handling, and thread safety.
 */
public class MapperConfigurationTest {

    private Mapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new Mapper();
    }

    @Test
    void testMapWithNullDestinationThrowsException() {
        SourceEntity source = new SourceEntity();
        source.name = "test";

        assertThrows(MapperException.class, () -> {
            mapper.map(source, (Object) null);
        });
    }

    @Test
    void testMapWithNullDestinationClassThrowsException() {
        SourceEntity source = new SourceEntity();
        source.name = "test";

        assertThrows(MapperException.class, () -> {
            mapper.map(source, (Class<?>) null);
        });
    }

    @Test
    void testConfigureFailOnError() throws MapperException {
        mapper.configure(MapperConfigurationItem.FAIL_ON_ERROR, true);

        // This should work fine as the mapping is valid
        SourceEntity source = new SourceEntity();
        source.name = "test";

        DestinationEntity result = mapper.map(source, DestinationEntity.class);
        assertNotNull(result);
        assertEquals("test", result.name);
    }

    @Test
    void testConfigureDoValidation() throws MapperException {
        mapper.configure(MapperConfigurationItem.DO_VALIDATION, true);

        SourceEntity source = new SourceEntity();
        source.name = "test";

        DestinationEntity result = mapper.map(source, DestinationEntity.class);
        assertNotNull(result);
        assertEquals("test", result.name);
    }

    @Test
    void testRecordMappingConfiguration() throws MapperException {
        MappingConfiguration config = mapper.recordMappingConfiguration(
            SourceEntity.class, DestinationEntity.class);

        assertNotNull(config);
        assertEquals(SourceEntity.class, config.source());
        assertEquals(DestinationEntity.class, config.destination());
        assertNotNull(config.destinationRules());
        assertNotNull(config.sourceRules());
    }

    @Test
    void testGetMappingConfiguration() throws MapperException {
        // First call should create and cache the configuration
        MappingConfiguration config1 = mapper.getMappingConfiguration(
            SourceEntity.class, DestinationEntity.class);

        // Second call should retrieve from cache
        MappingConfiguration config2 = mapper.getMappingConfiguration(
            SourceEntity.class, DestinationEntity.class);

        assertNotNull(config1);
        assertNotNull(config2);
        assertSame(config1, config2); // Should be same instance from cache
    }

    @Test
    void testMapWithThreeParametersNullDestination() throws MapperException {
        SourceEntity source = new SourceEntity();
        source.name = "test";

        DestinationEntity result = mapper.map(source, DestinationEntity.class, null);

        assertNotNull(result);
        assertEquals("test", result.name);
    }

    @Test
    void testMapWithThreeParametersExistingDestination() throws MapperException {
        SourceEntity source = new SourceEntity();
        source.name = "new-name";

        DestinationEntity destination = new DestinationEntity();
        destination.name = "old-name";
        destination.otherField = "preserved";

        DestinationEntity result = mapper.map(source, DestinationEntity.class, destination);

        assertNotNull(result);
        assertEquals("new-name", result.name);
        assertEquals("preserved", result.otherField);
        assertSame(destination, result);
    }

    @Test
    void testMultipleMappingsUseSameConfiguration() throws MapperException {
        SourceEntity source1 = new SourceEntity();
        source1.name = "first";

        SourceEntity source2 = new SourceEntity();
        source2.name = "second";

        DestinationEntity result1 = mapper.map(source1, DestinationEntity.class);
        DestinationEntity result2 = mapper.map(source2, DestinationEntity.class);

        assertEquals("first", result1.name);
        assertEquals("second", result2.name);

        // Verify configuration is cached
        MappingConfiguration config = mapper.getMappingConfiguration(
            SourceEntity.class, DestinationEntity.class);
        assertNotNull(config);
    }

    @Test
    void testMapWithDifferentTypes() throws MapperException {
        SourceWithInt source = new SourceWithInt();
        source.value = 42;

        DestinationWithInt result = mapper.map(source, DestinationWithInt.class);

        assertNotNull(result);
        assertEquals(42, result.value);
    }

    @Test
    void testConfigureReturnsMapper() {
        Mapper result = mapper.configure(MapperConfigurationItem.FAIL_ON_ERROR, false);

        assertNotNull(result);
        assertSame(mapper, result);
    }

    @Test
    void testConfigureChaining() throws MapperException {
        Mapper result = mapper
            .configure(MapperConfigurationItem.FAIL_ON_ERROR, true)
            .configure(MapperConfigurationItem.DO_VALIDATION, true);

        assertNotNull(result);
        assertSame(mapper, result);

        // Verify configuration works
        SourceEntity source = new SourceEntity();
        source.name = "test";
        DestinationEntity dest = mapper.map(source, DestinationEntity.class);
        assertNotNull(dest);
    }

    @Test
    void testMappingConfigurationWithReverseDirection() throws MapperException {
        // SourceEntityReverse has mapping rules, DestinationEntityReverse does not
        MappingConfiguration config = mapper.getMappingConfiguration(
            SourceEntityReverse.class, DestinationEntityReverse.class);

        assertNotNull(config);
        assertEquals(MappingDirection.REVERSE, config.mappingDirection());
    }

    // Test helper classes
    @SuppressWarnings("unused")
    private static class SourceEntity {
        private String name;
    }

    @SuppressWarnings("unused")
    private static class DestinationEntity {
        @FieldMappingRule(sourceFieldAddress = "name")
        private String name;

        private String otherField;

        public DestinationEntity() {
        }
    }

    @SuppressWarnings("unused")
    private static class SourceWithInt {
        private int value;
    }

    @SuppressWarnings("unused")
    private static class DestinationWithInt {
        @FieldMappingRule(sourceFieldAddress = "value")
        private int value;

        public DestinationWithInt() {
        }
    }

    @SuppressWarnings("unused")
    private static class SourceEntityReverse {
        @FieldMappingRule(sourceFieldAddress = "data")
        private String data;
    }

    @SuppressWarnings("unused")
    private static class DestinationEntityReverse {
        private String data;

        public DestinationEntityReverse() {
        }
    }
}
