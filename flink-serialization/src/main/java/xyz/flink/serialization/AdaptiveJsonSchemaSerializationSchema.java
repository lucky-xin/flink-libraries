package xyz.flink.serialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.client.SchemaMetadata;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import org.apache.flink.util.WrappingRuntimeException;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * AdaptiveJsonSchemaSerializationSchema
 *
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2024-06-21
 */
@SuperBuilder
public class AdaptiveJsonSchemaSerializationSchema extends SchemaRegistryJsonSchemaSerializationSchema<Object> {
    private static final long serialVersionUID = -1671641202177852775L;

    /**
     * Writer that writes the serialized record to {@link ByteArrayOutputStream}.
     */
    private transient ParsedSchema surSchema;
    private transient ObjectMapper mapper;

    @NonNull
    private final String surSubject;

    @Override
    public byte[] serialize(Object origValue) {
        if (origValue == null) {
            return new byte[0];
        }
        try {
            checkInitialized();
            JsonNode jsonNode = mapper.valueToTree(origValue);
            if (jsonNode.has("schema")) {
                JsonNode payload = jsonNode.get("payload");
                if (payload != null) {
                    jsonNode = payload;
                }
            }
            Schema surConnectSchema = this.jsonSchemaData.toConnectSchema((JsonSchema) this.surSchema);
            if (surConnectSchema.type()!= Schema.Type.STRUCT) {
                throw new IllegalStateException("source schema type must be struct.");
            }

            Struct surStruct = (Struct) this.jsonSchemaData.toConnectData(surConnectSchema, jsonNode);
            JsonSchema dstJsonSchema = (JsonSchema) getSchema();
            Schema dstConnectSchema = this.jsonSchemaData.toConnectSchema(dstJsonSchema);
            List<Field> fields = dstConnectSchema.fields();
            Struct dstStruct = new Struct(dstConnectSchema);
            for (Field field : fields) {
                Field f = surConnectSchema.field(field.name());
                Object object = surStruct.get(f);
                dstStruct.put(field, object);
            }
            JsonNode dstValueValue = this.jsonSchemaData.fromConnectData(dstConnectSchema, dstStruct);
            ObjectNode result = JsonNodeFactory.instance.objectNode();
            result.set("schema", ((JsonSchema) this.surSchema).toJsonNode());
            result.set("payload", dstValueValue);
            return super.serialize(result);
        } catch (IOException e) {
            throw new WrappingRuntimeException("Failed to serialize schema registry.", e);
        } catch (RestClientException e) {
            throw new WrappingRuntimeException("Failed to get schema from schema registry.", e);
        }
    }

    @Override
    protected void checkInitialized() throws IOException, RestClientException {
        if (this.surSchema != null) {
            return;
        }
        super.checkInitialized();
        SchemaMetadata meta = this.schemaRegistryClient.getLatestSchemaMetadata(this.surSubject);
        this.surSchema = this.schemaRegistryClient.parseSchema(
                meta.getSchemaType(),
                meta.getSchema(),
                meta.getReferences()
        ).orElseThrow(() -> new IllegalStateException("Failed to parse schema"));
        this.mapper = new ObjectMapper();
    }

    @Override
    public void close() throws IOException {
        super.close();
    }
}
