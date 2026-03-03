package com.garganttua.core.configuration;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.garganttua.core.configuration.IConfigurationNode.NodeType;
import com.garganttua.core.configuration.node.ConfigurationNode;

class ConfigurationNodeTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        this.mapper = new ObjectMapper();
    }

    @Test
    void testObjectNode() {
        ObjectNode obj = this.mapper.createObjectNode();
        obj.put("name", "test");
        obj.put("value", 42);

        var node = new ConfigurationNode(obj);

        assertEquals(NodeType.OBJECT, node.type());
        assertTrue(node.isObject());
        assertFalse(node.isArray());
        assertFalse(node.isValue());
        assertFalse(node.isNull());
    }

    @Test
    void testGetChild() {
        ObjectNode obj = this.mapper.createObjectNode();
        obj.put("name", "test");

        var node = new ConfigurationNode(obj);
        var child = node.get("name");

        assertTrue(child.isPresent());
        assertEquals(NodeType.VALUE, child.get().type());
        assertEquals("test", child.get().asText().orElse(null));
    }

    @Test
    void testGetMissing() {
        ObjectNode obj = this.mapper.createObjectNode();
        var node = new ConfigurationNode(obj);

        assertTrue(node.get("missing").isEmpty());
    }

    @Test
    void testChildren() {
        ObjectNode obj = this.mapper.createObjectNode();
        obj.put("a", 1);
        obj.put("b", 2);

        var node = new ConfigurationNode(obj);
        var children = node.children();

        assertEquals(2, children.size());
        assertTrue(children.containsKey("a"));
        assertTrue(children.containsKey("b"));
    }

    @Test
    void testArrayNode() {
        ArrayNode arr = this.mapper.createArrayNode();
        arr.add("one");
        arr.add("two");
        arr.add("three");

        var node = new ConfigurationNode(arr);

        assertEquals(NodeType.ARRAY, node.type());
        assertTrue(node.isArray());

        var elements = node.elements();
        assertEquals(3, elements.size());
        assertEquals("one", elements.get(0).asText().orElse(null));
        assertEquals("two", elements.get(1).asText().orElse(null));
        assertEquals("three", elements.get(2).asText().orElse(null));
    }

    @Test
    void testValueTypes() {
        ObjectNode obj = this.mapper.createObjectNode();
        obj.put("str", "hello");
        obj.put("num", 123);
        obj.put("bool", true);
        obj.put("dec", 3.14);

        var node = new ConfigurationNode(obj);

        assertEquals("hello", node.get("str").get().as(String.class).orElse(null));
        assertEquals(123, node.get("num").get().as(Integer.class).orElse(null));
        assertEquals(true, node.get("bool").get().as(Boolean.class).orElse(null));
        assertEquals(3.14, node.get("dec").get().as(Double.class).orElse(null), 0.001);
    }

    @Test
    void testNullNode() {
        var node = new ConfigurationNode(null);

        assertEquals(NodeType.NULL, node.type());
        assertTrue(node.isNull());
        assertTrue(node.asText().isEmpty());
        assertTrue(node.as(String.class).isEmpty());
    }

    @Test
    void testNestedPath() {
        ObjectNode root = this.mapper.createObjectNode();
        ObjectNode child = this.mapper.createObjectNode();
        child.put("host", "localhost");
        root.set("database", child);

        var node = new ConfigurationNode(root);
        var db = node.get("database").get();

        assertEquals("database", db.path());

        var host = db.get("host").get();
        assertEquals("database.host", host.path());
    }

    @Test
    void testArrayPath() {
        ObjectNode root = this.mapper.createObjectNode();
        ArrayNode arr = this.mapper.createArrayNode();
        arr.add("a");
        arr.add("b");
        root.set("items", arr);

        var node = new ConfigurationNode(root);
        var items = node.get("items").get();
        var elements = items.elements();

        assertEquals("items[0]", elements.get(0).path());
        assertEquals("items[1]", elements.get(1).path());
    }
}
