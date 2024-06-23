package org.apache.flink.dynamic.impl.json.spec;

import lombok.Getter;
import org.apache.flink.cep.pattern.conditions.IterativeCondition;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The util class to (de)serialize {@link IterativeCondition } of a specific class in json format.
 */
@Getter
public class ClassConditionSpec extends ConditionSpec {
    private final String className;

    public ClassConditionSpec(@JsonProperty("className") String className) {
        super(ConditionType.CLASS);
        this.className = className;
    }

    public ClassConditionSpec(IterativeCondition<?> condition) {
        super(ConditionType.CLASS);
        this.className = condition.getClass().getCanonicalName();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> IterativeCondition<T> toIterativeCondition(ClassLoader classLoader) throws Exception {
        if (this.className.equals("null")) {
            throw new IllegalAccessException(
                    "It is not supported to save/load a local or anonymous class as a IterativeCondition for dynamic patterns.");
        }
        return (IterativeCondition<T>) classLoader.loadClass(this.getClassName()).newInstance();
    }
}