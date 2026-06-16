package com.garganttua.example.aot;

import com.garganttua.core.reflection.annotations.Reflected;

/**
 * A plain business value object — the kind of class a real consumer app owns.
 *
 * <p>The single {@link Reflected @Reflected} annotation is the whole contract: at
 * compile time the {@code garganttua-aot-annotation-processor} emits a rich
 * {@code AOTClass_Product} descriptor (fields, methods, constructors) plus an
 * {@code IAOTSelfRegistering} service entry that force-loads it into the
 * {@code AOTRegistry} at cold start. No runtime classpath scan, no bytecode magic.
 */
@Reflected
public record Product(String name, int quantity, double unitPrice) {

    public double total() {
        return quantity * unitPrice;
    }
}
