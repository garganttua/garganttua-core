package com.garganttua.objects.mapper.rules;

import java.util.Objects;

import com.garganttua.reflection.GGObjectAddress;

public record GGMappingRule (
		GGObjectAddress sourceFieldAddress,
		GGObjectAddress destinationFieldAddress,
		Class<?> destinationClass,
		GGObjectAddress fromSourceMethodAddress,
		GGObjectAddress toSourceMethodAddress
	){

    @Override
    public String toString() {
        return "GGAPIMappingRule{" +
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
        GGMappingRule that = (GGMappingRule) o;
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
