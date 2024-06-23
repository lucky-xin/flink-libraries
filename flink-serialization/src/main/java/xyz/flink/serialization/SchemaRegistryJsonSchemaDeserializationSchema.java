package xyz.flink.serialization;

import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializer;
import lombok.experimental.SuperBuilder;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;

import java.io.IOException;

/**
 * SchemaRegistryAvroDeserializationSchema
 *
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2024-06-21
 */
@SuperBuilder
public class SchemaRegistryJsonSchemaDeserializationSchema<T>
        extends AbstractSchemaRegistrySchema<T> implements DeserializationSchema<T> {
    private static final long serialVersionUID = -1671641202177852775L;

    private transient KafkaJsonSchemaDeserializer<T> deserializer;


    @Override
    public T deserialize(byte[] byts) throws IOException {
        if (byts == null) {
            return null;
        }
        try {
            checkInitialized();
        } catch (RestClientException e) {
            throw new IOException("Init error: ", e);
        }

        return this.deserializer.deserialize(getSubject(), byts);
    }

    @Override
    public boolean isEndOfStream(T nextElement) {
        return false;
    }

    @Override
    protected void checkInitialized() throws IOException, RestClientException {
        if (this.deserializer != null) {
            return;
        }
        super.checkInitialized();
        this.deserializer = new KafkaJsonSchemaDeserializer<>(this.schemaRegistryClient);
        this.deserializer.configure(this.getRegistryConfigs(), this.key);
    }

    @Override
    public TypeInformation<T> getProducedType() {
        return TypeInformation.of(type);
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (this.deserializer != null) {
            this.deserializer.close();
        }
    }
}
