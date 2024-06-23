package org.apache.flink.dynamic.impl.json.spec;

import lombok.Getter;
import org.apache.flink.cep.pattern.Quantifier;
import org.apache.flink.cep.pattern.Quantifier.ConsumingStrategy;
import org.apache.flink.cep.pattern.Quantifier.QuantifierProperty;
import org.apache.flink.cep.pattern.Quantifier.Times;
import org.apache.flink.cep.pattern.conditions.IterativeCondition;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * This class is to (de)serialize {@link Quantifier} in json format. It contains Times and
 * untilCondition as well, which are logically a part of a Quantifier.
 */
@Getter
public class QuantifierSpec {

    private final ConsumingStrategy consumingStrategy;
    private final EnumSet<QuantifierProperty> properties;
    private final @Nullable Times times;
    private final @Nullable ConditionSpec untilCondition;

    public QuantifierSpec(
            @JsonProperty("consumingStrategy") ConsumingStrategy consumingStrategy,
            @JsonProperty("properties") EnumSet<QuantifierProperty> properties,
            @Nullable @JsonProperty("times") Times times,
            @Nullable @JsonProperty("untilCondition") ConditionSpec untilCondition) {
        this.consumingStrategy = consumingStrategy;
        this.properties = properties;
        this.times = times;
        this.untilCondition = untilCondition;
    }

    public QuantifierSpec(Quantifier quantifier, Times times, IterativeCondition<?> untilCondition) {
        this.consumingStrategy = quantifier.getInnerConsumingStrategy();
        this.properties = EnumSet.noneOf(QuantifierProperty.class);
        for (QuantifierProperty property : QuantifierProperty.values()) {
            if (quantifier.hasProperty(property)) {
                this.properties.add(property);
            }
        }

        this.times =
                times == null
                        ? null
                        : Times.of(times.getFrom(), times.getTo(), times.getWindowSize().orElse(null));

        this.untilCondition =
                untilCondition == null ? null : new ClassConditionSpec(untilCondition);
    }
}