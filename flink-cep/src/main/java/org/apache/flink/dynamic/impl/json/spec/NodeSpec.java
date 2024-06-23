package org.apache.flink.dynamic.impl.json.spec;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.apache.flink.cep.nfa.aftermatch.AfterMatchSkipStrategy;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.cep.pattern.Quantifier.ConsumingStrategy;
import org.apache.flink.cep.pattern.Quantifier.QuantifierProperty;
import org.apache.flink.cep.pattern.Quantifier.Times;
import org.apache.flink.cep.pattern.conditions.IterativeCondition;
import org.apache.flink.cep.pattern.conditions.RichOrCondition;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Node is used to describe a Pattern and contains all necessary fields of a Pattern. This class
 * is to (de)serialize Nodes in json format.
 */
@Getter
@SuperBuilder
public class NodeSpec {
    private final String name;
    private final QuantifierSpec quantifier;
    private final ConditionSpec condition;

    private final PatternNodeType type;

    public NodeSpec(String name, QuantifierSpec quantifier, ConditionSpec condition) {
        this(name, quantifier, condition, PatternNodeType.ATOMIC);
    }

    public NodeSpec(
            @JsonProperty("name") String name,
            @JsonProperty("quantifier") QuantifierSpec quantifier,
            @JsonProperty("condition") ConditionSpec condition,
            @JsonProperty("type") PatternNodeType type) {
        this.name = name;
        this.quantifier = quantifier;
        this.condition = condition;
        this.type = type;
    }

    /**
     * Build NodeSpec from given Pattern.
     */
    public static NodeSpec fromPattern(Pattern<?, ?> pattern) {
        QuantifierSpec quantifier =
                new QuantifierSpec(
                        pattern.getQuantifier(), pattern.getTimes(), pattern.getUntilCondition());
        return NodeSpec.builder()
                .name(pattern.getName())
                .quantifier(quantifier)
                .condition(ConditionSpec.fromCondition(pattern.getCondition()))
                .build();
    }

    /**
     * Converts the {@link GraphSpec} to the {@link Pattern}.
     *
     * @param classLoader The {@link ClassLoader} of the {@link Pattern}.
     * @return The converted {@link Pattern}.
     * @throws Exception Exceptions thrown while deserialization of the Pattern.
     */
    @SuppressWarnings("all")
    public <T, F extends T> Pattern<T, F> toPattern(
            final Pattern<T, F> previous,
            final AfterMatchSkipStrategy afterMatchSkipStrategy,
            final ConsumingStrategy consumingStrategy,
            final ClassLoader classLoader)
            throws Exception {
        // Build pattern
        if (this instanceof GraphSpec) {
            // TODO: should log if AfterMatchSkipStrategy of subgraph diff from the larger graph
            return ((GraphSpec) this).toPattern(classLoader);
        }
        Pattern<T, F> pattern =
                new NodePattern<>(this.getName(), previous, consumingStrategy, afterMatchSkipStrategy);
        final ConditionSpec conditionSpec = this.getCondition();
        if (conditionSpec != null) {
            IterativeCondition iterativeCondition = conditionSpec.toIterativeCondition(classLoader);
            if (iterativeCondition instanceof RichOrCondition) {
                pattern.or(iterativeCondition);
            } else {
                pattern.where(iterativeCondition);
            }
        }

        // Process quantifier's properties
        for (QuantifierProperty property : this.getQuantifier().getProperties()) {
            if (property.equals(QuantifierProperty.OPTIONAL)) {
                pattern.optional();
            } else if (property.equals(QuantifierProperty.GREEDY)) {
                pattern.greedy();
            } else if (property.equals(QuantifierProperty.LOOPING)) {
                final Times times = this.getQuantifier().getTimes();
                if (times != null && times.getWindowSize().isPresent()) {
                    pattern.timesOrMore(times.getFrom(), times.getWindowSize().get());
                }
            } else if (property.equals(QuantifierProperty.TIMES)) {
                final Times times = this.getQuantifier().getTimes();
                if (times != null) {
                    pattern.times(times.getFrom(), times.getTo());
                }
            }
        }

        // Process innerConsumingStrategy of the quantifier
        final ConsumingStrategy innerConsumingStrategy = this.getQuantifier().getConsumingStrategy();
        if (innerConsumingStrategy.equals(ConsumingStrategy.SKIP_TILL_ANY)) {
            pattern.allowCombinations();
        } else if (innerConsumingStrategy.equals(ConsumingStrategy.STRICT)) {
            pattern.consecutive();
        }

        // Process until condition
        final ConditionSpec untilCondition = this.getQuantifier().getUntilCondition();
        if (untilCondition != null) {
            final IterativeCondition iterativeCondition = untilCondition.toIterativeCondition(classLoader);
            pattern.until(iterativeCondition);
        }
        return pattern;
    }

    /**
     * Type of Node.
     */
    public enum PatternNodeType {
        // ATOMIC Node is the basic Pattern
        ATOMIC,
        // COMPOSITE Node is a Graph
        COMPOSITE
    }

    public static class NodePattern<T, F extends T> extends Pattern<T, F> {
        public NodePattern(String name, Pattern<T, F> previous, ConsumingStrategy consumingStrategy, AfterMatchSkipStrategy afterMatchSkipStrategy) {
            super(name, previous, consumingStrategy, afterMatchSkipStrategy);
        }
    }
}

