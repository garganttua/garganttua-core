package com.garganttua.core.configuration;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.garganttua.core.reflection.IClass;

/**
 * Format-agnostic view of a parsed configuration tree.
 *
 * <p>
 * A node is one of {@link NodeType#OBJECT object}, {@link NodeType#ARRAY array},
 * scalar {@link NodeType#VALUE value}, or {@link NodeType#NULL null}, exposing
 * navigation and typed extraction regardless of the underlying format.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 */
public interface IConfigurationNode {

    /** The structural kind of a configuration node. */
    enum NodeType {
        /** A keyed object with named children. */
        OBJECT,
        /** An ordered list of element nodes. */
        ARRAY,
        /** A scalar leaf value. */
        VALUE,
        /** An explicit null value. */
        NULL
    }

    /**
     * @return the structural kind of this node
     */
    NodeType type();

    /**
     * Looks up a child by key on an object node.
     *
     * @param key the child key
     * @return the child node, or empty if absent or this is not an object
     */
    Optional<IConfigurationNode> get(String key);

    /**
     * @return the named children of an object node, keyed by name
     */
    Map<String, IConfigurationNode> children();

    /**
     * @return the ordered elements of an array node
     */
    List<IConfigurationNode> elements();

    /**
     * @return this node's scalar value as text, or empty if not a scalar
     */
    Optional<String> asText();

    /**
     * Converts this node's value to the requested type.
     *
     * @param type the target type
     * @param <T>  the target type parameter
     * @return the converted value, or empty if conversion is not possible
     */
    <T> Optional<T> as(IClass<T> type);

    /**
     * @return the dot-notation path of this node from the configuration root
     */
    String path();

    /**
     * @return {@code true} if this node is an object
     */
    boolean isObject();

    /**
     * @return {@code true} if this node is an array
     */
    boolean isArray();

    /**
     * @return {@code true} if this node is a scalar value
     */
    boolean isValue();

    /**
     * @return {@code true} if this node represents an explicit null
     */
    boolean isNull();
}
