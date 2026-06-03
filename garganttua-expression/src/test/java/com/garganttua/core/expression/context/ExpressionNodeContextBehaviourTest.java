package com.garganttua.core.expression.context;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.expression.ExpressionNode;
import com.garganttua.core.expression.IExpressionNode;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.ISupplier;

/**
 * Behaviour tests for {@link ExpressionNodeContext}: parameter exposure, inferred parameter types
 * (node vs raw value), contextual-build detection, and the {@code matches(...)} assignment check
 * including the {@code ISupplier} (lazy) and {@code Object} (dynamic) wildcard rules.
 */
public class ExpressionNodeContextBehaviourTest {

    @BeforeEach
    void setUp() {
        IReflection reflection = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider(), 1)
                .build();
        IClass.setReflection(reflection);
    }

    private <V> IExpressionNode<V, ISupplier<V>> nodeOfType(IClass<V> type) {
        return new ExpressionNode<>("n",
                params -> new ISupplier<V>() {
                    @Override
                    public Optional<V> supply() {
                        return Optional.empty();
                    }

                    @Override
                    public Type getSuppliedType() {
                        return type.getType();
                    }

                    @Override
                    public IClass<V> getSuppliedClass() {
                        return type;
                    }
                }, type);
    }

    @Test
    public void constructor_nullParameters_throwsNPE() {
        assertThrows(NullPointerException.class, () -> new ExpressionNodeContext(null));
    }

    @Test
    public void parameters_returnsTheSuppliedList() {
        List<Object> params = List.of("a", Integer.valueOf(1));
        ExpressionNodeContext ctx = new ExpressionNodeContext(params);
        assertEquals(params, ctx.parameters());
    }

    @Test
    public void parameterTypes_rawValuesUseRuntimeClass() {
        ExpressionNodeContext ctx = new ExpressionNodeContext(List.of("a", Integer.valueOf(1)));
        IClass<?>[] types = ctx.parameterTypes();
        assertArrayEquals(new IClass<?>[] { IClass.getClass(String.class), IClass.getClass(Integer.class) }, types);
    }

    @Test
    public void parameterTypes_nodeUsesFinalSuppliedClass() {
        ExpressionNodeContext ctx = new ExpressionNodeContext(List.of(nodeOfType(IClass.getClass(Double.class))));
        IClass<?>[] types = ctx.parameterTypes();
        assertEquals(1, types.length);
        assertEquals(IClass.getClass(Double.class), types[0]);
    }

    @Test
    public void buildContextual_falseForRawValues() {
        ExpressionNodeContext ctx = new ExpressionNodeContext(List.of("a", Integer.valueOf(1)));
        assertFalse(ctx.buildContextual());
    }

    @Test
    public void buildContextual_trueWhenAnyParamIsContextualSupplier() {
        IContextualSupplier<String, IExpressionContext> contextual = new IContextualSupplier<String, IExpressionContext>() {
            @Override
            public Optional<String> supply(IExpressionContext c, Object... o) {
                return Optional.of("x");
            }

            @Override
            public IClass<IExpressionContext> getOwnerContextType() {
                return IClass.getClass(IExpressionContext.class);
            }

            @Override
            public Type getSuppliedType() {
                return String.class;
            }

            @Override
            public IClass<String> getSuppliedClass() {
                return IClass.getClass(String.class);
            }
        };
        ExpressionNodeContext ctx = new ExpressionNodeContext(List.of("plain", contextual));
        assertTrue(ctx.buildContextual());
    }

    // ---- matches ----

    @Test
    public void matches_arityMismatch_false() {
        ExpressionNodeContext ctx = new ExpressionNodeContext(List.of("a"));
        assertFalse(ctx.matches(new IClass<?>[] { IClass.getClass(String.class), IClass.getClass(String.class) }));
    }

    @Test
    public void matches_assignableRawValue_true() {
        ExpressionNodeContext ctx = new ExpressionNodeContext(List.of("a"));
        assertTrue(ctx.matches(new IClass<?>[] { IClass.getClass(String.class) }));
        // String is assignable to CharSequence / Object
        assertTrue(ctx.matches(new IClass<?>[] { IClass.getClass(CharSequence.class) }));
        assertTrue(ctx.matches(new IClass<?>[] { IClass.getClass(Object.class) }));
    }

    @Test
    public void matches_nonAssignableRawValue_false() {
        ExpressionNodeContext ctx = new ExpressionNodeContext(List.of("a"));
        assertFalse(ctx.matches(new IClass<?>[] { IClass.getClass(Integer.class) }));
    }

    @Test
    public void matches_nodeWithObjectType_acceptsAnyTarget() {
        // a node whose final supplied class is Object is a dynamic value (e.g. variable reference)
        ExpressionNodeContext ctx = new ExpressionNodeContext(List.of(nodeOfType(IClass.getClass(Object.class))));
        assertTrue(ctx.matches(new IClass<?>[] { IClass.getClass(String.class) }));
        assertTrue(ctx.matches(new IClass<?>[] { IClass.getClass(Integer.class) }));
    }

    @Test
    public void matches_nodeNonAssignable_false() {
        ExpressionNodeContext ctx = new ExpressionNodeContext(List.of(nodeOfType(IClass.getClass(String.class))));
        assertFalse(ctx.matches(new IClass<?>[] { IClass.getClass(Integer.class) }));
    }

    @Test
    public void matches_lazyISupplierParam_acceptsAnyArgument() {
        // factory expects ISupplier (lazy) -> any argument type is accepted
        ExpressionNodeContext ctxRaw = new ExpressionNodeContext(List.of(Integer.valueOf(5)));
        assertTrue(ctxRaw.matches(new IClass<?>[] { IClass.getClass(ISupplier.class) }));

        ExpressionNodeContext ctxNode = new ExpressionNodeContext(List.of(nodeOfType(IClass.getClass(String.class))));
        assertTrue(ctxNode.matches(new IClass<?>[] { IClass.getClass(ISupplier.class) }));
    }
}
