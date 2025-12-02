package com.garganttua.core.runtime.nativve;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.Condition;

import com.garganttua.core.nativve.image.config.NativeImageConfig;
import com.garganttua.core.nativve.image.config.reflection.ReflectConfigEntryBuilder;
import com.garganttua.core.nativve.image.config.reflection.ReflectionConfiguration;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.runtime.annotations.Catch;
import com.garganttua.core.runtime.annotations.Code;
import com.garganttua.core.runtime.annotations.Context;
import com.garganttua.core.runtime.annotations.ExceptionMessage;
import com.garganttua.core.runtime.annotations.FallBack;
import com.garganttua.core.runtime.annotations.Input;
import com.garganttua.core.runtime.annotations.OnException;
import com.garganttua.core.runtime.annotations.Operation;
import com.garganttua.core.runtime.annotations.Output;
import com.garganttua.core.runtime.annotations.RuntimeDefinition;
import com.garganttua.core.runtime.annotations.Stages;
import com.garganttua.core.runtime.annotations.Step;
import com.garganttua.core.runtime.annotations.Variable;
import com.garganttua.core.runtime.annotations.Variables;

public class NativeImageConfigBuilder {
	static {
		ObjectReflectionHelper.annotationScanner = new ReflectionsAnnotationScanner();
	}

	public static void main(String[] args) throws IOException {
		createReflectConfig(args[0]);
		createResourceConfig(args[0]);
	}

	private static void createResourceConfig(String path) throws IOException {
		File resourceConfigFile = NativeImageConfig.getResourceConfigFile(path);
		if (!resourceConfigFile.exists())
			resourceConfigFile.createNewFile();

		// ResourceConfig.addResource(resourceConfigFile, GenericGGAPIDto.class);
	}

	private static void createReflectConfig(String path) throws IOException {
		File reflectConfigFile = NativeImageConfig.getReflectConfigFile(path);
		if (!reflectConfigFile.exists())
			reflectConfigFile.createNewFile();

		ReflectionConfiguration reflectConfig = ReflectionConfiguration.loadFromFile(reflectConfigFile);

		reflectConfig.addEntry(new ReflectConfigEntryBuilder(Catch.class).allPublicClasses(true)
				.allDeclaredClasses(true).allDeclaredFields(true).queryAllDeclaredMethods(true)
				.queryAllDeclaredConstructors(true).build());
		reflectConfig.addEntry(new ReflectConfigEntryBuilder(Code.class).allPublicClasses(true)
				.allDeclaredClasses(true).allDeclaredFields(true).queryAllDeclaredMethods(true)
				.queryAllDeclaredConstructors(true).build());
		reflectConfig.addEntry(new ReflectConfigEntryBuilder(Condition.class).allPublicClasses(true)
				.allDeclaredClasses(true).allDeclaredFields(true).queryAllDeclaredMethods(true)
				.queryAllDeclaredConstructors(true).build());
		reflectConfig.addEntry(new ReflectConfigEntryBuilder(Context.class).allPublicClasses(true)
				.allDeclaredClasses(true).allDeclaredFields(true).queryAllDeclaredMethods(true)
				.queryAllDeclaredConstructors(true).build());
		reflectConfig.addEntry(new ReflectConfigEntryBuilder(Exception.class).allPublicClasses(true)
				.allDeclaredClasses(true).allDeclaredFields(true).queryAllDeclaredMethods(true)
				.queryAllDeclaredConstructors(true).build());
		reflectConfig.addEntry(new ReflectConfigEntryBuilder(ExceptionMessage.class).allPublicClasses(true)
				.allDeclaredClasses(true).allDeclaredFields(true).queryAllDeclaredMethods(true)
				.queryAllDeclaredConstructors(true).build());
		reflectConfig.addEntry(new ReflectConfigEntryBuilder(FallBack.class).allPublicClasses(true)
				.allDeclaredClasses(true).allDeclaredFields(true).queryAllDeclaredMethods(true)
				.queryAllDeclaredConstructors(true).build());
		reflectConfig.addEntry(new ReflectConfigEntryBuilder(Input.class).allPublicClasses(true)
				.allDeclaredClasses(true).allDeclaredFields(true).queryAllDeclaredMethods(true)
				.queryAllDeclaredConstructors(true).build());
		reflectConfig.addEntry(new ReflectConfigEntryBuilder(OnException.class).allPublicClasses(true)
				.allDeclaredClasses(true).allDeclaredFields(true).queryAllDeclaredMethods(true)
				.queryAllDeclaredConstructors(true).build());
		reflectConfig.addEntry(new ReflectConfigEntryBuilder(Operation.class).allPublicClasses(true)
				.allDeclaredClasses(true).allDeclaredFields(true).queryAllDeclaredMethods(true)
				.queryAllDeclaredConstructors(true).build());
		reflectConfig.addEntry(new ReflectConfigEntryBuilder(Output.class).allPublicClasses(true)
				.allDeclaredClasses(true).allDeclaredFields(true).queryAllDeclaredMethods(true)
				.queryAllDeclaredConstructors(true).build());
		reflectConfig.addEntry(new ReflectConfigEntryBuilder(RuntimeDefinition.class).allPublicClasses(true)
				.allDeclaredClasses(true).allDeclaredFields(true).queryAllDeclaredMethods(true)
				.queryAllDeclaredConstructors(true).build());
		reflectConfig.addEntry(new ReflectConfigEntryBuilder(Stages.class).allPublicClasses(true)
				.allDeclaredClasses(true).allDeclaredFields(true).queryAllDeclaredMethods(true)
				.queryAllDeclaredConstructors(true).build());
		reflectConfig.addEntry(new ReflectConfigEntryBuilder(Step.class).allPublicClasses(true)
				.allDeclaredClasses(true).allDeclaredFields(true).queryAllDeclaredMethods(true)
				.queryAllDeclaredConstructors(true).build());
		reflectConfig.addEntry(new ReflectConfigEntryBuilder(Variable.class).allPublicClasses(true)
				.allDeclaredClasses(true).allDeclaredFields(true).queryAllDeclaredMethods(true)
				.queryAllDeclaredConstructors(true).build());
		reflectConfig.addEntry(new ReflectConfigEntryBuilder(Variables.class).allPublicClasses(true)
				.allDeclaredClasses(true).allDeclaredFields(true).queryAllDeclaredMethods(true)
				.queryAllDeclaredConstructors(true).build());

		reflectConfig.saveToFile(reflectConfigFile);
	}
}