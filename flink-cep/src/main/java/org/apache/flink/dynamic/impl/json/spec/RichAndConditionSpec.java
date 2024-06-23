package org.apache.flink.dynamic.impl.json.spec;

import org.apache.flink.cep.pattern.conditions.IterativeCondition;
import org.apache.flink.cep.pattern.conditions.RichAndCondition;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * This class is to (de)serialize {@link RichAndCondition} in json format.
 */
public class RichAndConditionSpec extends RichCompositeConditionSpec {

    public RichAndConditionSpec(
            @JsonProperty("nestedConditions") List<ConditionSpec> nestedConditions) {
        super(RichAndCondition.class.getCanonicalName(), nestedConditions);
    }

    @Override
    public <T> IterativeCondition<T> toIterativeCondition(ClassLoader classLoader) throws Exception {
        return new RichAndCondition<>(
                this.getNestedConditions().get(0).toIterativeCondition(classLoader),
                this.getNestedConditions().get(1).toIterativeCondition(classLoader));
    }
}
