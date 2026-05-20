package com.garganttua.core.aot.annotation.processor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

/**
 * Compile-time annotation processor that generates AOT descriptor classes
 * for every type annotated with {@code @Reflected}.
 *
 * <p>Enabled by the compiler option {@code -Agarganttua.direct.binders=true}.
 * If the option is missing or not {@code "true"} the processor does nothing.</p>
 *
 * <p>For each {@code @Reflected} type, the processor emits:</p>
 * <ul>
 *   <li>An {@code AOTClass_<SimpleName>} source file referencing per-member
 *       descriptor singletons (no member metadata is built at runtime).</li>
 *   <li>One {@code AOTField_<SimpleName>_<field>} per included field, with
 *       direct (no-reflection) {@code get}/{@code set}.</li>
 *   <li>One {@code AOTMethod_<SimpleName>_<method>_<i>} per included method,
 *       with direct {@code invoke}.</li>
 *   <li>One {@code AOTConstructor_<SimpleName>_<i>} per included constructor,
 *       with direct {@code newInstance}.</li>
 *   <li>A listing entry in {@code META-INF/garganttua/aot/classes/<fqn>}.</li>
 * </ul>
 *
 * <p>Fields, methods and constructors may carry an explicit {@code @Reflected}
 * to be included individually, in addition to the class-level
 * {@code queryAll* / allDeclaredFields} flags. The enclosing type must itself
 * be annotated with {@code @Reflected}; a member-only annotation is rejected
 * at compile time.</p>
 *
 * <p><strong>Visibility constraint:</strong> any included member that is
 * {@code private} causes a compile-time error. Direct binders cannot bypass
 * Java visibility — promote the member to package-private (or change the
 * scope of the {@code queryAll*} flag).</p>
 *
 * @since 2.0.0-ALPHA01
 */
@SupportedAnnotationTypes("com.garganttua.core.reflection.annotations.Reflected")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@SupportedOptions("garganttua.direct.binders")
public class DirectBinderGenerator extends AbstractProcessor {

    private static final String REFLECTED_ANNOTATION = "com.garganttua.core.reflection.annotations.Reflected";
    private static final String AOT_CLASSES_DIR = "META-INF/garganttua/aot/classes/";

    private Messager messager;
    private boolean enabled;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.messager = processingEnv.getMessager();
        String option = processingEnv.getOptions().get("garganttua.direct.binders");
        this.enabled = "true".equalsIgnoreCase(option);
        if (enabled) {
            messager.printMessage(Diagnostic.Kind.NOTE,
                    "[garganttua-aot] DirectBinderGenerator enabled");
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!enabled || roundEnv.processingOver()) {
            return false;
        }

        for (TypeElement annotation : annotations) {
            if (!REFLECTED_ANNOTATION.equals(annotation.getQualifiedName().toString())) {
                continue;
            }

            Map<TypeElement, Set<Element>> membersByType = new LinkedHashMap<>();
            Set<TypeElement> reflectedTypes = new LinkedHashSet<>();

            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                if (element instanceof TypeElement typeElement) {
                    reflectedTypes.add(typeElement);
                    membersByType.computeIfAbsent(typeElement, k -> new LinkedHashSet<>());
                } else if (isMemberKind(element.getKind())) {
                    Element enclosing = element.getEnclosingElement();
                    if (enclosing instanceof TypeElement enclosingType) {
                        membersByType
                                .computeIfAbsent(enclosingType, k -> new LinkedHashSet<>())
                                .add(element);
                    } else {
                        messager.printMessage(Diagnostic.Kind.ERROR,
                                "[garganttua-aot] @Reflected member is not enclosed by a class/interface",
                                element);
                    }
                }
            }

            // Members annotated without a class-level @Reflected on their enclosing type → error
            for (Map.Entry<TypeElement, Set<Element>> entry : membersByType.entrySet()) {
                TypeElement type = entry.getKey();
                Set<Element> members = entry.getValue();
                if (!reflectedTypes.contains(type) && !members.isEmpty()) {
                    for (Element member : members) {
                        messager.printMessage(Diagnostic.Kind.ERROR,
                                "[garganttua-aot] @Reflected on a member requires its enclosing type "
                                        + type.getQualifiedName() + " to also be annotated with @Reflected",
                                member);
                    }
                }
            }

            for (TypeElement type : reflectedTypes) {
                Set<Element> members = membersByType.getOrDefault(type, Set.of());
                processReflectedType(type, members);
            }
        }
        return false;
    }

    private static boolean isMemberKind(ElementKind kind) {
        return kind == ElementKind.METHOD
                || kind == ElementKind.CONSTRUCTOR
                || kind == ElementKind.FIELD;
    }

    private void processReflectedType(TypeElement typeElement, Set<Element> explicitMembers) {
        String qualifiedName = typeElement.getQualifiedName().toString();
        try {
            MemberInclusion.Flags flags = new MemberInclusion.Flags(
                    getAnnotationBooleanValue(typeElement, "queryAllDeclaredConstructors"),
                    getAnnotationBooleanValue(typeElement, "queryAllPublicConstructors"),
                    getAnnotationBooleanValue(typeElement, "queryAllDeclaredMethods"),
                    getAnnotationBooleanValue(typeElement, "queryAllPublicMethods"),
                    getAnnotationBooleanValue(typeElement, "allDeclaredFields"));

            List<VariableElement> fields = MemberInclusion.includedFields(typeElement, flags, explicitMembers);
            List<ExecutableElement> methods = MemberInclusion.includedMethods(typeElement, flags, explicitMembers);
            List<ExecutableElement> constructors = MemberInclusion.includedConstructors(typeElement, flags, explicitMembers);

            warnOnRedundantMembers(explicitMembers, flags);

            if (rejectPrivateMembers(fields, methods, constructors)) {
                // One or more private members → errors already emitted, skip generation
                return;
            }

            Map<ExecutableElement, String> methodNames = AOTNaming.methodDescriptorNames(typeElement, methods);
            Map<ExecutableElement, String> constructorNames = AOTNaming.constructorDescriptorNames(typeElement, constructors);

            // 1. Per-field descriptors
            for (VariableElement field : fields) {
                AOTFieldSourceGenerator gen = new AOTFieldSourceGenerator(typeElement, field);
                writeSource(gen.getGeneratedQualifiedName(), gen.generate(), typeElement);
            }
            // 2. Per-method descriptors
            for (ExecutableElement method : methods) {
                AOTMethodSourceGenerator gen = new AOTMethodSourceGenerator(typeElement, method, methodNames.get(method));
                writeSource(gen.getGeneratedQualifiedName(), gen.generate(), typeElement);
            }
            // 3. Per-constructor descriptors
            for (ExecutableElement ctor : constructors) {
                AOTConstructorSourceGenerator gen = new AOTConstructorSourceGenerator(typeElement, ctor, constructorNames.get(ctor));
                writeSource(gen.getGeneratedQualifiedName(), gen.generate(), typeElement);
            }
            // 4. The class descriptor (refers to the above)
            AOTClassSourceGenerator classGen = new AOTClassSourceGenerator(
                    typeElement, fields, methods, methodNames, constructors, constructorNames);
            writeSource(classGen.getGeneratedQualifiedName(), classGen.generate(), typeElement);

            messager.printMessage(Diagnostic.Kind.NOTE,
                    "[garganttua-aot] Generated AOT descriptor: " + classGen.getGeneratedQualifiedName());

            writeListingEntry(qualifiedName, classGen.getGeneratedQualifiedName());
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "[garganttua-aot] Failed to generate AOT class for " + qualifiedName + ": " + e.getMessage(),
                    typeElement);
        }
    }

    /**
     * Emits an ERROR for every included member that is {@code private}. Direct
     * binders cannot bypass Java visibility — the user must change the member's
     * visibility or pick a less broad flag.
     *
     * @return {@code true} if at least one private member was rejected.
     */
    private boolean rejectPrivateMembers(List<VariableElement> fields,
                                         List<ExecutableElement> methods,
                                         List<ExecutableElement> constructors) {
        List<Element> offenders = new ArrayList<>();
        for (VariableElement f : fields) if (f.getModifiers().contains(Modifier.PRIVATE)) offenders.add(f);
        for (ExecutableElement m : methods) if (m.getModifiers().contains(Modifier.PRIVATE)) offenders.add(m);
        for (ExecutableElement c : constructors) if (c.getModifiers().contains(Modifier.PRIVATE)) offenders.add(c);
        for (Element offender : offenders) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "[garganttua-aot] AOT direct binders cannot access private members. "
                            + "Promote this member to package-private, or narrow the queryAll* flag on the enclosing class.",
                    offender);
        }
        return !offenders.isEmpty();
    }

    private void warnOnRedundantMembers(Set<Element> explicitMembers, MemberInclusion.Flags flags) {
        for (Element member : explicitMembers) {
            boolean redundant = switch (member.getKind()) {
                case FIELD -> flags.allDeclaredFields();
                case METHOD -> flags.queryAllDeclaredMethods()
                        || (flags.queryAllPublicMethods() && member.getModifiers().contains(Modifier.PUBLIC));
                case CONSTRUCTOR -> flags.queryAllDeclaredConstructors()
                        || (flags.queryAllPublicConstructors() && member.getModifiers().contains(Modifier.PUBLIC));
                default -> false;
            };
            if (redundant) {
                messager.printMessage(Diagnostic.Kind.WARNING,
                        "[garganttua-aot] @Reflected on this member is redundant: "
                                + "the enclosing class already includes it via a queryAll* / allDeclaredFields flag",
                        member);
            }
        }
    }

    private void writeSource(String generatedFqn, String sourceCode, TypeElement originator) throws IOException {
        JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(generatedFqn, originator);
        try (Writer writer = sourceFile.openWriter()) {
            writer.write(sourceCode);
        }
    }

    private void writeListingEntry(String originalFqn, String generatedFqn) throws IOException {
        String resourcePath = AOT_CLASSES_DIR + originalFqn;
        FileObject fileObject = processingEnv.getFiler().createResource(
                StandardLocation.CLASS_OUTPUT, "", resourcePath);
        try (Writer writer = fileObject.openWriter();
             BufferedWriter bw = new BufferedWriter(writer)) {
            bw.write(generatedFqn);
            bw.newLine();
        }
    }

    private boolean getAnnotationBooleanValue(TypeElement typeElement, String attributeName) {
        for (AnnotationMirror mirror : typeElement.getAnnotationMirrors()) {
            TypeElement annoElement = (TypeElement) mirror.getAnnotationType().asElement();
            if (REFLECTED_ANNOTATION.equals(annoElement.getQualifiedName().toString())) {
                for (var entry : mirror.getElementValues().entrySet()) {
                    ExecutableElement key = entry.getKey();
                    AnnotationValue value = entry.getValue();
                    if (attributeName.equals(key.getSimpleName().toString())
                            && value.getValue() instanceof Boolean b) {
                        return b;
                    }
                }
                return false;
            }
        }
        return false;
    }
}
