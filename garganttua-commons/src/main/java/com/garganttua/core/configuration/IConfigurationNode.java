package com.garganttua.core.configuration;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.garganttua.core.reflection.IClass;

public interface IConfigurationNode {

    enum NodeType {
        OBJECT,
        ARRAY,
        VALUE,
        NULL
    }

    NodeType type();

    Optional<IConfigurationNode> get(String key);

    Map<String, IConfigurationNode> children();

    List<IConfigurationNode> elements();

    Optional<String> asText();

    <T> Optional<T> as(IClass<T> type);

    String path();

    boolean isObject();

    boolean isArray();

    boolean isValue();

    boolean isNull();
}
