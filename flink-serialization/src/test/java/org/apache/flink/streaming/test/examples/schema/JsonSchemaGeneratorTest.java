package org.apache.flink.streaming.test.examples.schema;

import cn.hutool.core.io.IoUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kjetland.jackson.jsonSchema.JsonSchemaConfig;
import com.kjetland.jackson.jsonSchema.JsonSchemaDraft;
import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator;
import com.kjetland.jackson.jsonSchema.SubclassesResolverImpl;
import io.confluent.connect.avro.AvroDataConfig;
import io.confluent.connect.json.JsonSchemaData;
import io.confluent.connect.json.JsonSchemaDataConfig;
import io.confluent.connect.schema.AbstractDataConfig;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import org.apache.flink.streaming.test.examples.entity.bo.TestData;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Values;
import org.junit.jupiter.api.Test;
import scala.collection.immutable.HashMap;
import scala.collection.immutable.HashSet;
import xyz.avro.AvroData;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static io.confluent.connect.json.JsonSchemaDataConfig.USE_OPTIONAL_FOR_NON_REQUIRED_CONFIG;
import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS;
import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.LATEST_COMPATIBILITY_STRICT;
import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.USE_LATEST_VERSION;

/**
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2024-06-22
 */
class JsonSchemaGeneratorTest {

    @Test
    void generateSchemaTest() throws Exception {
        JsonSchemaData jsonSchemaData = new JsonSchemaData(new JsonSchemaDataConfig(Map.of(
                LATEST_COMPATIBILITY_STRICT, false,
                AUTO_REGISTER_SCHEMAS, false,
                USE_LATEST_VERSION, true,
                USE_OPTIONAL_FOR_NON_REQUIRED_CONFIG, true
        )));
        AvroData avroData = new AvroData(new AvroDataConfig(
                Map.of(
                        AbstractDataConfig.SCHEMAS_CACHE_SIZE_CONFIG, 1000,
                        AbstractDataConfig.GENERALIZED_SUM_TYPE_SUPPORT_CONFIG, true,
                        AbstractDataConfig.IGNORE_DEFAULT_FOR_NULLABLES_CONFIG, true,
                        AvroDataConfig.ENHANCED_AVRO_SCHEMA_SUPPORT_CONFIG, true,
                        AvroDataConfig.SCRUB_INVALID_NAMES_CONFIG, true,
                        AvroDataConfig.ALLOW_OPTIONAL_MAP_KEYS_CONFIG, true
                )
        ));

        ObjectMapper objectMapper = new ObjectMapper();
        JsonSchemaConfig config = new JsonSchemaConfig(
                true,
                null,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                new HashMap<>(),
                true,
                new HashSet<>(),
                new HashMap<>(),
                new HashMap<>(),
                new SubclassesResolverImpl(),
                true,
                new Class[]{},
                JsonSchemaDraft.DRAFT_07
        );
        JsonSchemaConfig jsonSchemaConfig = JsonSchemaConfig.vanillaJsonSchemaDraft4();
        jsonSchemaConfig.withJsonSchemaDraft(JsonSchemaDraft.DRAFT_07);
        JsonSchemaGenerator jsonSchemaGenerator = new JsonSchemaGenerator(objectMapper, config);

        // If using JsonSchema to generate HTML5 GUI:
        // JsonSchemaGenerator html5 = new JsonSchemaGenerator(objectMapper, JsonSchemaConfig.html5EnabledSchema() );

        // If you want to configure it manually:
        // JsonSchemaConfig config = JsonSchemaConfig.create(...);
        // JsonSchemaGenerator generator = new JsonSchemaGenerator(objectMapper, config);

        JsonNode jsonSchema = jsonSchemaGenerator.generateJsonSchema(TestData.class);
//        scala.$less$colon$less
        String jsonSchemaAsString = objectMapper.writeValueAsString(jsonSchema);
        System.out.println(jsonSchemaAsString);

        Schema connectSchema = jsonSchemaData.toConnectSchema(new JsonSchema(jsonSchemaAsString));
        org.apache.avro.Schema avroSchema = avroData.fromConnectSchema(connectSchema);
        System.out.println(avroSchema.toString());

        JsonNode jsonSchema1 = jsonSchemaGenerator.generateJsonSchema(HashMap.class);
        System.out.println(jsonSchema1);
    }

    @Test
    void schemaConvertTest() throws Exception {
        JsonSchemaData jsonSchemaData = new JsonSchemaData(new JsonSchemaDataConfig(Map.of(
                LATEST_COMPATIBILITY_STRICT, false,
                AUTO_REGISTER_SCHEMAS, false,
                USE_LATEST_VERSION, true
        )));
        AvroData avroData = new AvroData(new AvroDataConfig(
                Map.of(
                        AbstractDataConfig.SCHEMAS_CACHE_SIZE_CONFIG, 1000,
                        AbstractDataConfig.GENERALIZED_SUM_TYPE_SUPPORT_CONFIG, true,
                        AbstractDataConfig.IGNORE_DEFAULT_FOR_NULLABLES_CONFIG, true,
                        AvroDataConfig.ENHANCED_AVRO_SCHEMA_SUPPORT_CONFIG, true,
                        AvroDataConfig.SCRUB_INVALID_NAMES_CONFIG, true,
                        AvroDataConfig.ALLOW_OPTIONAL_MAP_KEYS_CONFIG, true
                )
        ));

        ObjectMapper objectMapper = new ObjectMapper();
        try (InputStream in = JsonSchemaGeneratorTest.class.getResourceAsStream("/Test_Data-1-value.json.json")) {
            String jsonSchemaAsString = IoUtil.read(in, StandardCharsets.UTF_8);
            Schema connectSchema = jsonSchemaData.toConnectSchema(new JsonSchema(jsonSchemaAsString));
            org.apache.avro.Schema avroSchema = avroData.fromConnectSchema(connectSchema);
            System.out.println(avroSchema.toString());

            Map<?, ?> map = Values.convertToMap(Schema.STRING_SCHEMA, "{\"regex\":\"(MA|MU)[\\\\s\\\\S]+\"}");
            System.out.println(map);
        }
    }
}
