package org.apache.flink.dynamic.impl.json.serde;

import org.apache.flink.cep.pattern.Quantifier;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonParser;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.DeserializationContext;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.time.Duration;

/**
 * Customized StdDeserializer of Time.
 */
public class PatternTimesStdDeserializer extends StdDeserializer<Quantifier.Times> {

    public static final PatternTimesStdDeserializer INSTANCE = new PatternTimesStdDeserializer();
    private static final long serialVersionUID = 1L;

    public PatternTimesStdDeserializer() {
        super(Quantifier.Times.class);
    }
    public PatternTimesStdDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Quantifier.Times deserialize(JsonParser jsonParser, DeserializationContext ctx) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        Duration duration = jsonParser.getCodec().treeToValue(node.get("windowSize"), Duration.class);
        return Quantifier.Times.of(
                node.get("from").asInt(),
                node.get("to").asInt(),
                duration
        );
    }
}
