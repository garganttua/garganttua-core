/**
 * Domain-Specific Language (DSL) builder framework for creating fluent, hierarchical APIs.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides the foundation for creating type-safe, fluent builder APIs throughout
 * the Garganttua ecosystem. It defines builder patterns that enable expressive, chainable
 * method calls for configuring complex objects.
 * </p>
 *
 * <h2>Core Interfaces</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.dsl.IBuilder} - Base builder interface for constructing objects</li>
 *   <li>{@link com.garganttua.core.dsl.ILinkedBuilder} - Builder supporting parent-child relationships</li>
 *   <li>{@link com.garganttua.core.dsl.IAutomaticBuilder} - Self-building pattern with build() method</li>
 *   <li>{@link com.garganttua.core.dsl.IAutomaticLinkedBuilder} - Linked builder with automatic building</li>
 * </ul>
 *
 * <h2>Builder Patterns</h2>
 *
 * <h3>Simple Builder</h3>
 * <pre>{@code
 * public class UserBuilder implements IBuilder<User> {
 *     private String name;
 *     private int age;
 *
 *     public UserBuilder name(String name) {
 *         this.name = name;
 *         return this;
 *     }
 *
 *     public UserBuilder age(int age) {
 *         this.age = age;
 *         return this;
 *     }
 *
 *     @Override
 *     public User build() {
 *         return new User(name, age);
 *     }
 * }
 * }</pre>
 *
 * <h3>Linked Builder (Hierarchical)</h3>
 * <pre>{@code
 * public class CompanyBuilder implements IBuilder<Company> {
 *     private List<Department> departments = new ArrayList<>();
 *
 *     public DepartmentBuilder department(String name) {
 *         return new DepartmentBuilder(this, name);
 *     }
 *
 *     void addDepartment(Department dept) {
 *         departments.add(dept);
 *     }
 *
 *     @Override
 *     public Company build() {
 *         return new Company(departments);
 *     }
 * }
 *
 * public class DepartmentBuilder implements ILinkedBuilder<Department, CompanyBuilder> {
 *     private final CompanyBuilder parent;
 *     private final String name;
 *     private List<Employee> employees = new ArrayList<>();
 *
 *     public DepartmentBuilder(CompanyBuilder parent, String name) {
 *         this.parent = parent;
 *         this.name = name;
 *     }
 *
 *     public DepartmentBuilder employee(String employeeName) {
 *         employees.add(new Employee(employeeName));
 *         return this;
 *     }
 *
 *     @Override
 *     public CompanyBuilder end() {
 *         parent.addDepartment(build());
 *         return parent;
 *     }
 *
 *     @Override
 *     public Department build() {
 *         return new Department(name, employees);
 *     }
 * }
 *
 * // Usage
 * Company company = new CompanyBuilder()
 *     .department("Engineering")
 *         .employee("Alice")
 *         .employee("Bob")
 *         .end()
 *     .department("Sales")
 *         .employee("Charlie")
 *         .end()
 *     .build();
 * }</pre>
 *
 * <h2>Design Benefits</h2>
 * <ul>
 *   <li><b>Type safety</b> - Compile-time checking of configuration</li>
 *   <li><b>Readability</b> - Self-documenting, fluent method chains</li>
 *   <li><b>Hierarchy</b> - Natural nesting via linked builders</li>
 *   <li><b>Immutability</b> - Build immutable objects safely</li>
 *   <li><b>Consistency</b> - Uniform API style across modules</li>
 * </ul>
 *
 * <h2>Used By</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.injection.context.dsl} - DI context configuration</li>
 *   <li>{@link com.garganttua.core.runtime.dsl} - Runtime workflow definitions</li>
 *   <li>{@link com.garganttua.core.condition.dsl} - Condition expression building</li>
 *   <li>{@link com.garganttua.core.supply.dsl} - Object supplier configuration</li>
 *   <li>{@link com.garganttua.core.reflection.binders.dsl} - Reflection binder configuration</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.dsl.IBuilder
 * @see com.garganttua.core.dsl.ILinkedBuilder
 */
package com.garganttua.core.dsl;
