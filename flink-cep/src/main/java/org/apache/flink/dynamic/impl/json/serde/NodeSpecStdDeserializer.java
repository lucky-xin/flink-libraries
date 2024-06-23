package org.apache.flink.dynamic.impl.json.serde;

import org.apache.flink.dynamic.impl.json.spec.AfterMatchSkipStrategySpec;
import org.apache.flink.dynamic.impl.json.spec.ConditionSpec;
import org.apache.flink.dynamic.impl.json.spec.EdgeSpec;
import org.apache.flink.dynamic.impl.json.spec.GraphSpec;
import org.apache.flink.dynamic.impl.json.spec.NodeSpec;
import org.apache.flink.dynamic.impl.json.spec.QuantifierSpec;
import org.apache.flink.dynamic.impl.json.spec.WindowSpec;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonParser;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.DeserializationContext;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * The customized StdDeserializer for NodeSpec.
 */
public class NodeSpecStdDeserializer extends StdDeserializer<NodeSpec> {

    public static final NodeSpecStdDeserializer INSTANCE = new NodeSpecStdDeserializer();
    private static final long serialVersionUID = 1L;

    public NodeSpecStdDeserializer() {
        this(NodeSpec.class);
    }

    public NodeSpecStdDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public NodeSpec deserialize(JsonParser jsonParser, DeserializationContext ctx) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        NodeSpec.PatternNodeType type = NodeSpec.PatternNodeType.valueOf(node.get("type").asText());
        String name = node.get("name").asText();
        QuantifierSpec quantifierSpec = jsonParser.getCodec().treeToValue(node.get("quantifier"), QuantifierSpec.class);
        ConditionSpec conditionSpec = jsonParser.getCodec().treeToValue(node.get("condition"), ConditionSpec.class);
        if (type.equals(NodeSpec.PatternNodeType.COMPOSITE)) {
            List<NodeSpec> nodeSpecs = new LinkedList<>();
            Iterator<JsonNode> embeddedElementNames = node.get("nodes").elements();
            while (embeddedElementNames.hasNext()) {
                JsonNode jsonNode = embeddedElementNames.next();
                NodeSpec embedNode = jsonParser.getCodec().treeToValue(jsonNode, NodeSpec.class);
                nodeSpecs.add(embedNode);
            }

            List<EdgeSpec> edgeSpecs = new LinkedList<>();
            Iterator<JsonNode> jsonNodeIterator = node.get("edges").elements();
            while (jsonNodeIterator.hasNext()) {
                JsonNode jsonNode = jsonNodeIterator.next();
                EdgeSpec embedNode = jsonParser.getCodec().treeToValue(jsonNode, EdgeSpec.class);
                edgeSpecs.add(embedNode);
            }

            WindowSpec window = jsonParser.getCodec().treeToValue(node.get("window"), WindowSpec.class);
            AfterMatchSkipStrategySpec afterMatchStrategy =
                    jsonParser.getCodec().treeToValue(node.get("afterMatchStrategy"), AfterMatchSkipStrategySpec.class);
            return new GraphSpec(
                    name,
                    quantifierSpec,
                    conditionSpec,
                    nodeSpecs,
                    edgeSpecs,
                    window,
                    afterMatchStrategy);
        } else {
            return new NodeSpec(name, quantifierSpec, conditionSpec);
        }
    }
}
