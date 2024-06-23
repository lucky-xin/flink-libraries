package org.apache.flink.dynamic.impl.json.spec;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.apache.flink.cep.pattern.GroupPattern;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.cep.pattern.Quantifier.ConsumingStrategy;
import org.apache.flink.cep.pattern.WithinType;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The Graph is used to describe a complex Pattern which contains Nodes(i.e {@link Pattern}) and
 * Edges. The Node of a Graph can be a embedded Graph as well. This class is to (de)serialize Graphs
 * in json format.
 */
@Getter
@SuperBuilder
public class GraphSpec extends NodeSpec {
    private final int version = 1;
    private final List<NodeSpec> nodes;
    private final List<EdgeSpec> edges;
    private final WindowSpec window;
    private final AfterMatchSkipStrategySpec afterMatchStrategy;

    public GraphSpec(
            @JsonProperty("name") String name,
            @JsonProperty("quantifier") QuantifierSpec quantifier,
            @JsonProperty("condition") ConditionSpec condition,
            @JsonProperty("nodes") List<NodeSpec> nodes,
            @JsonProperty("edges") List<EdgeSpec> edges,
            @JsonProperty("window") WindowSpec window,
            @JsonProperty("afterMatchStrategy") AfterMatchSkipStrategySpec afterMatchStrategy) {
        super(name, quantifier, condition, PatternNodeType.COMPOSITE);
        this.nodes = nodes;
        this.edges = edges;
        this.window = window;
        this.afterMatchStrategy = afterMatchStrategy;
    }

    public static GraphSpec fromPattern(Pattern<?, ?> pattern) {
        // Build the metadata of the GraphSpec from pattern
        // Name
        String name = pattern instanceof GroupPattern
                ? ((GroupPattern<?, ?>) pattern).getRawPattern().getName()
                : pattern.getName();

        // Quantifier
        QuantifierSpec quantifier =
                new QuantifierSpec(
                        pattern.getQuantifier(), pattern.getTimes(), pattern.getUntilCondition());

        // Window
        Map<WithinType, Duration> window = new EnumMap<>(WithinType.class);
        Optional<Duration> op1 = pattern.getWindowSize(WithinType.FIRST_AND_LAST);
        Optional<Duration> op2 = pattern.getWindowSize(WithinType.PREVIOUS_AND_CURRENT);
        if (op1.isPresent()) {
            op1.ifPresent(c -> window.put(WithinType.FIRST_AND_LAST, c));
        } else if (op2.isPresent()) {
            op2.ifPresent(c -> window.put(WithinType.PREVIOUS_AND_CURRENT, c));
        }
        GraphSpec.GraphSpecBuilder<?, ?> builder = GraphSpec.builder()
                .name(name)
                .quantifier(quantifier)
                .afterMatchStrategy(AfterMatchSkipStrategySpec.fromAfterMatchSkipStrategy(pattern.getAfterMatchSkipStrategy()));
        if (!window.isEmpty()) {
            builder.window(WindowSpec.fromWindowTime(window));
        }

        // Build nested pattern sequence
        List<NodeSpec> nodes = new ArrayList<>();
        List<EdgeSpec> edges = new ArrayList<>();
        while (pattern != null) {
            if (pattern instanceof GroupPattern) {
                // Process sub graph recursively
                GraphSpec subgraphSpec = GraphSpec.fromPattern(((GroupPattern<?, ?>) pattern).getRawPattern());
                nodes.add(subgraphSpec);
            } else {
                // Build nodeSpec
                NodeSpec nodeSpec = NodeSpec.fromPattern(pattern);
                nodes.add(nodeSpec);
            }
            if (pattern.getPrevious() != null) {
                edges.add(
                        new EdgeSpec(
                                pattern.getPrevious() instanceof GroupPattern
                                        ? ((GroupPattern<?, ?>) pattern.getPrevious()).getRawPattern().getName()
                                        : pattern.getPrevious().getName(),
                                pattern instanceof GroupPattern
                                        ? ((GroupPattern<?, ?>) pattern).getRawPattern().getName()
                                        : pattern.getName(),
                                pattern.getQuantifier().getConsumingStrategy()
                        )
                );
            }
            pattern = pattern.getPrevious();
        }
        builder.nodes(nodes).edges(edges);
        return builder.build();
    }

    /**
     * Converts the {@link GraphSpec} to the {@link Pattern}.
     *
     * @param classLoader The {@link ClassLoader} of the {@link Pattern}.
     * @return The converted {@link Pattern}.
     * @throws Exception Exceptions thrown while deserialization of the Pattern.
     */
    public <T, F extends T> Pattern<T, F> toPattern(final ClassLoader classLoader) throws Exception {
        // Construct cache of nodes and edges for later use
        final Map<String, NodeSpec> nodeCache = new HashMap<>(nodes.size());
        for (NodeSpec node : nodes) {
            nodeCache.put(node.getName(), node);
        }
        final Map<String, EdgeSpec> edgeCache = new HashMap<>(edges.size());
        for (EdgeSpec edgeSpec : edges) {
            edgeCache.put(edgeSpec.getSource(), edgeSpec);
        }

        // Build pattern sequence
        String currentNodeName = findBeginPatternName();
        Pattern<T, F> prevPattern = null;
        String prevNodeName = null;
        while (currentNodeName != null) {
            NodeSpec currentNodeSpec = nodeCache.get(currentNodeName);
            EdgeSpec edgeToCurrentNode = edgeCache.get(prevNodeName);
            // Build the atomic pattern
            // Note, the afterMatchStrategy of neighboring patterns should adopt the GraphSpec's
            // afterMatchStrategy
            Pattern<T, F> currentPattern =
                    currentNodeSpec.toPattern(
                            prevPattern,
                            afterMatchStrategy.toAfterMatchSkipStrategy(),
                            prevNodeName == null ? ConsumingStrategy.STRICT : edgeToCurrentNode.getType(),
                            classLoader
                    );
            if (currentNodeSpec instanceof GraphSpec) {
                ConsumingStrategy strategy = prevNodeName == null ? ConsumingStrategy.STRICT : edgeToCurrentNode.getType();
                prevPattern = buildGroupPattern(strategy, currentPattern, prevPattern, prevNodeName == null);
            } else {
                prevPattern = currentPattern;
            }
            prevNodeName = currentNodeName;
            currentNodeName = edgeCache.get(currentNodeName) == null ? null : edgeCache.get(currentNodeName).getTarget();
        }
        // Add window semantics
        if (window != null && prevPattern != null) {
            prevPattern.within(this.window.getDuration(), this.window.getType());
        }

        return prevPattern;
    }

    private String findBeginPatternName() {
        final Set<String> nodeSpecSet = new HashSet<>();
        for (NodeSpec node : nodes) {
            nodeSpecSet.add(node.getName());
        }
        for (EdgeSpec edgeSpec : edges) {
            nodeSpecSet.remove(edgeSpec.getTarget());
        }
        if (nodeSpecSet.size() != 1) {
            throw new IllegalStateException(
                    "There must be exactly one begin node, but there are "
                            + nodeSpecSet.size()
                            + " nodes that are not pointed by any other nodes.");
        }
        Iterator<String> iterator = nodeSpecSet.iterator();

        if (!iterator.hasNext()) {
            throw new IllegalStateException("Could not find the begin node.");
        }

        return iterator.next();
    }

    @SuppressWarnings("all")
    private <T, F extends T> GroupPattern<T, F> buildGroupPattern(
            ConsumingStrategy strategy,
            Pattern<?, ?> currentPattern,
            Pattern<?, ?> prevPattern,
            boolean isBeginPattern) {
        // construct GroupPattern
        if (strategy.equals(ConsumingStrategy.STRICT)) {
            if (isBeginPattern) {
                currentPattern = Pattern.begin(currentPattern);
            } else {
                currentPattern = prevPattern.next((Pattern) currentPattern);
            }
        } else if (strategy.equals(ConsumingStrategy.SKIP_TILL_NEXT)) {
            currentPattern = prevPattern.followedBy((Pattern) currentPattern);
        } else if (strategy.equals(ConsumingStrategy.SKIP_TILL_ANY)) {
            currentPattern = prevPattern.followedByAny((Pattern) currentPattern);
        }
        return (GroupPattern<T, F>) currentPattern;
    }
}
