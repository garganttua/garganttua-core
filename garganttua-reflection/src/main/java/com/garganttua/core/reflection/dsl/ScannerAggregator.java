package com.garganttua.core.reflection.dsl;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Function;

import com.garganttua.core.reflection.IAnnotationScanner;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethod;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class ScannerAggregator implements IAnnotationScanner {

    private final List<IAnnotationScanner> scanners;

    ScannerAggregator(List<IAnnotationScanner> scanners) {
        this.scanners = scanners;
    }

    @Override
    public List<IClass<?>> getClassesWithAnnotation(IClass<? extends Annotation> annotation) {
        return aggregate(s -> s.getClassesWithAnnotation(annotation));
    }

    @Override
    public List<IClass<?>> getClassesWithAnnotation(String packageName, IClass<? extends Annotation> annotation) {
        return aggregate(s -> s.getClassesWithAnnotation(packageName, annotation));
    }

    @Override
    public List<IMethod> getMethodsWithAnnotation(IClass<? extends Annotation> annotation) {
        return aggregate(s -> s.getMethodsWithAnnotation(annotation));
    }

    @Override
    public List<IMethod> getMethodsWithAnnotation(String packageName, IClass<? extends Annotation> annotation) {
        return aggregate(s -> s.getMethodsWithAnnotation(packageName, annotation));
    }

    private <T> List<T> aggregate(Function<IAnnotationScanner, List<T>> extractor) {
        log.atTrace().log("Aggregating scanner results across {} scanners", scanners.size());
        LinkedHashSet<T> result = new LinkedHashSet<>();
        for (IAnnotationScanner scanner : scanners) {
            result.addAll(extractor.apply(scanner));
        }
        return new ArrayList<>(result);
    }
}
