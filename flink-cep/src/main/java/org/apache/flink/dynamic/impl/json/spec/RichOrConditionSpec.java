package org.apache.flink.dynamic.impl.json.spec;

import org.apache.flink.cep.pattern.conditions.IterativeCondition;
import org.apache.flink.cep.pattern.conditions.RichOrCondition;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * This class is to (de)serialize {@link RichOrCondition} in json format.
 */
public class RichOrConditionSpec extends RichCompositeConditionSpec {
    public RichOrConditionSpec(
            @JsonProperty("nestedConditions") List<ConditionSpec> nestedConditions) {
        super(RichOrCondition.class.getCanonicalName(), nestedConditions);
    }

    @Override
    public <T> IterativeCondition<T> toIterativeCondition(ClassLoader classLoader) throws Exception {
        return new RichOrCondition<>(
                this.getNestedConditions().get(0).toIterativeCondition(classLoader),
                this.getNestedConditions().get(1).toIterativeCondition(classLoader));
    }
}