package xyz.flink.serialization;

import io.confluent.connect.avro.AvroDataConfig;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import org.apache.flink.util.WrappingRuntimeException;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.apache.kafka.connect.data.Struct;
import xyz.avro.AvroData;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * SchemaRegistryAdaptiveAvroSerializationSchema
 *
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2024-06-21
 */
@SuperBuilder
public class SchemaRegistryAdaptiveAvroSerializationSchema
        extends SchemaRegistryAvroSerializationSchema<Object> {
    private static final long serialVersionUID = -1671641202177852775L;

    @NonNull
    private final String surSubject;

    /**
     * Writer that writes the serialized record to {@link ByteArrayOutputStream}.
     */
    private transient AvroSchema surSchema;

    private transient AvroData avroData;

    @Override
    protected void checkInitialized() throws IOException {
        if (avroData != null) {
            return;
        }
        super.checkInitialized();
        this.avroData = new AvroData(new AvroDataConfig(getConfigs()));
        this.surSchema = getSchema(this.surSubject);
    }

    @Override
    public byte[] serialize(Object origValue) {
        if (origValue == null) {
            return new byte[0];
        }
        try {
            checkInitialized();
            Schema surConnectSchema = this.avroData.toConnectSchema(this.surSchema.rawSchema());
            if (surConnectSchema.type() != Schema.Type.STRUCT) {
                throw new IllegalStateException("source schema type must be struct.");
            }
            SchemaAndValue sav = this.avroData.toConnectData(this.surSchema.rawSchema(), origValue);
            Struct surStruct = (Struct) sav.value();
            AvroSchema dstJsonSchema = getSchema();
            Schema dstConnectSchema = this.avroData.toConnectSchema(dstJsonSchema.rawSchema());
            Struct dstStruct = new Struct(dstConnectSchema);
            for (Field field : dstConnectSchema.fields()) {
                Field f = surConnectSchema.field(field.name());
                Object object = surStruct.get(field.name());
                org.apache.avro.Schema schema = this.avroData.fromConnectSchema(f.schema());
                SchemaAndValue schemaAndValue = this.avroData.toConnectData(schema, object);
                Object val = null;
                if (schemaAndValue != null) {
                    val = schemaAndValue.value();
                }
                dstStruct.put(field, val);
            }
            Object dstValueValue = this.avroData.fromConnectData(dstConnectSchema, dstStruct);
            return super.serialize(dstValueValue);
        } catch (IOException e) {
            throw new WrappingRuntimeException("Failed to serialize schema registry.", e);
        }
    }

    @Override
    public void close() throws IOException {
        super.close();
    }
}
