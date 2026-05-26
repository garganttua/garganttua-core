package com.garganttua.core.reflection.utils;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Objects;

public class WildcardTypeImpl implements WildcardType {
    private static final IDiagnostic log = Diagnostics.of(WildcardTypeImpl.class);

    private final Type[] upperBounds;
    private final Type[] lowerBounds;

    public WildcardTypeImpl(Type[] upperBounds, Type[] lowerBounds) {
        log.trace("Creating WildcardTypeImpl: upperBounds={}, lowerBounds={}", upperBounds, lowerBounds);
        this.upperBounds = upperBounds != null ? upperBounds.clone() : new Type[] { Object.class };
        this.lowerBounds = lowerBounds != null ? lowerBounds.clone() : new Type[0];
        log.debug("Created WildcardType: {}", this);
    }

    public static WildcardType extends_(Type upperBound) {
        log.debug("Creating extends wildcard with upper bound: {}", upperBound);
        return new WildcardTypeImpl(new Type[]{ upperBound }, new Type[0]);
    }

    public static WildcardType super_(Type lowerBound) {
        log.debug("Creating super wildcard with lower bound: {}", lowerBound);
        return new WildcardTypeImpl(new Type[]{ Object.class }, new Type[]{ lowerBound });
    }

    public static WildcardType unbounded() {
        log.debug("Creating unbounded wildcard");
        return new WildcardTypeImpl(new Type[]{ Object.class }, new Type[0]);
    }

    @Override
    public Type[] getUpperBounds() {
        return upperBounds.clone();
    }

    @Override
    public Type[] getLowerBounds() {
        return lowerBounds.clone();
    }

    @Override
    public String toString() {
        if (lowerBounds.length > 0) {
            return "? super " + lowerBounds[0].getTypeName();
        }
        if (upperBounds.length == 1 && upperBounds[0] == Object.class) {
            return "?";
        }
        return "? extends " + upperBounds[0].getTypeName();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof WildcardType)) return false;
        WildcardType other = (WildcardType) o;
        return Arrays.equals(getUpperBounds(), other.getUpperBounds())
                && Arrays.equals(getLowerBounds(), other.getLowerBounds());
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(upperBounds), Arrays.hashCode(lowerBounds));
    }

}
