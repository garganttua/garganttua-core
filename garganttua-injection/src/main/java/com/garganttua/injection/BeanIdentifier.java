package com.garganttua.injection;

import java.util.Objects;
import java.util.Optional;

import lombok.Getter;

public class BeanIdentifier {

    @Getter
    public Optional<String> scope;

    @Getter
    public String name;

    public BeanIdentifier(String identifier) {
        Objects.requireNonNull(identifier, "Bean identifier cannot be null");

        String[] parts = identifier.split(":");

        this.scope = parts.length == 2 ? Optional.of(parts[0]) : Optional.empty();
        this.name = parts.length == 2 ? parts[1] : parts[0];
    }

}
