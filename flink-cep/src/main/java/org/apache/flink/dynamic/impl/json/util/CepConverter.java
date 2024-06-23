package org.apache.flink.dynamic.impl.json.util;

import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.cep.pattern.Quantifier;
import org.apache.flink.dynamic.impl.json.serde.ConditionSpecStdDeserializer;
import org.apache.flink.dynamic.impl.json.serde.NodeSpecStdDeserializer;
import org.apache.flink.dynamic.impl.json.serde.PatternTimesStdDeserializer;
import org.apache.flink.dynamic.impl.json.serde.PatternTimesStdSerializer;
import org.apache.flink.dynamic.impl.json.spec.ConditionSpec;
import org.apache.flink.dynamic.impl.json.spec.GraphSpec;
import org.apache.flink.dynamic.impl.json.spec.NodeSpec;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.datatype.jsr310.deser.DurationDeserializer;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.datatype.jsr310.ser.DurationSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Utils for translating a Pattern to JSON string and vice versa.
 */
public class CepConverter {

    private static final Logger LOG = LoggerFactory.getLogger(CepConverter.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new SimpleModule()
                    .addDeserializer(ConditionSpec.class, ConditionSpecStdDeserializer.INSTANCE)
                    .addDeserializer(Duration.class, DurationDeserializer.INSTANCE)
                    .addSerializer(Duration.class, DurationSerializer.INSTANCE)
                    .addDeserializer(NodeSpec.class, NodeSpecStdDeserializer.INSTANCE)
                    .addDeserializer(Quantifier.Times.class, PatternTimesStdDeserializer.INSTANCE)
                    .addSerializer(Quantifier.Times.class, PatternTimesStdSerializer.INSTANCE)
            );

    public static String toJSONString(Pattern<?, ?> pattern) throws JsonProcessingException {
        GraphSpec graphSpec = GraphSpec.fromPattern(pattern);
        return MAPPER.writeValueAsString(graphSpec);
    }

    public static <T, F extends T> Pattern<T, F> toPattern(String jsonString) {
        return toPattern(jsonString, Thread.currentThread().getContextClassLoader());
    }

    public static <T, F extends T> Pattern<T, F> toPattern(
            String jsonString,
            ClassLoader userCodeClassLoader) {
        if (userCodeClassLoader == null) {
            LOG.warn(
                    "The given userCodeClassLoader is null. Will try to use ContextClassLoader of current thread.");
            return toPattern(jsonString);
        }
        try {
            GraphSpec deserializedGraphSpec = MAPPER.readValue(jsonString, GraphSpec.class);
            return deserializedGraphSpec.toPattern(userCodeClassLoader);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static GraphSpec toGraphSpec(String jsonString) throws JsonProcessingException {
        return MAPPER.readValue(jsonString, GraphSpec.class);
    }

    public static String toJSONString(GraphSpec graphSpec) throws JsonProcessingException {
        return MAPPER.writeValueAsString(graphSpec);
    }
}

