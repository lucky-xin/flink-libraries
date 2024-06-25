package xyz.flink.serialization;

import io.confluent.connect.avro.AvroDataConfig;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import org.apache.avro.AvroTypeException;
import org.apache.avro.UnresolvedUnionException;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.path.LocationStep;
import org.apache.avro.path.TracingAvroTypeException;
import org.apache.avro.path.TracingClassCastException;
import org.apache.avro.path.TracingNullPointException;
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
    /**
     * Writer that writes the serialized record to {@link ByteArrayOutputStream}.
     */
    private transient GenericDatumWriter<Object> datumWriter;
    private transient AvroData avroData;

    @Override
    protected void checkInitialized() throws IOException {
        if (datumWriter != null) {
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
                Object object = surStruct.get(f);
                dstStruct.put(field, object);
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

    class AvroGenericDatumWriter extends GenericDatumWriter<Object> {

        public AvroGenericDatumWriter(org.apache.avro.Schema root, GenericData data) {
            super(data);
            setSchema(root);
        }

        /**
         * Called to write a single field of a record. May be overridden for more
         * efficient or alternate implementations.
         */
        @Override
        protected void writeField(Object datum, org.apache.avro.Schema.Field f, Encoder out, Object state) throws IOException {
            Object value = null;
            try {
                if (datum instanceof GenericData.Record) {
                    GenericData.Record r = (GenericData.Record) datum;
                    value = r.get(f.name());
                    write(f.schema(), value, out);
                    return;
                }
                GenericData data = getData();
                value = data.getField(datum, f.name(), f.pos());
                write(f.schema(), value, out);
            } catch (final UnresolvedUnionException uue) { // recreate it with the right field info
                final UnresolvedUnionException unresolvedUnionException = new UnresolvedUnionException(f.schema(), f, value);
                unresolvedUnionException.addSuppressed(uue);
                throw unresolvedUnionException;
            } catch (TracingNullPointException | TracingClassCastException | TracingAvroTypeException e) {
                e.tracePath(new LocationStep(".", f.name()));
                throw e;
            } catch (NullPointerException e) {
                throw npe(e, " in field " + f.name());
            } catch (ClassCastException cce) {
                throw addClassCastMsg(cce, " in field " + f.name());
            } catch (AvroTypeException ate) {
                throw addAvroTypeMsg(ate, " in field " + f.name());
            }
        }
    }

}
