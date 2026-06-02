package com.garganttua.core.reflection.binders.dsl;

import com.garganttua.core.dsl.IAutomaticLinkedBuilder;
import com.garganttua.core.reflection.binders.IExecutableBinder;

/**
 * Base builder interface for constructing executable binders (methods/constructors).
 *
 * <p>
 * {@code IExecutableBinderBuilder} combines {@link IAutomaticLinkedBuilder} and
 * {@link IParametrableBuilder} to provide a complete fluent API for building
 * executable binders. It inherits automatic detection capabilities, hierarchical
 * navigation, and comprehensive parameter configuration methods.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Building a constructor binder
 * IExecutableBinder<UserService> constructor = ConstructorBinderBuilder
 *     .forClass(UserService.class)
 *     .withParam(UserRepository.class)
 *     .withParam(EmailService.class)
 *     .build();
 *
 * // Building a method binder with auto-detection
 * IExecutableBinder<String> method = MethodBinderBuilder
 *     .forClass(StringUtils.class)
 *     .method("concat")
 *     .autoDetect(true)  // Auto-detect parameter types
 *     .build();
 *
 * // Hierarchical builder navigation
 * IContextBuilder context = ContextBuilder
 *     .create()
 *     .bean(UserService.class)
 *         .constructor()
 *             .withParam(UserRepository.class)
 *             .withParam(EmailService.class)
 *             .up()  // Returns to bean builder
 *         .up()  // Returns to context builder
 *     .build();
 * }</pre>
 *
 * <h2>Combined Capabilities</h2>
 * <ul>
 *   <li><b>Parameter Configuration</b>: From {@link IParametrableBuilder} - comprehensive
 *       parameter specification methods</li>
 *   <li><b>Auto-Detection</b>: From {@link IAutomaticLinkedBuilder} - automatic parameter
 *       type and value detection</li>
 *   <li><b>Hierarchical Navigation</b>: From {@link IAutomaticLinkedBuilder} - navigate
 *       back to parent builders</li>
 * </ul>
 *
 * @param <ExecutionReturn> the return type of the executable element
 * @param <Builder> the concrete builder type for method chaining
 * @param <Link> the type of the parent builder for hierarchical navigation
 * @param <Built> the specific executable binder type being constructed
 * @since 2.0.0-ALPHA01
 * @see IAutomaticLinkedBuilder
 * @see IParametrableBuilder
 * @see IExecutableBinder
 */
public interface IExecutableBinderBuilder<ExecutionReturn, Builder extends IExecutableBinderBuilder<?, ?, ?, ?>, Link, Built extends IExecutableBinder<ExecutionReturn>>
        extends IAutomaticLinkedBuilder<Builder, Link, Built>, IParametrableBuilder<Builder, Built> { }
