package com.garganttua.example.aot;

import java.lang.annotation.Annotation;
import java.util.List;

import com.garganttua.core.aot.annotation.scanner.AOTAnnotationScanner;
import com.garganttua.core.aot.reflection.AOTReflectionProvider;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IConstructor;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.annotations.Reflected;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;

/**
 * Entry point of the "AOT-only" example.
 *
 * <p>It wires the Garganttua reflection facade by hand from the two pieces the AOT
 * starter ships — {@link AOTReflectionProvider} and {@link AOTAnnotationScanner} —
 * with <b>no {@code Bootstrap}, no {@code IInjectionContext}, no workflow engine</b>.
 * This is the exact pattern {@code PureAotIntegrationTest} uses, reduced to its
 * smallest runnable form.
 *
 * <p>Why {@code garganttua-reflection} is on the classpath alongside the starter: the
 * generated {@code AOTClass_Product} descriptor resolves its members' types through the
 * static {@link IClass#getClass(Class)} facade, which only works once an
 * {@link IReflection} has been installed via {@link IClass#setReflection(IReflection)}.
 * {@link ReflectionBuilder} (the composition root) lives in {@code garganttua-reflection}.
 * Apps built on {@code garganttua-bootstrap} get this wiring for free.
 */
public final class AotOnlyDemo {

    private AotOnlyDemo() {
    }

    public static void main(String[] args) {
        System.out.println("=== Garganttua AOT-only example ===\n");

        // Compose the IReflection facade from the AOT provider + scanner (both @20) and
        // install it as the global default. ServiceLoader force-loads every generated
        // AOTClass_* descriptor (incl. AOTClass_Product) into the AOTRegistry — no
        // runtime classpath scan, no bytecode generation.
        IReflection aot = ReflectionBuilder.builder()
                .withProvider(new AOTReflectionProvider(), 20)
                .withScanner(new AOTAnnotationScanner(), 20)
                .build();
        IClass.setReflection(aot);

        IClass<Product> product = IClass.getClass(Product.class);
        System.out.println("Resolved descriptor : " + product.getName());
        System.out.println("Source              : "
                + (product.getDeclaredFields().length > 0 ? "AOT-generated (rich members)"
                                                          : "fallback synthesis (identity only)"));

        System.out.println("\n-- Fields --");
        for (IField field : product.getDeclaredFields()) {
            System.out.println("  " + field.getType().getSimpleName() + " " + field.getName());
        }

        System.out.println("\n-- Methods --");
        for (IMethod method : product.getDeclaredMethods()) {
            System.out.println("  " + method.getReturnType().getSimpleName() + " " + method.getName() + "()");
        }

        System.out.println("\n-- Constructors --");
        for (IConstructor<?> ctor : product.getDeclaredConstructors()) {
            System.out.println("  <init> with " + ctor.getParameters().length + " parameter(s)");
        }

        // The AOT annotation scanner reads compile-time index files
        // (META-INF/garganttua/index/*) — again, no runtime classpath walk.
        System.out.println("\n-- Annotation scan: classes annotated with @Reflected --");
        try {
            IClass<? extends Annotation> reflectedAnno = IClass.getClass(Reflected.class);
            List<IClass<?>> annotated = new AOTAnnotationScanner().getClassesWithAnnotation(reflectedAnno);
            if (annotated.isEmpty()) {
                System.out.println("  (none indexed)");
            } else {
                annotated.forEach(c -> System.out.println("  " + c.getName()));
            }
        } catch (RuntimeException scanFailure) {
            System.out.println("  scan unavailable: " + scanFailure.getMessage());
        }

        System.out.println("\nDone — AOT reflection only, zero DI / runtime / bootstrap / workflow.");
    }
}
