package xyz.flink.serialization;

import com.google.protobuf.Message;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializer;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer;
import lombok.experimental.SuperBuilder;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;

import java.io.IOException;

/**
 * SchemaRegistryProtobufDeserializationSchema
 *
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2024-06-21
 */
@SuperBuilder
public class SchemaRegistryProtobufDeserializationSchema<T extends Message>
        extends AbstractSchemaRegistrySchema<T, ProtobufSchema> implements DeserializationSchema<T> {
    private static final long serialVersionUID = -1671641202177852775L;

    private transient KafkaProtobufDeserializer<T> deserializer;


    @Override
    public T deserialize(byte[] byts) throws IOException {
        if (byts == null) {
            return null;
        }
        checkInitialized();
        return this.deserializer.deserialize(getSubject(), byts);
    }

    @Override
    public boolean isEndOfStream(T nextElement) {
        return false;
    }

    @Override
    protected void checkInitialized() throws IOException {
        if (this.deserializer != null) {
            return;
        }
        super.checkInitialized();
        this.deserializer = new KafkaProtobufDeserializer<>(this.getSchemaRegistryClient());
        this.deserializer.configure(this.getConfigs(), this.isKey());
    }

    @Override
    public TypeInformation<T> getProducedType() {
        return TypeInformation.of(getType());
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (this.deserializer != null) {
            this.deserializer.close();
        }
    }
}
