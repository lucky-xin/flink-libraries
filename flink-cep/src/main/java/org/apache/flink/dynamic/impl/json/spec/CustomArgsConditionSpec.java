package org.apache.flink.dynamic.impl.json.spec;

import lombok.Getter;
import org.apache.flink.cep.pattern.conditions.IterativeCondition;
import org.apache.flink.dynamic.condition.CustomArgsCondition;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The util class to (de)serialize {@link CustomArgsCondition } in json format.
 */
@Getter
public class CustomArgsConditionSpec extends ConditionSpec {

    /**
     * The filter expression of the condition.
     */
    private final String[] args;

    private final String className;

    public CustomArgsConditionSpec(CustomArgsCondition<?> condition) {
        super(ConditionType.CLASS);
        this.args = condition.getArgs();
        this.className = condition.getClassName();
    }

    public CustomArgsConditionSpec(
            @JsonProperty("args") String[] args, @JsonProperty("className") String className) {
        super(ConditionType.CLASS);
        this.args = args;
        this.className = className;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> IterativeCondition<T> toIterativeCondition(ClassLoader classLoader) throws Exception {
        return (IterativeCondition<T>)
                classLoader.loadClass(className)
                        .getConstructor(String[].class, String.class)
                        .newInstance(args, className);
    }
}
