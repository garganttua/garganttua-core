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
 * Tests for {@link DirectBinderGenerator} covering:
 * <ul>
 *   <li>Member-level {@code @Reflected} inclusion (fields, methods, constructors).</li>
 *   <li>Direct binders: the generated sub-classes contain typed direct access
 *       (no {@code Field.get}, {@code Method.invoke}, {@code Constructor.newInstance}).</li>
 *   <li>Strict rejection of {@code private} members.</li>
 *   <li>Validation: a member-only {@code @Reflected} requires the enclosing class to be {@code @Reflected}.</li>
 *   <li>Redundancy warning when a member is already covered by a class-level flag.</li>
 * </ul>
 */
class DirectBinderGeneratorTest {

    // --- Inclusion: explicit members are included even without queryAll* flags ---

    @Test
    void explicitMethodIsIncludedWithoutQueryAllFlag(@TempDir Path tmp) throws IOException {
        String src = """
                package sample;
                import com.garganttua.core.reflection.annotations.Reflected;
                @Reflected
                public class WithMethod {
                    @Reflected
                    public String wanted() { return "x"; }
                    public String notWanted() { return "y"; }
                }
                """;
        CompileResult r = compile(tmp, "sample.WithMethod", src);
        assertCompiled(r);
        String aotClass = Files.readString(r.outputDir.resolve("sample/AOTClass_WithMethod.java"));
        assertTrue(aotClass.contains("AOTMethod_WithMethod_wanted_0.INSTANCE"),
                () -> "expected reference to AOTMethod_WithMethod_wanted_0 in:\n" + aotClass);
        assertFalse(aotClass.contains("notWanted"),
                () -> "method 'notWanted' should not be referenced; got:\n" + aotClass);
        // The per-method file was generated
        assertTrue(Files.exists(r.outputDir.resolve("sample/AOTMethod_WithMethod_wanted_0.java")));
    }

    @Test
    void explicitFieldIsIncludedWithoutAllDeclaredFieldsFlag(@TempDir Path tmp) throws IOException {
        String src = """
                package sample;
                import com.garganttua.core.reflection.annotations.Reflected;
                @Reflected
                public class WithField {
                    @Reflected
                    String wanted;
                    String notWanted;
                }
                """;
        CompileResult r = compile(tmp, "sample.WithField", src);
        assertCompiled(r);
        String aotClass = Files.readString(r.outputDir.resolve("sample/AOTClass_WithField.java"));
        assertTrue(aotClass.contains("AOTField_WithField_wanted.INSTANCE"),
                () -> "expected reference to AOTField_WithField_wanted in:\n" + aotClass);
        assertFalse(aotClass.contains("notWanted"),
                () -> "field 'notWanted' should not be referenced; got:\n" + aotClass);
    }

    @Test
    void explicitConstructorIsIncludedWithoutQueryAllFlag(@TempDir Path tmp) throws IOException {
        String src = """
                package sample;
                import com.garganttua.core.reflection.annotations.Reflected;
                @Reflected
                public class WithCtor {
                    @Reflected
                    public WithCtor(String wantedParam) {}
                    public WithCtor(int notWantedParam) {}
                }
                """;
        CompileResult r = compile(tmp, "sample.WithCtor", src);
        assertCompiled(r);
        String aotClass = Files.readString(r.outputDir.resolve("sample/AOTClass_WithCtor.java"));
        assertTrue(aotClass.contains("AOTConstructor_WithCtor_0.INSTANCE"),
                () -> "expected reference to AOTConstructor_WithCtor_0 in:\n" + aotClass);
        // Only one constructor is included → no _1
        assertFalse(aotClass.contains("AOTConstructor_WithCtor_1"),
                () -> "no second constructor descriptor expected; got:\n" + aotClass);
    }

    // --- Direct binders: the generated source uses direct typed access ---

    @Test
    void generatedFieldDescriptorUsesDirectAccess(@TempDir Path tmp) throws IOException {
        String src = """
                package sample;
                import com.garganttua.core.reflection.annotations.Reflected;
                @Reflected
                public class DirectField {
                    @Reflected String name;
                }
                """;
        CompileResult r = compile(tmp, "sample.DirectField", src);
        assertCompiled(r);
        String fieldSrc = Files.readString(r.outputDir.resolve("sample/AOTField_DirectField_name.java"));
        assertTrue(fieldSrc.contains("return ((DirectField) obj).name;"),
                () -> "expected direct read access in:\n" + fieldSrc);
        assertTrue(fieldSrc.contains("((DirectField) obj).name = (java.lang.String) value;"),
                () -> "expected direct write access in:\n" + fieldSrc);
        assertFalse(fieldSrc.contains("resolveField"),
                () -> "no Field.get() reflection should appear; got:\n" + fieldSrc);
    }

    @Test
    void generatedFieldDescriptorHandlesPrimitiveTyped(@TempDir Path tmp) throws IOException {
        String src = """
                package sample;
                import com.garganttua.core.reflection.annotations.Reflected;
                @Reflected
                public class PrimField {
                    @Reflected int score;
                }
                """;
        CompileResult r = compile(tmp, "sample.PrimField", src);
        assertCompiled(r);
        String fieldSrc = Files.readString(r.outputDir.resolve("sample/AOTField_PrimField_score.java"));
        assertTrue(fieldSrc.contains("public int getInt(Object obj)"),
                () -> "expected getInt variant in:\n" + fieldSrc);
        assertTrue(fieldSrc.contains("public void setInt(Object obj, int v)"),
                () -> "expected setInt variant in:\n" + fieldSrc);
        assertTrue(fieldSrc.contains("(Integer) value"),
                () -> "expected boxed unbox in generic set; got:\n" + fieldSrc);
    }

    @Test
    void generatedFieldDescriptorRejectsFinalSet(@TempDir Path tmp) throws IOException {
        String src = """
                package sample;
                import com.garganttua.core.reflection.annotations.Reflected;
                @Reflected
                public class FinalField {
                    @Reflected final String name = "x";
                }
                """;
        CompileResult r = compile(tmp, "sample.FinalField", src);
        assertCompiled(r);
        String fieldSrc = Files.readString(r.outputDir.resolve("sample/AOTField_FinalField_name.java"));
        assertTrue(fieldSrc.contains("UnsupportedOperationException"),
                () -> "expected UnsupportedOperationException on set for final field; got:\n" + fieldSrc);
        assertTrue(fieldSrc.contains("return ((FinalField) obj).name;"),
                () -> "final field should still allow read; got:\n" + fieldSrc);
    }

    @Test
    void generatedMethodDescriptorUsesDirectInvoke(@TempDir Path tmp) throws IOException {
        String src = """
                package sample;
                import com.garganttua.core.reflection.annotations.Reflected;
                @Reflected
                public class DirectMethod {
                    @Reflected public String greet(String who) { return "hi " + who; }
                }
                """;
        CompileResult r = compile(tmp, "sample.DirectMethod", src);
        assertCompiled(r);
        String methodSrc = Files.readString(r.outputDir.resolve("sample/AOTMethod_DirectMethod_greet_0.java"));
        assertTrue(methodSrc.contains("return ((DirectMethod) obj).greet((java.lang.String) args[0]);"),
                () -> "expected direct invocation in:\n" + methodSrc);
        assertFalse(methodSrc.contains("resolveMethod"),
                () -> "no Method.invoke() reflection should appear; got:\n" + methodSrc);
    }

    @Test
    void generatedMethodDescriptorHandlesVoidAndStatic(@TempDir Path tmp) throws IOException {
        String src = """
                package sample;
                import com.garganttua.core.reflection.annotations.Reflected;
                @Reflected
                public class StaticVoid {
                    @Reflected public static void log(String msg) {}
                }
                """;
        CompileResult r = compile(tmp, "sample.StaticVoid", src);
        assertCompiled(r);
        String methodSrc = Files.readString(r.outputDir.resolve("sample/AOTMethod_StaticVoid_log_0.java"));
        assertTrue(methodSrc.contains("StaticVoid.log((java.lang.String) args[0]);"),
                () -> "expected static call without receiver cast; got:\n" + methodSrc);
        assertTrue(methodSrc.contains("return null;"),
                () -> "void method must return null; got:\n" + methodSrc);
    }

    @Test
    void generatedConstructorDescriptorUsesNew(@TempDir Path tmp) throws IOException {
        String src = """
                package sample;
                import com.garganttua.core.reflection.annotations.Reflected;
                @Reflected
                public class DirectCtor {
                    @Reflected public DirectCtor(String n, int i) {}
                }
                """;
        CompileResult r = compile(tmp, "sample.DirectCtor", src);
        assertCompiled(r);
        String ctorSrc = Files.readString(r.outputDir.resolve("sample/AOTConstructor_DirectCtor_0.java"));
        assertTrue(ctorSrc.contains("return new DirectCtor((java.lang.String) args[0], (Integer) args[1]);"),
                () -> "expected direct new expression in:\n" + ctorSrc);
        assertFalse(ctorSrc.contains("resolveConstructor"),
                () -> "no Constructor.newInstance() reflection should appear; got:\n" + ctorSrc);
    }

    @Test
    void methodOverloadsAreNumbered(@TempDir Path tmp) throws IOException {
        String src = """
                package sample;
                import com.garganttua.core.reflection.annotations.Reflected;
                @Reflected(queryAllPublicMethods = true)
                public class Overloads {
                    public void doIt() {}
                    public void doIt(String s) {}
                    public void other() {}
                }
                """;
        CompileResult r = compile(tmp, "sample.Overloads", src);
        assertCompiled(r);
        assertTrue(Files.exists(r.outputDir.resolve("sample/AOTMethod_Overloads_doIt_0.java")));
        assertTrue(Files.exists(r.outputDir.resolve("sample/AOTMethod_Overloads_doIt_1.java")));
        assertTrue(Files.exists(r.outputDir.resolve("sample/AOTMethod_Overloads_other_0.java")));
    }

    // --- Generic members: type variables must be erased, never leaked ---

    @Test
    void genericMethodErasesTypeVariablesInParamsAndCasts(@TempDir Path tmp) throws IOException {
        // A method with its own type parameter (`<T>`) whose parameter and
        // return types mention T. The descriptor must erase T to its bound
        // (java.lang.Object here) — a bare `T` in the param-type array or an
        // `(T)` cast in invoke() would not compile (T is out of scope in the
        // generated class). Regression for the AOT generic-method bug.
        String src = """
                package sample;
                import com.garganttua.core.reflection.annotations.Reflected;
                @Reflected(queryAllDeclaredMethods = true)
                public class Box {
                    public <T> void put(String key, T value) {}
                }
                """;
        CompileResult r = compile(tmp, "sample.Box", src);
        assertCompiled(r);
        String methodSrc = Files.readString(r.outputDir.resolve("sample/AOTMethod_Box_put_0.java"));
        // Param-type metadata array: T erased to java.lang.Object, never "T".
        assertTrue(methodSrc.contains("\"java.lang.String\", \"java.lang.Object\""),
                () -> "type-variable param must be erased to java.lang.Object; got:\n" + methodSrc);
        assertFalse(methodSrc.contains("\"T\""),
                () -> "bare type-variable name 'T' must not appear in the descriptor; got:\n" + methodSrc);
        // invoke() cast: (java.lang.Object), never (T).
        assertTrue(methodSrc.contains("(java.lang.Object) args[1]"),
                () -> "type-variable arg must be cast to java.lang.Object; got:\n" + methodSrc);
        assertFalse(methodSrc.contains("(T)"),
                () -> "invoke() must not cast to the out-of-scope type variable T; got:\n" + methodSrc);
    }

    @Test
    void genericMethodErasesBoundedTypeVariableToItsBound(@TempDir Path tmp) throws IOException {
        // A bounded type variable `<T extends Number>` erases to its bound.
        String src = """
                package sample;
                import com.garganttua.core.reflection.annotations.Reflected;
                @Reflected(queryAllDeclaredMethods = true)
                public class Bounded {
                    public <T extends Number> T pick(T value) { return value; }
                }
                """;
        CompileResult r = compile(tmp, "sample.Bounded", src);
        assertCompiled(r);
        String methodSrc = Files.readString(r.outputDir.resolve("sample/AOTMethod_Bounded_pick_0.java"));
        assertTrue(methodSrc.contains("\"java.lang.Number\""),
                () -> "bounded type variable must erase to its bound java.lang.Number; got:\n" + methodSrc);
        assertTrue(methodSrc.contains("(java.lang.Number) args[0]"),
                () -> "bounded type-variable arg must be cast to its bound; got:\n" + methodSrc);
        assertFalse(methodSrc.contains("(T)"),
                () -> "no bare (T) cast expected; got:\n" + methodSrc);
    }

    @Test
    void parameterizedReturnTypeIsErased(@TempDir Path tmp) throws IOException {
        // A method returning Optional<T> must report the raw java.util.Optional
        // in its descriptor — type arguments are dropped, no bare T leaks.
        String src = """
                package sample;
                import com.garganttua.core.reflection.annotations.Reflected;
                import java.util.Optional;
                @Reflected(queryAllDeclaredMethods = true)
                public class Holder {
                    public <T> Optional<T> get(T value) { return Optional.of(value); }
                }
                """;
        CompileResult r = compile(tmp, "sample.Holder", src);
        assertCompiled(r);
        String methodSrc = Files.readString(r.outputDir.resolve("sample/AOTMethod_Holder_get_0.java"));
        assertTrue(methodSrc.contains("\"java.util.Optional\""),
                () -> "parameterized return type must be erased to raw java.util.Optional; got:\n" + methodSrc);
        assertFalse(methodSrc.contains("\"T\""),
                () -> "bare type-variable name 'T' must not appear; got:\n" + methodSrc);
    }

    @Test
    void genericFieldTypeIsErased(@TempDir Path tmp) throws IOException {
        // A field whose type is a class-level type variable must erase to its
        // bound — both the metadata string and the (T) cast in set() would
        // otherwise fail to compile.
        String src = """
                package sample;
                import com.garganttua.core.reflection.annotations.Reflected;
                @Reflected(allDeclaredFields = true)
                public class Cell<T> {
                    T value;
                }
                """;
        CompileResult r = compile(tmp, "sample.Cell", src);
        assertCompiled(r);
        String fieldSrc = Files.readString(r.outputDir.resolve("sample/AOTField_Cell_value.java"));
        assertTrue(fieldSrc.contains("(java.lang.Object) value"),
                () -> "type-variable field must be cast to java.lang.Object in set(); got:\n" + fieldSrc);
        assertFalse(fieldSrc.contains("(T) value"),
                () -> "set() must not cast to the out-of-scope type variable T; got:\n" + fieldSrc);
    }

    // --- Validation errors ---

    @Test
    void memberAnnotatedButClassNotIsCompileError(@TempDir Path tmp) {
        String src = """
                package sample;
                import com.garganttua.core.reflection.annotations.Reflected;
                public class NotReflectedClass {
                    @Reflected
                    public void method() {}
                }
                """;
        CompileResult r = compile(tmp, "sample.NotReflectedClass", src);
        assertTrue(hasError(r, "requires its enclosing type"),
                () -> "expected an enclosing-type ERROR; got: " + diagSummary(r));
    }

    @Test
    void privateMemberIsRejected(@TempDir Path tmp) {
        String src = """
                package sample;
                import com.garganttua.core.reflection.annotations.Reflected;
                @Reflected
                public class PrivateMember {
                    @Reflected private String secret;
                }
                """;
        CompileResult r = compile(tmp, "sample.PrivateMember", src);
        assertTrue(hasError(r, "cannot access private members"),
                () -> "expected a private-rejection ERROR; got: " + diagSummary(r));
    }

    @Test
    void privateMemberCapturedByFlagIsSilentlyFiltered(@TempDir Path tmp) throws IOException {
        // queryAll* / allDeclaredFields flags include all matching members,
        // INCLUDING private ones at the AST level. But the processor silently
        // drops private members from the final descriptor — only an explicit
        // @Reflected on a private member is a hard error (clear user mistake).
        // This is the lenient behaviour that allows framework "Functions"
        // utility classes with private static helpers to be safely processed
        // alongside their @Expression-annotated public methods.
        String src = """
                package sample;
                import com.garganttua.core.reflection.annotations.Reflected;
                @Reflected(allDeclaredFields = true)
                public class PrivateViaFlag {
                    private String secret;
                    String visible;
                }
                """;
        CompileResult r = compile(tmp, "sample.PrivateViaFlag", src);
        assertCompiled(r);
        assertFalse(hasError(r, "cannot access private members"),
                () -> "private fields captured via queryAll* must be silently filtered, not rejected; got: " + diagSummary(r));
        String aotClass = Files.readString(r.outputDir.resolve("sample/AOTClass_PrivateViaFlag.java"));
        assertTrue(aotClass.contains("AOTField_PrivateViaFlag_visible"),
                () -> "package-private field 'visible' must appear in descriptor: " + aotClass);
        assertFalse(aotClass.contains("AOTField_PrivateViaFlag_secret"),
                () -> "private field 'secret' must be silently filtered out: " + aotClass);
    }

    @Test
    void redundantMemberWithQueryAllFlagWarns(@TempDir Path tmp) {
        String src = """
                package sample;
                import com.garganttua.core.reflection.annotations.Reflected;
                @Reflected(queryAllDeclaredMethods = true)
                public class RedundantMethod {
                    @Reflected
                    public void method() {}
                }
                """;
        CompileResult r = compile(tmp, "sample.RedundantMethod", src);
        assertCompiled(r);
        boolean hasWarning = r.diagnostics.getDiagnostics().stream()
                .anyMatch(d -> d.getKind() == Diagnostic.Kind.WARNING
                        && d.getMessage(null).contains("redundant"));
        assertTrue(hasWarning,
                () -> "expected a redundancy WARNING; got: " + diagSummary(r));
    }

    // --- Auto-promotion: classes carrying @Indexed-meta annotations ---

    @Test
    void classLevelIndexedMetaAnnotationTriggersAutoPromotion(@TempDir Path tmp) throws IOException {
        // @Indexed-meta annotation at the class level → AOTClass_* generated
        // even without explicit @Reflected. This is the structural fix that
        // ends the "framework class needs @Reflected too" reporting loop.
        String src = """
                package sample;
                import com.garganttua.core.reflection.annotations.Indexed;

                @Indexed
                @interface MyMarker {}

                @MyMarker
                public class AutoPromoted {
                    public AutoPromoted() {}
                    public void doIt() {}
                }
                """;
        CompileResult r = compile(tmp, "sample.AutoPromoted", src);
        assertCompiled(r);
        assertTrue(Files.exists(r.outputDir.resolve("sample/AOTClass_AutoPromoted.java")),
                () -> "expected AOTClass_AutoPromoted.java to be generated; diagnostics: " + diagSummary(r));
        String aotClass = Files.readString(r.outputDir.resolve("sample/AOTClass_AutoPromoted.java"));
        // queryAllDeclaredConstructors + queryAllDeclaredMethods defaults
        assertFalse(aotClass.contains("new AOTConstructor[0]"),
                () -> "expected at least one constructor; got: " + aotClass);
        assertFalse(aotClass.contains("new AOTMethod[0]"),
                () -> "expected at least one method; got: " + aotClass);
    }

    @Test
    void methodLevelIndexedMetaAnnotationTriggersAutoPromotion(@TempDir Path tmp) throws IOException {
        // @Indexed-meta annotation at the method level → AOTClass_* generated
        // even without class-level @Reflected. Covers the @Expression /
        // @Step / @Catch family of methods on framework utility classes.
        String src = """
                package sample;
                import com.garganttua.core.reflection.annotations.Indexed;
                import java.lang.annotation.ElementType;
                import java.lang.annotation.Target;

                @Indexed
                @Target(ElementType.METHOD)
                @interface MyMethodMarker {}

                public class Utils {
                    @MyMethodMarker
                    public static void marked() {}
                    public static void unmarked() {}
                }
                """;
        CompileResult r = compile(tmp, "sample.Utils", src);
        assertCompiled(r);
        assertTrue(Files.exists(r.outputDir.resolve("sample/AOTClass_Utils.java")),
                () -> "expected AOTClass_Utils.java to be generated; diagnostics: " + diagSummary(r));
    }

    @Test
    void classWithNoIndexedAnnotationIsNotAutoPromoted(@TempDir Path tmp) throws IOException {
        // Negative test: a regular class with no @Reflected and no indexed
        // annotation must NOT get a descriptor.
        String src = """
                package sample;
                public class Plain {
                    public Plain() {}
                    public void hello() {}
                }
                """;
        CompileResult r = compile(tmp, "sample.Plain", src);
        assertCompiled(r);
        assertFalse(Files.exists(r.outputDir.resolve("sample/AOTClass_Plain.java")),
                "no AOTClass_* should be generated for a class without any indexed annotation");
    }

    @Test
    void explicitReflectedTakesPrecedenceOverAutoPromote(@TempDir Path tmp) throws IOException {
        // A class that is BOTH @Reflected AND has @Indexed-meta annotations
        // must only be processed once (user's @Reflected flags win).
        String src = """
                package sample;
                import com.garganttua.core.reflection.annotations.Indexed;
                import com.garganttua.core.reflection.annotations.Reflected;

                @Indexed
                @interface MyMarker {}

                @Reflected
                @MyMarker
                public class Both {
                    public Both() {}
                    public void m() {}
                }
                """;
        CompileResult r = compile(tmp, "sample.Both", src);
        assertCompiled(r);
        // Only one AOTClass file: @Reflected without query flags → empty arrays.
        // (If both passes processed it, the auto-promote pass would have used
        // queryAllDeclaredConstructors/Methods=true; presence of empty arrays
        // proves only the @Reflected pass fired.)
        String aotClass = Files.readString(r.outputDir.resolve("sample/AOTClass_Both.java"));
        assertTrue(aotClass.contains("new AOTConstructor[0]"),
                () -> "@Reflected without queryAll* must win — expected empty constructors; got: " + aotClass);
    }

    @Test
    void noFlagsAndNoExplicitMembersProducesEmptyArrays(@TempDir Path tmp) throws IOException {
        String src = """
                package sample;
                import com.garganttua.core.reflection.annotations.Reflected;
                @Reflected
                public class Bare {
                    String unused;
                    public void unusedMethod() {}
                }
                """;
        CompileResult r = compile(tmp, "sample.Bare", src);
        assertCompiled(r);
        String aotClass = Files.readString(r.outputDir.resolve("sample/AOTClass_Bare.java"));
        assertTrue(aotClass.contains("new AOTField[0]"), () -> aotClass);
        assertTrue(aotClass.contains("new AOTMethod[0]"), () -> aotClass);
        assertTrue(aotClass.contains("new AOTConstructor[0]"), () -> aotClass);
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
                    List.of("-Agarganttua.direct.binders=true", "-proc:only"),
                    null, List.of(file));
            task.setProcessors(List.of(new DirectBinderGenerator()));
            boolean ok = task.call();
            return new CompileResult(ok, outputDir, diagnostics);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * The processor writes generated sources, but javac will complain that it
     * cannot resolve AOTClass / AOTField / ... (the runtime AOT modules are
     * not on the test classpath). Those compilation errors are unrelated to
     * the processor's behaviour, so we only fail on diagnostics emitted by
     * the processor itself ({@code [garganttua-aot]}).
     */
    private static void assertCompiled(CompileResult r) {
        assertFalse(hasError(r, "[garganttua-aot]"),
                () -> "processor emitted an unexpected ERROR; got: " + diagSummary(r));
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
