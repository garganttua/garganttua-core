/**
 * Object supplier and lazy provisioning framework for deferred object creation.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides contracts for lazy object provisioning and contextual object supply.
 * Suppliers enable deferred creation of objects until they are needed, supporting dependency
 * injection, factory patterns, and object pooling scenarios.
 * </p>
 *
 * <h2>Core Interfaces</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.supply.ISupplier} - Basic object supplier</li>
 *   <li>{@link com.garganttua.core.supply.IContextualSupplier} - Context-aware supplier</li>
 * </ul>
 *
 * <h2>Supplier Types</h2>
 * <ul>
 *   <li><b>Fixed Supplier</b> - Returns a pre-defined object instance</li>
 *   <li><b>Factory Supplier</b> - Creates new instances on each invocation</li>
 *   <li><b>Lazy Supplier</b> - Creates instance only on first access</li>
 *   <li><b>Contextual Supplier</b> - Creates instances based on execution context</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Simple Non-Contextual Supplier</h3>
 * <pre>{@code
 * // Implement ISupplier for simple object provisioning
 * class SimpleStringSupplier implements ISupplier<String> {
 *     private final String value;
 *
 *     public SimpleStringSupplier(String value) {
 *         this.value = value;
 *     }
 *
 *     @Override
 *     public Optional<String> supply() throws SupplyException {
 *         return Optional.ofNullable(value);
 *     }
 *
 *     @Override
 *     public Type getSuppliedType() {
 *         return String.class;
 *     }
 * }
 *
 * // Use with contextualSupply utility
 * ISupplier<String> supplier = new SimpleStringSupplier("Hello World");
 * String result = Supplier.contextualSupply(supplier);
 * // result: "Hello World"
 * }</pre>
 *
 * <h3>Contextual Supplier with Typed Context</h3>
 * <pre>{@code
 * // Implement IContextualSupplier for context-aware provisioning
 * class ContextualStringSupplier implements IContextualSupplier<String, String> {
 *     private final String prefix;
 *
 *     public ContextualStringSupplier(String prefix) {
 *         this.prefix = prefix;
 *     }
 *
 *     @Override
 *     public Optional<String> supply(String context, Object... otherContexts)
 *             throws SupplyException {
 *         return Optional.of(prefix + context);
 *     }
 *
 *     @Override
 *     public Class<String> getOwnerContextType() {
 *         return String.class;
 *     }
 *
 *     @Override
 *     public Type getSuppliedType() {
 *         return String.class;
 *     }
 * }
 *
 * // Supply with matching context
 * IContextualSupplier<String, String> supplier = new ContextualStringSupplier("Hello, ");
 * String result = Supplier.contextualSupply(supplier, "World");
 * // result: "Hello, World"
 * }</pre>
 *
 * <h3>Multiple Context Matching</h3>
 * <pre>{@code
 * // Contextual supplier that requires Integer context
 * class ContextualIntegerSupplier implements IContextualSupplier<Integer, Integer> {
 *     private final int multiplier;
 *
 *     public ContextualIntegerSupplier(int multiplier) {
 *         this.multiplier = multiplier;
 *     }
 *
 *     @Override
 *     public Optional<Integer> supply(Integer context, Object... otherContexts)
 *             throws SupplyException {
 *         return Optional.of(context * multiplier);
 *     }
 *
 *     @Override
 *     public Class<Integer> getOwnerContextType() {
 *         return Integer.class;
 *     }
 *
 *     @Override
 *     public Type getSuppliedType() {
 *         return Integer.class;
 *     }
 * }
 *
 * // Pass multiple contexts - framework finds the matching Integer context
 * IContextualSupplier<Integer, Integer> supplier = new ContextualIntegerSupplier(10);
 * Integer result = Supplier.contextualSupply(supplier, "ignored", 5, "also ignored");
 * // result: 50 (5 * 10, using the Integer context)
 * }</pre>
 *
 * <h3>Recursive Supply - Nested Suppliers</h3>
 * <pre>{@code
 * // Supplier that returns another supplier
 * class NestedSupplier implements ISupplier<ISupplier<String>> {
 *     private final ISupplier<String> innerSupplier;
 *
 *     public NestedSupplier(ISupplier<String> innerSupplier) {
 *         this.innerSupplier = innerSupplier;
 *     }
 *
 *     @Override
 *     public Optional<ISupplier<String>> supply() throws SupplyException {
 *         return Optional.ofNullable(innerSupplier);
 *     }
 *
 *     @Override
 *     public Type getSuppliedType() {
 *         return ISupplier.class;
 *     }
 * }
 *
 * // contextualRecursiveSupply automatically resolves nested suppliers
 * ISupplier<String> innerSupplier = new SimpleStringSupplier("Nested value");
 * ISupplier<ISupplier<String>> outerSupplier = new NestedSupplier(innerSupplier);
 * String result = (String) Supplier.contextualRecursiveSupply(outerSupplier);
 * // result: "Nested value" (resolved through 2 levels)
 * }</pre>
 *
 * <h3>Deep Nesting Resolution</h3>
 * <pre>{@code
 * // Create deeply nested supplier (3 levels)
 * class DeeplyNestedSupplier implements ISupplier<ISupplier<ISupplier<String>>> {
 *     private final String finalValue;
 *
 *     public DeeplyNestedSupplier(String finalValue) {
 *         this.finalValue = finalValue;
 *     }
 *
 *     @Override
 *     public Optional<ISupplier<ISupplier<String>>> supply() throws SupplyException {
 *         return Optional.of(
 *             new NestedSupplier(
 *                 new SimpleStringSupplier(finalValue)
 *             )
 *         );
 *     }
 *
 *     @Override
 *     public Type getSuppliedType() {
 *         return ISupplier.class;
 *     }
 * }
 *
 * // Automatically resolves all 3 levels to the final String
 * ISupplier<ISupplier<ISupplier<String>>> deepSupplier = new DeeplyNestedSupplier("Deep value");
 * String result = (String) Supplier.contextualRecursiveSupply(deepSupplier);
 * // result: "Deep value" (resolved through 3 levels)
 * }</pre>
 *
 * <h3>Void Context Supplier</h3>
 * <pre>{@code
 * // Contextual supplier that doesn't require specific context
 * class VoidContextSupplier implements IContextualSupplier<String, Void> {
 *     @Override
 *     public Optional<String> supply(Void context, Object... otherContexts)
 *             throws SupplyException {
 *         return Optional.of("No context needed");
 *     }
 *
 *     @Override
 *     public Class<Void> getOwnerContextType() {
 *         return Void.class;
 *     }
 *
 *     @Override
 *     public Type getSuppliedType() {
 *         return String.class;
 *     }
 * }
 *
 * // Can be called without any context
 * IContextualSupplier<String, Void> supplier = new VoidContextSupplier();
 * String result = Supplier.contextualSupply(supplier);
 * // result: "No context needed"
 * }</pre>
 *
 * <h3>Subtype Context Matching</h3>
 * <pre>{@code
 * // Parent context type
 * class ParentContext {
 *     String value = "parent";
 * }
 *
 * // Child context type extends parent
 * class ChildContext extends ParentContext {
 *     String childValue = "child";
 * }
 *
 * // Supplier accepting parent context
 * IContextualSupplier<String, ParentContext> supplier =
 *     new IContextualSupplier<String, ParentContext>() {
 *         @Override
 *         public Optional<String> supply(ParentContext context, Object... otherContexts)
 *                 throws SupplyException {
 *             return Optional.of("Received: " + context.value);
 *         }
 *
 *         @Override
 *         public Class<ParentContext> getOwnerContextType() {
 *             return ParentContext.class;
 *         }
 *
 *         @Override
 *         public Type getSuppliedType() {
 *             return String.class;
 *         }
 *     };
 *
 * // Child context matches parent context type via isAssignableFrom
 * ChildContext childContext = new ChildContext();
 * String result = Supplier.contextualSupply(supplier, childContext);
 * // result: "Received: parent"
 * }</pre>
 *
 * <h2>Benefits</h2>
 * <ul>
 *   <li><b>Lazy loading</b> - Defer expensive object creation</li>
 *   <li><b>Decoupling</b> - Separate object creation from usage</li>
 *   <li><b>Testability</b> - Easy mocking and stubbing</li>
 *   <li><b>Flexibility</b> - Switch implementations at runtime</li>
 * </ul>
 *
 * <h2>Related Packages</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.supply.dsl} - Fluent builder APIs for supplier creation</li>
 *   <li>{@link com.garganttua.core.injection} - DI integration</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.supply.ISupplier
 * @see com.garganttua.core.supply.IContextualSupplier
 */
package com.garganttua.core.supply;
