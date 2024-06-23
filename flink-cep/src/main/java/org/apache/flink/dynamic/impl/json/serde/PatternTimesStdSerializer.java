package org.apache.flink.dynamic.impl.json.serde;

import org.apache.flink.cep.pattern.Quantifier;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonGenerator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.SerializerProvider;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

/**
 * Customized StdDeserializer of Time.
 */
public class PatternTimesStdSerializer extends StdSerializer<Quantifier.Times> {

    public static final PatternTimesStdSerializer INSTANCE = new PatternTimesStdSerializer();
    private static final long serialVersionUID = 1L;

    public PatternTimesStdSerializer() {
        super(Quantifier.Times.class);
    }

    @Override
    public void serialize(Quantifier.Times times, JsonGenerator generator, SerializerProvider provider) throws IOException {
        generator.writeStartObject();
        generator.writeNumberField("from", times.getFrom());
        generator.writeNumberField("to", times.getTo());
        if (times.getWindowSize().isPresent()) {
            generator.writeStringField("windowSize", times.getWindowSize().get().toString());
        }
        generator.writeEndObject();
    }
}
