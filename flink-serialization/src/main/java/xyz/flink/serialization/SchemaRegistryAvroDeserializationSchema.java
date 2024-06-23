package xyz.flink.serialization;

import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecord;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.formats.avro.AvroFormatOptions;
import org.apache.flink.formats.avro.typeutils.AvroFactory;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * SchemaRegistryAvroDeserializationSchema
 *
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2024-06-21
 */
@SuperBuilder
public class SchemaRegistryAvroDeserializationSchema<T> extends AbstractSchemaRegistrySchema<T> implements DeserializationSchema<T> {
    private static final long serialVersionUID = -1671641202177852775L;

    /**
     * Config option for the serialization approach to use.
     */
    @NonNull
    private final AvroFormatOptions.AvroEncoding encoding;

    /**
     * Reader that deserializes byte array into a record.
     */
    private transient GenericDatumReader<T> datumReader;

    @Override
    public T deserialize(@Nullable byte[] message) throws IOException {
        if (message == null) {
            return null;
        }
        int schemaId = -1;
        try {
            checkInitialized();
            ByteBuffer buffer = ByteBuffer.wrap(message);
            if (buffer.get() != MAGIC_BYTE) {
                throw new IOException("Unknown magic byte!");
            }
            schemaId = buffer.getInt();
            AvroSchema writerSchema = (AvroSchema) this.schemaRegistryClient.getSchemaById(schemaId);
            AvroSchema readerSchema = (AvroSchema) this.schema;
            datumReader.setSchema(writerSchema.rawSchema());
            datumReader.setExpected(readerSchema.rawSchema());
            int length = buffer.limit() - 1 - ID_SIZE;
            byte[] bytes = new byte[length];
            buffer.get(bytes, 0, length);
            try (InputStream in = new ByteArrayInputStream(bytes)) {
                return datumReader.read(null, createDecoder(in));
            }
        } catch (RestClientException e) {
            throw new IOException("Schema not found for id: " + schemaId);
        }

    }

    private Decoder createDecoder(InputStream in) throws IOException {
        if (encoding == AvroFormatOptions.AvroEncoding.JSON) {
            return DecoderFactory.get().jsonDecoder(((AvroSchema) this.schema).rawSchema(), in);
        } else {
            return DecoderFactory.get().binaryDecoder(in, null);
        }
    }

    @Override
    public boolean isEndOfStream(T nextElement) {
        return false;
    }

    @Override
    protected void checkInitialized() throws IOException, RestClientException {
        if (datumReader != null) {
            return;
        }
        super.checkInitialized();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (SpecificRecord.class.isAssignableFrom(type)) {
            @SuppressWarnings("unchecked")
            SpecificData specificData =
                    AvroFactory.getSpecificDataForClass((Class<? extends SpecificData>) type, cl);
            this.datumReader = new SpecificDatumReader<>(specificData);
            this.schema = new AvroSchema(AvroFactory.extractAvroSpecificSchema(type, specificData));
        } else {
            GenericData genericData = new GenericData(cl);
            this.datumReader = new GenericDatumReader<>(null, ((AvroSchema) this.schema).rawSchema(), genericData);
        }
    }

    @Override
    public TypeInformation<T> getProducedType() {
        return TypeInformation.of(type);
    }

    @Override
    public void close() throws IOException {
        super.close();
    }
}
