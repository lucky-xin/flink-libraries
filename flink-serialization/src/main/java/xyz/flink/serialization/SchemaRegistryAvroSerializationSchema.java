package xyz.flink.serialization;

import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import org.apache.avro.AvroTypeException;
import org.apache.avro.Schema;
import org.apache.avro.UnresolvedUnionException;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.path.LocationStep;
import org.apache.avro.path.TracingAvroTypeException;
import org.apache.avro.path.TracingClassCastException;
import org.apache.avro.path.TracingNullPointException;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.formats.avro.AvroFormatOptions;
import org.apache.flink.util.WrappingRuntimeException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

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

    /**
     * Config option for the serialization approach to use.
     */
    @NonNull
    private final AvroFormatOptions.AvroEncoding encoding;

    /**
     * Writer that writes the serialized record to {@link ByteArrayOutputStream}.
     */
    private transient GenericDatumWriter<T> datumWriter;

    @Override
    public byte[] serialize(T object) {
        try {
            checkInitialized();
            if (object == null) {
                return new byte[0];
            } else {
                try (ByteArrayOutputStream cache = new ByteArrayOutputStream(4096)) {
                    Encoder encoder = createEncoder(cache);
                    this.datumWriter.write(object, encoder);
                    encoder.flush();
                    byte[] byts = cache.toByteArray();
                    ByteBuffer buffer = ByteBuffer.allocate(byts.length + ID_SIZE + 1);
                    int registeredId = schemaRegistryClient.getId(getSubject(), schema);
                    buffer.put(MAGIC_BYTE)
                            .put(ByteBuffer.allocate(ID_SIZE).putInt(registeredId).array())
                            .put(byts);
                    return buffer.array();
                }
            }
        } catch (IOException e) {
            throw new WrappingRuntimeException("Failed to serialize schema registry.", e);
        } catch (RestClientException e) {
            throw new WrappingRuntimeException("Failed to get schema, subject:" + getSubject(), e);
        }
    }

    @Override
    protected void checkInitialized() throws IOException, RestClientException {
        if (datumWriter != null) {
            return;
        }
        super.checkInitialized();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (SpecificRecord.class.isAssignableFrom(type)) {
            Schema s = SpecificData.get().getSchema(type);
            this.datumWriter = new SpecificDatumWriter<>(s);
            this.schema = new AvroSchema(s);
        } else {
            GenericData genericData = new GenericData(cl);
            this.datumWriter = new AvroGenericDatumWriter(((AvroSchema) schema).rawSchema(), genericData);
        }
    }

    private Encoder createEncoder(OutputStream out) {
        if (encoding == AvroFormatOptions.AvroEncoding.JSON) {
            try {
                return EncoderFactory.get().jsonEncoder(((AvroSchema) schema).rawSchema(), out);
            } catch (IOException e) {
                throw new WrappingRuntimeException("Failed to create Avro encoder.", e);
            }
        } else {
            return EncoderFactory.get().directBinaryEncoder(out, null);
        }
    }

    @Override
    public void close() throws IOException {
        super.close();
    }

    class AvroGenericDatumWriter extends GenericDatumWriter<T> {

        public AvroGenericDatumWriter(Schema root, GenericData data) {
            super(data);
            setSchema(root);
        }

        /**
         * Called to write a single field of a record. May be overridden for more
         * efficient or alternate implementations.
         */
        @Override
        protected void writeField(Object datum, Schema.Field f, Encoder out, Object state) throws IOException {
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
