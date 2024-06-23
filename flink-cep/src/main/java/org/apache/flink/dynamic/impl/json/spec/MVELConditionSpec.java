package org.apache.flink.dynamic.impl.json.spec;

import lombok.Getter;
import org.apache.flink.cep.pattern.conditions.IterativeCondition;
import org.apache.flink.dynamic.condition.MVELCondition;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The util class to (de)serialize {@link MVELConditionSpec } in json format.
 */
@Getter
public class MVELConditionSpec extends ConditionSpec {
    /**
     * The filter expression of the condition.
     */
    private final String expression;

    public MVELConditionSpec(@JsonProperty("expression") String expression) {
        super(ConditionType.MVEL);
        this.expression = expression;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> IterativeCondition<T> toIterativeCondition(ClassLoader classLoader) throws Exception {
        return (IterativeCondition<T>)
                classLoader.loadClass(MVELCondition.class.getCanonicalName())
                        .getConstructor(String.class)
                        .newInstance(expression);
    }
}
