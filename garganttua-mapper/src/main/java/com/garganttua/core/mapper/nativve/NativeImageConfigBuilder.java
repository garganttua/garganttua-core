package com.garganttua.core.mapper.nativve;

import java.io.File;
import java.io.IOException;

import com.garganttua.core.mapper.annotations.FieldMappingRule;
import com.garganttua.core.mapper.annotations.ObjectMappingRule;
import com.garganttua.core.nativve.image.config.NativeImageConfig;
import com.garganttua.core.nativve.image.config.reflection.ReflectConfigEntryBuilder;
import com.garganttua.core.nativve.image.config.reflection.ReflectionConfiguration;

public class NativeImageConfigBuilder {

    public static void main(String[] args) throws IOException {
        createReflectConfig(args[0]);
        createResourceConfig(args[0]);
    }

    private static void createResourceConfig(String path) throws IOException {
        File resourceConfigFile = NativeImageConfig.getResourceConfigFile(path);
        if (!resourceConfigFile.exists())
            resourceConfigFile.createNewFile();
    }

    private static void createReflectConfig(String path) throws IOException {
        File reflectConfigFile = NativeImageConfig.getReflectConfigFile(path);
        if (!reflectConfigFile.exists())
            reflectConfigFile.createNewFile();

        ReflectionConfiguration reflectConfig = ReflectionConfiguration.loadFromFile(reflectConfigFile);

        reflectConfig.addEntry(new ReflectConfigEntryBuilder(FieldMappingRule.class).allPublicClasses(true)
                .allDeclaredClasses(true).allDeclaredFields(true).queryAllDeclaredMethods(true)
                .queryAllDeclaredConstructors(true).build());
        reflectConfig.addEntry(new ReflectConfigEntryBuilder(ObjectMappingRule.class).allPublicClasses(true)
                .allDeclaredClasses(true).allDeclaredFields(true).queryAllDeclaredMethods(true)
                .queryAllDeclaredConstructors(true).build());

        reflectConfig.saveToFile(reflectConfigFile);
    }
}
