package com.garganttua.core.injection.nativve;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Qualifier;
import javax.inject.Singleton;

import com.garganttua.core.nativve.image.config.NativeImageConfig;
import com.garganttua.core.nativve.image.config.reflection.ReflectConfigEntryBuilder;
import com.garganttua.core.nativve.image.config.reflection.ReflectionConfiguration;

import jakarta.annotation.Nullable;
import lombok.NonNull;

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

		reflectConfig.addEntry(new ReflectConfigEntryBuilder(Inject.class).allPublicClasses(true)
				.allDeclaredClasses(true).allDeclaredFields(true).queryAllDeclaredMethods(true)
				.queryAllDeclaredConstructors(true).build());
		reflectConfig.addEntry(new ReflectConfigEntryBuilder(Singleton.class).allPublicClasses(true)
				.allDeclaredClasses(true).allDeclaredFields(true).queryAllDeclaredMethods(true)
				.queryAllDeclaredConstructors(true).build());
		reflectConfig.addEntry(new ReflectConfigEntryBuilder(Inject.class).allPublicClasses(true)
				.allDeclaredClasses(true).allDeclaredFields(true).queryAllDeclaredMethods(true)
				.queryAllDeclaredConstructors(true).build());
		reflectConfig.addEntry(new ReflectConfigEntryBuilder(Qualifier.class).allPublicClasses(true)
				.allDeclaredClasses(true).allDeclaredFields(true).queryAllDeclaredMethods(true)
				.queryAllDeclaredConstructors(true).build());
		reflectConfig.addEntry(new ReflectConfigEntryBuilder(Nullable.class).allPublicClasses(true)
				.allDeclaredClasses(true).allDeclaredFields(true).queryAllDeclaredMethods(true)
				.queryAllDeclaredConstructors(true).build());
		reflectConfig.addEntry(new ReflectConfigEntryBuilder(NonNull.class).allPublicClasses(true)
				.allDeclaredClasses(true).allDeclaredFields(true).queryAllDeclaredMethods(true)
				.queryAllDeclaredConstructors(true).build());

		reflectConfig.saveToFile(reflectConfigFile);
	}
}
