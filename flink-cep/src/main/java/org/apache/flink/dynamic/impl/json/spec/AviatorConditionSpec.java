package org.apache.flink.dynamic.impl.json.spec;

import lombok.Getter;
import org.apache.flink.cep.pattern.conditions.IterativeCondition;
import org.apache.flink.dynamic.condition.AviatorCondition;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The util class to (de)serialize {@link AviatorCondition } in json format.
 */
@Getter
public class AviatorConditionSpec extends ConditionSpec {
    /**
     * The filter expression of the condition.
     */
    private final String expression;

    public AviatorConditionSpec(@JsonProperty("expression") String expression) {
        super(ConditionType.AVIATOR);
        this.expression = expression;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> IterativeCondition<T> toIterativeCondition(ClassLoader classLoader) throws Exception {
        return (IterativeCondition<T>)
                classLoader.loadClass(AviatorCondition.class.getCanonicalName())
                        .getConstructor(String.class)
                        .newInstance(expression);
    }
}
