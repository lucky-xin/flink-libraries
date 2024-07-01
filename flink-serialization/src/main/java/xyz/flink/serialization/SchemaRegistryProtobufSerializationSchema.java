package xyz.flink.serialization;

import com.google.protobuf.Message;
import io.confluent.connect.protobuf.ProtobufConverter;
import io.confluent.connect.protobuf.ProtobufData;
import io.confluent.connect.protobuf.ProtobufDataConfig;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import lombok.experimental.SuperBuilder;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.util.WrappingRuntimeException;
import org.apache.kafka.connect.data.Schema;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * SchemaRegistryJsonSerializationSchema
 *
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2024-06-21
 */
@SuperBuilder
public class SchemaRegistryProtobufSerializationSchema<T extends Message>
        extends AbstractSchemaRegistrySchema<T, ProtobufSchema> implements SerializationSchema<T> {
    private static final long serialVersionUID = -1671641202177852775L;

    /**
     * Writer that writes the serialized record to {@link ByteArrayOutputStream}.
     */
    protected transient ProtobufConverter converter;
    protected transient ProtobufData protobufData;

    @Override
    public byte[] serialize(T object) {
        if (object == null) {
            return new byte[0];
        }
        try {
            checkInitialized();
        } catch (IOException e) {
            throw new WrappingRuntimeException("Failed to serialize schema registry.", e);
        }
        Schema connectSchema = this.protobufData.toConnectSchema(getSchema());
        return this.converter.fromConnectData(getSubject(), connectSchema, object);
    }

    @Override
    protected void checkInitialized() throws IOException {
        if (this.converter != null) {
            return;
        }
        super.checkInitialized();
        this.converter = new ProtobufConverter(this.getSchemaRegistryClient());
        this.converter.configure(getConfigs(), this.isKey());
        this.protobufData = new ProtobufData(new ProtobufDataConfig(getConfigs()));
    }
}
