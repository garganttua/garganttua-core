package com.garganttua.core.configuration.format;

import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;

public class XmlConfigurationFormat extends AbstractConfigurationFormat {

    private static final String XML_MAPPER_CLASS = "com.fasterxml.jackson.dataformat.xml.XmlMapper";

    @Override
    public String name() {
        return "xml";
    }

    @Override
    public Set<String> extensions() {
        return Set.of("xml");
    }

    @Override
    public Set<String> mediaTypes() {
        return Set.of("application/xml", "text/xml");
    }

    @Override
    protected ObjectMapper createMapper() {
        try {
            var mapperClass = Class.forName(XML_MAPPER_CLASS);
            return (ObjectMapper) ObjectReflectionHelper.instanciateNewObject(mapperClass);
        } catch (Exception e) {
            throw new IllegalStateException("XML support not available", e);
        }
    }

    @Override
    public boolean isAvailable() {
        return isClassAvailable(XML_MAPPER_CLASS);
    }
}
