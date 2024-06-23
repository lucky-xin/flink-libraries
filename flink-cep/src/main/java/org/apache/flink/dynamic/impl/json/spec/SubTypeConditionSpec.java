package org.apache.flink.dynamic.impl.json.spec;

import lombok.Getter;
import org.apache.flink.cep.pattern.conditions.IterativeCondition;
import org.apache.flink.cep.pattern.conditions.SubtypeCondition;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class is to (de)serialize {@link SubtypeCondition} in json format.
 */
@Getter
public class SubTypeConditionSpec extends ClassConditionSpec {

    private String subClassName;
    private IterativeCondition<?> condition;

    public SubTypeConditionSpec(
            @JsonProperty("className") String className,
            @JsonProperty("subClassName") String subClassName) {
        super(className);
        this.subClassName = subClassName;
    }

    public SubTypeConditionSpec(IterativeCondition<?> condition) {
        super(condition);
        this.condition = condition;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> IterativeCondition<T> toIterativeCondition(ClassLoader classLoader) throws Exception {
        if (condition != null) {
            return (IterativeCondition<T>) condition;
        }
        return (IterativeCondition<T>) new SubtypeCondition<>(classLoader.loadClass(this.getSubClassName()));
    }

}
