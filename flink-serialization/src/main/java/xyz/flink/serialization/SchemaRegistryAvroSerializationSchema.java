package xyz.flink.serialization;

import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import lombok.experimental.SuperBuilder;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.specific.SpecificRecord;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.util.WrappingRuntimeException;

import java.io.IOException;

/**
 * SchemaRegistryAvroSerializationSchema
 *
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2024-06-21
 */
@SuperBuilder
public class SchemaRegistryAvroSerializationSchema<T>
        extends AbstractSchemaRegistrySchema<T> implements SerializationSchema<T> {
    private static final long serialVersionUID = -1671641202177852775L;

    private transient KafkaAvroSerializer serializer;

    @Override
    public byte[] serialize(T object) {
        if (object == null) {
            return new byte[0];
        }
        try {
            checkInitialized();
            return this.serializer.serialize(getSubject(), object);
        } catch (IOException e) {
            throw new WrappingRuntimeException("Failed to serialize schema registry.", e);
        }
    }

    @Override
    protected void checkInitialized() throws IOException {
        if (this.serializer != null) {
            return;
        }
        super.checkInitialized();
        this.serializer = new KafkaAvroSerializer(this.getSchemaRegistryClient());
        this.serializer.configure(this.getConfigs(), this.isKey());
    }

    @Override
    public ParsedSchema createSchema() throws IOException {
        if (SpecificRecord.class.isAssignableFrom(getType())) {
            Schema s = SpecificData.get().getSchema(getType());
            return new AvroSchema(s);
        }
        return super.createSchema();
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (this.serializer != null) {
            this.serializer.close();
        }
    }
}
