package org.apache.flink.dynamic.impl.json.spec;

import lombok.Getter;
import org.apache.flink.cep.pattern.conditions.RichCompositeIterativeCondition;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

/** This class is to (de)serialize {@link RichCompositeIterativeCondition} in json format. */
@Getter
public class RichCompositeConditionSpec extends ClassConditionSpec {
    private final List<ConditionSpec> nestedConditions;

    public RichCompositeConditionSpec(
            @JsonProperty("className") String className,
            @JsonProperty("nestedConditions") List<ConditionSpec> nestedConditions) {
        super(className);
        this.nestedConditions = Collections.unmodifiableList(nestedConditions);
    }
}
