package org.apache.flink.dynamic.condition;

import org.apache.flink.annotation.Internal;
import org.apache.flink.cep.pattern.conditions.SimpleCondition;

/**
 * Condition that accepts custom args in json.
 */
@Internal
public abstract class CustomArgsCondition<T> extends SimpleCondition<T> {

    private static final long serialVersionUID = 1L;

    /**
     * The args of the condition.
     */
    private final String[] args;

    private final String className;

    public CustomArgsCondition(String[] args, String className) {
        this.args = args;
        this.className = className;
    }

    public String[] getArgs() {
        return args;
    }

    public String getClassName() {
        return className;
    }
}
