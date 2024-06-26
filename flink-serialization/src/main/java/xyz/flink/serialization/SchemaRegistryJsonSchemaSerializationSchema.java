package xyz.flink.serialization;

import io.confluent.connect.json.JsonSchemaData;
import io.confluent.connect.json.JsonSchemaDataConfig;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer;
import lombok.experimental.SuperBuilder;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.util.WrappingRuntimeException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * SchemaRegistryAvroSerializationSchema
 *
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2024-06-21
 */
@SuperBuilder
public class SchemaRegistryJsonSchemaSerializationSchema<T>
        extends AbstractSchemaRegistrySchema<T> implements SerializationSchema<T> {
    private static final long serialVersionUID = -1671641202177852775L;

    /**
     * Writer that writes the serialized record to {@link ByteArrayOutputStream}.
     */
    protected transient KafkaJsonSchemaSerializer<T> serializer;
    protected transient JsonSchemaData jsonSchemaData;

    @Override
    public byte[] serialize(T object) {
        if (object == null) {
            return new byte[0];
        }
        try {
            checkInitialized();
        } catch (IOException e) {
            throw new WrappingRuntimeException("Failed to serialize schema registry.", e);
        } catch (RestClientException e) {
            throw new WrappingRuntimeException("Failed to get schema from registry, subject:" + getSchema(), e);
        }
        return this.serializer.serialize(getSubject(), object);
    }

    @Override
    protected void checkInitialized() throws IOException, RestClientException {
        if (this.serializer != null) {
            return;
        }
        super.checkInitialized();
        this.serializer = new KafkaJsonSchemaSerializer<>(this.schemaRegistryClient);
        this.serializer.configure(getRegistryConfigs(), this.key);
        this.jsonSchemaData = new JsonSchemaData(new JsonSchemaDataConfig(getRegistryConfigs()));
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (this.serializer != null) {
            this.serializer.close();
        }
    }
}
