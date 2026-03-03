package com.garganttua.core.configuration.populator;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.garganttua.core.configuration.ConfigurationException;
import com.garganttua.core.configuration.IConfigurationFormat;
import com.garganttua.core.configuration.IConfigurationNode;
import com.garganttua.core.configuration.IConfigurationNode.NodeType;
import com.garganttua.core.configuration.IConfigurationPopulator;
import com.garganttua.core.configuration.IConfigurationSource;
import com.garganttua.core.dsl.IBuilder;
import com.garganttua.core.dsl.ILinkedBuilder;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BuilderPopulator implements IConfigurationPopulator {

    private final List<IConfigurationFormat> formats;
    private final MethodMapping methodMapping;
    private final TypeConverter typeConverter;
    private final boolean strict;

    public BuilderPopulator(List<IConfigurationFormat> formats, MethodMappingStrategy strategy, boolean strict) {
        this.formats = formats;
        this.methodMapping = new MethodMapping(strategy);
        this.typeConverter = new TypeConverter();
        this.strict = strict;
    }

    @Override
    public <B extends IBuilder<?>> B populate(B builder, IConfigurationNode node) throws ConfigurationException {
        var context = new PopulationContext(this.strict);
        populateBuilder(builder, node, context);

        if (context.hasErrors()) {
            throw new ConfigurationException("Configuration errors: " + String.join("; ", context.getErrors()));
        }

        for (var warning : context.getWarnings()) {
            log.atWarn().log("{}", warning);
        }

        return builder;
    }

    @Override
    public <B extends IBuilder<?>> B populate(B builder, IConfigurationSource source) throws ConfigurationException {
        var format = resolveFormat(source);
        return populate(builder, source, format);
    }

    @Override
    public <B extends IBuilder<?>> B populate(B builder, IConfigurationSource source, IConfigurationFormat format)
            throws ConfigurationException {
        log.atDebug().log("Populating builder {} from {}", builder.getClass().getSimpleName(), source.getDescription());
        var node = format.parse(source.getInputStream());
        return populate(builder, node);
    }

    private void populateBuilder(Object builder, IConfigurationNode node, PopulationContext context)
            throws ConfigurationException {
        if (!node.isObject()) {
            throw new ConfigurationException("Expected OBJECT node at " + context.getCurrentPath()
                    + ", got " + node.type());
        }

        for (var entry : node.children().entrySet()) {
            var key = entry.getKey();
            var childNode = entry.getValue();

            context.pushPath(key);
            try {
                var method = this.methodMapping.resolve(builder.getClass(), key);

                if (method.isEmpty()) {
                    if (this.strict) {
                        context.addError("Unknown configuration key '" + key + "'");
                    } else {
                        context.addWarning("Unknown configuration key '" + key + "', ignoring");
                    }
                    continue;
                }

                invokeMethod(builder, method.get(), childNode, context);
            } finally {
                context.popPath();
            }
        }
    }

    private void invokeMethod(Object builder, Method method, IConfigurationNode node, PopulationContext context)
            throws ConfigurationException {
        try {
            if (node.type() == NodeType.OBJECT) {
                handleObjectNode(builder, method, node, context);
            } else if (node.type() == NodeType.ARRAY) {
                handleArrayNode(builder, method, node, context);
            } else if (node.type() == NodeType.VALUE) {
                handleValueNode(builder, method, node);
            } else {
                // NULL node - skip
                log.atDebug().log("Skipping null node at {}", context.getCurrentPath());
            }
        } catch (ConfigurationException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfigurationException("Failed to invoke " + method.getName() + " at "
                    + context.getCurrentPath(), e);
        }
    }

    private void handleObjectNode(Object builder, Method method, IConfigurationNode node,
            PopulationContext context) throws Exception {
        var returnType = method.getReturnType();

        // Check if return type is a child builder (IBuilder or ILinkedBuilder)
        if (isChildBuilder(returnType, builder.getClass())) {
            // Call the method to get the child builder, then populate recursively
            var childBuilder = ObjectReflectionHelper.invokeMethod(builder, method.getName(), method, method.getReturnType());
            if (childBuilder != null) {
                populateBuilder(childBuilder, node, context);
                // If it's a linked builder, call up() to return to parent
                if (childBuilder instanceof ILinkedBuilder<?, ?> linked) {
                    linked.up();
                }
            }
        } else if (method.getParameterCount() == 1 && Map.class.isAssignableFrom(method.getParameterTypes()[0])) {
            // Pass as Map
            var map = nodeToMap(node);
            ObjectReflectionHelper.invokeMethod(builder, method.getName(), method, method.getReturnType(), map);
        } else {
            // Try to pass the text representation
            var text = node.asText();
            if (text.isPresent() && method.getParameterCount() == 1) {
                var paramType = method.getParameterTypes()[0];
                var converted = this.typeConverter.convert(text.get(), paramType);
                ObjectReflectionHelper.invokeMethod(builder, method.getName(), method, method.getReturnType(), converted);
            }
        }
    }

    private void handleArrayNode(Object builder, Method method, IConfigurationNode node,
            PopulationContext context) throws Exception {
        var elements = node.elements();

        if (method.getParameterCount() == 1) {
            var paramType = method.getParameterTypes()[0];

            // If method accepts List
            if (List.class.isAssignableFrom(paramType)) {
                var list = new ArrayList<>();
                for (var element : elements) {
                    if (element.isValue()) {
                        list.add(element.asText().orElse(null));
                    } else {
                        list.add(element);
                    }
                }
                ObjectReflectionHelper.invokeMethod(builder, method.getName(), method, method.getReturnType(), list);
                return;
            }

            // If method accepts array
            if (paramType.isArray()) {
                var componentType = paramType.getComponentType();
                var array = java.lang.reflect.Array.newInstance(componentType, elements.size());
                for (int i = 0; i < elements.size(); i++) {
                    var element = elements.get(i);
                    if (element.isValue()) {
                        var text = element.asText().orElse(null);
                        java.lang.reflect.Array.set(array, i, this.typeConverter.convert(text, componentType));
                    }
                }
                ObjectReflectionHelper.invokeMethod(builder, method.getName(), method, method.getReturnType(), array);
                return;
            }
        }

        // Repeated calls for each element
        for (var element : elements) {
            if (element.isValue()) {
                if (method.getParameterCount() == 1) {
                    var paramType = method.getParameterTypes()[0];
                    var text = element.asText().orElse(null);
                    var converted = this.typeConverter.convert(text, paramType);
                    ObjectReflectionHelper.invokeMethod(builder, method.getName(), method, method.getReturnType(), converted);
                }
            } else if (element.isObject()) {
                var returnType = method.getReturnType();
                if (isChildBuilder(returnType, builder.getClass())) {
                    var childBuilder = ObjectReflectionHelper.invokeMethod(builder, method.getName(), method, method.getReturnType());
                    if (childBuilder != null) {
                        populateBuilder(childBuilder, element, context);
                        if (childBuilder instanceof ILinkedBuilder<?, ?> linked) {
                            linked.up();
                        }
                    }
                }
            }
        }
    }

    private void handleValueNode(Object builder, Method method, IConfigurationNode node)
            throws Exception {
        var text = node.asText().orElse(null);
        if (text == null) {
            return;
        }

        if (method.getParameterCount() == 0) {
            // No-arg method (flag-style), call if value is true
            if ("true".equalsIgnoreCase(text)) {
                ObjectReflectionHelper.invokeMethod(builder, method.getName(), method, method.getReturnType());
            }
            return;
        }

        if (method.getParameterCount() == 1) {
            var paramType = method.getParameterTypes()[0];
            var converted = this.typeConverter.convert(text, paramType);
            ObjectReflectionHelper.invokeMethod(builder, method.getName(), method, method.getReturnType(), converted);
            return;
        }

        log.atWarn().log("Cannot map value '{}' to method {} with {} parameters",
                text, method.getName(), method.getParameterCount());
    }

    private boolean isChildBuilder(Class<?> returnType, Class<?> builderClass) {
        if (returnType == null || returnType == void.class || returnType == Void.class) {
            return false;
        }
        // If return type is the same as the builder class, it's a setter returning this
        if (returnType.isAssignableFrom(builderClass)) {
            return false;
        }
        // If return type implements IBuilder or ILinkedBuilder, it's a child builder
        return IBuilder.class.isAssignableFrom(returnType)
                || ILinkedBuilder.class.isAssignableFrom(returnType);
    }

    private Map<String, String> nodeToMap(IConfigurationNode node) {
        var map = new java.util.LinkedHashMap<String, String>();
        for (var entry : node.children().entrySet()) {
            entry.getValue().asText().ifPresent(v -> map.put(entry.getKey(), v));
        }
        return map;
    }

    private IConfigurationFormat resolveFormat(IConfigurationSource source) throws ConfigurationException {
        var hint = source.getFormatHint();
        if (hint.isPresent()) {
            for (var format : this.formats) {
                if (format.isAvailable() && format.supports(hint.get())) {
                    return format;
                }
            }
        }
        throw new ConfigurationException("No format found for source: " + source.getDescription()
                + (hint.isPresent() ? " (hint: " + hint.get() + ")" : ""));
    }
}
