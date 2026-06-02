package com.garganttua.core.reflection.dsl;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Function;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IAnnotationScanner;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethod;

/**
 * Aggregates annotation-scanning results across every registered
 * {@link IAnnotationScanner}.
 *
 * <p>
 * With a single scanner it returns that scanner's list directly; with several it
 * merges their results into an insertion-ordered, deduplicated list.
 * </p>
 */
class ScannerAggregator implements IAnnotationScanner {
    private static final Logger log = Logger.getLogger(ScannerAggregator.class);

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
        log.trace("Aggregating scanner results across {} scanners", scanners.size());
        // Fast path for pure-AOT / pure-runtime configurations with a single
        // scanner — skip the LinkedHashSet+ArrayList round-trip and return
        // the scanner's own list directly. Callers don't mutate the result
        // (verified via grep: every caller is .stream() or .addAll(returned)
        // into a separate accumulator). Saves N×Set/List allocation per
        // bootstrap auto-detection sweep — multiplied across every builder
        // that calls getClassesWithAnnotation / getMethodsWithAnnotation
        // it's a real micro-win.
        if (scanners.size() == 1) {
            return extractor.apply(scanners.get(0));
        }
        LinkedHashSet<T> result = new LinkedHashSet<>();
        for (IAnnotationScanner scanner : scanners) {
            result.addAll(extractor.apply(scanner));
        }
        return new ArrayList<>(result);
    }
}
