package xyz.flink.serialization;

import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.SchemaProvider;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchemaProvider;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaMetadata;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig;
import io.confluent.kafka.schemaregistry.client.rest.RestService;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.kafka.schemaregistry.json.JsonSchemaProvider;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchemaProvider;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializerConfig;
import io.confluent.kafka.serializers.subject.strategy.SubjectNameStrategy;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import org.apache.kafka.common.config.SslConfigs;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.KEY_SUBJECT_NAME_STRATEGY;
import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.VALUE_SUBJECT_NAME_STRATEGY;
import static io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializerConfig.JSON_KEY_TYPE;
import static io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializerConfig.JSON_VALUE_TYPE;

/**
 * AbstractSchemaRegistrySchema
 *
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2024-06-21
 */
@SuperBuilder
public abstract class AbstractSchemaRegistrySchema<T, S extends ParsedSchema> implements Serializable, Closeable {
    private static final long serialVersionUID = -1671641202177852775L;

    protected static final byte MAGIC_BYTE = 0x0;
    protected static final int ID_SIZE = 4;

    /**
     * class of record
     */
    @Getter
    @NonNull
    private final Class<T> type;

    /**
     * subject of schema registry to produce
     */
    @Getter
    @NonNull
    private final String subject;

    /**
     * URL of schema registry to connect
     */
    @NonNull
    private final String schemaRegistryUrl;

    /**
     * map with additional schema configs (for example SSL properties)
     */
    @Getter
    @NonNull
    private Map<String, Serializable> configs;

    @Getter
    private final Map<String, String> registryHttpHeaders;

    @Getter
    @Builder.Default
    private int identityMapCapacity = 1000;

    @Getter
    private final boolean key;

    @Getter
    private transient S schema;

    @Getter
    private transient CachedSchemaRegistryClient schemaRegistryClient;

    protected CachedSchemaRegistryClient createSchemaRegistryClient() {
        Map<String, Object> restConfigs = this.configs.entrySet()
                .stream()
                .collect(Collectors.toMap(
                                e -> e.getKey().startsWith(SchemaRegistryClientConfig.CLIENT_NAMESPACE)
                                        ? e.getKey().substring(SchemaRegistryClientConfig.CLIENT_NAMESPACE.length())
                                        : e.getKey(),
                                Map.Entry::getValue,
                                (existing, replacement) -> replacement
                        )
                );
        RestService restService = new RestService(schemaRegistryUrl);
        restService.configure(restConfigs);
        Class<?> clazz = getClass();
        Type[] types = null;
        do {
            ParameterizedType pt = (ParameterizedType) clazz.getGenericSuperclass();
            clazz = (Class<?>) pt.getRawType();
            types = pt.getActualTypeArguments();
        } while (!AbstractSchemaRegistrySchema.class.equals(clazz));
        List<SchemaProvider> providers = new LinkedList<>();
        if (AvroSchema.class.equals(types[1])) {
            providers.add(new AvroSchemaProvider());
        } else if (JsonSchema.class.equals(types[1])) {
            providers.add(new JsonSchemaProvider());
        } else if (ProtobufSchema.class.equals(types[1])) {
            providers.add(new ProtobufSchemaProvider());
        } else {
            throw new IllegalArgumentException("Unsupported schema type: " + types[0]);
        }
        CachedSchemaRegistryClient client = new CachedSchemaRegistryClient(
                restService,
                identityMapCapacity,
                providers,
                configs,
                registryHttpHeaders
        );
        if (!restConfigs.containsKey(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG)) {
            restService.setSslSocketFactory(createSSLSocketFactory());
            restService.setHostnameVerifier(getHostnameVerifier(restConfigs));
        }
        return client;
    }

    private HostnameVerifier getHostnameVerifier(Map<String, Object> config) {
        String sslEndpointIdentificationAlgo =
                (String) config.get(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG);

        if (sslEndpointIdentificationAlgo == null
                || "none".equals(sslEndpointIdentificationAlgo)
                || sslEndpointIdentificationAlgo.isEmpty()) {
            return (hostname, session) -> true;
        }

        return null;
    }

    protected void checkInitialized() throws IOException {
        if (this.schemaRegistryClient != null) {
            return;
        }
        this.configs = new HashMap<>(configs);
        this.configs.putIfAbsent(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        this.configs.putIfAbsent(KafkaJsonSchemaDeserializerConfig.FAIL_UNKNOWN_PROPERTIES, false);
        this.configs.putIfAbsent(KafkaJsonSchemaDeserializerConfig.FAIL_INVALID_SCHEMA, true);
        if (this.key) {
            this.configs.putIfAbsent(KEY_SUBJECT_NAME_STRATEGY, IdentifySubjectNameStrategy.class.getName());
            this.configs.putIfAbsent(JSON_KEY_TYPE, this.type.getName());
        } else {
            this.configs.putIfAbsent(VALUE_SUBJECT_NAME_STRATEGY, IdentifySubjectNameStrategy.class.getName());
            this.configs.putIfAbsent(JSON_VALUE_TYPE, this.type.getName());
        }
        this.schemaRegistryClient = createSchemaRegistryClient();
        this.schema = createSchema();
    }

    private SSLSocketFactory createSSLSocketFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            TrustManager[] trustManagers = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
                            //  document why this method is empty
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
                            // document why this method is empty
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            };
            sslContext.init(null, trustManagers, new java.security.SecureRandom());
            return sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * 创建Schema
     *
     * @return
     */
    public S createSchema() throws IOException {
        return getSchema(this.subject);
    }

    /**
     * 创建Schema
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public <E extends ParsedSchema> E getSchema(String subject) throws IOException {
        try {
            if (this.schemaRegistryClient == null) {
                this.schemaRegistryClient = createSchemaRegistryClient();
            }
            SchemaMetadata metadata = this.schemaRegistryClient.getLatestSchemaMetadata(subject);
            return (E) this.schemaRegistryClient.parseSchema(
                            metadata.getSchemaType(),
                            metadata.getSchema(),
                            metadata.getReferences()
                    )
                    .orElseThrow(() -> new IllegalStateException("Failed to parse schema"));
        } catch (RestClientException e) {
            throw new IOException("get schema error,subject:" + subject, e);
        }
    }

    @Override
    public void close() throws IOException {
        if (this.schemaRegistryClient != null) {
            this.schemaRegistryClient.close();
        }
    }

    public static class IdentifySubjectNameStrategy implements SubjectNameStrategy {

        /**
         * For a given topic and message, returns the subject name under which the
         * schema should be registered in the schema registry.
         *
         * @param topic  The Kafka topic name to which the message is being published.
         * @param isKey  True when encoding a message key, false for a message value.
         * @param schema the schema of the record being serialized/deserialized
         * @return The subject name under which the schema should be registered.
         */
        @Override
        public String subjectName(String topic, boolean isKey, ParsedSchema schema) {
            return subjectName(topic, isKey);
        }

        /**
         * For a given topic and message, returns the subject name under which the
         * schema should be registered in the schema registry.
         *
         * @param topic The Kafka topic name to which the message is being published.
         * @param isKey True when encoding a message key, false for a message value.
         * @return The subject name under which the schema should be registered.
         */
        public String subjectName(String topic, boolean isKey) {
            return topic;
        }

        @Override
        public void configure(Map<String, ?> map) {
            // document why this method is empty
        }
    }
}
