package com.garganttua.reflection.beans;

import org.javatuples.Pair;

import com.garganttua.reflection.GGReflectionException;

public class GGBeanRefValidator {

	public static Pair<String, String> validate(String input) throws GGReflectionException {
		if (input == null || !input.contains(":")) {
			throw new GGReflectionException("The bean reference ["+input+"] must contain a colon ':' character.");
		}

		String[] parts = input.split(":", 2);
		return new Pair<>(parts[0], parts[1]);
	}
}
