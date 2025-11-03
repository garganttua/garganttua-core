package com.garganttua.core.reflections;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import com.garganttua.core.reflection.IAnnotationScanner;

public class ReflectionsAnnotationScanner implements IAnnotationScanner {

	@Override
	public List<Class<?>> getClassesWithAnnotation(String package_, Class<? extends Annotation> annotation) {
		Reflections reflections = new Reflections(package_, Scanners.TypesAnnotated);
		Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(annotation, true);
		return annotatedClasses.stream().collect(Collectors.toList());
	}

}
