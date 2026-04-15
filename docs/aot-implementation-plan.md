# Plan d'implémentation : Suite AOT & Harmonisation Native

## Contexte

Le framework Garganttua Core repose sur une abstraction de réflexion (`IClass<T>`, `IReflectionProvider`, `IAnnotationScanner`) qui permet de découpler le code métier de `java.lang.reflect`. Aujourd'hui, seule l'implémentation runtime existe (`RuntimeReflectionProvider`). L'objectif est de compléter la suite AOT pour :

1. **Éliminer la réflexion au runtime** — générer à la compilation des implémentations pré-calculées de `IClass`, des binders directs (appels `new`/`method()` au lieu de `Constructor.newInstance()`/`Method.invoke()`)
2. **Accélérer la découverte d'annotations** — indices pré-générés plutôt que scan classpath (Reflections lib)
3. **Harmoniser avec les modules Native/GraalVM** — Native consomme les métadonnées AOT pour générer `reflect-config.json`/`resource-config.json`

## État actuel

| Module | Statut | Contenu |
|--------|--------|---------|
| `aot-commons` | Vide | — |
| `aot-reflection` | Vide | — |
| `aot-annotation-scanner` | **Implémenté** | `AnnotationIndex` + `IndexedAnnotationScanner` |
| `aot-annotation-processor` | Stub SPI | Déclare `IndexedAnnotationProcessor` + `DirectBinderGenerator`, 0 code Java |
| `aot-maven-plugin` | Vide | — |
| `native-commons` | **Implémenté** | Config GraalVM (reflect-config.json, resource-config.json) |
| `native-annotation-processor` | Vide | — |
| `native-image-maven-plugin` | **Implémenté** | `NativeConfigMojo` |
| `garganttua-annotation-processor` (racine) | Commenté hors reactor, **répertoire supprimé** | Code source absent du disque |

## Architecture cible

```
garganttua-commons  (IClass, IReflectionProvider, IAnnotationScanner, @Reflected, @Indexed)
       │
       ├── aot-commons .............. Interfaces/registres AOT (IAOTRegistry, AOTMetadataConstants)
       │       │
       │       ├── aot-reflection ... AOTClass<T>, AOTField, AOTMethod, AOTReflectionProvider
       │       │
       │       └── aot-annotation-scanner [EXISTANT] AnnotationIndex, IndexedAnnotationScanner
       │
       ├── aot-annotation-processor . Génération compile-time (IndexedAnnotationProcessor + DirectBinderGenerator)
       │
       ├── aot-maven-plugin ......... Agrégation cross-module des indices et registres
       │
       ├── native-commons [EXISTANT]  Config GraalVM (NativeConfigurationBuilder, ReflectConfigEntry...)
       │       │
       │       └── native-image-maven-plugin [EXISTANT] (optionnellement utilise aot-annotation-scanner)
       │
       └── native-annotation-processor → SUPPRIMER (vide, le use-case est couvert par le Maven plugin)
```

**Principe clé** : AOT et Native sont deux pipelines qui partagent le même vocabulaire d'annotations (`@Reflected`, `@Indexed`) mais divergent à la sortie :
- **AOT** → classes Java pré-calculées (IClass, binders directs)
- **Native** → fichiers JSON GraalVM (reflect-config.json, resource-config.json)

Native ne dépend PAS de AOT au niveau module. Le plugin Maven Native peut optionnellement bénéficier des indices AOT.

---

## Phases d'implémentation

### Phase 1 : aot-commons — Fondation

**Fichiers à créer dans `garganttua-aot/garganttua-aot-commons/src/main/java/com/garganttua/core/aot/commons/` :**

- `IAOTRegistry.java` — Interface du registre central où les descripteurs AOT générés s'auto-enregistrent
  ```java
  public interface IAOTRegistry {
      <T> void register(String className, IClass<T> descriptor);
      <T> Optional<IClass<T>> get(String className);
      boolean contains(String className);
      Set<String> registeredClasses();
  }
  ```

- `AOTRegistry.java` — Singleton thread-safe (`ConcurrentHashMap`). Les classes générées appellent `AOTRegistry.register(...)` dans un bloc static.

- `IAOTClassDescriptor.java` — Marqueur étendant `IClass<T>` : `boolean isAOTGenerated()` (toujours `true`)

- `AOTMetadataConstants.java` — Chemins de ressources : `META-INF/garganttua/aot/classes/`, `META-INF/garganttua/aot/binders/`, `META-INF/garganttua/index/`

- `IAOTClassBuilder.java` — Interface fluent builder pour customiser les membres réflectés d'un `AOTClass` (ajout/suppression de champs, méthodes, constructeurs annotés `@Reflected`). Pattern identique à `IReflectionConfigurationEntryBuilder` du module native.

- `AOTException.java` — Exception spécifique AOT

**Dépendances POM** : `garganttua-commons` uniquement (déjà le cas)

---

### Phase 2 : aot-reflection — Implémentations IClass AOT

**Fichiers à créer dans `garganttua-aot/garganttua-aot-reflection/src/main/java/com/garganttua/core/aot/reflection/` :**

#### Descripteurs pré-calculés

- `AOTClass<T>` implements `IClass<T>`, `IAOTClassDescriptor`
  - Métadonnées stockées en champs `final` (name, modifiers, superclass, interfaces, fields, methods, constructors, annotations)
  - `getType()` résout paresseusement via `Class.forName()` quand nécessaire (cast, instanceof)
  - Les queries structurelles (getFields, getMethods...) utilisent les données pré-calculées

- `AOTField` implements `IField` — descripteur de champ pré-calculé
- `AOTMethod` implements `IMethod` — descripteur de méthode pré-calculé  
- `AOTConstructor<T>` implements `IConstructor<T>`
- `AOTParameter` implements `IParameter`

#### Builder de customisation — `AOTClassBuilder`

Builder fluent permettant de **personnaliser programmatiquement** les membres réflectés d'une classe AOT. Suit le même pattern que `IReflectionConfigurationEntryBuilder` du module native.

```java
public interface IAOTClassBuilder<T> extends IAutomaticBuilder<IAOTClassBuilder<T>, IClass<T>> {

    // --- Ajout de membres ---
    IAOTClassBuilder<T> field(String fieldName);
    IAOTClassBuilder<T> field(IField field);
    IAOTClassBuilder<T> fieldsAnnotatedWith(IClass<? extends Annotation> annotation);

    IAOTClassBuilder<T> method(String methodName, IClass<?>... parameterTypes);
    IAOTClassBuilder<T> method(IMethod method);
    IAOTClassBuilder<T> methodsAnnotatedWith(IClass<? extends Annotation> annotation);

    IAOTClassBuilder<T> constructor(IClass<?>... parameterTypes);
    IAOTClassBuilder<T> constructor(IConstructor<?> constructor);

    // --- Suppression de membres ---
    IAOTClassBuilder<T> removeField(String fieldName);
    IAOTClassBuilder<T> removeMethod(String methodName, IClass<?>... parameterTypes);
    IAOTClassBuilder<T> removeConstructor(IClass<?>... parameterTypes);

    // --- Flags globaux ---
    IAOTClassBuilder<T> queryAllDeclaredConstructors(boolean value);
    IAOTClassBuilder<T> queryAllDeclaredMethods(boolean value);
    IAOTClassBuilder<T> allDeclaredFields(boolean value);
}
```

**Usage** : le `DirectBinderGenerator` crée une instance `AOTClassBuilder` par classe `@Reflected`, la pré-configure à partir des annotations, puis appelle `build()` pour produire l'`AOTClass` finale. Les utilisateurs peuvent aussi créer des builders programmatiquement pour des cas dynamiques (plugins, extensions).

L'interface est déclarée dans `aot-commons` (car partagée entre le processeur et le runtime), l'implémentation est dans `aot-reflection`.

#### Provider AOT

- `AOTReflectionProvider` implements `IReflectionProvider`
  - `supports(type)` → `AOTRegistry.contains(type.getName())`
  - `getClass(clazz)` → retourne le `AOTClass` du registre
  - `forName(name)` → lookup par nom dans le registre

**Dépendances POM** : ajouter `garganttua-aot-commons`

---

### Phase 3 : aot-annotation-processor — Génération compile-time

**Fichiers à créer dans `garganttua-aot/garganttua-aot-annotation-processor/src/main/java/com/garganttua/core/aot/annotation/processor/` :**

**Processeur 1 — `IndexedAnnotationProcessor.java`**
- Ré-implémentation du processeur de l'ancien module racine (code absent du disque, à ré-écrire)
- Scanne les annotations méta-annotées `@Indexed`
- Génère `META-INF/garganttua/index/<annotation.fqn>` avec entrées `C:` et `M:`
- Indexe aussi les annotations JSR-330 (`@Inject`, `@Singleton`, `@Named`)

**Processeur 2 — `DirectBinderGenerator.java`** (le coeur de la stratégie AOT)

Le processeur d'annotation scanne automatiquement les classes annotées `@Reflected` à la compilation et **génère des implémentations concrètes de `IClass<T>`** pré-remplies avec toutes les métadonnées. Aucune introspection runtime n'est nécessaire pour ces classes.

Pour chaque type annoté `@Reflected`, le processeur génère :
  - `AOTClass_<SimpleName>.java` — implémentation pré-calculée de `IClass<T>` (voir exemple ci-dessous)
  - `AOTConstructorBinder_<SimpleName>.java` — appel direct `new ClassName(args)` au lieu de `Constructor.newInstance()`
  - `AOTMethodBinder_<SimpleName>_<methodName>.java` — appel direct `instance.method(args)` au lieu de `Method.invoke()`
  - Entrée dans `META-INF/garganttua/aot/classes/<fqn>` pour l'auto-registration
  - Bloc `static {}` qui enregistre l'AOTClass dans l'`AOTRegistry`

Contrôlé par `-Agarganttua.direct.binders=true` (déjà configuré dans le POM racine ligne 81).

**Localisation des fichiers générés :**
- Classes Java générées → `target/generated-sources/annotations/` (standard `processingEnv.getFiler().createSourceFile()`)
- Fichiers index → `target/classes/META-INF/garganttua/index/` (via `processingEnv.getFiler().createResource()`)
- Registre AOT → `target/classes/META-INF/garganttua/aot/classes/` (listing des descripteurs)
- Les classes générées sont automatiquement compilées par `javac` dans le même round d'annotation processing

**Exemple concret** — pour une classe source :
```java
@Reflected(queryAllDeclaredConstructors = true, allDeclaredFields = true)
public class UserService {
    private String name;
    public UserService(String name) { this.name = name; }
    public String getName() { return name; }
}
```

Le processeur génère automatiquement :
```java
public final class AOTClass_UserService extends AOTClass<UserService> {
    public static final AOTClass_UserService INSTANCE = new AOTClass_UserService();
    
    static {
        // Auto-registration dans le registre AOT
        AOTRegistry.getInstance().register("com.example.UserService", INSTANCE);
    }

    private AOTClass_UserService() {
        super(
            "com.example.UserService",      // name
            Modifier.PUBLIC,                 // modifiers
            "java.lang.Object",             // superclass
            List.of(),                       // interfaces
            List.of(                         // fields (pré-calculés)
                new AOTField("name", "java.lang.String", Modifier.PRIVATE, List.of())
            ),
            List.of(                         // methods (pré-calculés)
                new AOTMethod("getName", "java.lang.String", List.of(), Modifier.PUBLIC, List.of())
            ),
            List.of(                         // constructors (pré-calculés)
                new AOTConstructor<>(List.of("java.lang.String"), Modifier.PUBLIC, List.of())
            ),
            List.of(Reflected.class)         // annotations
        );
    }

    @Override
    public Class<UserService> getType() {
        return UserService.class;  // résolution directe, pas Class.forName()
    }
}
```

**Résultat** : quand l'AOT est sur le classpath, `IClass.getClass(UserService.class)` retourne `AOTClass_UserService.INSTANCE` (priorité 20) au lieu d'un `RuntimeClass` (priorité 10). Toutes les queries structurelles (getFields, getMethods, getAnnotations...) sont des lectures de champs `final` — zéro réflexion.

**Helpers :**
- `AOTClassGenerator.java` — construit le code source d'une implémentation `AOTClass` via templates
- `AOTBinderGenerator.java` — construit le code source des binders directs (constructeurs et méthodes)

**Dépendances POM** : `garganttua-commons` uniquement (c'est un outil compile-time, le code *généré* dépendra de `aot-commons`/`aot-reflection`)

---

### Phase 4 : aot-annotation-scanner — Mise à jour

**Fichier à modifier : `AnnotationIndex.java`**
- **Ligne 73** : `RuntimeClass::ofUnchecked` → `IClass::getClass` (dispatch via composite reflection, retourne AOTClass si disponible)
- **Ligne 79** : `RuntimeMethod::of` → résolution via `IClass.getClass(method.getDeclaringClass()).getDeclaredMethod(...)`

Ceci assure que le scanner retourne des descripteurs AOT quand le provider AOT est actif.

---

### Phase 5 : aot-maven-plugin — Agrégation cross-module

**Fichiers à créer :**

- `AggregateIndexMojo.java` (goal: `aggregate-index`, phase: `process-classes`)
  - Fusionne les fichiers `META-INF/garganttua/index/` de toutes les dépendances dans le module courant
  - Équivalent du `AppendingTransformer` du shade plugin, mais au compile-time

- `AggregateAOTRegistryMojo.java` (goal: `aggregate-registry`, phase: `process-classes`)
  - Fusionne les listings `META-INF/garganttua/aot/classes/` des dépendances

- `ValidateAOTMojo.java` (goal: `validate-aot`, phase: `verify`)
  - Vérifie que tous les types `@Reflected` ont un descripteur AOT correspondant

---

### Phase 6 : Intégration dans ReflectionBuilder

**Fichier à modifier : `ReflectionBuilder.java`**
- `doAutoDetection()` (actuellement no-op, ligne 74) :
  1. Tente `Class.forName("com.garganttua.core.aot.commons.AOTRegistry")` pour détecter AOT sur le classpath
  2. Si présent → instancie `AOTReflectionProvider` et l'enregistre avec **priorité 20** (> 10 du runtime)
  3. Tente de détecter `IndexedAnnotationScanner` → l'enregistre comme scanner priorité 20

L'AOT est ainsi **opt-in** : si les JARs AOT sont sur le classpath, ils prennent le dessus. Sinon, le `RuntimeReflectionProvider` reste le fallback universel.

---

### Phase 7 : Harmonisation Native (optionnel, après validation AOT)

- **Supprimer** `garganttua-native-annotation-processor` (vide, inutile)
- **Modifier** `NativeConfigMojo.java` : utiliser `IndexedAnnotationScanner` quand des fichiers index existent sur le classpath, évitant le scan Reflections coûteux
- Ajouter `garganttua-aot-annotation-scanner` comme dépendance optionnelle du plugin Maven Native

---

## Décisions de design

| Décision | Choix | Raison |
|----------|-------|--------|
| Séparation AOT / Native | Séparés, Native consomme optionnellement AOT | SRP, outputs différents (Java vs JSON) |
| native-annotation-processor | Supprimer | Vide, GraalVM config nécessite le classpath complet → Maven plugin |
| Consolidation annotation processors | Un seul module (`aot-annotation-processor`), 2 processeurs | Un SPI, un JAR, logique partagée |
| Stratégie de génération | Code source Java (pas bytecode) | Débuggable, inspectable dans l'IDE |
| Pattern de registre | Singleton statique `ConcurrentHashMap` | Classes générées s'enregistrent en bloc `static {}`, thread-safe |
| Priorité des providers | AOT = 20, Runtime = 10 | AOT prime pour les types enregistrés, runtime = filet de sécurité |
| AnnotationIndex | Migration vers `IClass.forName()` | Retourne des descripteurs AOT quand disponibles |

## Ordre d'implémentation recommandé

```
Phase 1 (aot-commons)
  └→ Phase 2 (aot-reflection)    ← peuvent être parallélisées
  └→ Phase 3 (aot-annotation-processor)   ← le plus gros morceau
       └→ Phase 4 (aot-annotation-scanner update)
       └→ Phase 5 (aot-maven-plugin)
            └→ Phase 6 (ReflectionBuilder integration)
                 └→ Phase 7 (Native harmonization)
```

## Vérification

1. **Tests unitaires** pour chaque module AOT (AOTClass, AOTReflectionProvider, registres)
2. **Test d'intégration** : compiler un module avec le processeur AOT activé, vérifier que les fichiers sont générés dans `META-INF/garganttua/`
3. **Test end-to-end** : application bootstrap avec AOT sur le classpath, vérifier que `IClass.getClass(MyBean.class)` retourne un `AOTClass` (pas un `RuntimeClass`)
4. **Test de régression** : build complet sans AOT sur le classpath → comportement identique à aujourd'hui (1175 tests passent)
5. **Test GraalVM** : `native-image-maven-plugin` utilise les indices AOT quand disponibles
6. Commande : `mvn clean test` sur tout le reactor
