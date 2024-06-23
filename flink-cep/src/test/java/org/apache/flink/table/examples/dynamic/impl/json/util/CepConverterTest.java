package org.apache.flink.table.examples.dynamic.impl.json.util;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ClassPathResource;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.cep.pattern.Quantifier;
import org.apache.flink.dynamic.impl.json.serde.ConditionSpecStdDeserializer;
import org.apache.flink.dynamic.impl.json.serde.NodeSpecStdDeserializer;
import org.apache.flink.dynamic.impl.json.serde.PatternTimesStdDeserializer;
import org.apache.flink.dynamic.impl.json.serde.PatternTimesStdSerializer;
import org.apache.flink.dynamic.impl.json.serde.TimeStdDeserializer;
import org.apache.flink.dynamic.impl.json.spec.ConditionSpec;
import org.apache.flink.dynamic.impl.json.spec.NodeSpec;
import org.apache.flink.dynamic.json.CepConverter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.datatype.jsr310.deser.DurationDeserializer;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.datatype.jsr310.ser.DurationSerializer;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2024-06-19
 */
class CepConverterTest {
    private static final ObjectMapper MAPPER = new ObjectMapper()
//            .disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
            .registerModule(new SimpleModule()
                    .addDeserializer(ConditionSpec.class, ConditionSpecStdDeserializer.INSTANCE)
                    .addDeserializer(Time.class, TimeStdDeserializer.INSTANCE)
                    .addDeserializer(Duration.class, DurationDeserializer.INSTANCE)
                    .addSerializer(Duration.class, DurationSerializer.INSTANCE)
                    .addDeserializer(NodeSpec.class, NodeSpecStdDeserializer.INSTANCE)
                    .addDeserializer(Quantifier.Times.class, PatternTimesStdDeserializer.INSTANCE)
                    .addSerializer(Quantifier.Times.class, PatternTimesStdSerializer.INSTANCE)
            );

    @Test
    void convertPatternToJSONString() throws Exception {
//        CepJsonUtils.convertPatternToJSONString()
        Quantifier.Times times = Quantifier.Times.of(3, 3, Duration.ofDays(1));
        String text = MAPPER.writeValueAsString(times);
        System.out.println(text);
        Duration parse = Duration.parse("PT30S");
        System.out.println(parse);
        Quantifier.Times times1 = MAPPER.readValue(text, Quantifier.Times.class);
        System.out.println(times1);
    }

    @Test
    void convertJSONStringToPattern() {
    }

    @Test
    void testConvertJSONStringToPattern() throws Exception {
        ClassPathResource resource = new ClassPathResource("dynamic_rule-1.json");
        try (InputStream in = resource.getStream()) {
            assert in != null;
            String json = IoUtil.read(in, StandardCharsets.UTF_8);
            System.out.println(json);
            Pattern<Object, Object> pattern = CepConverter.toPattern(json);
            System.out.println(pattern.getName());
        }
    }

    @Test
    void convertJSONStringToGraphSpec() {
    }

    @Test
    void convertGraphSpecToJSONString() {
    }
}