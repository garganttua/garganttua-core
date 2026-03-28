package com.garganttua.core.reflections;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import com.garganttua.core.reflection.IAnnotationScanner;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethod;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReflectionsAnnotationScanner implements IAnnotationScanner {

	@Override
	public List<IClass<?>> getClassesWithAnnotation(IClass<? extends Annotation> annotation) {
		return getClassesWithAnnotation("", annotation);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<IClass<?>> getClassesWithAnnotation(String packageName, IClass<? extends Annotation> annotation) {
		log.atTrace().log("Entering getClassesWithAnnotation(package={}, annotation={})", packageName, annotation.getName());

		Class<? extends Annotation> rawAnnotation = (Class<? extends Annotation>) annotation.getType();

		log.atDebug().log("Scanning package '{}' for classes with annotation '{}'", packageName, annotation.getName());
		Reflections reflections = new Reflections(packageName, Scanners.TypesAnnotated);
		Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(rawAnnotation, true);

		List<IClass<?>> result = new ArrayList<>(annotatedClasses.size());
		for (Class<?> clazz : annotatedClasses) {
			result.add(IClass.getClass(clazz));
		}

		log.atDebug().log("Found {} classes annotated with '{}' in package {}", result.size(), annotation.getName(), packageName);
		return result;
	}

	@Override
	public List<IMethod> getMethodsWithAnnotation(IClass<? extends Annotation> annotation) {
		return getMethodsWithAnnotation("", annotation);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<IMethod> getMethodsWithAnnotation(String packageName, IClass<? extends Annotation> annotation) {
		log.atTrace().log("Entering getMethodsWithAnnotation(package={}, annotation={})", packageName, annotation.getName());

		Class<? extends Annotation> rawAnnotation = (Class<? extends Annotation>) annotation.getType();

		log.atDebug().log("Scanning package '{}' for methods with annotation '{}'", packageName, annotation.getName());
		Reflections reflections = new Reflections(packageName, Scanners.MethodsAnnotated);
		Set<Method> annotatedMethods = reflections.getMethodsAnnotatedWith(rawAnnotation);

		List<IMethod> result = new ArrayList<>(annotatedMethods.size());
		for (Method method : annotatedMethods) {
			try {
				IMethod found = IClass.getClass(method.getDeclaringClass()).getMethod(method.getName(), Arrays.stream(method.getParameterTypes()).map(IClass::getClass).toArray(IClass[]::new));
				result.add(found);
			} catch (NoSuchMethodException | SecurityException e) {
				log.warn("Error", e);
			}
		}

		log.atDebug().log("Found {} methods annotated with '{}' in package {}", result.size(), annotation.getName(), packageName);
		return result;
	}
}
