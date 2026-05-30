package com.garganttua.core.aot.annotation.processor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Verifies that {@link IndexedAnnotationProcessor} writes a non-empty
 * {@code META-INF/garganttua/index/javax.inject.Qualifier} index whenever
 * the compiled unit contains an annotation type meta-annotated with
 * {@code @Qualifier} (or its jakarta equivalent).
 *
 * <p>This is the regression test for the user-reported "@Observer not
 * detected via @Qualifier" failure that bounced three times through
 * garganttua-api-example. The two bugs that cumulated:</p>
 * <ol>
 *   <li>{@code javax.inject.Qualifier} wasn't in {@code JSR330_ANNOTATIONS},
 *       so the processor never treated it as an "always index" marker.</li>
 *   <li>{@code toIndexEntry} dropped {@code ElementKind.ANNOTATION_TYPE},
 *       so annotation declarations bearing the marker were silently
 *       written as null entries.</li>
 * </ol>
 *
 * <p>Either bug alone causes the Qualifier index to be empty or absent.
 * Both must be fixed for framework qualifier discovery (Observer,
 * BeanProvider, custom user qualifiers) to work in pure-AOT mode.</p>
 */
class IndexedAnnotationProcessorTest {

    @Test
    void qualifierMetaAnnotatedTypeIsWrittenToQualifierIndex(@TempDir Path tmp) throws IOException {
        // Synthesise a user annotation type meta-annotated with @Qualifier.
        // After processing, the Qualifier index must list this annotation
        // type. Without the fix it would be missing — proving the bug
        // reported by the user.
        String src = """
                package sample;
                import javax.inject.Qualifier;
                import java.lang.annotation.ElementType;
                import java.lang.annotation.Retention;
                import java.lang.annotation.RetentionPolicy;
                import java.lang.annotation.Target;

                @Qualifier
                @Target(ElementType.TYPE)
                @Retention(RetentionPolicy.RUNTIME)
                public @interface MyMarker {
                }
                """;
        CompileResult r = compile(tmp, "sample.MyMarker", src);
        assertCompiled(r);

        Path qualifierIndex = r.outputDir.resolve(
                "META-INF/garganttua/index/javax.inject.Qualifier");
        assertTrue(Files.exists(qualifierIndex),
                () -> "javax.inject.Qualifier index not produced; outputs: "
                        + listOutputs(r.outputDir) + ";\ndiagnostics:" + diagSummary(r));

        String content = Files.readString(qualifierIndex);
        assertTrue(content.contains("C:sample.MyMarker"),
                () -> "Qualifier index missing the meta-annotated type "
                        + "(expected 'C:sample.MyMarker'); got:\n" + content);
    }

    @Test
    void qualifierMetaAnnotatedTypeAndUserClassBothEndUpInRespectiveIndices(
            @TempDir Path tmp) throws IOException {
        // The full chain the framework actually walks at bootstrap:
        //  - @MyQualifier (a user-defined @Qualifier-meta annotation)
        //    → must appear in the Qualifier index (this is the bug fix)
        //  - @MyQualifier MyService                                     ← user class
        //    → must appear in the MyQualifier index (already worked)
        String src = """
                package sample;
                import javax.inject.Qualifier;
                import java.lang.annotation.ElementType;
                import java.lang.annotation.Retention;
                import java.lang.annotation.RetentionPolicy;
                import java.lang.annotation.Target;

                @Qualifier
                @Target(ElementType.TYPE)
                @Retention(RetentionPolicy.RUNTIME)
                @interface MyQualifier {}

                @MyQualifier
                public class MyService {
                }
                """;
        CompileResult r = compile(tmp, "sample.MyService", src);
        assertCompiled(r);

        Path qualifierIndex = r.outputDir.resolve(
                "META-INF/garganttua/index/javax.inject.Qualifier");
        Path myQualifierIndex = r.outputDir.resolve(
                "META-INF/garganttua/index/sample.MyQualifier");

        assertTrue(Files.exists(qualifierIndex),
                "Qualifier index must be produced");
        assertTrue(Files.exists(myQualifierIndex),
                "MyQualifier index must be produced (this part worked before — "
                        + "asserting the broader chain still functions)");

        String qualifierContent = Files.readString(qualifierIndex);
        String myQualifierContent = Files.readString(myQualifierIndex);

        assertTrue(qualifierContent.contains("C:sample.MyQualifier"),
                () -> "Qualifier index missing @MyQualifier; got:\n" + qualifierContent);
        assertTrue(myQualifierContent.contains("C:sample.MyService"),
                () -> "MyQualifier index missing MyService; got:\n" + myQualifierContent);
    }

    // jakarta.inject path: symmetric to javax — covered by the JSR330_ANNOTATIONS
    // list in the processor. Not asserted here because the processor module's
    // test classpath only has javax.inject (jakarta would require an extra
    // dep). The javax test above proves the mechanism; the jakarta strings in
    // the list are exercised at consumer build time.

    @Test
    void frameworkObserverAnnotation_userClassEndsUpInObserverIndex(@TempDir Path tmp) throws IOException {
        // End-to-end reproduction of the user's reported scenario: a user
        // class annotated with the framework's REAL @Observer annotation
        // must end up in the Observer index. If this test passes, the
        // framework-side compile pipeline is correct and any residual
        // breakage on the consumer side is downstream (Maven cache, missing
        // processor wiring in the consumer pom, etc.).
        String src = """
                package sample;
                import com.garganttua.core.observability.annotations.Observer;

                @Observer
                public class MyObserverImpl {
                }
                """;
        CompileResult r = compile(tmp, "sample.MyObserverImpl", src);
        assertCompiled(r);

        Path observerIndex = r.outputDir.resolve(
                "META-INF/garganttua/index/com.garganttua.core.observability.annotations.Observer");
        assertTrue(Files.exists(observerIndex),
                () -> "Observer index missing — consumer apps using @Observer "
                        + "won't have their classes discovered. Outputs: "
                        + listOutputs(r.outputDir));

        String content = Files.readString(observerIndex);
        assertTrue(content.contains("C:sample.MyObserverImpl"),
                () -> "Observer index doesn't list MyObserverImpl; got:\n" + content);
    }

    @Test
    void scopeMetaAnnotatedTypeIsIndexed(@TempDir Path tmp) throws IOException {
        // @Scope is the sibling of @Qualifier — used to define scope markers
        // (e.g. @RequestScoped). Same indexing semantics expected.
        String src = """
                package sample;
                import javax.inject.Scope;
                import java.lang.annotation.ElementType;
                import java.lang.annotation.Retention;
                import java.lang.annotation.RetentionPolicy;
                import java.lang.annotation.Target;

                @Scope
                @Target(ElementType.TYPE)
                @Retention(RetentionPolicy.RUNTIME)
                public @interface RequestScoped {}
                """;
        CompileResult r = compile(tmp, "sample.RequestScoped", src);
        assertCompiled(r);

        Path scopeIndex = r.outputDir.resolve(
                "META-INF/garganttua/index/javax.inject.Scope");
        assertTrue(Files.exists(scopeIndex), "Scope index must be produced");
        assertTrue(Files.readString(scopeIndex).contains("C:sample.RequestScoped"));
    }

    // --- harness ---

    private record CompileResult(boolean success, Path outputDir,
                                 DiagnosticCollector<JavaFileObject> diagnostics) {}

    private CompileResult compile(Path tmp, String className, String source) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "this test requires a JDK (not JRE)");
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        Path outputDir = tmp.resolve("out");
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(diagnostics, null, null)) {
            fm.setLocationFromPaths(StandardLocation.CLASS_OUTPUT, List.of(outputDir));
            fm.setLocationFromPaths(StandardLocation.SOURCE_OUTPUT, List.of(outputDir));
            JavaFileObject file = new InMemorySource(className, source);
            JavaCompiler.CompilationTask task = compiler.getTask(
                    null, fm, diagnostics,
                    // -proc:only — run annotation processors but don't write classes;
                    // we only care about resources (the index files).
                    List.of("-proc:only"),
                    null, List.of(file));
            task.setProcessors(List.of(new IndexedAnnotationProcessor()));
            boolean ok = task.call();
            return new CompileResult(ok, outputDir, diagnostics);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Allow non-processor compilation errors — we only fail on diagnostics
     *  the processor emitted itself ({@code [garganttua-aot]}). */
    private static void assertCompiled(CompileResult r) {
        assertFalse(hasError(r, "[garganttua-aot]"),
                () -> "processor emitted ERROR; got: " + diagSummary(r));
    }

    private static boolean hasError(CompileResult r, String messageFragment) {
        return r.diagnostics.getDiagnostics().stream()
                .anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR
                        && d.getMessage(null).contains(messageFragment));
    }

    private static String diagSummary(CompileResult r) {
        StringBuilder sb = new StringBuilder("\n");
        for (Diagnostic<? extends JavaFileObject> d : r.diagnostics.getDiagnostics()) {
            sb.append("  ").append(d.getKind()).append(": ").append(d.getMessage(null)).append('\n');
        }
        return sb.toString();
    }

    private static String listOutputs(Path outputDir) {
        if (!Files.exists(outputDir)) return "(no outputDir)";
        StringBuilder sb = new StringBuilder("\n");
        try (var stream = Files.walk(outputDir)) {
            stream.filter(Files::isRegularFile)
                    .forEach(p -> sb.append("  ").append(outputDir.relativize(p)).append('\n'));
        } catch (IOException ignored) {}
        return sb.toString();
    }

    private static final class InMemorySource extends SimpleJavaFileObject {
        private final String content;
        InMemorySource(String className, String content) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension),
                    Kind.SOURCE);
            this.content = content;
        }
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return content;
        }
    }
}
