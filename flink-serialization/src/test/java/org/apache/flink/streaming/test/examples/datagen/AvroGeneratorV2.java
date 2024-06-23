/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.streaming.test.examples.datagen;

import io.confluent.avro.random.generator.Generator;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.flink.api.common.accumulators.IntCounter;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.connector.source.SourceReaderContext;
import org.apache.flink.api.connector.source.util.ratelimit.RateLimiterStrategy;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.datagen.source.DataGeneratorSource;
import org.apache.flink.connector.datagen.source.GeneratorFunction;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.formats.avro.AvroFormatOptions;
import org.apache.flink.formats.avro.typeutils.GenericRecordAvroTypeInfo;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import xyz.flink.serialization.SchemaRegistryAvroSerializationSchema;

import java.io.Serializable;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS;
import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.LATEST_COMPATIBILITY_STRICT;
import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.USE_LATEST_VERSION;

/**
 * An example for generating data with a {@link DataGeneratorSource}.
 */
public class AvroGeneratorV2 {
    private static final Random random = new Random();

    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironment();
        env.setParallelism(1);
        long maxRecords = 4000;
        random.setSeed(maxRecords);
        String keySubject = "Test_Data-1-key.avro";
        String valueSubject = "Test_Data-1-value.avro";
        String schemaRegistryUrl = System.getenv("SCHEMA_REGISTRY_URL");
        String schemaRegistryUserInfo = System.getenv("SCHEMA_REGISTRY_USER_INFO");
        Map<String, Serializable> registryConfigs = Map.of(
                SchemaRegistryClientConfig.CLIENT_NAMESPACE + SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO",
                SchemaRegistryClientConfig.CLIENT_NAMESPACE + SchemaRegistryClientConfig.USER_INFO_CONFIG, schemaRegistryUserInfo,
                SchemaRegistryClientConfig.CLIENT_NAMESPACE + SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "",
                LATEST_COMPATIBILITY_STRICT, false,
                AUTO_REGISTER_SCHEMAS, false,
                USE_LATEST_VERSION, true
        );
        SchemaRegistryAvroSerializationSchema<GenericRecord> avroKeySchema =
                SchemaRegistryAvroSerializationSchema.<GenericRecord>builder()
                        .type(GenericRecord.class)
                        .schemaRegistryUrl(schemaRegistryUrl)
                        .subject(keySubject)
                        .registryConfigs(registryConfigs)
                        .encoding(AvroFormatOptions.AvroEncoding.JSON)
                        .schemaType(AvroSchema.TYPE)
                        .key(true)
                        .build();
        SchemaRegistryAvroSerializationSchema<GenericRecord> avroValueSchema =
                SchemaRegistryAvroSerializationSchema.<GenericRecord>builder()
                        .type(GenericRecord.class)
                        .schemaRegistryUrl(schemaRegistryUrl)
                        .subject(keySubject)
                        .registryConfigs(registryConfigs)
                        .encoding(AvroFormatOptions.AvroEncoding.JSON)
                        .schemaType(AvroSchema.TYPE)
                        .key(false)
                        .build();
        AvroSchema schema = (AvroSchema) avroValueSchema.getSchema();
        GenericRecordAvroTypeInfo avroTypeInfo = new GenericRecordAvroTypeInfo(schema.rawSchema());
        GeneratorFunction<Long, GenericRecord> generatorFunction = new AvroGeneratorFunction(maxRecords, schema.rawSchema());
        DataGeneratorSource<GenericRecord> generatorSource = new DataGeneratorSource<>(
                generatorFunction,
                maxRecords,
                RateLimiterStrategy.perSecond(4),
                avroTypeInfo
        );
        DataStreamSource<GenericRecord> streamSource = env.fromSource(
                generatorSource,
                WatermarkStrategy.noWatermarks(),
                "Data Generator"
        );
        KafkaSink<GenericRecord> kafkaSink = KafkaSink.<GenericRecord>builder()
                .setRecordSerializer(KafkaRecordSerializationSchema.<GenericRecord>builder()
                        .setTopic("vehicle-alarm-rule-l")
                        .setKeySerializationSchema(avroKeySchema)
                        .setValueSerializationSchema(avroValueSchema)
                        .build()
                )
                .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
                .setKafkaProducerConfig(properties())
                .build();
        streamSource.process(new ProcessFunction<GenericRecord, GenericRecord>() {
            private final IntCounter counter = new IntCounter();

            @Override
            public void open(Configuration parameters) {
                getRuntimeContext().addAccumulator("total", this.counter);
            }

            @Override
            public void processElement(GenericRecord value,
                                       ProcessFunction<GenericRecord, GenericRecord>.Context ctx,
                                       Collector<GenericRecord> out) throws Exception {
                counter.add(1);
                int total = counter.getLocalValuePrimitive();
                if (total % 100 == 0) {
                    value.put("vehicleModelCode", "MU");
                    Object o = value.get("dataList");
                    System.out.println(o);
                }
            }
        });

        streamSource.sinkTo(kafkaSink);
        env.execute("Data Generator Source Example");
    }

    static class AvroGeneratorFunction implements GeneratorFunction<Long, GenericRecord> {

        private transient Generator generator;
        private Random random;
        private final long maxRecords;
        private final Schema schema;

        public AvroGeneratorFunction(long maxRecords, Schema schema) {
            this.maxRecords = maxRecords;
            this.schema = schema;
            this.random = new Random();
            this.generator = new Generator.Builder()
                    .random(random)
                    .generation(maxRecords)
                    .schema(schema)
                    .build();
        }

        @Override
        public void open(SourceReaderContext ctx) throws Exception {
            GeneratorFunction.super.open(ctx);
            Configuration config = ctx.getConfiguration();
            Map<String, String> map = config.toMap();
            System.out.println(config);
        }

        @Override
        public GenericRecord map(Long offset) throws Exception {
            tryInit();
            random.setSeed(random.nextLong());
            GenericRecord generate = (GenericRecord) generator.generate();
            generate.put("offset", offset);
            return generate;
        }

        private void tryInit() {
            if (this.random == null) {
                this.random = new Random();
            }
            if (generator == null) {
                this.generator = new Generator.Builder()
                        .random(random)
                        .generation(maxRecords)
                        .schema(schema)
                        .build();
            }
        }
    }

    private static Properties properties() {
        Properties props = new Properties();
        props.put("bootstrap.servers", System.getenv("KAFKA_BOOTSTRAP_SERVERS"));
        props.put("acks", "all");
        props.put("retries", 0);
        props.put("batch.size", 16384);
        props.put("sasl.mechanism", "PLAIN");
        props.put("security.protocol", "PLAINTEXT");
        props.put("key.serializer", ByteArraySerializer.class.getName());
        props.put("value.serializer", ByteArraySerializer.class.getName());
        return props;
    }
}
