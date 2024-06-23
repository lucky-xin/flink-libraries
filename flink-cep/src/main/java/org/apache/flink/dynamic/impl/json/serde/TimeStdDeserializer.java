package org.apache.flink.dynamic.impl.json.serde;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonParser;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.DeserializationContext;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Customized StdDeserializer of Time.
 */
public class TimeStdDeserializer extends StdDeserializer<Time> {

    public static final TimeStdDeserializer INSTANCE = new TimeStdDeserializer();
    private static final long serialVersionUID = 1L;

    public TimeStdDeserializer() {
        this(Time.class);
    }

    public TimeStdDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Time deserialize(JsonParser jsonParser, DeserializationContext ctx) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        return Time.of(node.get("size").asLong(), TimeUnit.valueOf(node.get("unit").asText()));
    }
}
