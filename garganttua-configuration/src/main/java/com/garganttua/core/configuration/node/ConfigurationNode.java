package com.garganttua.core.configuration.node;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.garganttua.core.configuration.IConfigurationNode;
import com.garganttua.core.reflection.IClass;

/**
 * Jackson-backed {@link IConfigurationNode} wrapping a {@link JsonNode} and tracking
 * its dot/bracket path within the parsed configuration tree.
 */
public class ConfigurationNode implements IConfigurationNode {

    private final JsonNode jsonNode;
    private final String path;

    /**
     * Creates a root node with an empty path.
     *
     * @param jsonNode the underlying Jackson node
     */
    public ConfigurationNode(JsonNode jsonNode) {
        this(jsonNode, "");
    }

    /**
     * Creates a node at the given path within the configuration tree.
     *
     * @param jsonNode the underlying Jackson node
     * @param path     the dot/bracket path of this node from the root
     */
    public ConfigurationNode(JsonNode jsonNode, String path) {
        this.jsonNode = jsonNode;
        this.path = path;
    }

    @Override
    public NodeType type() {
        if (this.jsonNode == null || this.jsonNode.isNull()) {
            return NodeType.NULL;
        }
        if (this.jsonNode.isObject()) {
            return NodeType.OBJECT;
        }
        if (this.jsonNode.isArray()) {
            return NodeType.ARRAY;
        }
        return NodeType.VALUE;
    }

    @Override
    public Optional<IConfigurationNode> get(String key) {
        if (this.jsonNode == null || !this.jsonNode.has(key)) {
            return Optional.empty();
        }
        var childPath = this.path.isEmpty() ? key : this.path + "." + key;
        return Optional.of(new ConfigurationNode(this.jsonNode.get(key), childPath));
    }

    @Override
    public Map<String, IConfigurationNode> children() {
        var result = new LinkedHashMap<String, IConfigurationNode>();
        if (this.jsonNode != null && this.jsonNode.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = this.jsonNode.fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                var childPath = this.path.isEmpty() ? entry.getKey() : this.path + "." + entry.getKey();
                result.put(entry.getKey(), new ConfigurationNode(entry.getValue(), childPath));
            }
        }
        return result;
    }

    @Override
    public List<IConfigurationNode> elements() {
        var result = new ArrayList<IConfigurationNode>();
        if (this.jsonNode != null && this.jsonNode.isArray()) {
            for (int i = 0; i < this.jsonNode.size(); i++) {
                var childPath = this.path + "[" + i + "]";
                result.add(new ConfigurationNode(this.jsonNode.get(i), childPath));
            }
        }
        return result;
    }

    @Override
    public Optional<String> asText() {
        if (this.jsonNode == null || this.jsonNode.isNull()) {
            return Optional.empty();
        }
        if (this.jsonNode.isValueNode()) {
            return Optional.of(this.jsonNode.asText());
        }
        return Optional.of(this.jsonNode.toString());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> as(IClass<T> type) {
        if (this.jsonNode == null || this.jsonNode.isNull()) {
            return Optional.empty();
        }
        var rawType = type.getType();
        if (rawType == String.class) {
            return Optional.of((T) this.jsonNode.asText());
        }
        if (rawType == Integer.class || rawType == int.class) {
            return Optional.of((T) Integer.valueOf(this.jsonNode.asInt()));
        }
        if (rawType == Long.class || rawType == long.class) {
            return Optional.of((T) Long.valueOf(this.jsonNode.asLong()));
        }
        if (rawType == Double.class || rawType == double.class) {
            return Optional.of((T) Double.valueOf(this.jsonNode.asDouble()));
        }
        if (rawType == Boolean.class || rawType == boolean.class) {
            return Optional.of((T) Boolean.valueOf(this.jsonNode.asBoolean()));
        }
        if (rawType == Float.class || rawType == float.class) {
            return Optional.of((T) Float.valueOf((float) this.jsonNode.asDouble()));
        }
        return Optional.empty();
    }

    @Override
    public String path() {
        return this.path;
    }

    @Override
    public boolean isObject() {
        return type() == NodeType.OBJECT;
    }

    @Override
    public boolean isArray() {
        return type() == NodeType.ARRAY;
    }

    @Override
    public boolean isValue() {
        return type() == NodeType.VALUE;
    }

    @Override
    public boolean isNull() {
        return type() == NodeType.NULL;
    }

    /**
     * Returns the underlying Jackson node backing this configuration node.
     *
     * @return the wrapped {@link JsonNode}, may be {@code null}
     */
    public JsonNode getJsonNode() {
        return this.jsonNode;
    }

    @Override
    public String toString() {
        return "ConfigurationNode{path='" + this.path + "', type=" + type() + "}";
    }
}
