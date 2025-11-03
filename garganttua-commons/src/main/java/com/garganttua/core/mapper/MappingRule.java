package com.garganttua.core.mapper;

import java.util.Objects;

import com.garganttua.core.reflection.ObjectAddress;

public record MappingRule (
		ObjectAddress sourceFieldAddress,
		ObjectAddress destinationFieldAddress,
		Class<?> destinationClass,
		ObjectAddress fromSourceMethodAddress,
		ObjectAddress toSourceMethodAddress
	){

    @Override
    public String toString() {
        return "APIMappingRule{" +
                "sourceFieldAddress='" + sourceFieldAddress + '\'' +
                ", destinationFieldAddress='" + destinationFieldAddress + '\'' +
                ", destinationClass=" + destinationClass +
                ", fromSourceMethod=" + fromSourceMethodAddress +
                ", toSourceMethod=" + toSourceMethodAddress +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MappingRule that = (MappingRule) o;
        return Objects.equals(sourceFieldAddress, that.sourceFieldAddress) &&
                Objects.equals(destinationFieldAddress, that.destinationFieldAddress) &&
                Objects.equals(destinationClass, that.destinationClass) &&
                Objects.equals(fromSourceMethodAddress, that.fromSourceMethodAddress) &&
                Objects.equals(toSourceMethodAddress, that.toSourceMethodAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceFieldAddress, destinationFieldAddress, destinationClass, fromSourceMethodAddress, toSourceMethodAddress);
    }

}
