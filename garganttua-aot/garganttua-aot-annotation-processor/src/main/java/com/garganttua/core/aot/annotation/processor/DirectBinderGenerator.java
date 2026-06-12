package com.garganttua.core.aot.annotation.processor;

import java.io.BufferedReader;
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
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@SupportedOptions("garganttua.direct.binders")
public class DirectBinderGenerator extends AbstractProcessor {

    private static final String REFLECTED_ANNOTATION = "com.garganttua.core.reflection.annotations.Reflected";
    private static final String INDEXED_ANNOTATION = "com.garganttua.core.reflection.annotations.Indexed";
    private static final String AOT_CLASSES_DIR = "META-INF/garganttua/aot/classes/";

    /** Types already processed across all rounds — guards against double-generation
     *  when a class is BOTH @Reflected AND has @Indexed-meta annotations. */
    private final java.util.Set<String> processedTypes = new LinkedHashSet<>();

    private Messager messager;
    private boolean enabled;

    /**
     * Captures the {@link Messager} and reads the {@code garganttua.direct.binders}
     * option, enabling generation only when it is set to {@code "true"}.
     *
     * @param processingEnv the processing environment supplied by the compiler
     */
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

    /** FQNs of every AOTClass_* generated across all processing rounds.
     *  Written to META-INF/services/...IAOTSelfRegistering in the final round
     *  so ServiceLoader (and GraalVM native-image) can discover them. */
    private final java.util.Set<String> generatedDescriptorFqns = new LinkedHashSet<>();

    /**
     * Generates AOT descriptors for {@code @Reflected} types (and auto-promoted
     * {@code @Indexed}-meta types) each round, then writes the ServiceLoader
     * descriptor once processing is over. Does nothing when generation is disabled.
     *
     * @param annotations the annotation types requested to be processed this round
     * @param roundEnv     information about the current and prior rounds
     * @return {@code false} — this processor never claims the annotations it reads
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!enabled) {
            return false;
        }
        if (roundEnv.processingOver()) {
            // Final round: emit the ServiceLoader descriptor accumulated across rounds.
            writeSelfRegisteringServiceFile();
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

        // Pass 2: auto-promote classes carrying any @Indexed-meta annotation
        // (class-level OR on any member). This closes the "index says yes,
        // descriptor says no" inconsistency where a class is indexed by the
        // scanner (via @Indexed-marked annotations like @ChildContext,
        // @Expression, @MutexFactory, …) but has no AOTClass_* because it
        // forgot to add @Reflected. The framework instantiates / introspects
        // these classes at runtime; without a full descriptor they crash on
        // missing constructors / methods in pure-AOT mode.
        for (Element root : roundEnv.getRootElements()) {
            if (root instanceof TypeElement type) {
                autoPromoteIfIndexed(type);
            }
        }

        return false;
    }

    /**
     * If {@code type} has any class-level or member-level annotation that is
     * itself meta-annotated with {@code @Indexed}, and it hasn't been processed
     * already (via explicit {@code @Reflected} or a prior round), generates a
     * full descriptor with broad-coverage defaults
     * ({@code queryAllDeclaredConstructors=true, queryAllDeclaredMethods=true}).
     */
    private void autoPromoteIfIndexed(TypeElement type) {
        String fqn = type.getQualifiedName().toString();
        if (processedTypes.contains(fqn)) {
            return;
        }
        if (!hasIndexedMetaAnnotation(type)) {
            return;
        }
        MemberInclusion.Flags flags = new MemberInclusion.Flags(
                true,   // queryAllDeclaredConstructors — needed for @ChildContext etc.
                false,  // queryAllPublicConstructors
                true,   // queryAllDeclaredMethods    — needed for @Expression etc.
                false,  // queryAllPublicMethods
                false   // allDeclaredFields           — conservative, opt-in via @Reflected
        );
        try {
            // strict=false: auto-promote is implicit on the user's behalf,
            // private members are silently filtered out.
            processTypeWithFlags(type, Set.of(), flags, false);
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "[garganttua-aot] Failed to auto-promote AOT class for " + fqn + ": " + e.getMessage(),
                    type);
        }
    }

    private boolean hasIndexedMetaAnnotation(TypeElement type) {
        // Class-level
        for (AnnotationMirror am : type.getAnnotationMirrors()) {
            if (isIndexedMetaAnnotated(am)) {
                return true;
            }
        }
        // Member-level (methods, fields, constructors)
        for (Element member : type.getEnclosedElements()) {
            ElementKind kind = member.getKind();
            if (kind == ElementKind.METHOD || kind == ElementKind.FIELD
                    || kind == ElementKind.CONSTRUCTOR) {
                for (AnnotationMirror am : member.getAnnotationMirrors()) {
                    if (isIndexedMetaAnnotated(am)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** True if the annotation type itself bears {@code @Indexed}. */
    private boolean isIndexedMetaAnnotated(AnnotationMirror am) {
        Element annoElement = am.getAnnotationType().asElement();
        if (!(annoElement instanceof TypeElement annoTypeElement)) {
            return false;
        }
        // Don't auto-promote on the @Reflected marker itself — it's already
        // handled by pass 1 with whatever flags the user explicitly chose.
        String annoName = annoTypeElement.getQualifiedName().toString();
        if (REFLECTED_ANNOTATION.equals(annoName)) {
            return false;
        }
        for (AnnotationMirror meta : annoTypeElement.getAnnotationMirrors()) {
            Element metaElement = meta.getAnnotationType().asElement();
            if (metaElement instanceof TypeElement metaTypeElement
                    && INDEXED_ANNOTATION.equals(metaTypeElement.getQualifiedName().toString())) {
                return true;
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
        MemberInclusion.Flags flags = new MemberInclusion.Flags(
                getAnnotationBooleanValue(typeElement, "queryAllDeclaredConstructors"),
                getAnnotationBooleanValue(typeElement, "queryAllPublicConstructors"),
                getAnnotationBooleanValue(typeElement, "queryAllDeclaredMethods"),
                getAnnotationBooleanValue(typeElement, "queryAllPublicMethods"),
                getAnnotationBooleanValue(typeElement, "allDeclaredFields"));
        try {
            // strict=true: explicit @Reflected with a private member is a user
            // intent we must enforce — emit an error.
            processTypeWithFlags(typeElement, explicitMembers, flags, true);
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "[garganttua-aot] Failed to generate AOT class for "
                            + typeElement.getQualifiedName() + ": " + e.getMessage(),
                    typeElement);
        }
    }

    /**
     * Shared body used by both the explicit {@code @Reflected} pass and the
     * auto-promotion pass (Indexed-meta annotations). Idempotent across rounds
     * via {@link #processedTypes}.
     *
     * @param strict when true, private members trigger an error (explicit
     *               {@code @Reflected} path). When false, private members are
     *               silently filtered out (auto-promote path — the user did
     *               not opt-in to direct binders on this type).
     */
    private void processTypeWithFlags(TypeElement typeElement, Set<Element> explicitMembers,
            MemberInclusion.Flags flags, boolean strict) throws IOException {
        String qualifiedName = typeElement.getQualifiedName().toString();
        if (!processedTypes.add(qualifiedName)) {
            return; // already generated in a previous round
        }

        List<VariableElement> fields = MemberInclusion.includedFields(typeElement, flags, explicitMembers);
        List<ExecutableElement> methods = MemberInclusion.includedMethods(typeElement, flags, explicitMembers);
        List<ExecutableElement> constructors = MemberInclusion.includedConstructors(typeElement, flags, explicitMembers);

        warnOnRedundantMembers(explicitMembers, flags);

        if (strict) {
            // Strict path: a private member ONLY triggers an error if the
            // user explicitly annotated it with @Reflected — a clear mistake.
            // queryAll* flags inclusively pull privates too, but those are
            // silently filtered, same as the auto-promote path. This matches
            // the user's intent: "all public surface" is rarely meant to
            // include private utility helpers.
            List<Element> explicitPrivate = new ArrayList<>();
            for (Element member : explicitMembers) {
                if (member.getModifiers().contains(Modifier.PRIVATE)) {
                    explicitPrivate.add(member);
                }
            }
            if (!explicitPrivate.isEmpty()) {
                for (Element offender : explicitPrivate) {
                    messager.printMessage(Diagnostic.Kind.ERROR,
                            "[garganttua-aot] AOT direct binders cannot access private members. "
                                    + "Promote this member to package-private, or remove its @Reflected annotation.",
                            offender);
                }
                return;
            }
        }
        // Filter privates silently — applies to both strict (from queryAll*)
        // and lenient (auto-promote) paths.
        fields = fields.stream()
                .filter(f -> !f.getModifiers().contains(Modifier.PRIVATE))
                .toList();
        methods = methods.stream()
                .filter(m -> !m.getModifiers().contains(Modifier.PRIVATE))
                .toList();
        constructors = constructors.stream()
                .filter(c -> !c.getModifiers().contains(Modifier.PRIVATE))
                .toList();

        Map<ExecutableElement, String> methodNames = AOTNaming.methodDescriptorNames(typeElement, methods);
        Map<ExecutableElement, String> constructorNames = AOTNaming.constructorDescriptorNames(typeElement, constructors);

        javax.lang.model.util.Types types = processingEnv.getTypeUtils();
        // 1. Per-field descriptors
        for (VariableElement field : fields) {
            AOTFieldSourceGenerator gen = new AOTFieldSourceGenerator(types, typeElement, field);
            writeSource(gen.getGeneratedQualifiedName(), gen.generate(), typeElement);
        }
        // 2. Per-method descriptors
        for (ExecutableElement method : methods) {
            AOTMethodSourceGenerator gen = new AOTMethodSourceGenerator(types, typeElement, method, methodNames.get(method));
            writeSource(gen.getGeneratedQualifiedName(), gen.generate(), typeElement);
        }
        // 3. Per-constructor descriptors
        for (ExecutableElement ctor : constructors) {
            AOTConstructorSourceGenerator gen = new AOTConstructorSourceGenerator(types, typeElement, ctor, constructorNames.get(ctor));
            writeSource(gen.getGeneratedQualifiedName(), gen.generate(), typeElement);
        }
        // 4. The class descriptor (refers to the above)
        AOTClassSourceGenerator classGen = new AOTClassSourceGenerator(
                types, typeElement, fields, methods, methodNames, constructors, constructorNames);
        writeSource(classGen.getGeneratedQualifiedName(), classGen.generate(), typeElement);

        messager.printMessage(Diagnostic.Kind.NOTE,
                "[garganttua-aot] Generated AOT descriptor: " + classGen.getGeneratedQualifiedName());

        writeListingEntry(qualifiedName, classGen.getGeneratedQualifiedName());
        this.generatedDescriptorFqns.add(classGen.getGeneratedQualifiedName());
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

    /**
     * Writes {@code META-INF/services/com.garganttua.core.aot.commons.IAOTSelfRegistering}
     * listing every {@code AOTClass_*} generated across all rounds. This is
     * the GraalVM-native-image-friendly entry point: ServiceLoader picks the
     * service descriptor up automatically without reachability-metadata
     * configuration, triggers each class's static init, which self-registers
     * into the AOTRegistry.
     */
    private void writeSelfRegisteringServiceFile() {
        if (this.generatedDescriptorFqns.isEmpty()) {
            return;
        }
        String resourcePath = "META-INF/services/com.garganttua.core.aot.commons.IAOTSelfRegistering";
        try {
            FileObject existing = null;
            try {
                existing = processingEnv.getFiler().getResource(
                        StandardLocation.CLASS_OUTPUT, "", resourcePath);
            } catch (IOException ignored) {
                // first round; no existing file
            }
            // Merge with whatever a previous incremental round may have written.
            java.util.Set<String> all = new LinkedHashSet<>(this.generatedDescriptorFqns);
            if (existing != null) {
                try (BufferedReader br = new BufferedReader(existing.openReader(true))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        String t = line.trim();
                        if (!t.isEmpty() && !t.startsWith("#")) {
                            all.add(t);
                        }
                    }
                } catch (IOException ignored) {
                    // file didn't exist or unreadable — proceed with our set
                }
            }
            FileObject fileObject = processingEnv.getFiler().createResource(
                    StandardLocation.CLASS_OUTPUT, "", resourcePath);
            try (Writer writer = fileObject.openWriter();
                 BufferedWriter bw = new BufferedWriter(writer)) {
                for (String fqn : all) {
                    bw.write(fqn);
                    bw.newLine();
                }
            }
            messager.printMessage(Diagnostic.Kind.NOTE,
                    "[garganttua-aot] Wrote ServiceLoader descriptor with "
                            + all.size() + " AOT classes");
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.WARNING,
                    "[garganttua-aot] Failed to write " + resourcePath + ": " + e.getMessage());
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
