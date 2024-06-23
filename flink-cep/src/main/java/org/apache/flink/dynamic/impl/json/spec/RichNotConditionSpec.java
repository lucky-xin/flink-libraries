package org.apache.flink.dynamic.impl.json.spec;

import org.apache.flink.cep.pattern.conditions.IterativeCondition;
import org.apache.flink.cep.pattern.conditions.RichNotCondition;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * This class is to (de)serialize {@link RichNotCondition} in json format.
 */
public class RichNotConditionSpec extends RichCompositeConditionSpec {
    public RichNotConditionSpec(
            @JsonProperty("nestedConditions") List<ConditionSpec> nestedConditions) {
        super(RichNotCondition.class.getCanonicalName(), nestedConditions);
    }

    @Override
    public <T> IterativeCondition<T> toIterativeCondition(ClassLoader classLoader) throws Exception {
        return new RichNotCondition<>(
                this.getNestedConditions().get(0).toIterativeCondition(classLoader));
    }
}
