package com.garganttua.core.reflections;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import com.garganttua.core.reflection.IAnnotationScanner;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.runtime.RuntimeClass;
import com.garganttua.core.reflection.runtime.RuntimeMethod;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReflectionsAnnotationScanner implements IAnnotationScanner {

	private final Map<String, Reflections> typeAnnotationCache = new ConcurrentHashMap<>();
	private final Map<String, Reflections> methodAnnotationCache = new ConcurrentHashMap<>();

	@Override
	public List<IClass<?>> getClassesWithAnnotation(IClass<? extends Annotation> annotation) {
		return getClassesWithAnnotation("", annotation);
	}

	@Override
	public List<IClass<?>> getClassesWithAnnotation(String packageName, IClass<? extends Annotation> annotation) {
		log.atTrace().log("Entering getClassesWithAnnotation(package={}, annotation={})", packageName, annotation.getName());

		Class<? extends Annotation> rawAnnotation = unwrapAnnotation(annotation);

		Reflections reflections = typeAnnotationCache.computeIfAbsent(packageName, pkg -> {
			log.atDebug().log("Initializing Reflections scanner for package '{}' (TypesAnnotated)", pkg);
			return new Reflections(pkg, Scanners.TypesAnnotated);
		});

		log.atDebug().log("Fetching annotated classes for annotation '{}' in package {}", annotation.getName(), packageName);
		Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(rawAnnotation, true);

		List<IClass<?>> result = new ArrayList<>(annotatedClasses.size());
		for (Class<?> clazz : annotatedClasses) {
			result.add(RuntimeClass.ofUnchecked(clazz));
		}

		log.atDebug().log("Found {} classes annotated with '{}' in package {}", result.size(), annotation.getName(), packageName);
		log.atTrace().log("Exiting getClassesWithAnnotation(package={}, annotation={})", packageName, annotation.getName());
		return result;
	}

	@Override
	public List<IMethod> getMethodsWithAnnotation(IClass<? extends Annotation> annotation) {
		return getMethodsWithAnnotation("", annotation);
	}

	@Override
	public List<IMethod> getMethodsWithAnnotation(String packageName, IClass<? extends Annotation> annotation) {
		log.atTrace().log("Entering getMethodsWithAnnotation(package={}, annotation={})", packageName, annotation.getName());

		Class<? extends Annotation> rawAnnotation = unwrapAnnotation(annotation);

		Reflections reflections = methodAnnotationCache.computeIfAbsent(packageName, pkg -> {
			log.atDebug().log("Initializing Reflections scanner for package '{}' (MethodsAnnotated)", pkg);
			return new Reflections(pkg, Scanners.MethodsAnnotated);
		});

		log.atDebug().log("Fetching annotated methods for annotation '{}' in package {}", annotation.getName(), packageName);
		Set<Method> annotatedMethods = reflections.getMethodsAnnotatedWith(rawAnnotation);

		List<IMethod> result = new ArrayList<>(annotatedMethods.size());
		for (Method method : annotatedMethods) {
			result.add(RuntimeMethod.of(method));
		}

		log.atDebug().log("Found {} methods annotated with '{}' in package {}", result.size(), annotation.getName(), packageName);
		log.atTrace().log("Exiting getMethodsWithAnnotation(package={}, annotation={})", packageName, annotation.getName());
		return result;
	}

	@SuppressWarnings("unchecked")
	private static Class<? extends Annotation> unwrapAnnotation(IClass<? extends Annotation> annotation) {
		if (annotation instanceof RuntimeClass<?> rc) {
			return (Class<? extends Annotation>) rc.unwrap();
		}
		try {
			return (Class<? extends Annotation>) Class.forName(annotation.getName());
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("Cannot resolve annotation class: " + annotation.getName(), e);
		}
	}
}
