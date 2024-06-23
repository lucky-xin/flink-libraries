package org.apache.flink.dynamic.impl.json.spec;

import lombok.Getter;
import org.apache.flink.cep.pattern.conditions.IterativeCondition;
import org.apache.flink.cep.pattern.conditions.RichAndCondition;
import org.apache.flink.cep.pattern.conditions.RichCompositeIterativeCondition;
import org.apache.flink.cep.pattern.conditions.RichNotCondition;
import org.apache.flink.cep.pattern.conditions.RichOrCondition;
import org.apache.flink.cep.pattern.conditions.SubtypeCondition;
import org.apache.flink.dynamic.condition.AviatorCondition;
import org.apache.flink.dynamic.condition.CustomArgsCondition;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The util class to (de)serialize {@link IterativeCondition } of a specific class in json format.
 */
@Getter
public abstract class ConditionSpec {
    private final ConditionType type;

    ConditionSpec(@JsonProperty("type") ConditionType type) {
        this.type = type;
    }

    public static ConditionSpec fromCondition(IterativeCondition<?> condition) {
        if (condition instanceof SubtypeCondition) {
            return new SubTypeConditionSpec(condition);
        } else if (condition instanceof AviatorCondition) {
            return new AviatorConditionSpec(((AviatorCondition<?>) condition).getExpression());
        } else if (condition instanceof CustomArgsCondition) {
            return new CustomArgsConditionSpec((CustomArgsCondition<?>) condition);
        } else if (condition instanceof RichCompositeIterativeCondition) {
            IterativeCondition<?>[] nestedConditions = ((RichCompositeIterativeCondition<?>) condition).getNestedConditions();
            if (condition instanceof RichOrCondition) {
                List<ConditionSpec> specs = Stream.of(nestedConditions)
                        .map(ConditionSpec::fromCondition)
                        .collect(Collectors.toList());
                return new RichOrConditionSpec(specs);
            } else if (condition instanceof RichAndCondition) {
                List<ConditionSpec> specs = Stream.of(nestedConditions)
                        .map(ConditionSpec::fromCondition)
                        .collect(Collectors.toList());
                return new RichAndConditionSpec(specs);
            } else if (condition instanceof RichNotCondition) {
                List<ConditionSpec> specs = Stream.of(nestedConditions)
                        .map(ConditionSpec::fromCondition)
                        .collect(Collectors.toList());
                return new RichNotConditionSpec(specs);
            }
        }
        return new ClassConditionSpec(condition);
    }

    public abstract <T> IterativeCondition<T> toIterativeCondition(ClassLoader classLoader) throws Exception;
}
