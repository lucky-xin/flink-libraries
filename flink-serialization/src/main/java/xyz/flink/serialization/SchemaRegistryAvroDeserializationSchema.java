package xyz.flink.serialization;

import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import lombok.experimental.SuperBuilder;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.specific.SpecificRecord;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.formats.avro.typeutils.AvroFactory;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * SchemaRegistryAvroDeserializationSchema
 *
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2024-06-21
 */
@SuperBuilder
public class SchemaRegistryAvroDeserializationSchema<T>
        extends AbstractSchemaRegistrySchema<T, AvroSchema> implements DeserializationSchema<T> {
    private static final long serialVersionUID = -1671641202177852775L;

    private transient KafkaAvroDeserializer deserializer;

    @Override
    @SuppressWarnings("unchecked")
    public T deserialize(@Nullable byte[] byts) throws IOException {
        if (byts == null) {
            return null;
        }
        checkInitialized();
        return (T) this.deserializer.deserialize(this.getSubject(), byts);
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
        this.deserializer = new KafkaAvroDeserializer(this.getSchemaRegistryClient());
        this.deserializer.configure(this.getConfigs(), this.isKey());
    }

    @Override
    public AvroSchema createSchema() throws IOException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (SpecificRecord.class.isAssignableFrom(getType())) {
            @SuppressWarnings("unchecked")
            SpecificData specificData =
                    AvroFactory.getSpecificDataForClass((Class<? extends SpecificData>) getType(), cl);
            return new AvroSchema(AvroFactory.extractAvroSpecificSchema(getType(), specificData));
        }
        return super.createSchema();
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
