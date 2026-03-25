# Roadmap : Unification Reflection / AOT / GraalVM

## Stratégie

Les annotations décrivent la **vérité** sur le code, pas la stratégie de build :

- `@Indexed` — cette annotation doit être découvrable (existe déjà)
- `@Reflected` — ce code est accédé par réflexion (remplace `@Native`)
- `@ReflectedBuilder` — cette classe utilise la réflexion et sait dire sur quoi (remplace `@NativeConfigurationBuilder`)

Deux pipelines consomment les mêmes annotations :

```
@Reflected / @ReflectedBuilder / @Indexed
        │
        ├── garganttua-graalvm-*     → reflect-config.json / resource-config.json
        │
        └── garganttua-aot-*         → IClass<T> + direct binders + IAnnotationScanner pré-indexé
```

## Graphe de dépendances

```
Phase 1 ──── #1  @Reflected + @ReflectedBuilder
         │── #6  aot-common : interfaces AOT
         │
Phase 2 ──── #2  Migrer @Native → @Reflected              [← #1]
         │── #7  aot-reflection : IClass<T> pré-générés     [← #6]
         │── #8  aot-annotation-scanner : scanner pré-indexé [← #6]
         │
Phase 3 ──── #3  Renommer native → graalvm                  [← #1, #2]
         │── #9  aot-annotation-processor : code gen         [← #6, #7, #8]
         │
Phase 4 ──── #4  Renommer plugin Maven GraalVM              [← #3]
         │── #5  Nettoyer interfaces native dans commons     [← #3]
         │── #10 aot-maven-plugin (si nécessaire)            [← #9]
         │── #11 Supprimer DirectBinderGenerator             [← #9]
         │── #16 Supprimer dépendance native des modules core [← #2, #5]
         │
Phase 5 ──── #12 Intégrer AOT dans CompositeReflection      [← #7, #8, #11]
         │── #15 Déplacer modules GraalVM dans bindings      [← #4]
         │── #17 Rendre IClass et IReflection thread-safe    [indépendant]
         │
Phase 6 ──── #13 Tests des deux pipelines                   [← #12]
         │── #14 POM parent, reactor, documentation          [← #4, #5, #12, #15, #16]
```

---

## Phase 1 — Fondations (parallélisable)

### #1 — Créer @Reflected et @ReflectedBuilder dans garganttua-commons

Créer les nouvelles annotations dans `com.garganttua.core.reflection.annotations` :

- `@Reflected` (TARGET: TYPE, FIELD, CONSTRUCTOR, METHOD) marquée `@Indexed`, avec attributs :
  - `queryAllDeclaredConstructors`, `queryAllPublicConstructors`
  - `queryAllDeclaredMethods`, `queryAllPublicMethods`
  - `allDeclaredClasses`, `allPublicClasses`
  - `allDeclaredFields`
- `@ReflectedBuilder` (TARGET: TYPE) marquée `@Indexed`
- Créer l'interface `IReflectionUsageReporter` que les classes `@ReflectedBuilder` implémentent, retournant la cartographie des usages de réflexion

**Bloque :** #2, #3

---

### #6 — Implémenter garganttua-aot-common : interfaces et annotations AOT

Remplir le module `garganttua-aot-common` (actuellement vide) :

- Interfaces du registre AOT : `IAOTRegistry`, `IAOTReflectionProvider`, `IAOTAnnotationScanner`
- Interfaces pour les descripteurs AOT générés
- Conventions de nommage et packaging pour le code généré
- Format des fichiers d'index AOT (`META-INF/garganttua/aot/*`)
- Dépendance unique : `garganttua-commons`

**Bloque :** #7, #8, #9

---

## Phase 2 — Migration annotations + implémentations AOT

### #2 — Migrer toutes les utilisations de @Native → @Reflected

- [ ] Remplacer tous les imports/usages de `@Native` par `@Reflected`
- [ ] Remplacer `@NativeConfigurationBuilder` par `@ReflectedBuilder` sur les 3 classes existantes :
  - `InjectionContextBuilder`
  - `RuntimeNativeConfigurationBuilder`
  - `ExpressionNativeConfigurationBuilder`
- [ ] Mettre à jour `INativeReflectionConfiguration` → `IReflectionUsageReporter`
- [ ] Supprimer les anciennes annotations `@Native` et `@NativeConfigurationBuilder`

**Bloqué par :** #1

---

### #7 — Implémenter garganttua-aot-reflection : IClass\<T\> pré-générés

- Implémentations AOT de `IClass<T>`, `IMethod`, `IField`, `IConstructor`, `IParameter`, `IRecordComponent`
- Métadonnées pré-calculées (pas de `java.lang.reflect` au runtime)
- `AOTReflectionProvider` implémentant `IReflectionProvider` avec support du système de priorité
- Le provider charge les descripteurs depuis un registre statique peuplé par le code généré
- Dépendances : `garganttua-commons`, `garganttua-aot-common`

**Bloqué par :** #6

---

### #8 — Implémenter garganttua-aot-annotation-scanner : scanner pré-indexé

- `AOTAnnotationScanner` implémentant `IAnnotationScanner`
- Charge les index pré-calculés au démarrage (zéro classpath scanning)
- Format d'index : fichiers dans `META-INF/garganttua/aot/` listant annotation → classes/méthodes
- Supporte `getClassesWithAnnotation`, `getMethodsWithAnnotation`, etc.
- S'intègre dans `CompositeReflection` via le système de priorité existant
- Dépendances : `garganttua-commons`, `garganttua-aot-common`

**Bloqué par :** #6

---

## Phase 3 — Renommage GraalVM + processeur AOT

### #3 — Renommer garganttua-native → garganttua-graalvm

- [ ] Renommer le répertoire `garganttua-native` → `garganttua-graalvm`
- [ ] Mettre à jour `artifactId` dans `pom.xml`
- [ ] Renommer le package `com.garganttua.core.nativve` → `com.garganttua.core.graalvm`
- [ ] Renommer les classes :
  - `NativeConfigurationBuilder` → `GraalVMConfigurationBuilder`
  - `NativeConfiguration` → `GraalVMConfiguration`
- [ ] Adapter le code pour consommer `@Reflected` et `@ReflectedBuilder`
- [ ] Mettre à jour toutes les références (imports, dépendances POM)
- [ ] Mettre à jour les interfaces dans commons : `INativeConfigurationBuilder` → `IGraalVMConfigurationBuilder`, etc.

**Bloqué par :** #1, #2

---

### #9 — Implémenter garganttua-aot-annotation-processor : génération de code

Processeur javac qui remplace `DirectBinderGenerator` et l'étend :

- Scanne `@Reflected` → génère :
  - `IClass<T>` AOT complets (métadonnées champs/méthodes/constructeurs)
  - `IConstructorBinder` directs (`new ClassName(...)` sans `Constructor.newInstance`)
  - `IMethodBinder` directs (appel direct sans `Method.invoke`)
  - `IFieldBinder` directs si pertinent
- Scanne `@Indexed` → génère les entrées pour `AOTAnnotationScanner`
- Scanne `@ReflectedBuilder` → génère les entrées pour les usages dynamiques déclarés
- Absorbe la logique existante de `DirectBinderGenerator` (binders pour `@Expression`, `@Prototype`, `@Singleton`, `@Inject`)
- Génère un fichier de registre pour l'auto-enregistrement au runtime
- Désactive `-proc:none` sur lui-même
- Dépendances : `garganttua-commons`, `garganttua-aot-common`

**Bloqué par :** #6, #7, #8

---

## Phase 4 — Nettoyage + finalisation des modules

### #4 — Renommer garganttua-native-image-maven-plugin → garganttua-graalvm-maven-plugin

- [ ] Renommer le répertoire
- [ ] Mettre à jour `artifactId`, package Java
- [ ] Renommer `NativeConfigMojo` → `GraalVMConfigMojo`
- [ ] Adapter pour utiliser les classes renommées de `garganttua-graalvm`
- [ ] Mettre à jour la dépendance vers `garganttua-graalvm`
- [ ] Mettre à jour les références dans le POM parent

**Bloqué par :** #3

---

### #5 — Nettoyer les interfaces native dans garganttua-commons

Le package `com.garganttua.core.nativve` contient des interfaces spécifiques GraalVM :

- [ ] Évaluer ce qui reste dans commons (consommé par plusieurs modules) vs ce qui est purement GraalVM
- [ ] Déplacer les interfaces GraalVM-only dans le module `garganttua-graalvm`
- [ ] Renommer/déplacer le reste dans `com.garganttua.core.graalvm` ou package approprié
- [ ] Supprimer le package `nativve` une fois la migration terminée

**Bloqué par :** #3

---

### #10 — Implémenter garganttua-aot-maven-plugin (si nécessaire)

Évaluer si un plugin Maven AOT est nécessaire au-delà du processeur javac :

- Agrégation des index AOT de multiples JARs (comme le shade plugin pour les annotation indexes)
- Post-traitement ou validation des descripteurs générés
- Orchestration avec le plugin GraalVM
- Si pas nécessaire immédiatement, documenter comme réservé pour usage futur

**Bloqué par :** #9

---

### #11 — Supprimer DirectBinderGenerator de garganttua-annotation-processor

- [ ] Supprimer `DirectBinderGenerator.java`
- [ ] Supprimer les registres : `DirectBinderRegistry`, `DirectConstructorBinderRegistry` de `garganttua-reflection`
- [ ] Supprimer les index files : `META-INF/garganttua/generated-binders`, `META-INF/garganttua/generated-constructor-binders`
- [ ] Mettre à jour les points d'intégration :
  - `ExpressionNodeFactory.bindNode()` → utiliser registre AOT
  - `AbstractConstructorBinderBuilder.doBuild()` → utiliser registre AOT
- [ ] Mettre à jour le shade plugin (`AppendingTransformer`) pour les nouveaux fichiers d'index AOT
- [ ] Vérifier que `garganttua-annotation-processor` ne contient plus que `IndexedAnnotationProcessor`

**Bloqué par :** #9

---

## Phase 5 — Intégration

### #12 — Intégrer AOT dans CompositeReflection et Bootstrap

- [ ] Mettre à jour `ReflectionBuilder` / `CompositeReflection` pour enregistrer :
  - `AOTReflectionProvider` (priorité haute)
  - `AOTAnnotationScanner` (priorité haute)
- [ ] Fallback : `RuntimeReflectionProvider` + `ReflectionsAnnotationScanner`
- [ ] Mettre à jour le Bootstrap pour détecter et charger les providers AOT automatiquement
- [ ] Valider le fonctionnement sans AOT (fallback pur runtime) et avec AOT (providers prioritaires)

**Bloqué par :** #7, #8, #11

---

## Phase 6 — Validation + documentation

### #13 — Tests : valider les deux pipelines (GraalVM + AOT)

- [ ] Tests unitaires pour `@Reflected` / `@ReflectedBuilder` (détection, attributs)
- [ ] Tests pipeline GraalVM : annotation → `reflect-config.json` (migration iso-fonctionnelle)
- [ ] Tests processeur AOT : annotation → `IClass<T>`, binders directs, index scanner
- [ ] Tests d'intégration : `CompositeReflection` avec AOT provider + runtime fallback
- [ ] Tests de régression : mêmes résultats que `DirectBinderGenerator`
- [ ] Tests du scanner AOT : `getClassesWithAnnotation` retourne les mêmes résultats qu'un scan runtime
- [ ] Build complet avec les deux pipelines actives

**Bloqué par :** #12

---

### #14 — Mettre à jour POM parent, reactor et documentation

- [ ] POM parent : renommer les modules dans `<modules>`, ajouter les modules AOT
- [ ] Dépendances inter-modules : `garganttua-native` → `garganttua-graalvm` partout
- [ ] Configurer le processeur AOT dans `maven-compiler-plugin` du POM parent
- [ ] Mettre à jour `CLAUDE.md` : architecture, module layers, dependency chains, build commands
- [ ] Mettre à jour les READMEs des modules concernés
- [ ] Mettre à jour `.claude/rules/` si nécessaire
- [ ] Mettre à jour CI/CD (`.github/workflows/`) si impacté
- [ ] Mettre à jour `scripts/run_all.py` pour la génération README

**Bloqué par :** #4, #5, #12, #15, #16

---

### #15 — Déplacer les modules GraalVM dans garganttua-bindings

GraalVM est un binding/intégration spécifique, pas un module core de premier niveau (même logique que Spring/Reflections) :

- [ ] Créer `garganttua-bindings/garganttua-graalvm/` (anciennement `garganttua-graalvm` à la racine)
- [ ] Créer `garganttua-bindings/garganttua-graalvm-maven-plugin/` (anciennement à la racine)
- [ ] Mettre à jour le POM parent de `garganttua-bindings` pour inclure ces modules
- [ ] Retirer ces modules du reactor racine
- [ ] Mettre à jour les dépendances inter-modules

**Bloqué par :** #4

---

### #16 — Supprimer la dépendance garganttua-native des modules core

Les modules de premier niveau du core ne doivent pas dépendre de garganttua-native/graalvm :

- [ ] Supprimer la dépendance `garganttua-native` (scope `provided`) de `garganttua-injection`
- [ ] Chercher et supprimer toute autre dépendance vers `garganttua-native` dans les modules core (expression, runtime, bootstrap, etc.)
- [ ] Supprimer les classes `@NativeConfigurationBuilder` des modules core :
  - `InjectionContextBuilder` (injection)
  - `RuntimeNativeConfigurationBuilder` (runtime)
  - `ExpressionNativeConfigurationBuilder` (expression)
- [ ] Ces modules utilisent désormais `@Reflected` et `@ReflectedBuilder` (dans commons) — le pipeline GraalVM dans bindings consomme ces annotations sans couplage

**Bloqué par :** #2, #5

---

### #17 — Rendre IClass et IReflection thread-safe

`IClass.setReflection()` est un point de mutation globale non thread-safe. Avec l'AOT qui ajoute des providers chargés dynamiquement, c'est critique :

- [ ] Sécuriser `IClass.setReflection()` (volatile, `AtomicReference`, ou synchronization)
- [ ] Vérifier que `CompositeReflection` est thread-safe (accès concurrent aux providers/scanners)
- [ ] Vérifier que les registres statiques (AOT providers) sont thread-safe au chargement
- [ ] Vérifier que les implémentations de `IClass<T>` (`RuntimeClass`, futures `AOTClass`) sont immutables ou thread-safe
- [ ] Utiliser des collections concurrentes (`ConcurrentHashMap`, etc.) là où nécessaire
- [ ] S'assurer que `ReflectionBuilder.build()` + `IClass.setReflection()` sont safe en contexte multi-thread

**Indépendant** — peut être traité à tout moment, mais idéalement avant #12 (intégration AOT)
