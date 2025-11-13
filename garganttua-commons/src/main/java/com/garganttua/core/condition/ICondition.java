package com.garganttua.core.condition;

@FunctionalInterface
public interface ICondition {

    boolean evaluate() throws ConditionException;

}
